# 基线修复定向测试

`cpu-tests` **不能**专门覆盖下面这些路径（最多说明「主路径没炸」）：

| 修复 | 为何 cpu-tests 测不到 |
|------|------------------------|
| DIV kill | 需要 **冲刷发生时 DIV 仍在 EXU 运算**；C 测例几乎不会构造 |
| FENCE.I | 需要 **自修改代码 + fence.i + 再执行** |
| RAS `jal ra` | 只影响 **预测命中率**，功能对错靠 mispred 冲刷兜底；正确性测不到「是否 push」 |

本目录用 **手写汇编** 做定向验证。

## 测试列表

| 文件 | 验证点 |
|------|--------|
| `t1_div_flush.S` | 分支误预测冲刷在飞 DIV 后，后续指令结果不被旧商污染 |
| `t2_fencei_smc.S` | FENCE.I 后执行到自修改后的新指令 |
| `t3_call_ret.S` | 大量 `jal ra` / `ret` 功能正确（间接压 RAS；错则仍应靠冲刷对） |

## 编译与运行

```bash
export NPC_HOME=.../npc
export NEMU_HOME=.../nemu
cd $NPC_HOME/tests/baseline_fix
./build_and_run.sh
```

依赖：`riscv64-linux-gnu-gcc`（或 `riscv64-unknown-elf-gcc`）、已编译的 `obj_dir/Vysyx_25020039`、NEMU so。
