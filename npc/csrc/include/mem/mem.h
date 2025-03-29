#pragma once

#include <stdint.h>

#include <cstring>
#include <queue>

struct rresp_t {
  uint64_t data;
  bool last;

  rresp_t(uint64_t data, bool last) : data(data), last(last) {}
  rresp_t() : last(false) {}
};

struct mem_input_t {
  bool reset;

  bool ar_valid;
  uint64_t ar_addr;
  uint64_t ar_size;
  uint64_t ar_len;

  bool aw_valid;
  uint64_t aw_addr;
  uint64_t aw_size;
  uint64_t aw_len;

  bool w_valid;
  uint8_t w_strb;
  uint64_t w_data;
  bool w_last;

  bool r_ready;

  bool b_ready;
};

class mem_t {
 public:
  mem_t() : storing(false), bresp(false) {}
  ~mem_t() {}

  bool ar_ready() { return true; }
  bool aw_ready() { return !storing; }
  bool w_ready() { return storing; }
  bool b_valid() { return bresp; }
  uint64_t b_resp() { return 0; }  // success
  bool r_valid() { return !rresp.empty(); }
  uint64_t r_resp() { return 0; }  // success
  bool r_last() { return r_valid() ? rresp.front().last : false; }
  uint64_t r_data() { return rresp.empty() ? 0 : rresp.front().data; }
  bool has_r_data() { return !rresp.empty(); }

  void tick(mem_input_t in, char* name);

 private:
  bool storing;
  bool bresp;
  std::queue<rresp_t> rresp;
  uint64_t waddr;
  uint64_t wcount;
  uint64_t wsize;
};