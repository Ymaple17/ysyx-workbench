// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See Vtop.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "Vtop__Syms.h"
#include "Vtop___024root.h"

extern "C" void nemu_trap();

VL_INLINE_OPT void Vtop___024root____Vdpiimwrap_top__DOT__my_id__DOT__nemu_trap_TOP() {
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root____Vdpiimwrap_top__DOT__my_id__DOT__nemu_trap_TOP\n"); );
    // Body
    nemu_trap();
}

#ifdef VL_DEBUG
VL_ATTR_COLD void Vtop___024root___dump_triggers__act(Vtop___024root* vlSelf);
#endif  // VL_DEBUG

void Vtop___024root___eval_triggers__act(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___eval_triggers__act\n"); );
    // Body
    vlSelf->__VactTriggered.at(0U) = ((IData)(vlSelf->clk) 
                                      & (~ (IData)(vlSelf->__Vtrigrprev__TOP__clk)));
    vlSelf->__VactTriggered.at(1U) = (vlSelf->top__DOT__out 
                                      != vlSelf->__Vtrigrprev__TOP__top__DOT__out);
    vlSelf->__VactTriggered.at(2U) = (((IData)(vlSelf->top__DOT__rs1) 
                                       != (IData)(vlSelf->__Vtrigrprev__TOP__top__DOT__rs1)) 
                                      | ((IData)(vlSelf->top__DOT__rs2) 
                                         != (IData)(vlSelf->__Vtrigrprev__TOP__top__DOT__rs2)));
    vlSelf->__Vtrigrprev__TOP__clk = vlSelf->clk;
    vlSelf->__Vtrigrprev__TOP__top__DOT__out = vlSelf->top__DOT__out;
    vlSelf->__Vtrigrprev__TOP__top__DOT__rs1 = vlSelf->top__DOT__rs1;
    vlSelf->__Vtrigrprev__TOP__top__DOT__rs2 = vlSelf->top__DOT__rs2;
    if (VL_UNLIKELY((1U & (~ (IData)(vlSelf->__VactDidInit))))) {
        vlSelf->__VactDidInit = 1U;
        vlSelf->__VactTriggered.at(1U) = 1U;
        vlSelf->__VactTriggered.at(2U) = 1U;
    }
#ifdef VL_DEBUG
    if (VL_UNLIKELY(vlSymsp->_vm_contextp__->debug())) {
        Vtop___024root___dump_triggers__act(vlSelf);
    }
#endif
}
