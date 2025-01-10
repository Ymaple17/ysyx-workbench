// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Tracing implementation internals
#include "verilated_vcd_c.h"
#include "Vaddr__Syms.h"


VL_ATTR_COLD void Vaddr___024root__trace_init_sub__TOP__0(Vaddr___024root* vlSelf, VerilatedVcd* tracep) {
    if (false && vlSelf) {}  // Prevent unused
    Vaddr__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vaddr___024root__trace_init_sub__TOP__0\n"); );
    // Init
    const int c = vlSymsp->__Vm_baseCode;
    // Body
    tracep->declBit(c+1,"a", false,-1);
    tracep->declBit(c+2,"b", false,-1);
    tracep->declBus(c+3,"sum", false,-1, 1,0);
    tracep->pushNamePrefix("adder ");
    tracep->declBit(c+1,"a", false,-1);
    tracep->declBit(c+2,"b", false,-1);
    tracep->declBus(c+3,"sum", false,-1, 1,0);
    tracep->popNamePrefix(1);
}

VL_ATTR_COLD void Vaddr___024root__trace_init_top(Vaddr___024root* vlSelf, VerilatedVcd* tracep) {
    if (false && vlSelf) {}  // Prevent unused
    Vaddr__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vaddr___024root__trace_init_top\n"); );
    // Body
    Vaddr___024root__trace_init_sub__TOP__0(vlSelf, tracep);
}

VL_ATTR_COLD void Vaddr___024root__trace_full_top_0(void* voidSelf, VerilatedVcd::Buffer* bufp);
void Vaddr___024root__trace_chg_top_0(void* voidSelf, VerilatedVcd::Buffer* bufp);
void Vaddr___024root__trace_cleanup(void* voidSelf, VerilatedVcd* /*unused*/);

VL_ATTR_COLD void Vaddr___024root__trace_register(Vaddr___024root* vlSelf, VerilatedVcd* tracep) {
    if (false && vlSelf) {}  // Prevent unused
    Vaddr__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vaddr___024root__trace_register\n"); );
    // Body
    tracep->addFullCb(&Vaddr___024root__trace_full_top_0, vlSelf);
    tracep->addChgCb(&Vaddr___024root__trace_chg_top_0, vlSelf);
    tracep->addCleanupCb(&Vaddr___024root__trace_cleanup, vlSelf);
}

VL_ATTR_COLD void Vaddr___024root__trace_full_sub_0(Vaddr___024root* vlSelf, VerilatedVcd::Buffer* bufp);

VL_ATTR_COLD void Vaddr___024root__trace_full_top_0(void* voidSelf, VerilatedVcd::Buffer* bufp) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vaddr___024root__trace_full_top_0\n"); );
    // Init
    Vaddr___024root* const __restrict vlSelf VL_ATTR_UNUSED = static_cast<Vaddr___024root*>(voidSelf);
    Vaddr__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    // Body
    Vaddr___024root__trace_full_sub_0((&vlSymsp->TOP), bufp);
}

VL_ATTR_COLD void Vaddr___024root__trace_full_sub_0(Vaddr___024root* vlSelf, VerilatedVcd::Buffer* bufp) {
    if (false && vlSelf) {}  // Prevent unused
    Vaddr__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vaddr___024root__trace_full_sub_0\n"); );
    // Init
    uint32_t* const oldp VL_ATTR_UNUSED = bufp->oldp(vlSymsp->__Vm_baseCode);
    // Body
    bufp->fullBit(oldp+1,(vlSelf->a));
    bufp->fullBit(oldp+2,(vlSelf->b));
    bufp->fullCData(oldp+3,(vlSelf->sum),2);
}
