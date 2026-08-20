# t3_call_ret.S — jal ra / ret 深度调用（不用 GOT）

    .option norvc
    .text
    .globl _start
_start:
    # sp = stack top (bss 末尾)
1:  auipc sp, %pcrel_hi(_stack_top)
    addi  sp, sp, %pcrel_lo(1b)

    li    s0, 0
    li    s1, 0
    li    s2, 20
outer:
    mv    a0, s1
    jal   ra, triangle
    add   s0, s0, a0
    addi  s1, s1, 1
    blt   s1, s2, outer

    li    t0, 1330
    bne   s0, t0, fail

    # jalr call leaf_add
2:  auipc t0, %pcrel_hi(leaf_add)
    addi  t0, t0, %pcrel_lo(2b)
    li    a0, 10
    li    a1, 32
    jalr  ra, t0, 0
    li    t1, 42
    bne   a0, t1, fail

    li    a0, 0
    ebreak
    j     .

fail:
    li    a0, 1
    ebreak
    j     .

triangle:
    addi  sp, sp, -16
    sw    ra, 12(sp)
    sw    s0, 8(sp)
    mv    s0, a0
    beq   s0, x0, tri_zero
    addi  a0, s0, -1
    jal   ra, triangle
    add   a0, a0, s0
    j     tri_ret
tri_zero:
    li    a0, 0
tri_ret:
    lw    ra, 12(sp)
    lw    s0, 8(sp)
    addi  sp, sp, 16
    ret

leaf_add:
    add   a0, a0, a1
    ret

    .bss
    .align 4
    .space 4096
_stack_top:
