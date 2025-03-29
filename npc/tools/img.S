.global _start

_start:
  li t0,0x80000000
  li a0,3
  sb a0,0(t0)
  li a1,0
  addi a1,a1,1
  bne a0,a1,0x14
  lb a2,0(t0)
  mv a0,a2
  li a0,0
  ebreak
