.section .text
.globl _start
.type _start, @function
_start:
  lui   t0, 0x10000
  addi  t1, zero, 72
  sw    t1, 0(t0)
  addi  t1, zero, 69
  sw    t1, 0(t0)
  addi  t1, zero, 76
  sw    t1, 0(t0)
  addi  t1, zero, 76
  sw    t1, 0(t0)
  addi  t1, zero, 79
  sw    t1, 0(t0)
  addi  t1, zero, 10
  sw    t1, 0(t0)
  addi  t2, zero, 1
loop:
  sw    t2, 4(t0)
  xori  t2, t2, 3
  jal   zero, loop
