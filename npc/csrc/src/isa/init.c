#include <isa.h>
#include <memory/paddr.h>


// this is not consistent with uint8_t
// but it is ok since we do not access the array directly
static const uint32_t img [] = {
 // 0x054000ef,
  //0x00112623,	sw
  // 0x00c000ef,
  // 0x00c12083,	//lw
  // 0x00053513,	//seqzq
  0x800002b7,  // lui t0,0x80000
  // 0x00050463,  //beq
  0x0002a023,  // sw  zero,0(t0)
  0x0002a503,  // lw  a0,0(t0)
  // 0x01851513,  //slli a0,a0,0x18
  /* 0x41855513,  //srai a0,a0,0x18 */
  0x00100073,  // ebreak (used as nemu_trap)
};

static void restart(){
    /* Set the initial program counter */
    cpu.pc = RESET_VECTOR;

    /* The zero register is always 0 */
    cpu.gpr[0] = 0;
}

void init_isa(){
    /* Load built-in image. */
    memcpy(guest_to_host(RESET_VECTOR), img, sizeof(img));

    /* Initialize this virtual computer system. */
    restart();
}
