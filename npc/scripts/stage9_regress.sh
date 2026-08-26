#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: scripts/stage9_regress.sh [options]

Modes:
  --mode compile      Run mill compile only.
  --mode cpu          Regenerate Chisel SV and run selected cpu-tests.
  --mode microbench   Regenerate Chisel SV, run microbench(test), and extract perf CSV.
  --mode quick        Run compile + cpu smoke + microbench(test). Default.
  --mode full         Run compile + OoOUnitTest + cpu smoke + microbench(test).
  --mode parse        Extract perf CSV from an existing microbench log.

Options:
  --tag NAME          Label used in output CSV/log directory. Default: stage9d.
  --out-dir DIR       Output directory. Default: $NPC_HOME/build/stage9_regress/TAG_TIMESTAMP.
  --cpu-tests LIST    Space-separated cpu-tests. Default: dummy add add-longlong bit load-store shift string.
  --parse-log FILE    Existing log for --mode parse.
  --cpu-timeout SEC   Timeout for each cpu-test simulation. Default: 240.
  --micro-timeout SEC Timeout for microbench simulation. Default: 900.
  -h, --help          Show this help.

Environment:
  NPC_HOME, NEMU_HOME, AM_HOME, and AMK are inferred from this script location
  when possible, but may be overridden by the caller. CPU_TEST_HOME defaults
  to $AMK/tests/cpu-tests.
USAGE
}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NPC_HOME="${NPC_HOME:-$(cd "${SCRIPT_DIR}/.." && pwd)}"
NEMU_HOME="${NEMU_HOME:-$(cd "${NPC_HOME}/.." && pwd)/nemu}"
AM_HOME="${AM_HOME:-$(cd "${NPC_HOME}/.." && pwd)/abstract-machine}"
AMK="${AMK:-$(cd "${NPC_HOME}/.." && pwd)/am-kernels}"
CPU_TEST_HOME="${CPU_TEST_HOME:-${AMK}/tests/cpu-tests}"
export NPC_HOME NEMU_HOME AM_HOME AMK CPU_TEST_HOME

MODE="quick"
TAG="stage9d"
OUT_DIR=""
CPU_TESTS="dummy add add-longlong bit load-store shift string"
PARSE_LOG=""
CPU_TIMEOUT="240"
MICRO_TIMEOUT="900"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --mode)
      MODE="${2:?missing value for --mode}"
      shift 2
      ;;
    --tag)
      TAG="${2:?missing value for --tag}"
      shift 2
      ;;
    --out-dir)
      OUT_DIR="${2:?missing value for --out-dir}"
      shift 2
      ;;
    --cpu-tests)
      CPU_TESTS="${2:?missing value for --cpu-tests}"
      shift 2
      ;;
    --parse-log)
      PARSE_LOG="${2:?missing value for --parse-log}"
      shift 2
      ;;
    --cpu-timeout)
      CPU_TIMEOUT="${2:?missing value for --cpu-timeout}"
      shift 2
      ;;
    --micro-timeout)
      MICRO_TIMEOUT="${2:?missing value for --micro-timeout}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

timestamp="$(date +%Y%m%d_%H%M%S)"
OUT_DIR="${OUT_DIR:-${NPC_HOME}/build/stage9_regress/${TAG}_${timestamp}}"
mkdir -p "${OUT_DIR}"

DIFF_SO="${NEMU_HOME}/build/riscv32-nemu-interpreter-so"

require_path() {
  local path="$1"
  local desc="$2"
  if [[ ! -e "${path}" ]]; then
    echo "missing ${desc}: ${path}" >&2
    exit 1
  fi
}

run_logged() {
  local name="$1"
  shift
  local log="${OUT_DIR}/${name}.log"
  echo "[run] ${name}"
  if "$@" >"${log}" 2>&1; then
    echo "[pass] ${name} -> ${log}"
  else
    echo "[fail] ${name} -> ${log}" >&2
    tail -80 "${log}" >&2 || true
    exit 1
  fi
}

extract_metric() {
  local log="$1"
  local key="$2"
  awk -F: -v key="${key}" '
    $1 == key {
      gsub(/^[ \t]+/, "", $2)
      split($2, a, /[ \t]+/)
      gsub(/%/, "", a[1])
      print a[1]
      exit
    }
  ' "${log}"
}

extract_microbench_csv() {
  local log="$1"
  local csv="$2"
  local tag="$3"
  require_path "${log}" "microbench log"
  if ! grep -q "MicroBench PASS" "${log}" ||
     ! grep -q "HIT GOOD TRAP" "${log}" ||
     grep -Eq "npc: .*ABORT|\[HANG\]|\[BUS-ORPHAN\]|difftest.*MISMATCH" "${log}"; then
    echo "[fail] microbench log did not reach a clean GOOD TRAP: ${log}" >&2
    tail -120 "${log}" >&2 || true
    exit 1
  fi

  local total_cycles ipc commit_inst ifu_fetch bpu_hit bpu_mispred bp_flush
  local dir_miss target_miss unpred_jalr fq_full fq_empty ifu_pipe
  local cdb_conflicts head_wait wait_store_commit
  local commit_slot0 commit_slot1 commit2_cycles commit_slot1_block commit_slot1_util
  local commit_slot1_not_ready commit_slot1_block_slot0_excl commit_slot1_block_mem
  local commit_slot1_block_ctrl commit_slot1_block_csr commit_slot1_block_special
  local commit_slot1_block_bp
  local commit_slot1_load commit_slot1_ctrl commit_slot1_store bpu_update_queue_block
  local store_buffer_full store_buffer_enq store_buffer_drain store_buffer_fwd
  local store_buffer_enq2
  local dcache_access dcache_hit dcache_miss dcache_bypass dcache_hit_rate
  local dcache_mshr_alloc dcache_hit_under_miss dcache_mshr_refill
  local lq_alloc lq_full lq_high_water load_replay replay_store_wait
  local replay_dcache_busy replay_cdb_busy stale_load_resp mem_order_violation
  local fetch_slot0 fetch_slot1 fetch_slot1_killed fq_enq2 fq_space_one
  local fetch_redirect_bubble fetch_slot1_util bpu_tagged_hit bpu_indirect_hit
  local fetch_blocks fetch_valid_inst fetch_block2 fetch_buffer_full ftq_full
  local fetch_line_tails spec_ghr_rollbacks ras_rollbacks ftq_high_water
  local ftq_stale_recover average_fetch_width
  local tage_use_alt tage_alloc itage_hit itage_alloc
  local alu1_issues dual_alu_issues fu_refill wb_age_reorders

  total_cycles="$(extract_metric "${log}" "Total Cycles")"
  ipc="$(extract_metric "${log}" "IPC")"
  commit_inst="$(extract_metric "${log}" "Commit Instructions")"
  ifu_fetch="$(extract_metric "${log}" "Instructions Fetched (IFU)")"
  bpu_hit="$(extract_metric "${log}" "Hit Rate")"
  bpu_mispred="$(extract_metric "${log}" "Mispredicts")"
  bp_flush="$(extract_metric "${log}" "BP Flushes")"
  dir_miss="$(extract_metric "${log}" "Direction Miss")"
  target_miss="$(extract_metric "${log}" "Target Miss")"
  unpred_jalr="$(extract_metric "${log}" "Unpredicted JALR")"
  fq_full="$(extract_metric "${log}" "FQ Full")"
  fq_empty="$(extract_metric "${log}" "FQ Empty")"
  ifu_pipe="$(extract_metric "${log}" "Pipeline Stall")"
  fetch_slot0="$(extract_metric "${log}" "Fetch Slot0 Valid")"
  fetch_slot1="$(extract_metric "${log}" "Fetch Slot1 Valid")"
  fetch_slot1_killed="$(extract_metric "${log}" "Fetch Slot1 Killed")"
  fq_enq2="$(extract_metric "${log}" "FQ Enq2")"
  fq_space_one="$(extract_metric "${log}" "FQ Space One")"
  fetch_redirect_bubble="$(extract_metric "${log}" "Fetch Redirect Bubble")"
  fetch_slot1_util="$(extract_metric "${log}" "Fetch Slot1 Util")"
  fetch_blocks="$(extract_metric "${log}" "Fetch Blocks")"
  fetch_valid_inst="$(extract_metric "${log}" "Fetch Valid Instructions")"
  fetch_block2="$(extract_metric "${log}" "Fetch Two-Instruction Blocks")"
  fetch_buffer_full="$(extract_metric "${log}" "FetchBuffer Full Cycles")"
  ftq_full="$(extract_metric "${log}" "FTQ Full Cycles")"
  fetch_line_tails="$(extract_metric "${log}" "Fetch Line Tails")"
  spec_ghr_rollbacks="$(extract_metric "${log}" "Spec GHR Rollbacks")"
  ras_rollbacks="$(extract_metric "${log}" "RAS Rollbacks")"
  ftq_high_water="$(extract_metric "${log}" "FTQ High Water")"
  ftq_stale_recover="$(extract_metric "${log}" "FTQ Stale Recover")"
  average_fetch_width="$(extract_metric "${log}" "Average Fetch Width")"
  bpu_tagged_hit="$(extract_metric "${log}" "Tagged Hits")"
  bpu_indirect_hit="$(extract_metric "${log}" "Indirect Hits")"
  tage_use_alt="$(extract_metric "${log}" "TAGE Alternate")"
  tage_alloc="$(extract_metric "${log}" "TAGE Allocations")"
  itage_hit="$(extract_metric "${log}" "ITAGE Hits")"
  itage_alloc="$(extract_metric "${log}" "ITAGE Allocations")"
  cdb_conflicts="$(extract_metric "${log}" "CDB Conflicts")"
  alu1_issues="$(extract_metric "${log}" "ALU1 Issues")"
  dual_alu_issues="$(extract_metric "${log}" "Dual ALU Issues")"
  fu_refill="$(extract_metric "${log}" "FU Same-Cycle Refill")"
  wb_age_reorders="$(extract_metric "${log}" "WB Age Reorders")"
  commit_slot0="$(extract_metric "${log}" "Commit Slot0")"
  commit_slot1="$(extract_metric "${log}" "Commit Slot1")"
  commit2_cycles="$(extract_metric "${log}" "Commit2 Cycles")"
  commit_slot1_block="$(extract_metric "${log}" "Commit Slot1 Block")"
  commit_slot1_not_ready="$(extract_metric "${log}" "Commit Slot1 NotReady")"
  commit_slot1_block_slot0_excl="$(extract_metric "${log}" "Commit Slot1 Block Slot0Excl")"
  commit_slot1_block_mem="$(extract_metric "${log}" "Commit Slot1 Block Mem")"
  commit_slot1_block_ctrl="$(extract_metric "${log}" "Commit Slot1 Block Ctrl")"
  commit_slot1_block_csr="$(extract_metric "${log}" "Commit Slot1 Block CSR")"
  commit_slot1_block_special="$(extract_metric "${log}" "Commit Slot1 Block Special")"
  commit_slot1_block_bp="$(extract_metric "${log}" "Commit Slot1 Block BP")"
  commit_slot1_load="$(extract_metric "${log}" "Commit Slot1 Load")"
  commit_slot1_ctrl="$(extract_metric "${log}" "Commit Slot1 Control")"
  commit_slot1_store="$(extract_metric "${log}" "Commit Slot1 Store")"
  bpu_update_queue_block="$(extract_metric "${log}" "BPU Update Queue Block")"
  commit_slot1_util="$(extract_metric "${log}" "Commit Slot1 Util")"
  head_wait="$(extract_metric "${log}" "Head Not Ready")"
  wait_store_commit="$(extract_metric "${log}" "Wait Store Commit")"
  store_buffer_full="$(extract_metric "${log}" "StoreBuffer Full")"
  store_buffer_enq="$(extract_metric "${log}" "StoreBuffer Enq")"
  store_buffer_enq2="$(extract_metric "${log}" "StoreBuffer Enq2")"
  store_buffer_drain="$(extract_metric "${log}" "StoreBuffer Drain")"
  store_buffer_fwd="$(extract_metric "${log}" "StoreBuffer Fwd")"
  dcache_access="$(extract_metric "${log}" "DCache Accesses")"
  dcache_hit="$(extract_metric "${log}" "DCache Hits")"
  dcache_miss="$(extract_metric "${log}" "DCache Misses")"
  dcache_bypass="$(extract_metric "${log}" "DCache Bypass")"
  dcache_mshr_alloc="$(extract_metric "${log}" "DCache MSHR Alloc")"
  dcache_hit_under_miss="$(extract_metric "${log}" "DCache Hit Under Miss")"
  dcache_mshr_refill="$(extract_metric "${log}" "DCache MSHR Refill")"
  dcache_hit_rate="$(extract_metric "${log}" "DCache Hit Rate")"
  lq_alloc="$(extract_metric "${log}" "LQ Alloc")"
  lq_full="$(extract_metric "${log}" "LQ Full Cycles")"
  lq_high_water="$(extract_metric "${log}" "LQ High Water")"
  load_replay="$(extract_metric "${log}" "Load Replays")"
  replay_store_wait="$(extract_metric "${log}" "Replay Store Wait")"
  replay_dcache_busy="$(extract_metric "${log}" "Replay DCache Busy")"
  replay_cdb_busy="$(extract_metric "${log}" "Replay CDB Busy")"
  stale_load_resp="$(extract_metric "${log}" "Stale Load Resp")"
  mem_order_violation="$(extract_metric "${log}" "Mem Order Violation")"

  if [[ -z "${ipc}" || -z "${total_cycles}" || -z "${commit_inst}" ]]; then
    echo "[fail] microbench performance summary is incomplete: ${log}" >&2
    exit 1
  fi

  {
    echo "tag,ipc,total_cycles,commit_inst,ifu_fetch,bpu_hit_pct,bpu_mispred,bp_flush,dir_miss,target_miss,unpred_jalr,fq_full,fq_empty,ifu_pipeline_stall,fetch_slot0,fetch_slot1,fetch_slot1_killed,fq_enq2,fq_space_one,fetch_redirect_bubble,fetch_slot1_util_pct,fetch_blocks,fetch_valid_inst,fetch_block2,fetch_buffer_full,ftq_full,fetch_line_tails,spec_ghr_rollbacks,ras_rollbacks,ftq_high_water,ftq_stale_recover,average_fetch_width,bpu_tagged_hit,bpu_indirect_hit,tage_use_alt,tage_alloc,itage_hit,itage_alloc,cdb_conflicts,alu1_issues,dual_alu_issues,fu_refill,wb_age_reorders,commit_slot0,commit_slot1,commit2_cycles,commit_slot1_block,commit_slot1_not_ready,commit_slot1_block_slot0_excl,commit_slot1_block_mem,commit_slot1_block_ctrl,commit_slot1_block_csr,commit_slot1_block_special,commit_slot1_block_bp,commit_slot1_load,commit_slot1_ctrl,commit_slot1_store,bpu_update_queue_block,commit_slot1_util_pct,head_wait,wait_store_commit,store_buffer_full,store_buffer_enq,store_buffer_enq2,store_buffer_drain,store_buffer_fwd,dcache_access,dcache_hit,dcache_miss,dcache_bypass,dcache_mshr_alloc,dcache_hit_under_miss,dcache_mshr_refill,dcache_hit_rate_pct,lq_alloc,lq_full,lq_high_water,load_replay,replay_store_wait,replay_dcache_busy,replay_cdb_busy,stale_load_resp,mem_order_violation,result"
    echo "${tag},${ipc},${total_cycles},${commit_inst},${ifu_fetch},${bpu_hit},${bpu_mispred},${bp_flush},${dir_miss},${target_miss},${unpred_jalr},${fq_full},${fq_empty},${ifu_pipe},${fetch_slot0},${fetch_slot1},${fetch_slot1_killed},${fq_enq2},${fq_space_one},${fetch_redirect_bubble},${fetch_slot1_util},${fetch_blocks},${fetch_valid_inst},${fetch_block2},${fetch_buffer_full},${ftq_full},${fetch_line_tails},${spec_ghr_rollbacks},${ras_rollbacks},${ftq_high_water},${ftq_stale_recover},${average_fetch_width},${bpu_tagged_hit},${bpu_indirect_hit},${tage_use_alt},${tage_alloc},${itage_hit},${itage_alloc},${cdb_conflicts},${alu1_issues},${dual_alu_issues},${fu_refill},${wb_age_reorders},${commit_slot0},${commit_slot1},${commit2_cycles},${commit_slot1_block},${commit_slot1_not_ready},${commit_slot1_block_slot0_excl},${commit_slot1_block_mem},${commit_slot1_block_ctrl},${commit_slot1_block_csr},${commit_slot1_block_special},${commit_slot1_block_bp},${commit_slot1_load},${commit_slot1_ctrl},${commit_slot1_store},${bpu_update_queue_block},${commit_slot1_util},${head_wait},${wait_store_commit},${store_buffer_full},${store_buffer_enq},${store_buffer_enq2},${store_buffer_drain},${store_buffer_fwd},${dcache_access},${dcache_hit},${dcache_miss},${dcache_bypass},${dcache_mshr_alloc},${dcache_hit_under_miss},${dcache_mshr_refill},${dcache_hit_rate},${lq_alloc},${lq_full},${lq_high_water},${load_replay},${replay_store_wait},${replay_dcache_busy},${replay_cdb_busy},${stale_load_resp},${mem_order_violation},PASS"
  } > "${csv}"
  echo "[summary] ${csv}"
}

run_compile() {
  run_logged "compile" bash -lc "cd \"${NPC_HOME}/oood_chisel_vsrc\" && ./mill -i mychisel.compile"
}

CHISEL_GEN_DONE=0
run_chisel_gen() {
  if [[ "${CHISEL_GEN_DONE}" == "0" ]]; then
    run_logged "chisel_gen" \
      make -C "${NPC_HOME}" chisel-gen
    CHISEL_GEN_DONE=1
  fi
}

run_unit() {
  run_logged "oounit" bash -lc "cd \"${NPC_HOME}/oood_chisel_vsrc\" && ./mill -i mychisel.test.testOnly unit.OoOUnitTest"
}

run_cpu_tests() {
  require_path "${DIFF_SO}" "NEMU difftest shared object"
  require_path "${CPU_TEST_HOME}" "cpu-tests directory"
  run_chisel_gen
  for test_name in ${CPU_TESTS}; do
    run_logged "cpu_${test_name}_build" \
      make -C "${CPU_TEST_HOME}" ARCH=riscv32e-npc ALL="${test_name}"
    local bin="${CPU_TEST_HOME}/build/${test_name}-riscv32e-npc.bin"
    require_path "${bin}" "cpu-test binary ${test_name}"
    run_logged "cpu_${test_name}" \
      bash -lc "cd \"${NPC_HOME}\" && timeout \"${CPU_TIMEOUT}\" make sim PROGRAM=\"${bin}\""
    if grep -q "HIT GOOD TRAP" "${OUT_DIR}/cpu_${test_name}.log"; then
      echo "[pass] cpu ${test_name}"
    else
      echo "[fail] cpu ${test_name}: GOOD TRAP not found" >&2
      tail -80 "${OUT_DIR}/cpu_${test_name}.log" >&2 || true
      exit 1
    fi
  done
}

run_microbench() {
  run_chisel_gen
  run_logged "microbench_build" \
    make -C "${AMK}/benchmarks/microbench" AM_HOME="${AM_HOME}" ARCH=riscv32e-npc mainargs=test insert-arg
  local bin="${AMK}/benchmarks/microbench/build/microbench-riscv32e-npc.bin"
  require_path "${bin}" "microbench binary"
  run_logged "microbench" \
    bash -lc "cd \"${NPC_HOME}\" && timeout \"${MICRO_TIMEOUT}\" make sim PROGRAM=\"${bin}\""
  if grep -q "MicroBench PASS" "${OUT_DIR}/microbench.log" &&
     grep -q "HIT GOOD TRAP" "${OUT_DIR}/microbench.log" &&
     ! grep -Eq "npc: .*ABORT|\[HANG\]|\[BUS-ORPHAN\]|difftest.*MISMATCH" "${OUT_DIR}/microbench.log"; then
    extract_microbench_csv "${OUT_DIR}/microbench.log" "${OUT_DIR}/microbench_summary.csv" "${TAG}"
  else
    echo "[fail] microbench: clean PASS/GOOD TRAP sequence not found" >&2
    tail -120 "${OUT_DIR}/microbench.log" >&2 || true
    exit 1
  fi
}

case "${MODE}" in
  compile)
    run_compile
    ;;
  cpu)
    run_cpu_tests
    ;;
  microbench)
    run_microbench
    ;;
  unit)
    run_compile
    run_unit
    ;;
  quick)
    run_compile
    run_cpu_tests
    run_microbench
    ;;
  full)
    run_compile
    run_unit
    run_cpu_tests
    run_microbench
    ;;
  parse)
    if [[ -z "${PARSE_LOG}" ]]; then
      echo "--parse-log is required for --mode parse" >&2
      exit 2
    fi
    extract_microbench_csv "${PARSE_LOG}" "${OUT_DIR}/microbench_summary.csv" "${TAG}"
    ;;
  *)
    echo "unknown mode: ${MODE}" >&2
    usage >&2
    exit 2
    ;;
esac

echo "[done] mode=${MODE} tag=${TAG} out=${OUT_DIR}"
