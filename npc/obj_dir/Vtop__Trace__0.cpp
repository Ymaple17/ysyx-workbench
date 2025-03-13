// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Tracing implementation internals
#include "verilated_vcd_c.h"
#include "Vtop__Syms.h"


void Vtop___024root__trace_chg_sub_0(Vtop___024root* vlSelf, VerilatedVcd::Buffer* bufp);

void Vtop___024root__trace_chg_top_0(void* voidSelf, VerilatedVcd::Buffer* bufp) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root__trace_chg_top_0\n"); );
    // Init
    Vtop___024root* const __restrict vlSelf VL_ATTR_UNUSED = static_cast<Vtop___024root*>(voidSelf);
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    if (VL_UNLIKELY(!vlSymsp->__Vm_activity)) return;
    // Body
    Vtop___024root__trace_chg_sub_0((&vlSymsp->TOP), bufp);
}

void Vtop___024root__trace_chg_sub_0(Vtop___024root* vlSelf, VerilatedVcd::Buffer* bufp) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root__trace_chg_sub_0\n"); );
    // Init
    uint32_t* const oldp VL_ATTR_UNUSED = bufp->oldp(vlSymsp->__Vm_baseCode + 1);
    // Body
    if (VL_UNLIKELY(vlSelf->__Vm_traceActivity[1U])) {
        bufp->chgIData(oldp+0,(vlSelf->top__DOT__instr),32);
        bufp->chgCData(oldp+1,((0x1fU & (vlSelf->top__DOT__instr 
                                         >> 7U))),5);
        bufp->chgCData(oldp+2,(vlSelf->top__DOT__ALUctr),4);
        bufp->chgBit(oldp+3,(vlSelf->top__DOT__ALUBsrc));
        bufp->chgBit(oldp+4,(vlSelf->top__DOT__RegWr));
        bufp->chgCData(oldp+5,(vlSelf->top__DOT__MemOp),3);
        bufp->chgBit(oldp+6,(vlSelf->top__DOT__MemtoReg));
        bufp->chgIData(oldp+7,(vlSelf->top__DOT__pc_next),32);
        bufp->chgCData(oldp+8,(vlSelf->top__DOT__branch),3);
        bufp->chgBit(oldp+9,((IData)((0x23U == (0x7fU 
                                                & vlSelf->top__DOT__instr)))));
        bufp->chgIData(oldp+10,(vlSelf->top__DOT__data_out),32);
        bufp->chgIData(oldp+11,(vlSelf->top__DOT__imm),32);
        bufp->chgCData(oldp+12,((0x7fU & vlSelf->top__DOT__instr)),7);
        bufp->chgCData(oldp+13,((7U & (vlSelf->top__DOT__instr 
                                       >> 0xcU))),3);
        bufp->chgCData(oldp+14,((vlSelf->top__DOT__instr 
                                 >> 0x19U)),7);
        bufp->chgIData(oldp+15,(vlSelf->top__DOT__my_id__DOT__immI),32);
        bufp->chgIData(oldp+16,(vlSelf->top__DOT__my_id__DOT__immS),32);
        bufp->chgIData(oldp+17,(vlSelf->top__DOT__my_id__DOT__immB),32);
        bufp->chgIData(oldp+18,((((- (IData)((vlSelf->top__DOT__instr 
                                              >> 0x1fU))) 
                                  << 0x14U) | ((0xff000U 
                                                & vlSelf->top__DOT__instr) 
                                               | ((0x800U 
                                                   & (vlSelf->top__DOT__instr 
                                                      >> 9U)) 
                                                  | (0x7feU 
                                                     & (vlSelf->top__DOT__instr 
                                                        >> 0x14U)))))),32);
        bufp->chgIData(oldp+19,(vlSelf->top__DOT__my_pc__DOT__cnt),32);
        bufp->chgIData(oldp+20,(vlSelf->top__DOT__my_pc__DOT__PCa),32);
        bufp->chgIData(oldp+21,(vlSelf->top__DOT__my_pc__DOT__PCb),32);
    }
    if (VL_UNLIKELY(vlSelf->__Vm_traceActivity[2U])) {
        bufp->chgCData(oldp+22,(vlSelf->top__DOT__rs1),5);
        bufp->chgCData(oldp+23,(vlSelf->top__DOT__rs2),5);
        bufp->chgIData(oldp+24,(vlSelf->top__DOT__my_reg__DOT__general_register[0]),32);
        bufp->chgIData(oldp+25,(vlSelf->top__DOT__my_reg__DOT__general_register[1]),32);
        bufp->chgIData(oldp+26,(vlSelf->top__DOT__my_reg__DOT__general_register[2]),32);
        bufp->chgIData(oldp+27,(vlSelf->top__DOT__my_reg__DOT__general_register[3]),32);
        bufp->chgIData(oldp+28,(vlSelf->top__DOT__my_reg__DOT__general_register[4]),32);
        bufp->chgIData(oldp+29,(vlSelf->top__DOT__my_reg__DOT__general_register[5]),32);
        bufp->chgIData(oldp+30,(vlSelf->top__DOT__my_reg__DOT__general_register[6]),32);
        bufp->chgIData(oldp+31,(vlSelf->top__DOT__my_reg__DOT__general_register[7]),32);
        bufp->chgIData(oldp+32,(vlSelf->top__DOT__my_reg__DOT__general_register[8]),32);
        bufp->chgIData(oldp+33,(vlSelf->top__DOT__my_reg__DOT__general_register[9]),32);
        bufp->chgIData(oldp+34,(vlSelf->top__DOT__my_reg__DOT__general_register[10]),32);
        bufp->chgIData(oldp+35,(vlSelf->top__DOT__my_reg__DOT__general_register[11]),32);
        bufp->chgIData(oldp+36,(vlSelf->top__DOT__my_reg__DOT__general_register[12]),32);
        bufp->chgIData(oldp+37,(vlSelf->top__DOT__my_reg__DOT__general_register[13]),32);
        bufp->chgIData(oldp+38,(vlSelf->top__DOT__my_reg__DOT__general_register[14]),32);
        bufp->chgIData(oldp+39,(vlSelf->top__DOT__my_reg__DOT__general_register[15]),32);
        bufp->chgIData(oldp+40,(vlSelf->top__DOT__my_reg__DOT__general_register[16]),32);
        bufp->chgIData(oldp+41,(vlSelf->top__DOT__my_reg__DOT__general_register[17]),32);
        bufp->chgIData(oldp+42,(vlSelf->top__DOT__my_reg__DOT__general_register[18]),32);
        bufp->chgIData(oldp+43,(vlSelf->top__DOT__my_reg__DOT__general_register[19]),32);
        bufp->chgIData(oldp+44,(vlSelf->top__DOT__my_reg__DOT__general_register[20]),32);
        bufp->chgIData(oldp+45,(vlSelf->top__DOT__my_reg__DOT__general_register[21]),32);
        bufp->chgIData(oldp+46,(vlSelf->top__DOT__my_reg__DOT__general_register[22]),32);
        bufp->chgIData(oldp+47,(vlSelf->top__DOT__my_reg__DOT__general_register[23]),32);
        bufp->chgIData(oldp+48,(vlSelf->top__DOT__my_reg__DOT__general_register[24]),32);
        bufp->chgIData(oldp+49,(vlSelf->top__DOT__my_reg__DOT__general_register[25]),32);
        bufp->chgIData(oldp+50,(vlSelf->top__DOT__my_reg__DOT__general_register[26]),32);
        bufp->chgIData(oldp+51,(vlSelf->top__DOT__my_reg__DOT__general_register[27]),32);
        bufp->chgIData(oldp+52,(vlSelf->top__DOT__my_reg__DOT__general_register[28]),32);
        bufp->chgIData(oldp+53,(vlSelf->top__DOT__my_reg__DOT__general_register[29]),32);
        bufp->chgIData(oldp+54,(vlSelf->top__DOT__my_reg__DOT__general_register[30]),32);
        bufp->chgIData(oldp+55,(vlSelf->top__DOT__my_reg__DOT__general_register[31]),32);
    }
    if (VL_UNLIKELY(vlSelf->__Vm_traceActivity[3U])) {
        bufp->chgBit(oldp+56,(vlSelf->top__DOT__less));
        bufp->chgBit(oldp+57,(vlSelf->top__DOT__zero));
        bufp->chgBit(oldp+58,(vlSelf->top__DOT__of));
        bufp->chgBit(oldp+59,(vlSelf->top__DOT__cf));
        bufp->chgBit(oldp+60,(vlSelf->top__DOT__zf));
        bufp->chgBit(oldp+61,(vlSelf->top__DOT__PCAsrc));
        bufp->chgBit(oldp+62,(vlSelf->top__DOT__PCBsrc));
        bufp->chgIData(oldp+63,(vlSelf->top__DOT__ALUout),32);
        bufp->chgIData(oldp+64,(vlSelf->top__DOT__out),32);
        bufp->chgIData(oldp+65,(vlSelf->top__DOT__my_alu__DOT__B),32);
        bufp->chgIData(oldp+66,(vlSelf->top__DOT__my_alu__DOT__xb),32);
    }
    bufp->chgBit(oldp+67,(vlSelf->clk));
    bufp->chgIData(oldp+68,(vlSelf->top__DOT__op1),32);
    bufp->chgIData(oldp+69,(vlSelf->top__DOT__op2),32);
}

void Vtop___024root__trace_cleanup(void* voidSelf, VerilatedVcd* /*unused*/) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root__trace_cleanup\n"); );
    // Init
    Vtop___024root* const __restrict vlSelf VL_ATTR_UNUSED = static_cast<Vtop___024root*>(voidSelf);
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    // Body
    vlSymsp->__Vm_activity = false;
    vlSymsp->TOP.__Vm_traceActivity[0U] = 0U;
    vlSymsp->TOP.__Vm_traceActivity[1U] = 0U;
    vlSymsp->TOP.__Vm_traceActivity[2U] = 0U;
    vlSymsp->TOP.__Vm_traceActivity[3U] = 0U;
}
