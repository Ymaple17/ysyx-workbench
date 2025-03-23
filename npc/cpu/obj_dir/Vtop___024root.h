// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design internal header
// See Vtop.h for the primary calling header

#ifndef VERILATED_VTOP___024ROOT_H_
#define VERILATED_VTOP___024ROOT_H_  // guard

#include "verilated.h"

class Vtop__Syms;

class Vtop___024root final : public VerilatedModule {
  public:

    // DESIGN SPECIFIC STATE
    VL_IN8(clk,0,0);
    CData/*4:0*/ top__DOT__rs1;
    CData/*4:0*/ top__DOT__rs2;
    CData/*3:0*/ top__DOT__ALUctr;
    CData/*0:0*/ top__DOT__ALUBsrc;
    CData/*0:0*/ top__DOT__less;
    CData/*0:0*/ top__DOT__zero;
    CData/*0:0*/ top__DOT__of;
    CData/*0:0*/ top__DOT__cf;
    CData/*0:0*/ top__DOT__zf;
    CData/*0:0*/ top__DOT__RegWr;
    CData/*0:0*/ top__DOT__PCAsrc;
    CData/*0:0*/ top__DOT__PCBsrc;
    CData/*2:0*/ top__DOT__MemOp;
    CData/*0:0*/ top__DOT__MemtoReg;
    CData/*2:0*/ top__DOT__branch;
    CData/*6:0*/ top__DOT__opcode;
    CData/*4:0*/ __Vtableidx1;
    CData/*4:0*/ __Vdlyvdim0__top__DOT__my_reg__DOT__general_register__v0;
    CData/*0:0*/ __Vdlyvset__top__DOT__my_reg__DOT__general_register__v0;
    CData/*0:0*/ __Vtrigrprev__TOP__clk;
    CData/*4:0*/ __Vtrigrprev__TOP__top__DOT__rs1;
    CData/*4:0*/ __Vtrigrprev__TOP__top__DOT__rs2;
    CData/*0:0*/ __VactDidInit;
    CData/*0:0*/ __VactContinue;
    IData/*31:0*/ top__DOT__instr;
    IData/*31:0*/ top__DOT__pc_next;
    IData/*31:0*/ top__DOT__ALUout;
    IData/*31:0*/ top__DOT__data_out;
    IData/*31:0*/ top__DOT__op1;
    IData/*31:0*/ top__DOT__op2;
    IData/*31:0*/ top__DOT__out;
    IData/*31:0*/ top__DOT__imm;
    IData/*31:0*/ top__DOT__my_id__DOT__immI;
    IData/*31:0*/ top__DOT__my_id__DOT__immS;
    IData/*31:0*/ top__DOT__my_id__DOT__immB;
    IData/*31:0*/ top__DOT__my_pc__DOT__cnt;
    IData/*31:0*/ top__DOT__my_pc__DOT__PCa;
    IData/*31:0*/ top__DOT__my_pc__DOT__PCb;
    IData/*31:0*/ top__DOT__my_alu__DOT__B;
    IData/*31:0*/ top__DOT__my_alu__DOT__xb;
    IData/*31:0*/ __Vdlyvval__top__DOT__my_reg__DOT__general_register__v0;
    IData/*31:0*/ __VstlIterCount;
    IData/*31:0*/ __Vtrigrprev__TOP__top__DOT__out;
    IData/*31:0*/ __VactIterCount;
    VlUnpacked<IData/*31:0*/, 65536> top__DOT__my_instrmem__DOT__instr_memory;
    VlUnpacked<IData/*31:0*/, 32> top__DOT__my_reg__DOT__general_register;
    VlUnpacked<IData/*31:0*/, 65536> top__DOT__my_ram__DOT__data_memory;
    VlUnpacked<CData/*0:0*/, 4> __Vm_traceActivity;
    VlTriggerVec<1> __VstlTriggered;
    VlTriggerVec<3> __VactTriggered;
    VlTriggerVec<3> __VnbaTriggered;

    // INTERNAL VARIABLES
    Vtop__Syms* const vlSymsp;

    // CONSTRUCTORS
    Vtop___024root(Vtop__Syms* symsp, const char* v__name);
    ~Vtop___024root();
    VL_UNCOPYABLE(Vtop___024root);

    // INTERNAL METHODS
    void __Vconfigure(bool first);
} VL_ATTR_ALIGNED(VL_CACHE_LINE_BYTES);


#endif  // guard
