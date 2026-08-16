# 乘除法器（M 扩展）实现计划

项目：`npc/ioid_chisel_vsrc`（Chisel 顺序五级流水线 IFU→IDU→EXU→LSU→WBU）

> 前置：`config.scala` 已加 `muldiv: Boolean = false` 开关（默认关，本计划做完后改成 `true`）。

## 0. 现状与目标

- 当前 ISA 是 **rv32e（无 M 扩展）**：`mul`/`div` 指令不存在，编译器生成 `__mulsi3`/`__divsi3` **软实现**（每个乘除 ~100-200 条指令的循环）。
- ALU 是纯组合 1 拍；EXU 每拍可输出。
- 目标：硬件支持 M 扩展的 8 条指令，`-march` 开 M 后编译器改用硬件乘除。

| 指令 | 行为 | 实现 |
|---|---|---|
| MUL | (a×b) 低 32 位 | 组合，1 拍 |
| MULH / MULHSU / MULHU | (a×b) 高 32 位（有×有 / 有×无 / 无×无） | 组合，1 拍 |
| DIV / DIVU | 有符号/无符号除法（截断向零） | **多周期**（状态机） |
| REM / REMU | 有符号/无符号余数 | 同除法器 |

**核心设计决策**：除法不能进组合 ALU——做成**独立 `DivUnit` 模块 + 握手接口**。这个接口在乱序改造时直接迁移为"长延迟执行单元"（和 load 同级），不重写。

## 1. 改动清单

| 文件 | 操作 |
|---|---|
| `common/consts.scala` | `ALU_OP` 扩到 5 位，加 MUL/MULH/MULHSU/MULHU/DIV/DIVU/REM/REMU |
| `common/inst.scala` | 加 8 个 M 扩展指令位模式 |
| `unit/control.scala` | 加 8 行映射（rs1/rs2 都要读） |
| `unit/alu.scala` | 4 个乘法操作（64 位中间结果） |
| `unit/div.scala` | **新建**：DivUnit 状态机 |
| `core/exu.scala` | 集成：检测 DIV/REM → 交 DivUnit → 完成才输出 |
| `config.scala` | 改 `muldiv = true`（做完后） |

## 2. 步骤 2.1：译码（consts / inst / control）

**`common/consts.scala`**：

```scala
object ALU_OP{
  // 现在 10 个值占满 4 位，扩到 5 位
  val ALU_ADD  = 0.U(5.W)
  ...
  val ALU_NONE = 10.U(5.W)
  val ALU_MUL  = 11.U(5.W)
  val ALU_MULH = 12.U(5.W)
  val ALU_MULHSU = 13.U(5.W)
  val ALU_MULHU = 14.U(5.W)
  val ALU_DIV  = 15.U(5.W)
  val ALU_DIVU = 16.U(5.W)
  val ALU_REM  = 17.U(5.W)
  val ALU_REMU = 18.U(5.W)
}
```

**`common/inst.scala`**（照现有模式）：

```scala
  // M extension
  def MUL    = BitPat("b0000001??????????000?????0110011")
  def MULH   = BitPat("b0000001??????????001?????0110011")
  def MULHSU = BitPat("b0000001??????????010?????0110011")
  def MULHU  = BitPat("b0000001??????????011?????0110011")
  def DIV    = BitPat("b0000001??????????100?????0110011")
  def DIVU   = BitPat("b0000001??????????101?????0110011")
  def REM    = BitPat("b0000001??????????110?????0110011")
  def REMU   = BitPat("b0000001??????????111?????0110011")
```

**`unit/control.scala`** 的 `map` 加 8 行（都读 rs1+rs2、写 rd）：

```scala
    MUL   -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE, NONE_CSR_WRITE, ALU_A_RD1, ALU_B_RD2, ImmX, ALU_MUL,  NONE_VALID, RNONE, WNONE, ALU_SEL, CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ, REG2_READ),
    DIV   -> List(... ALU_DIV ... REG1_READ, REG2_READ),
    ...（DIVU/REM/REMU/MULH/MULHSU/MULHU 同理）
```

⚠️ 注意：**DIV/REM 的 `control` 输出里，`ifu.valid`（fencei 相关）不能置位**，照抄现有算数指令的写法即可。

## 3. 步骤 2.2：乘法（`unit/alu.scala`）

`ALU_IO.alu_control` 改成 5 位。乘法操作：

```scala
    // 64 位中间结果，按符号组合取高/低 32 位
    ALU_MUL    -> (io.A * io.B)(31, 0),
    ALU_MULHU  -> (io.A * io.B)(63, 32),
    ALU_MULH   -> (io.A.asSInt * io.B.asSInt)(63, 32).asUInt,
    ALU_MULHSU -> (io.A.asSInt * io.B.asUInt)(63, 32).asUInt,
```

注意：Chisel 里 `UInt * UInt` 是 64 位无符号；`asSInt * asSInt` 是 64 位有符号；`asSInt * asUInt` 需要先把无符号扩展到 64 位再乘（`(io.A.asSInt * io.B.asUInt)` 中 Chisel 会把宽度统一到 64——若编译报宽度问题，手动 `io.B.asUInt.asSInt` 后补零扩展）。

## 4. 步骤 2.3：DivUnit（新建 `unit/div.scala`）——本计划的核心

**接口**（这个接口乱序要原样用，别改）：

```scala
class DivUnit_IO extends Bundle {
  val req_valid = Input(Bool())        // EXU 请求开始除法
  val req_ready = Output(Bool())       // 空闲可接收（= !busy）
  val a = Input(UInt(32.W))
  val b = Input(UInt(32.W))
  val op = Input(UInt(3.W))            // DIV/DIVU/REM/REMU
  val result_valid = Output(Bool())    // 完成（持续 1 拍）
  val result = Output(UInt(32.W))
}
```

**实现**：restoring 除法，1 位/拍 = 32 拍（可后续优化为 4 位/拍 = 8 拍）。

> ⚠️ **时序对齐（踩过的坑，直接抄这份）**：
> - 迭代、退出、valid 三者的拍数必须对齐：`cnt=1` 拍处理完最低位（bit0）后
>   `cnt` 变 0，**下一拍（cnt=0，仍处 BUSY）才拉 `result_valid`**，此时结果已锁存完整；
> - 退出条件必须写 `cnt === 0.U`（不是 `cnt === 1.U`）——若在 `cnt===1` 就退出，
>   `cnt=0` 拍 state 已变 IDLE，`result_valid` **永远拉不高**；
> - **迭代必须用 `when(cnt =/= 0.U)` 门控**——否则 `cnt=0` 拍会再跑一轮，
>   `cnt-1.U` 下溢成 31，重读 `a_abs(31)`，商余数全错。

```scala
class DivUnit extends Module {
  val io = IO(new DivUnit_IO)

  val s_IDLE :: s_BUSY :: Nil = Enum(2)
  val state = RegInit(s_IDLE)
  val cnt = RegInit(0.U(5.W))          // 32..1（迭代计数），0 = 完成

  // 工作寄存器：转成无符号幅度做恢复除法，最后按符号修正
  val a_abs = RegInit(0.U(32.W))
  val b_abs = RegInit(0.U(32.W))
  val rem = RegInit(0.U(32.W))
  val quo = RegInit(0.U(32.W))
  val a_neg = RegInit(false.B)
  val b_neg = RegInit(false.B)
  val is_rem = RegInit(false.B)
  val div_by_zero = RegInit(false.B)

  // 握手契约：BUSY 期间 req_ready=0，上游必须等到 req_ready 才拉 req_valid
  io.req_ready := state === s_IDLE
  // ★ cnt=0 拍（仍处 BUSY）拉高，此时结果已锁存完整
  io.result_valid := state === s_BUSY && cnt === 0.U

  // 结果选择：除零 / 正常 / 符号修正
  // 除零：DIV/DIVU by 0 = -1；REM/REMU by 0 = 被除数（原始 io.a，规范如此）
  // 溢出：INT_MIN / -1 = INT_MIN（-quo 在 32 位下对 0x80000000 回绕为自身，符合规范）
  io.result := Mux(div_by_zero, Mux(is_rem, io.a, 0xFFFFFFFF.U(32.W)),
                Mux(is_rem, Mux(a_neg, -rem, rem), Mux(a_neg ^ b_neg, -quo, quo)))

  when(state === s_IDLE && io.req_valid) {
    // 锁存输入（EXU 侧保证 req_valid 期间 a/b/op 稳定）
    val is_signed = io.op === ALU_DIV || io.op === ALU_REM   // 可读性：提前提取
    a_abs := Mux(is_signed, Mux(io.a(31), -io.a, io.a), io.a)
    b_abs := Mux(io.op === ALU_DIVU || io.op === ALU_REMU, io.b, Mux(io.b(31), -io.b, io.b))
    a_neg := io.a(31) && is_signed
    b_neg := io.b(31) && is_signed
    is_rem := io.op === ALU_REM || io.op === ALU_REMU
    div_by_zero := io.b === 0.U
    rem := 0.U
    quo := 0.U
    cnt := 32.U
    state := s_BUSY
  }
  when(state === s_BUSY) {
    // ★ 门控：cnt=0 拍不再迭代（避免 cnt-1 下溢成 31、重读 a_abs(31) 的错误迭代）
    when(cnt =/= 0.U) {
      cnt := cnt - 1.U
      rem := Cat(rem(30, 0), a_abs(cnt - 1.U))      // 左移并入被除数下一位
      when(rem >= b_abs) {                          // restoring：够减才减
        rem := rem - b_abs
        quo(cnt - 1.U) := true.B
      }
    }
    when(cnt === 0.U) { state := s_IDLE }           // ★ cnt=0 拍退出
  }
}
```

**时序推演**（对照上面的坑检查）：

```
请求拍:     锁存 a_abs/b_abs/..., cnt=32, → BUSY
BUSY cnt=32..2: 处理 bit31..bit1, 每拍 cnt--
BUSY cnt=1:     处理 bit0, cnt→0（本拍不退出）
BUSY cnt=0:     ★ result_valid=1（结果完整）, state→IDLE
IDLE cnt=0:     不再迭代, 等下一次 req_valid
```

**语义要点**（RISC-V 规范，别漏）：
- **除零**：`DIV/DIVU by 0 = 0xFFFFFFFF(-1)`；`REM/REMU by 0 = 被除数`（上面的 mux 已处理）
- **溢出**：`INT_MIN / -1 = INT_MIN`（C 语言 UB，RISC-V 定义为 INT_MIN；符号修正路径天然正确——`-quo` 在 quo=0x80000000 时会回绕）
- **截断向零**：商符号 = a_sign XOR b_sign，余数符号 = a_sign（RISC-V 与 C 一致）

**必须仿真验证的边界用例**（不能只靠理论推导）：
`INT_MIN / -1`、`INT_MIN / 1`、`-7 / 2`、`7 / -2`、`-7 % 2`、`7 % -2`、四种除零（DIV/DIVU/REM/REMU by 0）、以及 `INT_MIN` 参与的无符号混合。

## 5. 步骤 2.4：EXU 集成（`core/exu.scala`）——关键的 3 行门控

```scala
    val is_div = io.in.valid &&
      (io.in.bits.signals.exu.alu_control === ALU_DIV ||
       io.in.bits.signals.exu.alu_control === ALU_DIVU ||
       io.in.bits.signals.exu.alu_control === ALU_REM ||
       io.in.bits.signals.exu.alu_control === ALU_REMU)

    val div = Module(new DivUnit)
    div.io.req_valid := io.in.valid && is_div && !div_done_reg && div.io.req_ready
    div.io.a := io.in.bits.rd1
    div.io.b := io.in.bits.rd2
    div.io.op := ... // 从 alu_control 映射

    // ★ 门控 1：除法期间 EXU 必须拒绝新指令（否则输入寄存器被覆盖，除法数据丢失）
    io.in.ready := (!io.in.valid || io.out.ready) && !div_busy
    // ★ 门控 2：输出/PC 有效 = 除法完成（普通指令恒真）
    io.out.valid := io.in.valid && (!is_div || div_done)
    io.pc.valid  := io.out.valid && io.in.ready
    // ★ 门控 3：结果 mux——转发网络读的是 exu.io.out.bits.alu_result
    io.out.bits.alu_result := Mux(is_div, div.io.result, alu.io.result)
```

其中 `div_busy`/`div_done` 是 DivUnit 的 `req_ready`/`result_valid` 的寄存器版本（或用状态推断）。

**为什么这 3 行就够了（不用改 core.scala 的转发/停顿）**：
- 除法期间 `in.ready=0` → IDU 冻结 → 依赖指令进不来 → 不存在错误转发
- 完成那拍 `alu_result = div.result` → RsForward 读到的就是正确结果
- 分支对账：除法期间 `pc.valid=0` → `is_jump=0` → **已修的 `mis_predict = is_jump && ...` 门控天然免疫**，不会误报/死锁

## 6. 步骤 2.5：验证

1. **功能**（先不换工具链，用软实现跑通回归）：`make` + 35 个 cpu-tests + microbench（difftest 全开）——M 扩展指令一个都不该出现，纯回归。
2. **硬件乘除验证**（两个办法）：
   - **改工具链**：abstract-machine 的 npc 平台 `march` 从 `rv32e` 改 `rv32em`（或 `rv32im`），重编 am-kernels——编译器开始发射 `mul`/`div`，difftest 直接对 NEMU（NEMU 默认支持 M）。
   - 或者写一个手搓汇编测试：`.word 0x02a58533`（`add a0,a1,a0` 换成 `mul a0,a0,a1`）塞进程序，配合 itrace 看是否执行。
3. **性能**：对比同一程序（软实现 vs 硬件）的周期数——`mul-longlong`/`div` 测试和 microbench 的 fib/15pz 等。

## 7. 坑（对照检查）

1. **`in.ready` 不门控 = 数据丢失**：除法期间 EXU 输入被下一条覆盖（最大的坑）。
2. **除零/溢出语义**：`DIV by 0 = -1`、`REM by 0 = 被除数`、`INT_MIN / -1 = INT_MIN`。
3. **MULH 三种符号组合**：MULH=有×有、MULHSU=有×无、MULHU=无×无，别混。
4. **有符号除法截断向零**：先取绝对值、最后按符号修正，余数符号跟随被除数。
5. **`alu_control` 宽度**：4 位 → 5 位后，`Control` 的输出和 ALU 的输入要同步改。
6. **验证时 march 只改 am-kernels 的编译**，别动 NEMU（NEMU 已支持 M）。

## 8. 与乱序的衔接（为什么这个接口不白做）

乱序改造（`OoO_实现计划.md`）里，`DivUnit` **原样保留**：
- 接口不变：`(req_valid/req_ready, a, b, op, result_valid/result)` 就是"发射 → 长延迟单元 → 完成/广播"的零件
- 唯一变化：EXU 的"忙就冻结"（`in.ready` 门控 + `out.valid` 门控）删掉，换成"发射进保留站、完成后广播唤醒"
- 乘法在 ALU 里，乱序也是 1 拍——不用动
