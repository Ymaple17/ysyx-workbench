# muldiv.S — 手搓 M 扩展乘除法指令测试
# 覆盖 MUL / MULH / MULHSU / MULHU / DIV / DIVU / REM / REMU
# 含边界：符号、溢出(0x80000000/-1)、除零、无符号大数
# 编译: riscv64-linux-gnu-as -march=rv32em -mabi=ilp32e
# 链接: 入口 0x80000000, ebreak 结束 (difftest 自动对比寄存器)

    .text
    .globl _start
_start:
    # ===== MUL (低32位) =====
    li  a0, 5
    li  a1, 7
    mul a2, a0, a1           #  35
    li  a0, -100
    li  a1, 3
    mul a2, a0, a1           #  -300
    li  a0, -8
    li  a1, -9
    mul a2, a0, a1           #  72
    li  a0, 0x80000000
    li  a1, -1
    mul a2, a0, a1           #  0x80000000 (溢出 wrap)
    li  a0, 0x7fffffff
    li  a1, 2
    mul a2, a0, a1           #  0xFFFFFFFE
    li  a0, -1
    li  a1, -1
    mul a2, a0, a1           #  1

    # ===== MULH (有符号高32位) =====
    li  a0, 0x80000000
    li  a1, 0x80000000
    mulh a2, a0, a1          #  (-2^31)^2 = 2^62   -> 0x40000000
    li  a0, 0x7fffffff
    li  a1, 0x7fffffff
    mulh a2, a0, a1          #  (2^31-1)^2         -> 0x3FFFFFFF
    li  a0, 0x80000000
    li  a1, 0x7fffffff
    mulh a2, a0, a1          #  -2^31*(2^31-1)     -> 0xC0000000
    li  a0, -1
    li  a1, -1
    mulh a2, a0, a1          #  1                  -> 0x00000000
    li  a0, 0x80000000
    li  a1, 0x80000001
    mulh a2, a0, a1          #  (-2^31)*(-2^31+1)  -> 0x3FFFFFFF

    # ===== MULHSU (有符号×无符号 高32位) =====
    li  a0, 0x80000000
    li  a1, 0x80000000
    mulhsu a2, a0, a1        #  -2^31 * 2^31       -> 0xC0000000
    li  a0, 0x7fffffff
    li  a1, 0x80000000
    mulhsu a2, a0, a1        #  (2^31-1)*2^31      -> 0x3FFFFFFF
    li  a0, -1
    li  a1, 0xffffffff
    mulhsu a2, a0, a1        #  -1 * (2^32-1)      -> 0xFFFFFFFF
    li  a0, 0x80000000
    li  a1, 0
    mulhsu a2, a0, a1        #  0                  -> 0x00000000

    # ===== MULHU (无符号高32位) =====
    li  a0, 0x80000000
    li  a1, 0x80000000
    mulhu a2, a0, a1         #  0x80000000^2       -> 0x40000000
    li  a0, 0xffffffff
    li  a1, 0xffffffff
    mulhu a2, a0, a1         #  (2^32-1)^2         -> 0xFFFFFFFE
    li  a0, 0xffffffff
    li  a1, 0x80000000
    mulhu a2, a0, a1         #  (2^32-1)*2^31      -> 0x7FFFFFFF

    # ===== DIV (有符号除法) =====
    li  a0, 20
    li  a1, 7
    div a2, a0, a1           #  2
    li  a0, -20
    li  a1, 7
    div a2, a0, a1           #  -2
    li  a0, 20
    li  a1, -7
    div a2, a0, a1           #  -2
    li  a0, -20
    li  a1, -7
    div a2, a0, a1           #  2
    li  a0, 0x80000000
    li  a1, -1
    div a2, a0, a1           #  0x80000000 (溢出)
    li  a0, 0x80000000
    li  a1, 2
    div a2, a0, a1           #  0xC0000000
    li  a0, 123
    li  a1, 0
    div a2, a0, a1           #  0xFFFFFFFF (除零)
    li  a0, 0x80000000
    li  a1, 0x80000000
    div a2, a0, a1           #  1

    # ===== REM (有符号取余) =====
    li  a0, 20
    li  a1, 7
    rem a2, a0, a1           #  6
    li  a0, -20
    li  a1, 7
    rem a2, a0, a1           #  -6
    li  a0, 20
    li  a1, -7
    rem a2, a0, a1           #  6
    li  a0, -20
    li  a1, -7
    rem a2, a0, a1           #  -6
    li  a0, 0x80000000
    li  a1, -1
    rem a2, a0, a1           #  0 (溢出)
    li  a0, 123
    li  a1, 0
    rem a2, a0, a1           #  123 (除零=被除数)

    # ===== DIVU (无符号除法) =====
    li  a0, 0x80000000
    li  a1, 2
    divu a2, a0, a1          #  0x40000000
    li  a0, 0xffffffff
    li  a1, 1
    divu a2, a0, a1          #  0xFFFFFFFF
    li  a0, 0x80000000
    li  a1, 0x80000000
    divu a2, a0, a1          #  1
    li  a0, 0xffffffff
    li  a1, 0
    divu a2, a0, a1          #  0xFFFFFFFF (除零)
    li  a0, 0x80000000
    li  a1, 3
    divu a2, a0, a1          #  0x2AAAAAAA

    # ===== REMU (无符号取余) =====
    li  a0, 0x80000000
    li  a1, 3
    remu a2, a0, a1          #  2
    li  a0, 0xffffffff
    li  a1, 0xffffffff
    remu a2, a0, a1          #  0
    li  a0, 0x80000001
    li  a1, 0x80000000
    remu a2, a0, a1          #  1
    li  a0, 0xffffffff
    li  a1, 0
    remu a2, a0, a1          #  0xFFFFFFFF (除零)

    # 最后结果存内存方便人工查看
    li  t0, 0x80002000
    sw  a2, 0(t0)

    li  a0, 0              # NEMU: a0==0 才是 GOOD TRAP
    ebreak
    j   .
