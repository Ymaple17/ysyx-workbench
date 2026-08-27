#!/usr/bin/env bash
set -euo pipefail

NPC_HOME="${NPC_HOME:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"
NEMU_HOME="${NEMU_HOME:-${NPC_HOME}/../nemu}"
AM_HOME="${AM_HOME:-${NPC_HOME}/../abstract-machine}"
AMK="${AMK:-${NPC_HOME}/../am-kernels}"
STAMP="$(date +%Y%m%d_%H%M%S)"
OUT_ROOT="${NPC_HOME}/build/stage9_regress/stage11_final_${STAMP}"
RUN1="${OUT_ROOT}/run1_full"
RUN2="${OUT_ROOT}/run2_repeat"

export NPC_HOME NEMU_HOME AM_HOME AMK
mkdir -p "${RUN1}" "${RUN2}"

"${NPC_HOME}/scripts/stage9_regress.sh" \
  --mode full \
  --tag stage11_final \
  --out-dir "${RUN1}" \
  --cpu-timeout 900 \
  --micro-timeout 1200

MICRO_BIN="${AMK}/benchmarks/microbench/build/microbench-riscv32e-npc.bin"
SIM="${NPC_HOME}/obj_dir/Vysyx_25020039"
timeout 1200 "${SIM}" "${MICRO_BIN}" >"${RUN2}/microbench.log" 2>&1
"${NPC_HOME}/scripts/stage9_regress.sh" \
  --mode parse \
  --tag stage11_final_repeat \
  --out-dir "${RUN2}" \
  --parse-log "${RUN2}/microbench.log"

metric() {
  local log="$1"
  local key="$2"
  awk -F: -v key="${key}" '$1 == key { gsub(/^[ \t]+/, "", $2); split($2, a, /[ \t]+/); gsub(/%/, "", a[1]); print a[1]; exit }' "${log}"
}

require_integer_gt_zero() {
  local value="$1"
  local name="$2"
  [[ "${value}" =~ ^[0-9]+$ && "${value}" -gt 0 ]] || {
    echo "[fail] ${name} must be a positive integer, got '${value}'" >&2
    exit 1
  }
}

check_run() {
  local log="$1"
  local label="$2"
  local ipc stale_ftq mem_violation hit_rate alu1 dual refill commit1
  local direct secondary merge write_bursts write_beats

  ipc="$(metric "${log}" "IPC")"
  stale_ftq="$(metric "${log}" "FTQ Stale Recover")"
  mem_violation="$(metric "${log}" "Mem Order Violation")"
  hit_rate="$(metric "${log}" "DCache Hit Rate")"
  alu1="$(metric "${log}" "ALU1 Issues")"
  dual="$(metric "${log}" "Dual ALU Issues")"
  refill="$(metric "${log}" "FU Same-Cycle Refill")"
  commit1="$(metric "${log}" "Commit Slot1")"
  direct="$(metric "${log}" "Control Direct Complete")"
  secondary="$(metric "${log}" "DCache Secondary Alloc")"
  merge="$(metric "${log}" "DCache MSHR Merge")"
  write_bursts="$(metric "${log}" "StoreBuffer Write Bursts")"
  write_beats="$(metric "${log}" "StoreBuffer Write Beats")"

  awk -v ipc="${ipc}" 'BEGIN { exit !(ipc + 0 >= 1.0) }' || {
    echo "[fail] ${label} IPC ${ipc} is below 1.0" >&2
    exit 1
  }
  awk -v rate="${hit_rate}" 'BEGIN { exit !(rate + 0 >= 97.0) }' || {
    echo "[fail] ${label} DCache hit rate ${hit_rate}% is below 97.0%" >&2
    exit 1
  }
  [[ "${stale_ftq}" == "0" ]] || {
    echo "[fail] ${label} FTQ stale recover=${stale_ftq}" >&2
    exit 1
  }
  [[ "${mem_violation}" == "0" ]] || {
    echo "[fail] ${label} memory-order violations=${mem_violation}" >&2
    exit 1
  }

  require_integer_gt_zero "${alu1}" "${label} ALU1 issues"
  require_integer_gt_zero "${dual}" "${label} dual ALU issues"
  require_integer_gt_zero "${refill}" "${label} FU same-cycle refill"
  require_integer_gt_zero "${commit1}" "${label} commit slot1"
  require_integer_gt_zero "${direct}" "${label} direct control completion"
  require_integer_gt_zero "${secondary}" "${label} DCache secondary allocation"
  require_integer_gt_zero "${merge}" "${label} DCache MSHR merge"
  require_integer_gt_zero "${write_bursts}" "${label} StoreBuffer write bursts"
  require_integer_gt_zero "${write_beats}" "${label} StoreBuffer write beats"
  [[ "${write_beats}" -gt "${write_bursts}" ]] || {
    echo "[fail] ${label} did not observe multi-beat store bursts" >&2
    exit 1
  }

  printf '[accept] %s ipc=%s dcache_hit=%s%% ftq_stale=%s mem_violation=%s commit1=%s secondary=%s merge=%s\n' \
    "${label}" "${ipc}" "${hit_rate}" "${stale_ftq}" "${mem_violation}" "${commit1}" "${secondary}" "${merge}"
}

check_run "${RUN1}/microbench.log" run1
check_run "${RUN2}/microbench.log" run2

IPC1="$(metric "${RUN1}/microbench.log" "IPC")"
IPC2="$(metric "${RUN2}/microbench.log" "IPC")"
awk -v a="${IPC1}" -v b="${IPC2}" 'BEGIN { d = a - b; if (d < 0) d = -d; exit !(d <= 0.005) }' || {
  echo "[fail] IPC repeatability exceeded 0.005: run1=${IPC1}, run2=${IPC2}" >&2
  exit 1
}

printf '[done] stage11 final validation: %s\n' "${OUT_ROOT}"
