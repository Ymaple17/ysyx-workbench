#!/usr/bin/env bash
# 编译并运行 baseline 定向汇编测试
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
NPC_HOME="${NPC_HOME:-$(cd "$DIR/../.." && pwd)}"
NEMU_HOME="${NEMU_HOME:-$(cd "$NPC_HOME/../nemu" && pwd)}"
NPC_BIN="${NPC_BIN:-$NPC_HOME/obj_dir/Vysyx_25020039}"
DIFF_SO="${DIFF_SO:-$NEMU_HOME/build/riscv32-nemu-interpreter-so}"

if [[ ! -x "$NPC_BIN" ]]; then
  echo "[ERR] NPC binary not found: $NPC_BIN"
  echo "      cd \$NPC_HOME && make chisel-gen && make -j\$(nproc) all"
  exit 1
fi
if [[ ! -f "$DIFF_SO" ]]; then
  echo "[ERR] NEMU so not found: $DIFF_SO"
  exit 1
fi

CC=""
for c in riscv64-linux-gnu-gcc riscv64-unknown-elf-gcc; do
  if command -v "$c" >/dev/null 2>&1; then CC=$c; break; fi
done
if [[ -z "$CC" ]]; then
  echo "[ERR] no riscv gcc"
  exit 1
fi

OBJCOPY="${CC%-gcc}-objcopy"
BUILD="$DIR/build"
mkdir -p "$BUILD"

pass=0
fail=0
tests=(t1_div_flush t2_fencei_smc t3_call_ret)

for t in "${tests[@]}"; do
  src="$DIR/$t.S"
  elf="$BUILD/$t.elf"
  bin="$BUILD/$t.bin"
  echo "======== BUILD $t ========"
  # -fno-pic：禁止 GOT 间接 la；裸机没有动态链接器
  "$CC" -march=rv32i_zicsr_zifencei_m -mabi=ilp32 -nostdlib -nostartfiles \
    -fno-pic -mno-relax \
    -T "$DIR/link.ld" -o "$elf" "$src" 2>/dev/null \
  || "$CC" -march=rv32im -mabi=ilp32 -nostdlib -nostartfiles \
    -fno-pic -mno-relax \
    -T "$DIR/link.ld" -o "$elf" "$src"
  "$OBJCOPY" -O binary "$elf" "$bin"

  echo "======== RUN   $t ========"
  set +e
  out=$(cd "$NPC_HOME" && "$NPC_BIN" "$bin" -b --diff="$DIFF_SO" -e "$elf" 2>&1)
  rc=$?
  set -e
  if echo "$out" | grep -q "HIT GOOD TRAP"; then
    # a0==0 才是业务成功；HIT GOOD TRAP 且 halt_ret=0
    if echo "$out" | grep -qE "HIT GOOD TRAP|halt_ret = 0"; then
      # npc 打印格式: npc: HIT GOOD TRAP at pc = ...
      if echo "$out" | grep -qi "ABORT\|MISMATCH\|HIT BAD"; then
        echo "[FAIL] $t (abort/mismatch)"
        echo "$out" | tail -40
        fail=$((fail+1))
      else
        echo "[PASS] $t"
        pass=$((pass+1))
      fi
    else
      echo "[FAIL] $t"
      echo "$out" | tail -40
      fail=$((fail+1))
    fi
  else
    echo "[FAIL] $t (no GOOD TRAP)"
    echo "$out" | tail -50
    fail=$((fail+1))
  fi
done

echo "======== TOTAL pass=$pass fail=$fail ========"
exit "$fail"
