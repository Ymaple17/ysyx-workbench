// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Tracing implementation internals
#include "verilated_vcd_c.h"
#include "Vtop__Syms.h"


VL_ATTR_COLD void Vtop___024root__trace_init_sub__TOP__0(Vtop___024root* vlSelf, VerilatedVcd* tracep) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root__trace_init_sub__TOP__0\n"); );
    // Init
    const int c = vlSymsp->__Vm_baseCode;
    // Body
    tracep->declBit(c+68,"clk", false,-1);
    tracep->pushNamePrefix("top ");
    tracep->declBit(c+68,"clk", false,-1);
    tracep->declBus(c+1,"instr", false,-1, 31,0);
    tracep->declBus(c+23,"rs1", false,-1, 4,0);
    tracep->declBus(c+24,"rs2", false,-1, 4,0);
    tracep->declBus(c+2,"rd", false,-1, 4,0);
    tracep->declBus(c+3,"ALUctr", false,-1, 3,0);
    tracep->declBit(c+4,"ALUBsrc", false,-1);
    tracep->declBit(c+57,"less", false,-1);
    tracep->declBit(c+58,"zero", false,-1);
    tracep->declBit(c+59,"of", false,-1);
    tracep->declBit(c+60,"cf", false,-1);
    tracep->declBit(c+61,"zf", false,-1);
    tracep->declBit(c+5,"RegWr", false,-1);
    tracep->declBit(c+62,"PCAsrc", false,-1);
    tracep->declBit(c+63,"PCBsrc", false,-1);
    tracep->declBus(c+6,"MemOp", false,-1, 2,0);
    tracep->declBit(c+7,"MemtoReg", false,-1);
    tracep->declBus(c+8,"pc_next", false,-1, 31,0);
    tracep->declBus(c+9,"branch", false,-1, 2,0);
    tracep->declBus(c+64,"ALUout", false,-1, 31,0);
    tracep->declBit(c+10,"MemWr", false,-1);
    tracep->declBus(c+11,"data_out", false,-1, 31,0);
    tracep->declBus(c+71,"opcode", false,-1, 6,0);
    tracep->declBus(c+69,"op1", false,-1, 31,0);
    tracep->declBus(c+70,"op2", false,-1, 31,0);
    tracep->declBus(c+65,"out", false,-1, 31,0);
    tracep->declBus(c+12,"imm", false,-1, 31,0);
    tracep->pushNamePrefix("my_alu ");
    tracep->declBus(c+69,"A", false,-1, 31,0);
    tracep->declBus(c+70,"rs2", false,-1, 31,0);
    tracep->declBus(c+12,"imm", false,-1, 31,0);
    tracep->declBus(c+3,"ALUctr", false,-1, 3,0);
    tracep->declBit(c+4,"ALUBsrc", false,-1);
    tracep->declBit(c+57,"less", false,-1);
    tracep->declBit(c+58,"zero", false,-1);
    tracep->declBus(c+64,"ALUout", false,-1, 31,0);
    tracep->declBit(c+59,"of", false,-1);
    tracep->declBit(c+61,"zf", false,-1);
    tracep->declBit(c+60,"cf", false,-1);
    tracep->declBus(c+66,"B", false,-1, 31,0);
    tracep->declBus(c+67,"xb", false,-1, 31,0);
    tracep->popNamePrefix(1);
    tracep->pushNamePrefix("my_branch ");
    tracep->declBit(c+61,"zf", false,-1);
    tracep->declBit(c+57,"less", false,-1);
    tracep->declBit(c+58,"zero", false,-1);
    tracep->declBus(c+9,"branch", false,-1, 2,0);
    tracep->declBit(c+62,"PCAsrc", false,-1);
    tracep->declBit(c+63,"PCBsrc", false,-1);
    tracep->popNamePrefix(1);
    tracep->pushNamePrefix("my_id ");
    tracep->declBus(c+1,"instr", false,-1, 31,0);
    tracep->declBus(c+12,"imm", false,-1, 31,0);
    tracep->declBus(c+3,"ALUctr", false,-1, 3,0);
    tracep->declBit(c+4,"ALUBsrc", false,-1);
    tracep->declBit(c+5,"RegWr", false,-1);
    tracep->declBus(c+9,"branch", false,-1, 2,0);
    tracep->declBit(c+7,"MemtoReg", false,-1);
    tracep->declBus(c+6,"MemOp", false,-1, 2,0);
    tracep->declBit(c+10,"MemWr", false,-1);
    tracep->declBus(c+13,"opcode", false,-1, 6,0);
    tracep->declBus(c+14,"func3", false,-1, 2,0);
    tracep->declBus(c+15,"func7", false,-1, 6,0);
    tracep->declBus(c+16,"immI", false,-1, 31,0);
    tracep->declBus(c+17,"immS", false,-1, 31,0);
    tracep->declBus(c+18,"immB", false,-1, 31,0);
    tracep->declBus(c+19,"immJ", false,-1, 31,0);
    tracep->popNamePrefix(1);
    tracep->pushNamePrefix("my_instrmem ");
    tracep->declBus(c+8,"instr_addr", false,-1, 31,0);
    tracep->declBus(c+1,"instr", false,-1, 31,0);
    tracep->popNamePrefix(1);
    tracep->pushNamePrefix("my_pc ");
    tracep->declBit(c+68,"clk", false,-1);
    tracep->declBus(c+12,"imm", false,-1, 31,0);
    tracep->declBus(c+69,"op1", false,-1, 31,0);
    tracep->declBit(c+62,"PCAsrc", false,-1);
    tracep->declBit(c+63,"PCBsrc", false,-1);
    tracep->declBus(c+8,"pc_next", false,-1, 31,0);
    tracep->declBus(c+20,"cnt", false,-1, 31,0);
    tracep->declBus(c+21,"PCa", false,-1, 31,0);
    tracep->declBus(c+22,"PCb", false,-1, 31,0);
    tracep->popNamePrefix(1);
    tracep->pushNamePrefix("my_ram ");
    tracep->declBit(c+68,"Rdclk", false,-1);
    tracep->declBit(c+68,"Wrclk", false,-1);
    tracep->declBus(c+64,"Addr", false,-1, 31,0);
    tracep->declBus(c+6,"MemOp", false,-1, 2,0);
    tracep->declBus(c+70,"data_in", false,-1, 31,0);
    tracep->declBit(c+10,"Wr_en", false,-1);
    tracep->declBus(c+11,"data_out", false,-1, 31,0);
    tracep->popNamePrefix(1);
    tracep->pushNamePrefix("my_reg ");
    tracep->declBit(c+68,"Wrclk", false,-1);
    tracep->declBit(c+5,"RegWr", false,-1);
    tracep->declBus(c+23,"Ra", false,-1, 4,0);
    tracep->declBus(c+24,"Rb", false,-1, 4,0);
    tracep->declBus(c+2,"Rw", false,-1, 4,0);
    tracep->declBus(c+69,"busA", false,-1, 31,0);
    tracep->declBus(c+70,"busB", false,-1, 31,0);
    tracep->declBus(c+65,"busW", false,-1, 31,0);
    for (int i = 0; i < 32; ++i) {
        tracep->declBus(c+25+i*1,"general_register", true,(i+0), 31,0);
    }
    tracep->popNamePrefix(2);
}

VL_ATTR_COLD void Vtop___024root__trace_init_top(Vtop___024root* vlSelf, VerilatedVcd* tracep) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root__trace_init_top\n"); );
    // Body
    Vtop___024root__trace_init_sub__TOP__0(vlSelf, tracep);
}

VL_ATTR_COLD void Vtop___024root__trace_full_top_0(void* voidSelf, VerilatedVcd::Buffer* bufp);
void Vtop___024root__trace_chg_top_0(void* voidSelf, VerilatedVcd::Buffer* bufp);
void Vtop___024root__trace_cleanup(void* voidSelf, VerilatedVcd* /*unused*/);

VL_ATTR_COLD void Vtop___024root__trace_register(Vtop___024root* vlSelf, VerilatedVcd* tracep) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root__trace_register\n"); );
    // Body
    tracep->addFullCb(&Vtop___024root__trace_full_top_0, vlSelf);
    tracep->addChgCb(&Vtop___024root__trace_chg_top_0, vlSelf);
    tracep->addCleanupCb(&Vtop___024root__trace_cleanup, vlSelf);
}

VL_ATTR_COLD void Vtop___024root__trace_full_sub_0(Vtop___024root* vlSelf, VerilatedVcd::Buffer* bufp);

VL_ATTR_COLD void Vtop___024root__trace_full_top_0(void* voidSelf, VerilatedVcd::Buffer* bufp) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root__trace_full_top_0\n"); );
    // Init
    Vtop___024root* const __restrict vlSelf VL_ATTR_UNUSED = static_cast<Vtop___024root*>(voidSelf);
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    // Body
    Vtop___024root__trace_full_sub_0((&vlSymsp->TOP), bufp);
}

VL_ATTR_COLD void Vtop___024root__trace_full_sub_0(Vtop___024root* vlSelf, VerilatedVcd::Buffer* bufp) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root__trace_full_sub_0\n"); );
    // Init
    uint32_t* const oldp VL_ATTR_UNUSED = bufp->oldp(vlSymsp->__Vm_baseCode);
    // Body
    bufp->fullIData(oldp+1,(vlSelf->top__DOT__instr),32);
    bufp->fullCData(oldp+2,((0x1fU & (vlSelf->top__DOT__instr 
                                      >> 7U))),5);
    bufp->fullCData(oldp+3,(vlSelf->top__DOT__ALUctr),4);
    bufp->fullBit(oldp+4,(vlSelf->top__DOT__ALUBsrc));
    bufp->fullBit(oldp+5,(vlSelf->top__DOT__RegWr));
    bufp->fullCData(oldp+6,(vlSelf->top__DOT__MemOp),3);
    bufp->fullBit(oldp+7,(vlSelf->top__DOT__MemtoReg));
    bufp->fullIData(oldp+8,(vlSelf->top__DOT__pc_next),32);
    bufp->fullCData(oldp+9,(vlSelf->top__DOT__branch),3);
    bufp->fullBit(oldp+10,((IData)((0x23U == (0x7fU 
                                              & vlSelf->top__DOT__instr)))));
    bufp->fullIData(oldp+11,(vlSelf->top__DOT__data_out),32);
    bufp->fullIData(oldp+12,(vlSelf->top__DOT__imm),32);
    bufp->fullCData(oldp+13,((0x7fU & vlSelf->top__DOT__instr)),7);
    bufp->fullCData(oldp+14,((7U & (vlSelf->top__DOT__instr 
                                    >> 0xcU))),3);
    bufp->fullCData(oldp+15,((vlSelf->top__DOT__instr 
                              >> 0x19U)),7);
    bufp->fullIData(oldp+16,(vlSelf->top__DOT__my_id__DOT__immI),32);
    bufp->fullIData(oldp+17,(vlSelf->top__DOT__my_id__DOT__immS),32);
    bufp->fullIData(oldp+18,(vlSelf->top__DOT__my_id__DOT__immB),32);
    bufp->fullIData(oldp+19,((((- (IData)((vlSelf->top__DOT__instr 
                                           >> 0x1fU))) 
                               << 0x14U) | ((0xff000U 
                                             & vlSelf->top__DOT__instr) 
                                            | ((0x800U 
                                                & (vlSelf->top__DOT__instr 
                                                   >> 9U)) 
                                               | (0x7feU 
                                                  & (vlSelf->top__DOT__instr 
                                                     >> 0x14U)))))),32);
    bufp->fullIData(oldp+20,(vlSelf->top__DOT__my_pc__DOT__cnt),32);
    bufp->fullIData(oldp+21,(vlSelf->top__DOT__my_pc__DOT__PCa),32);
    bufp->fullIData(oldp+22,(vlSelf->top__DOT__my_pc__DOT__PCb),32);
    bufp->fullCData(oldp+23,(vlSelf->top__DOT__rs1),5);
    bufp->fullCData(oldp+24,(vlSelf->top__DOT__rs2),5);
    bufp->fullIData(oldp+25,(vlSelf->top__DOT__my_reg__DOT__general_register[0]),32);
    bufp->fullIData(oldp+26,(vlSelf->top__DOT__my_reg__DOT__general_register[1]),32);
    bufp->fullIData(oldp+27,(vlSelf->top__DOT__my_reg__DOT__general_register[2]),32);
    bufp->fullIData(oldp+28,(vlSelf->top__DOT__my_reg__DOT__general_register[3]),32);
    bufp->fullIData(oldp+29,(vlSelf->top__DOT__my_reg__DOT__general_register[4]),32);
    bufp->fullIData(oldp+30,(vlSelf->top__DOT__my_reg__DOT__general_register[5]),32);
    bufp->fullIData(oldp+31,(vlSelf->top__DOT__my_reg__DOT__general_register[6]),32);
    bufp->fullIData(oldp+32,(vlSelf->top__DOT__my_reg__DOT__general_register[7]),32);
    bufp->fullIData(oldp+33,(vlSelf->top__DOT__my_reg__DOT__general_register[8]),32);
    bufp->fullIData(oldp+34,(vlSelf->top__DOT__my_reg__DOT__general_register[9]),32);
    bufp->fullIData(oldp+35,(vlSelf->top__DOT__my_reg__DOT__general_register[10]),32);
    bufp->fullIData(oldp+36,(vlSelf->top__DOT__my_reg__DOT__general_register[11]),32);
    bufp->fullIData(oldp+37,(vlSelf->top__DOT__my_reg__DOT__general_register[12]),32);
    bufp->fullIData(oldp+38,(vlSelf->top__DOT__my_reg__DOT__general_register[13]),32);
    bufp->fullIData(oldp+39,(vlSelf->top__DOT__my_reg__DOT__general_register[14]),32);
    bufp->fullIData(oldp+40,(vlSelf->top__DOT__my_reg__DOT__general_register[15]),32);
    bufp->fullIData(oldp+41,(vlSelf->top__DOT__my_reg__DOT__general_register[16]),32);
    bufp->fullIData(oldp+42,(vlSelf->top__DOT__my_reg__DOT__general_register[17]),32);
    bufp->fullIData(oldp+43,(vlSelf->top__DOT__my_reg__DOT__general_register[18]),32);
    bufp->fullIData(oldp+44,(vlSelf->top__DOT__my_reg__DOT__general_register[19]),32);
    bufp->fullIData(oldp+45,(vlSelf->top__DOT__my_reg__DOT__general_register[20]),32);
    bufp->fullIData(oldp+46,(vlSelf->top__DOT__my_reg__DOT__general_register[21]),32);
    bufp->fullIData(oldp+47,(vlSelf->top__DOT__my_reg__DOT__general_register[22]),32);
    bufp->fullIData(oldp+48,(vlSelf->top__DOT__my_reg__DOT__general_register[23]),32);
    bufp->fullIData(oldp+49,(vlSelf->top__DOT__my_reg__DOT__general_register[24]),32);
    bufp->fullIData(oldp+50,(vlSelf->top__DOT__my_reg__DOT__general_register[25]),32);
    bufp->fullIData(oldp+51,(vlSelf->top__DOT__my_reg__DOT__general_register[26]),32);
    bufp->fullIData(oldp+52,(vlSelf->top__DOT__my_reg__DOT__general_register[27]),32);
    bufp->fullIData(oldp+53,(vlSelf->top__DOT__my_reg__DOT__general_register[28]),32);
    bufp->fullIData(oldp+54,(vlSelf->top__DOT__my_reg__DOT__general_register[29]),32);
    bufp->fullIData(oldp+55,(vlSelf->top__DOT__my_reg__DOT__general_register[30]),32);
    bufp->fullIData(oldp+56,(vlSelf->top__DOT__my_reg__DOT__general_register[31]),32);
    bufp->fullBit(oldp+57,(vlSelf->top__DOT__less));
    bufp->fullBit(oldp+58,(vlSelf->top__DOT__zero));
    bufp->fullBit(oldp+59,(vlSelf->top__DOT__of));
    bufp->fullBit(oldp+60,(vlSelf->top__DOT__cf));
    bufp->fullBit(oldp+61,(vlSelf->top__DOT__zf));
    bufp->fullBit(oldp+62,(vlSelf->top__DOT__PCAsrc));
    bufp->fullBit(oldp+63,(vlSelf->top__DOT__PCBsrc));
    bufp->fullIData(oldp+64,(vlSelf->top__DOT__ALUout),32);
    bufp->fullIData(oldp+65,(vlSelf->top__DOT__out),32);
    bufp->fullIData(oldp+66,(vlSelf->top__DOT__my_alu__DOT__B),32);
    bufp->fullIData(oldp+67,(vlSelf->top__DOT__my_alu__DOT__xb),32);
    bufp->fullBit(oldp+68,(vlSelf->clk));
    bufp->fullIData(oldp+69,(vlSelf->top__DOT__op1),32);
    bufp->fullIData(oldp+70,(vlSelf->top__DOT__op2),32);
    bufp->fullCData(oldp+71,(vlSelf->top__DOT__opcode),7);
}
