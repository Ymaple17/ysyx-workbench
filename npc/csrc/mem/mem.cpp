#include <mem/mem.h>
#include <mem/paddr.h>

void mem_t::tick(mem_input_t in, char *name) {
  bool ar_fire = !in.reset && in.ar_valid && ar_ready();
  bool r_fire = !in.reset && in.r_ready && r_valid();
  bool aw_fire = !in.reset && in.aw_valid && aw_ready();
  bool w_fire = !in.reset && in.w_valid && w_ready();
  bool b_fire = !in.reset && in.b_ready && b_valid();

  /* ------------------------ AR ------------------------ */
  if (ar_fire) {
    auto addr = in.ar_addr;
    for (size_t i = 0; i <= in.ar_len; i++, addr += 8) {
      // printf("%s read at " FMT_PADDR "\n", name, addr);
      word_t data;
      paddr_read(addr, &data);
      // uint64_t data = paddr_read(addr, 8);
      // if (addr >= 0x80008fe0 && addr < (0x80008fe0 + 8*4)) {
      //   printf("%s read at " FMT_PADDR ", data = " FMT_WORD "\n", name, addr, data);
      // }
      rresp.push(rresp_t(data, i == in.ar_len));
    }
  }

  /* ------------------------- R ------------------------ */
  if (r_fire) {
    rresp.pop();
  }

  /* ------------------------ WR ------------------------ */
  if (aw_fire) {
    // printf("%s aw_fire\n", name);
    waddr = in.aw_addr;
    wcount = in.aw_len + 1;
    wsize = 1 << in.aw_size;
    storing = true;
  }
  
  /* ------------------------- W ------------------------ */
  if (w_fire) {
    // if (waddr >= 0x80008fe0 && waddr < (0x80008fe0 + 8*4)) {
    //   printf("%s write at " FMT_PADDR ", data = " FMT_WORD ", strb = %x \n", name, waddr, in.w_data, in.w_strb);
    // }
    paddr_write(waddr, in.w_data, in.w_strb);
    waddr += wsize;
    wcount--;
    if (wcount == 0) {
      // printf("%s bresp = true\n", name);
      bresp = true;
      storing = false;
      assert(in.w_last);
    }
  }

  /* ------------------------- B ------------------------ */
  if (b_fire) {
    // printf("%s bresp = false\n", name);
    bresp = false;
  }

  if (in.reset) {
    bresp = false;
    storing = false;
    while (!rresp.empty()) rresp.pop();
  }
}