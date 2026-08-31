#!/usr/bin/env python3
"""Finite-table branch-context oracle for the Stage15 MicroBench trace.

The input is the committed-PC stream emitted by NPC_COMMIT_TRACE and the
flat RV32 binary loaded at 0x80000000. Predictors are updated online in
program order; no future outcome is used to make a prediction.
"""

from __future__ import annotations

import argparse
import json
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Callable, Iterable, Optional


BASE_PC = 0x80000000
GHR_BITS = 32
LOCAL_BITS = 12
PATH_BITS = 32
CYCLE_PER_CORRECTED_MISS = 2402.0 / 900.0

# Measured at the source-reproducible Stage15 IPC 2.3005 point. These maps
# are used only to estimate incremental opportunity; predictor identity and
# lookup never special-case a PC.
MEASURED_DIRECTION_MISSES = {
    0x80002710: 829,
    0x800026D4: 672,
    0x80003848: 220,
    0x80001850: 130,
    0x800026B8: 94,
    0x80005E6C: 80,
    0x80003868: 75,
    0x80005C68: 66,
    0x800018B0: 58,
    0x800021E4: 53,
    0x80002E58: 53,
    0x800023A0: 47,
}

MEASURED_TARGET_MISSES = {
    0x80001730: 805,
    0x80001630: 128,
    0x80002B24: 10,
    0x80002AAC: 9,
    0x80002ADC: 9,
    0x80005EB8: 3,
    0x80005F5C: 3,
    0x8000291C: 2,
    0x80005558: 2,
    0x80005934: 2,
    0x80005B04: 2,
    0x80005C88: 2,
}


def mix64(value: int) -> int:
    value &= (1 << 64) - 1
    value ^= value >> 30
    value = (value * 0xBF58476D1CE4E5B9) & ((1 << 64) - 1)
    value ^= value >> 27
    value = (value * 0x94D049BB133111EB) & ((1 << 64) - 1)
    return value ^ (value >> 31)


def rotl32(value: int, amount: int) -> int:
    amount &= 31
    value &= 0xFFFFFFFF
    return ((value << amount) | (value >> ((32 - amount) & 31))) & 0xFFFFFFFF


def path_step(history: int, target: int) -> int:
    target_hash = ((target >> 2) & 0xFFFF) ^ ((target >> 16) & 0xFFFF)
    return ((history << 8) | (target_hash & 0xFF)) & 0xFFFFFFFF


def load_pcs(path: Path) -> list[int]:
    pcs = []
    with path.open("r", encoding="ascii") as stream:
        for line_no, raw in enumerate(stream, 1):
            text = raw.strip()
            if not text:
                continue
            try:
                pcs.append(int(text, 16))
            except ValueError as exc:
                raise ValueError(f"{path}:{line_no}: invalid PC {text!r}") from exc
    return pcs


def read_inst(image: bytes, pc: int) -> Optional[int]:
    offset = pc - BASE_PC
    if offset < 0 or offset + 4 > len(image):
        return None
    return int.from_bytes(image[offset : offset + 4], "little")


@dataclass(frozen=True)
class Event:
    ordinal: int
    pc: int
    inst: int
    kind: str
    taken: bool
    target: int
    ghr: int
    local: int
    path: int
    loop_phase: int


def build_events(pcs: list[int], image: bytes) -> list[Event]:
    events: list[Event] = []
    ghr = 0
    path = 0
    local_history: dict[int, int] = defaultdict(int)
    loop_phase: dict[int, int] = defaultdict(int)

    for ordinal, (pc, next_pc) in enumerate(zip(pcs, pcs[1:])):
        inst = read_inst(image, pc)
        if inst is None:
            continue
        opcode = inst & 0x7F
        if opcode == 0x63:
            taken = next_pc != ((pc + 4) & 0xFFFFFFFF)
            events.append(
                Event(
                    ordinal,
                    pc,
                    inst,
                    "branch",
                    taken,
                    next_pc,
                    ghr,
                    local_history[pc],
                    path,
                    loop_phase[pc],
                )
            )
            ghr = ((ghr << 1) | int(taken)) & ((1 << GHR_BITS) - 1)
            local_history[pc] = (
                (local_history[pc] << 1) | int(taken)
            ) & ((1 << LOCAL_BITS) - 1)
            if taken and next_pc < pc:
                loop_phase[pc] = min(loop_phase[pc] + 1, 1023)
            else:
                loop_phase[pc] = 0
        elif opcode == 0x67:
            rs1 = (inst >> 15) & 0x1F
            is_return = rs1 == 1
            events.append(
                Event(
                    ordinal,
                    pc,
                    inst,
                    "return" if is_return else "jalr",
                    True,
                    next_pc,
                    ghr,
                    local_history[pc],
                    path,
                    loop_phase[pc],
                )
            )
            if not is_return:
                path = path_step(path, next_pc)
    return events


@dataclass
class DirectionEntry:
    valid: bool = False
    tag: int = 0
    counter: int = 3
    confidence: int = 0
    last_use: int = 0


@dataclass
class TargetEntry:
    valid: bool = False
    tag: int = 0
    target: int = 0
    confidence: int = 0
    last_use: int = 0


class DirectionPredictor:
    def __init__(
        self,
        entries: int,
        ways: int,
        context: Callable[[Event], int],
        confidence_threshold: int = 2,
    ) -> None:
        if entries <= 0 or ways <= 0 or entries % ways:
            raise ValueError("entries must be a positive multiple of ways")
        self.sets = entries // ways
        self.ways = ways
        self.context = context
        self.threshold = confidence_threshold
        self.table = [DirectionEntry() for _ in range(entries)]
        self.tick = 0

    def _identity(self, event: Event) -> tuple[int, int]:
        signature = self.context(event) & 0xFFFFFFFFFFFFFFFF
        hashed = mix64((event.pc >> 2) ^ mix64(signature))
        set_index = hashed % self.sets
        tag = mix64((event.pc << 32) ^ signature) & 0xFFFF
        return set_index, tag

    def _set(self, set_index: int) -> list[DirectionEntry]:
        start = set_index * self.ways
        return self.table[start : start + self.ways]

    def access(self, event: Event) -> Optional[bool]:
        self.tick += 1
        set_index, tag = self._identity(event)
        ways = self._set(set_index)
        hit = next((entry for entry in ways if entry.valid and entry.tag == tag), None)
        prediction = None
        if hit is not None:
            prediction = hit.counter >= 4 if hit.confidence >= self.threshold else None
            raw_prediction = hit.counter >= 4
            hit.counter = min(7, hit.counter + 1) if event.taken else max(0, hit.counter - 1)
            if raw_prediction == event.taken:
                hit.confidence = min(3, hit.confidence + 1)
            else:
                hit.confidence = max(0, hit.confidence - 1)
            hit.last_use = self.tick
            return prediction

        victim = min(
            ways,
            key=lambda entry: (
                entry.valid,
                entry.confidence,
                entry.last_use,
            ),
        )
        victim.valid = True
        victim.tag = tag
        victim.counter = 4 if event.taken else 3
        victim.confidence = 0
        victim.last_use = self.tick
        return None


class TargetPredictor:
    def __init__(
        self,
        entries: int,
        ways: int,
        context: Callable[[Event], int],
        confidence_threshold: int = 1,
    ) -> None:
        if entries <= 0 or ways <= 0 or entries % ways:
            raise ValueError("entries must be a positive multiple of ways")
        self.sets = entries // ways
        self.ways = ways
        self.context = context
        self.threshold = confidence_threshold
        self.table = [TargetEntry() for _ in range(entries)]
        self.tick = 0

    def _identity(self, event: Event) -> tuple[int, int]:
        signature = self.context(event) & 0xFFFFFFFFFFFFFFFF
        hashed = mix64((event.pc >> 2) ^ mix64(signature))
        set_index = hashed % self.sets
        tag = mix64((event.pc << 32) ^ signature) & 0xFFFF
        return set_index, tag

    def access(self, event: Event) -> Optional[int]:
        self.tick += 1
        set_index, tag = self._identity(event)
        start = set_index * self.ways
        ways = self.table[start : start + self.ways]
        hit = next((entry for entry in ways if entry.valid and entry.tag == tag), None)
        if hit is not None:
            prediction = hit.target if hit.confidence >= self.threshold else None
            if hit.target == event.target:
                hit.confidence = min(3, hit.confidence + 1)
            else:
                hit.target = event.target
                hit.confidence = 0
            hit.last_use = self.tick
            return prediction

        victim = min(
            ways,
            key=lambda entry: (
                entry.valid,
                entry.confidence,
                entry.last_use,
            ),
        )
        victim.valid = True
        victim.tag = tag
        victim.target = event.target
        victim.confidence = 0
        victim.last_use = self.tick
        return None


def context_functions() -> dict[str, Callable[[Event], int]]:
    return {
        "local": lambda event: event.local,
        "path": lambda event: event.path,
        "ghr_local": lambda event: event.ghr ^ rotl32(event.local, 11),
        "local_path": lambda event: event.local ^ rotl32(event.path, 13),
        "ghr_path": lambda event: event.ghr ^ rotl32(event.path, 7),
        "local_path_phase": lambda event: (
            event.local
            ^ rotl32(event.path, 13)
            ^ rotl32(event.loop_phase, 23)
        ),
        "full": lambda event: (
            event.ghr
            ^ rotl32(event.local, 11)
            ^ rotl32(event.path, 19)
            ^ rotl32(event.loop_phase, 27)
        ),
    }


def estimate_incremental(
    total_by_pc: Counter[int],
    predicted_by_pc: Counter[int],
    wrong_by_pc: Counter[int],
    measured_misses: dict[int, int],
) -> dict[str, float]:
    estimated_corrected = 0.0
    represented_misses = 0
    for pc, current_misses in measured_misses.items():
        total = total_by_pc[pc]
        if total == 0:
            continue
        coverage = predicted_by_pc[pc] / total
        represented_misses += current_misses
        estimated_corrected += max(
            0.0,
            current_misses * coverage - wrong_by_pc[pc],
        )
    return {
        "represented_current_misses": represented_misses,
        "estimated_net_corrected_misses": round(estimated_corrected, 2),
        "estimated_cycles": round(estimated_corrected * CYCLE_PER_CORRECTED_MISS, 1),
    }


def evaluate_direction(
    events: Iterable[Event],
    name: str,
    predictor: DirectionPredictor,
) -> dict[str, object]:
    total = 0
    predicted = 0
    wrong = 0
    total_by_pc: Counter[int] = Counter()
    predicted_by_pc: Counter[int] = Counter()
    wrong_by_pc: Counter[int] = Counter()

    for event in events:
        if event.kind != "branch":
            continue
        total += 1
        total_by_pc[event.pc] += 1
        result = predictor.access(event)
        if result is None:
            continue
        predicted += 1
        predicted_by_pc[event.pc] += 1
        if result != event.taken:
            wrong += 1
            wrong_by_pc[event.pc] += 1

    estimate = estimate_incremental(
        total_by_pc,
        predicted_by_pc,
        wrong_by_pc,
        MEASURED_DIRECTION_MISSES,
    )
    hot = []
    for pc, current_misses in sorted(
        MEASURED_DIRECTION_MISSES.items(), key=lambda item: item[1], reverse=True
    ):
        count = total_by_pc[pc]
        hot.append(
            {
                "pc": f"0x{pc:08x}",
                "dynamic": count,
                "current_misses": current_misses,
                "coverage": round(predicted_by_pc[pc] / count, 4) if count else 0.0,
                "helper_wrong": wrong_by_pc[pc],
            }
        )
    return {
        "name": name,
        "total": total,
        "predictions": predicted,
        "coverage": round(predicted / total, 6) if total else 0.0,
        "wrong": wrong,
        "accuracy": round((predicted - wrong) / predicted, 6) if predicted else 0.0,
        **estimate,
        "hotspots": hot,
    }


def evaluate_target(
    events: Iterable[Event],
    name: str,
    predictor: TargetPredictor,
) -> dict[str, object]:
    total = 0
    predicted = 0
    wrong = 0
    total_by_pc: Counter[int] = Counter()
    predicted_by_pc: Counter[int] = Counter()
    wrong_by_pc: Counter[int] = Counter()

    for event in events:
        if event.kind != "jalr":
            continue
        total += 1
        total_by_pc[event.pc] += 1
        result = predictor.access(event)
        if result is None:
            continue
        predicted += 1
        predicted_by_pc[event.pc] += 1
        if result != event.target:
            wrong += 1
            wrong_by_pc[event.pc] += 1

    estimate = estimate_incremental(
        total_by_pc,
        predicted_by_pc,
        wrong_by_pc,
        MEASURED_TARGET_MISSES,
    )
    hot = []
    for pc, current_misses in sorted(
        MEASURED_TARGET_MISSES.items(), key=lambda item: item[1], reverse=True
    ):
        count = total_by_pc[pc]
        hot.append(
            {
                "pc": f"0x{pc:08x}",
                "dynamic": count,
                "current_misses": current_misses,
                "coverage": round(predicted_by_pc[pc] / count, 4) if count else 0.0,
                "helper_wrong": wrong_by_pc[pc],
            }
        )
    return {
        "name": name,
        "total": total,
        "predictions": predicted,
        "coverage": round(predicted / total, 6) if total else 0.0,
        "wrong": wrong,
        "accuracy": round((predicted - wrong) / predicted, 6) if predicted else 0.0,
        **estimate,
        "hotspots": hot,
    }


def markdown_report(report: dict[str, object]) -> str:
    lines = [
        "# Stage15 Branch Oracle",
        "",
        f"- committed PCs: `{report['committed_pcs']}`",
        f"- conditional branches: `{report['conditional_branches']}`",
        f"- non-return JALR: `{report['jalr']}`",
        f"- measured cycle weight: `{CYCLE_PER_CORRECTED_MISS:.4f}` cycles/corrected miss",
        "",
        "The incremental-cycle columns are conservative hotspot estimates, not exact",
        "whole-core predictions. RTL still requires same-binary A/B.",
        "",
        "## Direction",
        "",
        "| model | coverage | accuracy | wrong | est. corrected misses | est. cycles |",
        "|---|---:|---:|---:|---:|---:|",
    ]
    for item in report["direction"]:
        lines.append(
            f"| {item['name']} | {item['coverage']:.2%} | {item['accuracy']:.2%} | "
            f"{item['wrong']} | {item['estimated_net_corrected_misses']} | "
            f"{item['estimated_cycles']} |"
        )
    lines.extend(
        [
            "",
            "## Indirect Target",
            "",
            "| model | coverage | accuracy | wrong | est. corrected misses | est. cycles |",
            "|---|---:|---:|---:|---:|---:|",
        ]
    )
    for item in report["target"]:
        lines.append(
            f"| {item['name']} | {item['coverage']:.2%} | {item['accuracy']:.2%} | "
            f"{item['wrong']} | {item['estimated_net_corrected_misses']} | "
            f"{item['estimated_cycles']} |"
        )
    lines.append("")
    return "\n".join(lines)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--trace", type=Path, required=True)
    parser.add_argument("--bin", dest="image", type=Path, required=True)
    parser.add_argument("--json", dest="json_path", type=Path)
    parser.add_argument("--markdown", dest="markdown_path", type=Path)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    pcs = load_pcs(args.trace)
    image = args.image.read_bytes()
    events = build_events(pcs, image)
    contexts = context_functions()

    direction_models = []
    for context_name in ("local", "ghr_local", "local_path", "local_path_phase", "full"):
        direction_models.append(
            evaluate_direction(
                events,
                f"dir-256x2-{context_name}",
                DirectionPredictor(256, 2, contexts[context_name]),
            )
        )

    target_models = []
    for context_name in ("path", "ghr_path", "local_path", "full"):
        target_models.append(
            evaluate_target(
                events,
                f"target-256x2-{context_name}",
                TargetPredictor(256, 2, contexts[context_name]),
            )
        )

    report: dict[str, object] = {
        "committed_pcs": len(pcs),
        "conditional_branches": sum(event.kind == "branch" for event in events),
        "jalr": sum(event.kind == "jalr" for event in events),
        "returns": sum(event.kind == "return" for event in events),
        "direction": direction_models,
        "target": target_models,
    }

    rendered = markdown_report(report)
    print(rendered, end="")
    if args.json_path is not None:
        args.json_path.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    if args.markdown_path is not None:
        args.markdown_path.write_text(rendered, encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
