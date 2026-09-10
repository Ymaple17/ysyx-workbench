module OOODSoCSimProbe (
  input logic        commit_valid0,
  input logic        commit_valid1,
  input logic        commit_valid2,
  input logic        commit_valid3,
  input logic [31:0] commit_pc0,
  input logic [31:0] commit_pc1,
  input logic [31:0] commit_pc2,
  input logic [31:0] commit_pc3,
  input logic [31:0] commit_mem_addr0,
  input logic [31:0] commit_mem_addr1,
  input logic [31:0] commit_mem_addr2,
  input logic [31:0] commit_mem_addr3,
  input logic        commit_is_load0,
  input logic        commit_is_load1,
  input logic        commit_is_load2,
  input logic        commit_is_load3,
  input logic [31:0] arch_rdata_0,
  input logic [31:0] arch_rdata_1,
  input logic [31:0] arch_rdata_2,
  input logic [31:0] arch_rdata_3,
  input logic [31:0] arch_rdata_4,
  input logic [31:0] arch_rdata_5,
  input logic [31:0] arch_rdata_6,
  input logic [31:0] arch_rdata_7,
  input logic [31:0] arch_rdata_8,
  input logic [31:0] arch_rdata_9,
  input logic [31:0] arch_rdata_10,
  input logic [31:0] arch_rdata_11,
  input logic [31:0] arch_rdata_12,
  input logic [31:0] arch_rdata_13,
  input logic [31:0] arch_rdata_14,
  input logic [31:0] arch_rdata_15,
  input logic [31:0] arch_rdata_16,
  input logic [31:0] arch_rdata_17,
  input logic [31:0] arch_rdata_18,
  input logic [31:0] arch_rdata_19,
  input logic [31:0] arch_rdata_20,
  input logic [31:0] arch_rdata_21,
  input logic [31:0] arch_rdata_22,
  input logic [31:0] arch_rdata_23,
  input logic [31:0] arch_rdata_24,
  input logic [31:0] arch_rdata_25,
  input logic [31:0] arch_rdata_26,
  input logic [31:0] arch_rdata_27,
  input logic [31:0] arch_rdata_28,
  input logic [31:0] arch_rdata_29,
  input logic [31:0] arch_rdata_30,
  input logic [31:0] arch_rdata_31
);

  export "DPI-C" function oood_soc_commit_mask;
  function int unsigned oood_soc_commit_mask();
    oood_soc_commit_mask = {28'b0, commit_valid3, commit_valid2,
                            commit_valid1, commit_valid0};
  endfunction

  export "DPI-C" function oood_soc_commit_pc;
  function int unsigned oood_soc_commit_pc(input int unsigned lane);
    case (lane)
      0: oood_soc_commit_pc = commit_pc0;
      1: oood_soc_commit_pc = commit_pc1;
      2: oood_soc_commit_pc = commit_pc2;
      3: oood_soc_commit_pc = commit_pc3;
      default: oood_soc_commit_pc = 32'b0;
    endcase
  endfunction

  export "DPI-C" function oood_soc_commit_mem_addr;
  function int unsigned oood_soc_commit_mem_addr(input int unsigned lane);
    case (lane)
      0: oood_soc_commit_mem_addr = commit_mem_addr0;
      1: oood_soc_commit_mem_addr = commit_mem_addr1;
      2: oood_soc_commit_mem_addr = commit_mem_addr2;
      3: oood_soc_commit_mem_addr = commit_mem_addr3;
      default: oood_soc_commit_mem_addr = 32'b0;
    endcase
  endfunction

  export "DPI-C" function oood_soc_commit_is_load;
  function int unsigned oood_soc_commit_is_load(input int unsigned lane);
    case (lane)
      0: oood_soc_commit_is_load = commit_is_load0;
      1: oood_soc_commit_is_load = commit_is_load1;
      2: oood_soc_commit_is_load = commit_is_load2;
      3: oood_soc_commit_is_load = commit_is_load3;
      default: oood_soc_commit_is_load = 1'b0;
    endcase
  endfunction

  export "DPI-C" function oood_soc_read_gpr;
  function int unsigned oood_soc_read_gpr(input int unsigned index);
    case (index)
      0: oood_soc_read_gpr = arch_rdata_0;
      1: oood_soc_read_gpr = arch_rdata_1;
      2: oood_soc_read_gpr = arch_rdata_2;
      3: oood_soc_read_gpr = arch_rdata_3;
      4: oood_soc_read_gpr = arch_rdata_4;
      5: oood_soc_read_gpr = arch_rdata_5;
      6: oood_soc_read_gpr = arch_rdata_6;
      7: oood_soc_read_gpr = arch_rdata_7;
      8: oood_soc_read_gpr = arch_rdata_8;
      9: oood_soc_read_gpr = arch_rdata_9;
      10: oood_soc_read_gpr = arch_rdata_10;
      11: oood_soc_read_gpr = arch_rdata_11;
      12: oood_soc_read_gpr = arch_rdata_12;
      13: oood_soc_read_gpr = arch_rdata_13;
      14: oood_soc_read_gpr = arch_rdata_14;
      15: oood_soc_read_gpr = arch_rdata_15;
      16: oood_soc_read_gpr = arch_rdata_16;
      17: oood_soc_read_gpr = arch_rdata_17;
      18: oood_soc_read_gpr = arch_rdata_18;
      19: oood_soc_read_gpr = arch_rdata_19;
      20: oood_soc_read_gpr = arch_rdata_20;
      21: oood_soc_read_gpr = arch_rdata_21;
      22: oood_soc_read_gpr = arch_rdata_22;
      23: oood_soc_read_gpr = arch_rdata_23;
      24: oood_soc_read_gpr = arch_rdata_24;
      25: oood_soc_read_gpr = arch_rdata_25;
      26: oood_soc_read_gpr = arch_rdata_26;
      27: oood_soc_read_gpr = arch_rdata_27;
      28: oood_soc_read_gpr = arch_rdata_28;
      29: oood_soc_read_gpr = arch_rdata_29;
      30: oood_soc_read_gpr = arch_rdata_30;
      31: oood_soc_read_gpr = arch_rdata_31;
      default: oood_soc_read_gpr = 32'b0;
    endcase
  endfunction

endmodule

bind ysyx_25020039 OOODSoCSimProbe oood_soc_sim_probe (
  .commit_valid0(io_commit_valid),
  .commit_valid1(io_commit_valid1),
  .commit_valid2(io_commit_valid2),
  .commit_valid3(io_commit_valid3),
  .commit_pc0(io_commit_pc),
  .commit_pc1(io_commit_pc1),
  .commit_pc2(io_commit_pc2),
  .commit_pc3(io_commit_pc3),
  .commit_mem_addr0(io_commit_mem_addr),
  .commit_mem_addr1(io_commit_mem_addr1),
  .commit_mem_addr2(io_commit_mem_addr2),
  .commit_mem_addr3(io_commit_mem_addr3),
  .commit_is_load0(io_commit_is_load),
  .commit_is_load1(io_commit_is_load1),
  .commit_is_load2(io_commit_is_load2),
  .commit_is_load3(io_commit_is_load3),
  .arch_rdata_0(io_arch_rdata_0),
  .arch_rdata_1(io_arch_rdata_1),
  .arch_rdata_2(io_arch_rdata_2),
  .arch_rdata_3(io_arch_rdata_3),
  .arch_rdata_4(io_arch_rdata_4),
  .arch_rdata_5(io_arch_rdata_5),
  .arch_rdata_6(io_arch_rdata_6),
  .arch_rdata_7(io_arch_rdata_7),
  .arch_rdata_8(io_arch_rdata_8),
  .arch_rdata_9(io_arch_rdata_9),
  .arch_rdata_10(io_arch_rdata_10),
  .arch_rdata_11(io_arch_rdata_11),
  .arch_rdata_12(io_arch_rdata_12),
  .arch_rdata_13(io_arch_rdata_13),
  .arch_rdata_14(io_arch_rdata_14),
  .arch_rdata_15(io_arch_rdata_15),
  .arch_rdata_16(io_arch_rdata_16),
  .arch_rdata_17(io_arch_rdata_17),
  .arch_rdata_18(io_arch_rdata_18),
  .arch_rdata_19(io_arch_rdata_19),
  .arch_rdata_20(io_arch_rdata_20),
  .arch_rdata_21(io_arch_rdata_21),
  .arch_rdata_22(io_arch_rdata_22),
  .arch_rdata_23(io_arch_rdata_23),
  .arch_rdata_24(io_arch_rdata_24),
  .arch_rdata_25(io_arch_rdata_25),
  .arch_rdata_26(io_arch_rdata_26),
  .arch_rdata_27(io_arch_rdata_27),
  .arch_rdata_28(io_arch_rdata_28),
  .arch_rdata_29(io_arch_rdata_29),
  .arch_rdata_30(io_arch_rdata_30),
  .arch_rdata_31(io_arch_rdata_31)
);
