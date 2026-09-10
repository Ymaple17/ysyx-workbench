#!/usr/bin/env bash
set -euo pipefail
: "${NEMU_HOME:?}" "${NPC_HOME:?}"
DIFF="${NEMU_HOME}/build/riscv32-nemu-interpreter-so"
NPC="${NPC_HOME}/obj_dir/Vysyx_25020039"
AMK="${AMK:-${NPC_HOME}/../am-kernels}"
AM_HOME="${AM_HOME:-${NPC_HOME}/../abstract-machine}"
FAIL=0

run_mill_case() {
  local name="$1"
  echo "-- ${name}"
  if ( cd "${NPC_HOME}/oood_chisel_4_issue_vsrc" && ./mill -i mychisel.test -z "${name}" ); then
    echo "PASS ${name}"
  else
    echo "FAIL ${name}"
    FAIL=1
  fi
}

echo "== unit tests =="
unit_tests=(
  "keep phys0 as x0"
  "write and read back"
  "dual write ports"
  "reset ready"
  "set then clear"
  "prefer clear over set same cycle"
  "clr_mask clear multiple"
  "not allocate when reg_write=0"
  "allocate and update RAT"
  "commit update arch_rat"
  "rollback restore mapping"
  "fill 16 entries"
  "issue wb commit pipeline"
  "flush after flush_idx"
  "issue oldest ready only"
  "issue younger when oldest not ready"
  "free_rob by idx not issue slot"
  "prefer oldest among multiple ready"
  "not issue load past older pending store"
  "flush clear all"
  "selective flush keep older"
  "enq then deq in order"
  "fill until full then block enq"
  "flush clear all (not just mask valid)"
)
for name in "${unit_tests[@]}"; do
  run_mill_case "$name"
done

echo "== cpu-tests =="
for b in "${AMK}/tests/cpu-tests/build/"*-riscv32e-npc.bin; do
  name=$(basename "$b" -riscv32e-npc.bin)
  if timeout 30 "$NPC" "$b" -b --diff="$DIFF" 2>&1 | grep -qE "HIT GOOD TRAP|HIT GOOD"; then
    echo "PASS $name"
  else
    echo "FAIL $name"
    FAIL=1
  fi
done

echo "== microbench(test) =="
make -C "${AMK}/benchmarks/microbench" AM_HOME="${AM_HOME}" ARCH=riscv32e-npc mainargs=test insert-arg
if timeout 900 "$NPC" "${AMK}/benchmarks/microbench/build/microbench-riscv32e-npc.bin" -b --diff="$DIFF" 2>&1 | tee /tmp/microbench_regress.log | grep -q "MicroBench PASS"; then
  echo "PASS microbench(test)"
else
  echo "FAIL microbench(test)"
  FAIL=1
fi

if [ "${RUN_YIELD_OS:-0}" = "1" ]; then
  echo "== yield-os smoke (60s, expect no DIFFTEST/HANG) =="
  set +e
  timeout 60 "$NPC" "${AMK}/kernels/yield-os/build/yield-os-riscv32e-npc.bin" -b --diff="$DIFF" >/tmp/yield_regress.log 2>&1
  yc=$?
  set -e
  if grep -aEq "DIFFTEST|HANG|ABORT" /tmp/yield_regress.log; then
    echo "FAIL yield-os smoke"
    FAIL=1
  else
    echo "PASS yield-os smoke (timeout/exit=$yc)"
  fi
fi

exit "$FAIL"
