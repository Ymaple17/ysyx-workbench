#!/usr/bin/env bash
set -euo pipefail

NPC_HOME="${NPC_HOME:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"
NEMU_HOME="${NEMU_HOME:-${NPC_HOME}/../nemu}"
AM_HOME="${AM_HOME:-${NPC_HOME}/../abstract-machine}"
AMK="${AMK:-${NPC_HOME}/../am-kernels}"
STAMP="$(date +%Y%m%d_%H%M%S)"
OUT_ROOT="${STAGE13_REUSE_ROOT:-${NPC_HOME}/build/stage9_regress/stage13_final_${STAMP}}"
RUN1="${OUT_ROOT}/run1_full"
RUN2="${OUT_ROOT}/run2_repeat"

export NPC_HOME NEMU_HOME AM_HOME AMK
mkdir -p "${RUN1}" "${RUN2}"

if [[ -z "${STAGE13_REUSE_ROOT:-}" ]]; then
  "${NPC_HOME}/scripts/stage9_regress.sh" \
    --mode full \
    --tag stage13_final \
    --out-dir "${RUN1}" \
    --cpu-timeout 900 \
    --micro-timeout 1200

  MICRO_BIN="${AMK}/benchmarks/microbench/build/microbench-riscv32e-npc.bin"
  SIM="${NPC_HOME}/obj_dir/Vysyx_25020039"
  timeout 1200 "${SIM}" "${MICRO_BIN}" >"${RUN2}/microbench.log" 2>&1
  "${NPC_HOME}/scripts/stage9_regress.sh" \
    --mode parse \
    --tag stage13_final_repeat \
    --out-dir "${RUN2}" \
    --parse-log "${RUN2}/microbench.log"
else
  [[ -f "${RUN1}/microbench.log" && -f "${RUN2}/microbench.log" ]] || {
    echo "[fail] STAGE13_REUSE_ROOT does not contain run1_full/run2_repeat logs: ${OUT_ROOT}" >&2
    exit 1
  }
  echo "[reuse] checking existing Stage13 logs under ${OUT_ROOT}"
fi

metric() {
  local log="$1"
  local key="$2"
  awk -F: -v key="${key}" '$1 == key { gsub(/^[ \t]+/, "", $2); split($2, a, /[ \t]+/); gsub(/%/, "", a[1]); print a[1]; exit }' "${log}"
}

require_positive_integer() {
  local value="$1"
  local name="$2"
  [[ "${value}" =~ ^[0-9]+$ && "${value}" -gt 0 ]] || {
    echo "[fail] ${name} must be a positive integer, got '${value}'" >&2
    exit 1
  }
}

require_max_integer() {
  local value="$1"
  local maximum="$2"
  local name="$3"
  [[ "${value}" =~ ^[0-9]+$ && "${value}" -le "${maximum}" ]] || {
    echo "[fail] ${name}=${value}, expected <=${maximum}" >&2
    exit 1
  }
}

check_run() {
  local log="$1"
  local label="$2"
  local ipc cycles stale_ftq mem_violation fetch_width commit1_util
  local target_miss unpred_jalr fq_full head_wait head_wait_alu hit_rate
  local branch_slot1 store_direct lsu_refill itage_hit loop_hit
  local cdb_conflicts fresh_issue

  ipc="$(metric "${log}" "IPC")"
  cycles="$(metric "${log}" "Total Cycles")"
  stale_ftq="$(metric "${log}" "FTQ Stale Recover")"
  mem_violation="$(metric "${log}" "Mem Order Violation")"
  fetch_width="$(metric "${log}" "Average Fetch Width")"
  commit1_util="$(metric "${log}" "Commit Slot1 Util")"
  target_miss="$(metric "${log}" "Target Miss")"
  unpred_jalr="$(metric "${log}" "Unpredicted JALR")"
  fq_full="$(metric "${log}" "FQ Full")"
  head_wait="$(metric "${log}" "Head Not Ready")"
  head_wait_alu="$(metric "${log}" "Head Wait ALU")"
  hit_rate="$(metric "${log}" "DCache Hit Rate")"
  branch_slot1="$(metric "${log}" "Dispatch Branch+Slot1")"
  store_direct="$(metric "${log}" "Store Direct Complete")"
  lsu_refill="$(metric "${log}" "LSU Address Refill")"
  itage_hit="$(metric "${log}" "ITAGE Hits")"
  loop_hit="$(metric "${log}" "Loop Predictor Hits")"
  cdb_conflicts="$(metric "${log}" "CDB Conflicts")"
  fresh_issue="$(metric "${log}" "RS Fresh Issue")"

  awk -v value="${ipc}" 'BEGIN { exit !(value + 0 >= 1.50) }' || {
    echo "[fail] ${label} IPC ${ipc} is below 1.50" >&2
    exit 1
  }
  awk -v value="${fetch_width}" 'BEGIN { exit !(value + 0 >= 1.82) }' || {
    echo "[fail] ${label} average fetch width ${fetch_width} is below 1.82" >&2
    exit 1
  }
  awk -v value="${commit1_util}" 'BEGIN { exit !(value + 0 >= 75.0) }' || {
    echo "[fail] ${label} commit-slot1 utilization ${commit1_util}% is below 75%" >&2
    exit 1
  }
  awk -v value="${hit_rate}" 'BEGIN { exit !(value + 0 >= 97.0) }' || {
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

  require_max_integer "${target_miss}" 600 "${label} target misses"
  require_max_integer "${unpred_jalr}" 1000 "${label} unpredicted JALR"
  require_max_integer "${fq_full}" 3000 "${label} FQ full cycles"
  require_max_integer "${head_wait}" 16000 "${label} head-not-ready cycles"
  require_max_integer "${head_wait_alu}" 100 "${label} ALU head-wait cycles"
  require_max_integer "${cdb_conflicts}" 500 "${label} CDB conflicts"
  require_positive_integer "${fresh_issue}" "${label} fresh RS issues"
  require_positive_integer "${branch_slot1}" "${label} branch successor lane1 dispatches"
  require_positive_integer "${store_direct}" "${label} direct store completions"
  require_positive_integer "${lsu_refill}" "${label} LSU address refills"
  require_positive_integer "${itage_hit}" "${label} ITAGE hits"
  require_positive_integer "${loop_hit}" "${label} loop predictor hits"

  printf '[accept] %s ipc=%s cycles=%s fetch_width=%s commit1_util=%s%% cdb=%s fresh_issue=%s head_wait=%s\n' \
    "${label}" "${ipc}" "${cycles}" "${fetch_width}" "${commit1_util}" \
    "${cdb_conflicts}" "${fresh_issue}" "${head_wait}"
}

check_run "${RUN1}/microbench.log" run1
check_run "${RUN2}/microbench.log" run2

IPC1="$(metric "${RUN1}/microbench.log" "IPC")"
IPC2="$(metric "${RUN2}/microbench.log" "IPC")"
awk -v a="${IPC1}" -v b="${IPC2}" 'BEGIN { d = a - b; if (d < 0) d = -d; exit !(d <= 0.005) }' || {
  echo "[fail] IPC repeatability exceeded 0.005: run1=${IPC1}, run2=${IPC2}" >&2
  exit 1
}

printf '[done] stage13 final validation: %s\n' "${OUT_ROOT}"
