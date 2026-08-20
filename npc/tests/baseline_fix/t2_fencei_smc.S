# t2_fencei_smc.S — FENCE.I + 自修改代码（无 GOT）

    .option norvc
    .text
    .globl _start
_start:
1:  auipc t0, %pcrel_hi(patch)
    addi  t0, t0, %pcrel_lo(1b)

    # patch <- addi a0, x0, 2  (0x00200513)
    lui   t1, 0x00200
    addi  t1, t1, 0x513
    sw    t1, 0(t0)

    fence.i

    jal   ra, patch
    li    t2, 2
    bne   a0, t2, fail

2:  auipc t0, %pcrel_hi(patch)
    addi  t0, t0, %pcrel_lo(2b)
    # patch <- addi a0, x0, 3  (0x00300513)
    lui   t1, 0x00300
    addi  t1, t1, 0x513
    sw    t1, 0(t0)
    fence.i
    jal   ra, patch
    li    t2, 3
    bne   a0, t2, fail

    li    a0, 0
    ebreak
    j     .

fail:
    li    a0, 1
    ebreak
    j     .

    .align 2
patch:
    addi  a0, x0, 1
    ret
