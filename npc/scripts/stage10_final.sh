#!/usr/bin/env bash
set -euo pipefail

NPC_HOME="${NPC_HOME:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"
NEMU_HOME="${NEMU_HOME:-${NPC_HOME}/../nemu}"
AM_HOME="${AM_HOME:-${NPC_HOME}/../abstract-machine}"
AMK="${AMK:-${NPC_HOME}/../am-kernels}"
STAMP="$(date +%Y%m%d_%H%M%S)"
OUT_ROOT="${NPC_HOME}/build/stage9_regress/stage10_final_${STAMP}"
RUN1="${OUT_ROOT}/run1_full"
RUN2="${OUT_ROOT}/run2_repeat"

export NPC_HOME NEMU_HOME AM_HOME AMK
mkdir -p "${RUN1}" "${RUN2}"

"${NPC_HOME}/scripts/stage9_regress.sh" \
  --mode full \
  --tag stage10_final \
  --out-dir "${RUN1}" \
  --cpu-timeout 900 \
  --micro-timeout 1200

MICRO_BIN="${AMK}/benchmarks/microbench/build/microbench-riscv32e-npc.bin"
SIM="${NPC_HOME}/obj_dir/Vysyx_25020039"
timeout 1200 "${SIM}" "${MICRO_BIN}" >"${RUN2}/microbench.log" 2>&1
"${NPC_HOME}/scripts/stage9_regress.sh" \
  --mode parse \
  --tag stage10_final_repeat \
  --out-dir "${RUN2}" \
  --parse-log "${RUN2}/microbench.log"

metric() {
  local log="$1"
  local key="$2"
  awk -F: -v key="${key}" '$1 == key { gsub(/^[ \t]+/, "", $2); split($2, a, /[ \t]+/); gsub(/%/, "", a[1]); print a[1]; exit }' "${log}"
}

check_run() {
  local log="$1"
  local label="$2"
  local ipc stale alu1 dual refill
  ipc="$(metric "${log}" "IPC")"
  stale="$(metric "${log}" "FTQ Stale Recover")"
  alu1="$(metric "${log}" "ALU1 Issues")"
  dual="$(metric "${log}" "Dual ALU Issues")"
  refill="$(metric "${log}" "FU Same-Cycle Refill")"

  awk -v ipc="${ipc}" 'BEGIN { exit !(ipc + 0 >= 0.6) }'
  [[ "${stale}" == "0" ]]
  [[ "${alu1}" =~ ^[0-9]+$ && "${alu1}" -gt 0 ]]
  [[ "${dual}" =~ ^[0-9]+$ && "${dual}" -gt 0 ]]
  [[ "${refill}" =~ ^[0-9]+$ && "${refill}" -gt 0 ]]
  printf '[accept] %s ipc=%s ftq_stale=%s alu1=%s dual_alu=%s fu_refill=%s\n' \
    "${label}" "${ipc}" "${stale}" "${alu1}" "${dual}" "${refill}"
}

check_run "${RUN1}/microbench.log" run1
check_run "${RUN2}/microbench.log" run2

IPC1="$(metric "${RUN1}/microbench.log" "IPC")"
IPC2="$(metric "${RUN2}/microbench.log" "IPC")"
awk -v a="${IPC1}" -v b="${IPC2}" 'BEGIN { d = a - b; if (d < 0) d = -d; exit !(d <= 0.005) }'

printf '[done] stage10 final validation: %s\n' "${OUT_ROOT}"
