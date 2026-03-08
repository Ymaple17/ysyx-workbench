// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See VysyxSoCFull.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "VysyxSoCFull__Syms.h"
#include "VysyxSoCFull___024root.h"

extern "C" void sim_exit();

VL_INLINE_OPT void VysyxSoCFull___024root____Vdpiimwrap_ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu__DOT__ebreak__DOT__sim_exit_TOP() {
    VL_DEBUG_IF(VL_DBG_MSGF("+    VysyxSoCFull___024root____Vdpiimwrap_ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu__DOT__ebreak__DOT__sim_exit_TOP\n"); );
    // Body
    sim_exit();
}

extern "C" void mrom_read(int raddr, int* rdata);

VL_INLINE_OPT void VysyxSoCFull___024root____Vdpiimwrap_ysyxSoCFull__DOT__asic__DOT__lmrom__DOT__mrom__DOT__mrom_read_TOP(IData/*31:0*/ raddr, IData/*31:0*/ &rdata) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    VysyxSoCFull___024root____Vdpiimwrap_ysyxSoCFull__DOT__asic__DOT__lmrom__DOT__mrom__DOT__mrom_read_TOP\n"); );
    // Body
    int raddr__Vcvt;
    for (size_t raddr__Vidx = 0; raddr__Vidx < 1; ++raddr__Vidx) raddr__Vcvt = raddr;
    int rdata__Vcvt;
    mrom_read(raddr__Vcvt, &rdata__Vcvt);
    rdata = rdata__Vcvt;
}

#ifdef VL_DEBUG
VL_ATTR_COLD void VysyxSoCFull___024root___dump_triggers__ico(VysyxSoCFull___024root* vlSelf);
#endif  // VL_DEBUG

void VysyxSoCFull___024root___eval_triggers__ico(VysyxSoCFull___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VysyxSoCFull__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VysyxSoCFull___024root___eval_triggers__ico\n"); );
    // Body
    vlSelf->__VicoTriggered.at(0U) = (0U == vlSelf->__VicoIterCount);
#ifdef VL_DEBUG
    if (VL_UNLIKELY(vlSymsp->_vm_contextp__->debug())) {
        VysyxSoCFull___024root___dump_triggers__ico(vlSelf);
    }
#endif
}

#ifdef VL_DEBUG
VL_ATTR_COLD void VysyxSoCFull___024root___dump_triggers__act(VysyxSoCFull___024root* vlSelf);
#endif  // VL_DEBUG

void VysyxSoCFull___024root___eval_triggers__act(VysyxSoCFull___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VysyxSoCFull__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VysyxSoCFull___024root___eval_triggers__act\n"); );
    // Body
    vlSelf->__VactTriggered.at(0U) = ((IData)(vlSelf->clock) 
                                      & (~ (IData)(vlSelf->__Vtrigrprev__TOP__clock)));
    vlSelf->__VactTriggered.at(1U) = (((IData)(vlSelf->clock) 
                                       & (~ (IData)(vlSelf->__Vtrigrprev__TOP__clock))) 
                                      | ((IData)(vlSelf->reset) 
                                         & (~ (IData)(vlSelf->__Vtrigrprev__TOP__reset))));
    vlSelf->__VactTriggered.at(2U) = (((IData)(vlSelf->ysyxSoCFull__DOT____Vcellinp__flash__ss) 
                                       & (~ (IData)(vlSelf->__Vtrigrprev__TOP__ysyxSoCFull__DOT____Vcellinp__flash__ss))) 
                                      | ((IData)(vlSelf->ysyxSoCFull__DOT___asic_spi_sck) 
                                         & (~ (IData)(vlSelf->__Vtrigrprev__TOP__ysyxSoCFull__DOT___asic_spi_sck))));
    vlSelf->__VactTriggered.at(3U) = ((IData)(vlSelf->ysyxSoCFull__DOT___asic_spi_sck) 
                                      & (~ (IData)(vlSelf->__Vtrigrprev__TOP__ysyxSoCFull__DOT___asic_spi_sck)));
    vlSelf->__VactTriggered.at(4U) = (((IData)(vlSelf->ysyxSoCFull__DOT____Vcellinp__bitrev__ss) 
                                       & (~ (IData)(vlSelf->__Vtrigrprev__TOP__ysyxSoCFull__DOT____Vcellinp__bitrev__ss))) 
                                      | ((~ (IData)(vlSelf->ysyxSoCFull__DOT___asic_spi_sck)) 
                                         & (IData)(vlSelf->__Vtrigrprev__TOP__ysyxSoCFull__DOT___asic_spi_sck)));
    vlSelf->__VactTriggered.at(5U) = (((IData)(vlSelf->ysyxSoCFull__DOT____Vcellinp__bitrev__ss) 
                                       & (~ (IData)(vlSelf->__Vtrigrprev__TOP__ysyxSoCFull__DOT____Vcellinp__bitrev__ss))) 
                                      | ((IData)(vlSelf->ysyxSoCFull__DOT___asic_spi_sck) 
                                         & (~ (IData)(vlSelf->__Vtrigrprev__TOP__ysyxSoCFull__DOT___asic_spi_sck))));
    vlSelf->__VactTriggered.at(6U) = (((IData)(vlSelf->ysyxSoCFull__DOT___asic_psram_ce_n) 
                                       & (~ (IData)(vlSelf->__Vtrigrprev__TOP__ysyxSoCFull__DOT___asic_psram_ce_n))) 
                                      | ((IData)(vlSelf->ysyxSoCFull__DOT___asic_psram_sck) 
                                         & (~ (IData)(vlSelf->__Vtrigrprev__TOP__ysyxSoCFull__DOT___asic_psram_sck))));
    vlSelf->__VactTriggered.at(7U) = ((~ (IData)(vlSelf->clock)) 
                                      & (IData)(vlSelf->__Vtrigrprev__TOP__clock));
    vlSelf->__Vtrigrprev__TOP__clock = vlSelf->clock;
    vlSelf->__Vtrigrprev__TOP__reset = vlSelf->reset;
    vlSelf->__Vtrigrprev__TOP__ysyxSoCFull__DOT____Vcellinp__flash__ss 
        = vlSelf->ysyxSoCFull__DOT____Vcellinp__flash__ss;
    vlSelf->__Vtrigrprev__TOP__ysyxSoCFull__DOT___asic_spi_sck 
        = vlSelf->ysyxSoCFull__DOT___asic_spi_sck;
    vlSelf->__Vtrigrprev__TOP__ysyxSoCFull__DOT____Vcellinp__bitrev__ss 
        = vlSelf->ysyxSoCFull__DOT____Vcellinp__bitrev__ss;
    vlSelf->__Vtrigrprev__TOP__ysyxSoCFull__DOT___asic_psram_ce_n 
        = vlSelf->ysyxSoCFull__DOT___asic_psram_ce_n;
    vlSelf->__Vtrigrprev__TOP__ysyxSoCFull__DOT___asic_psram_sck 
        = vlSelf->ysyxSoCFull__DOT___asic_psram_sck;
#ifdef VL_DEBUG
    if (VL_UNLIKELY(vlSymsp->_vm_contextp__->debug())) {
        VysyxSoCFull___024root___dump_triggers__act(vlSelf);
    }
#endif
}

VL_INLINE_OPT void VysyxSoCFull___024root___nba_sequent__TOP__0(VysyxSoCFull___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VysyxSoCFull__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VysyxSoCFull___024root___nba_sequent__TOP__0\n"); );
    // Init
    SData/*15:0*/ ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__unnamedblk1__DOT___GEN_4;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__unnamedblk1__DOT___GEN_4 = 0;
    SData/*15:0*/ ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__unnamedblk1__DOT___GEN_5;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__unnamedblk1__DOT___GEN_5 = 0;
    CData/*0:0*/ ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap__DOT__unnamedblk1__DOT__do_enq;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap__DOT__unnamedblk1__DOT__do_enq = 0;
    CData/*0:0*/ ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_1__DOT__unnamedblk1__DOT__do_enq;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_1__DOT__unnamedblk1__DOT__do_enq = 0;
    CData/*0:0*/ ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_2__DOT__unnamedblk1__DOT__do_enq;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_2__DOT__unnamedblk1__DOT__do_enq = 0;
    CData/*0:0*/ ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_3__DOT__unnamedblk1__DOT__do_enq;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_3__DOT__unnamedblk1__DOT__do_enq = 0;
    CData/*0:0*/ ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_4__DOT__unnamedblk1__DOT__do_enq;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_4__DOT__unnamedblk1__DOT__do_enq = 0;
    CData/*0:0*/ ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_5__DOT__unnamedblk1__DOT__do_enq;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_5__DOT__unnamedblk1__DOT__do_enq = 0;
    CData/*0:0*/ ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_6__DOT__unnamedblk1__DOT__do_enq;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_6__DOT__unnamedblk1__DOT__do_enq = 0;
    CData/*0:0*/ ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_7__DOT__unnamedblk1__DOT__do_enq;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_7__DOT__unnamedblk1__DOT__do_enq = 0;
    CData/*0:0*/ ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_8__DOT__unnamedblk1__DOT__do_enq;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_8__DOT__unnamedblk1__DOT__do_enq = 0;
    CData/*0:0*/ ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_9__DOT__unnamedblk1__DOT__do_enq;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_9__DOT__unnamedblk1__DOT__do_enq = 0;
    CData/*0:0*/ ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_10__DOT__unnamedblk1__DOT__do_enq;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_10__DOT__unnamedblk1__DOT__do_enq = 0;
    CData/*0:0*/ ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_11__DOT__unnamedblk1__DOT__do_enq;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_11__DOT__unnamedblk1__DOT__do_enq = 0;
    CData/*0:0*/ ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_12__DOT__unnamedblk1__DOT__do_enq;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_12__DOT__unnamedblk1__DOT__do_enq = 0;
    CData/*0:0*/ ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_13__DOT__unnamedblk1__DOT__do_enq;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_13__DOT__unnamedblk1__DOT__do_enq = 0;
    CData/*0:0*/ ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_14__DOT__unnamedblk1__DOT__do_enq;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_14__DOT__unnamedblk1__DOT__do_enq = 0;
    CData/*0:0*/ ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_15__DOT__unnamedblk1__DOT__do_enq;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_15__DOT__unnamedblk1__DOT__do_enq = 0;
    CData/*0:0*/ ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_16__DOT__unnamedblk1__DOT__do_enq;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_16__DOT__unnamedblk1__DOT__do_enq = 0;
    CData/*0:0*/ ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_17__DOT__unnamedblk1__DOT__do_enq;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_17__DOT__unnamedblk1__DOT__do_enq = 0;
    CData/*0:0*/ ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_18__DOT__unnamedblk1__DOT__do_enq;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_18__DOT__unnamedblk1__DOT__do_enq = 0;
    CData/*0:0*/ ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_19__DOT__unnamedblk1__DOT__do_enq;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_19__DOT__unnamedblk1__DOT__do_enq = 0;
    CData/*0:0*/ ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_20__DOT__unnamedblk1__DOT__do_enq;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_20__DOT__unnamedblk1__DOT__do_enq = 0;
    CData/*0:0*/ ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_21__DOT__unnamedblk1__DOT__do_enq;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_21__DOT__unnamedblk1__DOT__do_enq = 0;
    CData/*0:0*/ ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_22__DOT__unnamedblk1__DOT__do_enq;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_22__DOT__unnamedblk1__DOT__do_enq = 0;
    CData/*0:0*/ ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_23__DOT__unnamedblk1__DOT__do_enq;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_23__DOT__unnamedblk1__DOT__do_enq = 0;
    CData/*0:0*/ ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_24__DOT__unnamedblk1__DOT__do_enq;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_24__DOT__unnamedblk1__DOT__do_enq = 0;
    CData/*0:0*/ ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_25__DOT__unnamedblk1__DOT__do_enq;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_25__DOT__unnamedblk1__DOT__do_enq = 0;
    CData/*0:0*/ ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_26__DOT__unnamedblk1__DOT__do_enq;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_26__DOT__unnamedblk1__DOT__do_enq = 0;
    CData/*0:0*/ ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_27__DOT__unnamedblk1__DOT__do_enq;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_27__DOT__unnamedblk1__DOT__do_enq = 0;
    CData/*0:0*/ ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_28__DOT__unnamedblk1__DOT__do_enq;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_28__DOT__unnamedblk1__DOT__do_enq = 0;
    CData/*0:0*/ ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_29__DOT__unnamedblk1__DOT__do_enq;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_29__DOT__unnamedblk1__DOT__do_enq = 0;
    CData/*0:0*/ ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_30__DOT__unnamedblk1__DOT__do_enq;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_30__DOT__unnamedblk1__DOT__do_enq = 0;
    CData/*0:0*/ ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_31__DOT__unnamedblk1__DOT__do_enq;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_31__DOT__unnamedblk1__DOT__do_enq = 0;
    CData/*0:0*/ ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT___GEN_0;
    ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT___GEN_0 = 0;
    CData/*0:0*/ ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT___GEN_1;
    ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT___GEN_1 = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__idle_3;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__idle_3 = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__idle_2;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__idle_2 = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__wrap;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__wrap = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__wrap_1;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__wrap_1 = 0;
    CData/*0:0*/ __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__ram_ext__DOT__Memory__v0;
    __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__ram_ext__DOT__Memory__v0 = 0;
    CData/*1:0*/ __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__ram_ext__DOT__Memory__v0;
    __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__ram_ext__DOT__Memory__v0 = 0;
    CData/*0:0*/ __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__ram_ext__DOT__Memory__v0;
    __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__ram_ext__DOT__Memory__v0 = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__idle_3;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__idle_3 = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__idle_4;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__idle_4 = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__wrap;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__wrap = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__wrap_1;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__wrap_1 = 0;
    CData/*0:0*/ __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__ram_ext__DOT__Memory__v0;
    __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__ram_ext__DOT__Memory__v0 = 0;
    CData/*2:0*/ __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__ram_ext__DOT__Memory__v0;
    __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__ram_ext__DOT__Memory__v0 = 0;
    CData/*0:0*/ __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__ram_ext__DOT__Memory__v0;
    __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__ram_ext__DOT__Memory__v0 = 0;
    CData/*1:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__state;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__state = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__ar_handshake_done;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__ar_handshake_done = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__aw_handshake_done;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__aw_handshake_done = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__w_handshake_done;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__w_handshake_done = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__refill_cnt;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__refill_cnt = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__valid_array_0;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__valid_array_0 = 0;
    CData/*1:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__state;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__state = 0;
    CData/*3:0*/ __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__refile__DOT__ysyx_25020039_rf_ext__DOT__Memory__v0;
    __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__refile__DOT__ysyx_25020039_rf_ext__DOT__Memory__v0 = 0;
    IData/*31:0*/ __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__refile__DOT__ysyx_25020039_rf_ext__DOT__Memory__v0;
    __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__refile__DOT__ysyx_25020039_rf_ext__DOT__Memory__v0 = 0;
    CData/*0:0*/ __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__refile__DOT__ysyx_25020039_rf_ext__DOT__Memory__v0;
    __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__refile__DOT__ysyx_25020039_rf_ext__DOT__Memory__v0 = 0;
    CData/*3:0*/ __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT__Uregs__DOT__transmitter__DOT__fifo_tx__DOT__tfifo__DOT__ram__v0;
    __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT__Uregs__DOT__transmitter__DOT__fifo_tx__DOT__tfifo__DOT__ram__v0 = 0;
    CData/*7:0*/ __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT__Uregs__DOT__transmitter__DOT__fifo_tx__DOT__tfifo__DOT__ram__v0;
    __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT__Uregs__DOT__transmitter__DOT__fifo_tx__DOT__tfifo__DOT__ram__v0 = 0;
    CData/*0:0*/ __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT__Uregs__DOT__transmitter__DOT__fifo_tx__DOT__tfifo__DOT__ram__v0;
    __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT__Uregs__DOT__transmitter__DOT__fifo_tx__DOT__tfifo__DOT__ram__v0 = 0;
    CData/*3:0*/ __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT__Uregs__DOT__receiver__DOT__fifo_rx__DOT__rfifo__DOT__ram__v0;
    __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT__Uregs__DOT__receiver__DOT__fifo_rx__DOT__rfifo__DOT__ram__v0 = 0;
    CData/*7:0*/ __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT__Uregs__DOT__receiver__DOT__fifo_rx__DOT__rfifo__DOT__ram__v0;
    __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT__Uregs__DOT__receiver__DOT__fifo_rx__DOT__rfifo__DOT__ram__v0 = 0;
    CData/*0:0*/ __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT__Uregs__DOT__receiver__DOT__fifo_rx__DOT__rfifo__DOT__ram__v0;
    __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT__Uregs__DOT__receiver__DOT__fifo_rx__DOT__rfifo__DOT__ram__v0 = 0;
    CData/*2:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__ps2_clk_sync;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__ps2_clk_sync = 0;
    CData/*3:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__count;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__count = 0;
    CData/*6:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__w_ptr;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__w_ptr = 0;
    CData/*6:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__r_ptr;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__r_ptr = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__key_overflow;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__key_overflow = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__key_ready;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__key_ready = 0;
    CData/*6:0*/ __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__fifo__v0;
    __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__fifo__v0 = 0;
    CData/*0:0*/ __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__fifo__v0;
    __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__fifo__v0 = 0;
    CData/*6:0*/ __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__fifo__v1;
    __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__fifo__v1 = 0;
    CData/*7:0*/ __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__fifo__v1;
    __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__fifo__v1 = 0;
    CData/*0:0*/ __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__fifo__v1;
    __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__fifo__v1 = 0;
    SData/*9:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__x_cnt;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__x_cnt = 0;
    SData/*9:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__y_cnt;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__y_cnt = 0;
    CData/*1:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__vga_apb_state;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__vga_apb_state = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__w_full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__w_full = 0;
    SData/*10:0*/ __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v0;
    __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v0 = 0;
    CData/*4:0*/ __Vdlyvlsb__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v0;
    __Vdlyvlsb__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v0 = 0;
    CData/*7:0*/ __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v0;
    __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v0 = 0;
    CData/*0:0*/ __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v0;
    __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v0 = 0;
    SData/*10:0*/ __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v1;
    __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v1 = 0;
    CData/*4:0*/ __Vdlyvlsb__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v1;
    __Vdlyvlsb__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v1 = 0;
    CData/*7:0*/ __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v1;
    __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v1 = 0;
    CData/*0:0*/ __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v1;
    __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v1 = 0;
    SData/*10:0*/ __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v2;
    __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v2 = 0;
    CData/*4:0*/ __Vdlyvlsb__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v2;
    __Vdlyvlsb__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v2 = 0;
    CData/*7:0*/ __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v2;
    __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v2 = 0;
    CData/*0:0*/ __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v2;
    __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v2 = 0;
    SData/*10:0*/ __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v3;
    __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v3 = 0;
    CData/*4:0*/ __Vdlyvlsb__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v3;
    __Vdlyvlsb__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v3 = 0;
    CData/*7:0*/ __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v3;
    __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v3 = 0;
    CData/*0:0*/ __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v3;
    __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v3 = 0;
    CData/*1:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi42apb__DOT__state;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi42apb__DOT__state = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__wrap;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__wrap = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__wrap_1;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__wrap_1 = 0;
    CData/*0:0*/ __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__ram_ext__DOT__Memory__v0;
    __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__ram_ext__DOT__Memory__v0 = 0;
    QData/*46:0*/ __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__ram_ext__DOT__Memory__v0;
    __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__ram_ext__DOT__Memory__v0 = 0;
    CData/*0:0*/ __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__ram_ext__DOT__Memory__v0;
    __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__ram_ext__DOT__Memory__v0 = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__wrap;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__wrap = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__wrap_1;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__wrap_1 = 0;
    CData/*0:0*/ __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__ram_ext__DOT__Memory__v0;
    __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__ram_ext__DOT__Memory__v0 = 0;
    QData/*35:0*/ __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__ram_ext__DOT__Memory__v0;
    __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__ram_ext__DOT__Memory__v0 = 0;
    CData/*0:0*/ __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__ram_ext__DOT__Memory__v0;
    __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__ram_ext__DOT__Memory__v0 = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__wrap;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__wrap = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__wrap_1;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__wrap_1 = 0;
    CData/*0:0*/ __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__ram_ext__DOT__Memory__v0;
    __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__ram_ext__DOT__Memory__v0 = 0;
    CData/*5:0*/ __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__ram_ext__DOT__Memory__v0;
    __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__ram_ext__DOT__Memory__v0 = 0;
    CData/*0:0*/ __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__ram_ext__DOT__Memory__v0;
    __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__ram_ext__DOT__Memory__v0 = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__wrap;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__wrap = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__wrap_1;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__wrap_1 = 0;
    CData/*0:0*/ __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__ram_ext__DOT__Memory__v0;
    __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__ram_ext__DOT__Memory__v0 = 0;
    QData/*46:0*/ __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__ram_ext__DOT__Memory__v0;
    __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__ram_ext__DOT__Memory__v0 = 0;
    CData/*0:0*/ __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__ram_ext__DOT__Memory__v0;
    __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__ram_ext__DOT__Memory__v0 = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__wrap;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__wrap = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__wrap_1;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__wrap_1 = 0;
    CData/*0:0*/ __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__ram_ext__DOT__Memory__v0;
    __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__ram_ext__DOT__Memory__v0 = 0;
    QData/*38:0*/ __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__ram_ext__DOT__Memory__v0;
    __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__ram_ext__DOT__Memory__v0 = 0;
    CData/*0:0*/ __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__ram_ext__DOT__Memory__v0;
    __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__ram_ext__DOT__Memory__v0 = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap__DOT__full = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_1__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_1__DOT__full = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_2__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_2__DOT__full = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_3__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_3__DOT__full = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_4__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_4__DOT__full = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_5__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_5__DOT__full = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_6__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_6__DOT__full = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_7__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_7__DOT__full = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_8__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_8__DOT__full = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_9__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_9__DOT__full = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_10__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_10__DOT__full = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_11__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_11__DOT__full = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_12__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_12__DOT__full = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_13__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_13__DOT__full = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_14__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_14__DOT__full = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_15__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_15__DOT__full = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_16__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_16__DOT__full = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_17__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_17__DOT__full = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_18__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_18__DOT__full = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_19__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_19__DOT__full = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_20__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_20__DOT__full = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_21__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_21__DOT__full = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_22__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_22__DOT__full = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_23__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_23__DOT__full = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_24__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_24__DOT__full = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_25__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_25__DOT__full = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_26__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_26__DOT__full = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_27__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_27__DOT__full = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_28__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_28__DOT__full = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_29__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_29__DOT__full = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_30__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_30__DOT__full = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_31__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_31__DOT__full = 0;
    CData/*1:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_0;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_0 = 0;
    CData/*1:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_1;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_1 = 0;
    CData/*1:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_2;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_2 = 0;
    CData/*1:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_3;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_3 = 0;
    CData/*1:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_4;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_4 = 0;
    CData/*1:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_5;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_5 = 0;
    CData/*1:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_6;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_6 = 0;
    CData/*1:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_7;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_7 = 0;
    CData/*1:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_8;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_8 = 0;
    CData/*1:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_9;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_9 = 0;
    CData/*1:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_10;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_10 = 0;
    CData/*1:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_11;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_11 = 0;
    CData/*1:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_12;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_12 = 0;
    CData/*1:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_13;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_13 = 0;
    CData/*1:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_14;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_14 = 0;
    CData/*1:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_15;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_15 = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q__DOT__full = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q_1__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q_1__DOT__full = 0;
    CData/*0:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__in_wdeq_q__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__in_wdeq_q__DOT__full = 0;
    SData/*15:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__delayCnt;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__delayCnt = 0;
    CData/*7:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__burstCntOut;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__burstCntOut = 0;
    SData/*15:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_0;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_0 = 0;
    SData/*15:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_1;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_1 = 0;
    SData/*15:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_2;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_2 = 0;
    SData/*15:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_3;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_3 = 0;
    SData/*15:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_4;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_4 = 0;
    SData/*15:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_5;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_5 = 0;
    SData/*15:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_6;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_6 = 0;
    SData/*15:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_7;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_7 = 0;
    CData/*7:0*/ __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__burstCntIn;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__burstCntIn = 0;
    // Body
    __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__ps2_clk_sync 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__ps2_clk_sync;
    vlSelf->__Vdlyvset__ysyxSoCFull__DOT__asic__DOT__lpsram__DOT__mpsram__DOT__u0__DOT__MR__DOT__data__v0 = 0U;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__y_cnt 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__y_cnt;
    __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__ram_ext__DOT__Memory__v0 = 0U;
    if (VL_UNLIKELY(((~ (IData)(vlSelf->reset)) & (0U 
                                                   == 
                                                   ((0x200U 
                                                     & ((~ 
                                                         ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_wready) 
                                                          & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_wvalid))) 
                                                        << 9U)) 
                                                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__w_todo)))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:5055: Assertion failed in %NysyxSoCFull.asic.axi4frag: Assertion failed at Fragmenter.scala:181\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 5055, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:5057: Assertion failed in %NysyxSoCFull.asic.axi4frag\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 5057, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ (((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_wvalid)) 
                                  | (~ ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__in_wdeq_q__DOT__full)
                                         ? (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__in_wdeq_q__DOT__ram)
                                         : (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4delay_delayer_io_out_wlast)))) 
                                 | (1U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__w_todo)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:5061: Assertion failed in %NysyxSoCFull.asic.axi4frag: Assertion failed at Fragmenter.scala:190\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 5061, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:5063: Assertion failed in %NysyxSoCFull.asic.axi4frag\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 5063, "");
    }
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__in_wdeq_q__DOT__full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__in_wdeq_q__DOT__full;
    if (VL_UNLIKELY(((~ (IData)(vlSelf->reset)) & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__in_0_awvalid) 
                                                   & (0U 
                                                      == 
                                                      ((6U 
                                                        & (4U 
                                                           ^ 
                                                           (0x1eU 
                                                            & (vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awaddr 
                                                               >> 0x1bU)))) 
                                                       | (1U 
                                                          & (vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awaddr 
                                                             >> 0x18U)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2930: Assertion failed in %NysyxSoCFull.asic.lmrom: Assertion failed: do not support write operations\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2930, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2932: Assertion failed in %NysyxSoCFull.asic.lmrom\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2932, "");
    }
    if (VL_UNLIKELY(((~ (IData)(vlSelf->reset)) & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__in_0_wvalid) 
                                                   & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awIn_0_io_deq_bits) 
                                                      >> 1U))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2936: Assertion failed in %NysyxSoCFull.asic.lmrom: Assertion failed: do not support write operations\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2936, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2938: Assertion failed in %NysyxSoCFull.asic.lmrom\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2938, "");
    }
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_15__DOT__full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_15__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_14__DOT__full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_14__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_13__DOT__full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_13__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_12__DOT__full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_12__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_11__DOT__full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_11__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_10__DOT__full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_10__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_9__DOT__full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_9__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_8__DOT__full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_8__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_7__DOT__full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_7__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_6__DOT__full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_6__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_5__DOT__full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_5__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_4__DOT__full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_4__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_3__DOT__full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_3__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_2__DOT__full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_2__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_1__DOT__full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_1__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap__DOT__full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__wrap 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__wrap;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__wrap_1 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__wrap_1;
    __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v0 = 0U;
    __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v1 = 0U;
    __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v2 = 0U;
    __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v3 = 0U;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q__DOT__full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q__DOT__full;
    __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__ram_ext__DOT__Memory__v0 = 0U;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__x_cnt 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__x_cnt;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__vga_apb_state 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__vga_apb_state;
    if (VL_UNLIKELY(((~ (IData)(vlSelf->reset)) & (0U 
                                                   == 
                                                   ((8U 
                                                     & ((~ 
                                                         (((IData)(1U) 
                                                           << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_rid)) 
                                                          & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_h52ee47fe__0))) 
                                                        << 3U)) 
                                                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_0_count)))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:592: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 592, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:594: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 594, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___arFIFOMap_15_T_1)) 
                                 | (7U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_0_count)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:598: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:132\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 598, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:600: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 600, "");
    }
    if (VL_UNLIKELY(((~ (IData)(vlSelf->reset)) & (0U 
                                                   == 
                                                   ((8U 
                                                     & ((~ 
                                                         (((IData)(1U) 
                                                           << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_bid)) 
                                                          & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awFIFOMap_15_T_4))) 
                                                        << 3U)) 
                                                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_0_count)))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:604: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 604, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:606: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 606, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awFIFOMap_15_T_1)) 
                                 | (7U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_0_count)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:610: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:132\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 610, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:612: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 612, "");
    }
    if (VL_UNLIKELY(((~ (IData)(vlSelf->reset)) & (0U 
                                                   == 
                                                   ((8U 
                                                     & ((~ 
                                                         (0x7fffU 
                                                          & ((((IData)(1U) 
                                                               << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_rid)) 
                                                              >> 1U) 
                                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_h52ee47fe__0)))) 
                                                        << 3U)) 
                                                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_1_count)))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:616: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 616, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:618: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 618, "");
    }
    if (VL_UNLIKELY(((~ (IData)(vlSelf->reset)) & (0U 
                                                   == 
                                                   ((8U 
                                                     & ((~ 
                                                         (0x7fffU 
                                                          & ((((IData)(1U) 
                                                               << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_bid)) 
                                                              >> 1U) 
                                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awFIFOMap_15_T_4)))) 
                                                        << 3U)) 
                                                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_1_count)))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:628: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 628, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:630: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 630, "");
    }
    if (VL_UNLIKELY(((~ (IData)(vlSelf->reset)) & (0U 
                                                   == 
                                                   ((8U 
                                                     & ((~ 
                                                         (0x3fffU 
                                                          & ((((IData)(1U) 
                                                               << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_rid)) 
                                                              >> 2U) 
                                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_h52ee47fe__0)))) 
                                                        << 3U)) 
                                                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_2_count)))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:640: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 640, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:642: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 642, "");
    }
    if (VL_UNLIKELY(((~ (IData)(vlSelf->reset)) & (0U 
                                                   == 
                                                   ((8U 
                                                     & ((~ 
                                                         (0x3fffU 
                                                          & ((((IData)(1U) 
                                                               << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_bid)) 
                                                              >> 2U) 
                                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awFIFOMap_15_T_4)))) 
                                                        << 3U)) 
                                                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_2_count)))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:652: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 652, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:654: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 654, "");
    }
    if (VL_UNLIKELY(((~ (IData)(vlSelf->reset)) & (0U 
                                                   == 
                                                   ((8U 
                                                     & ((~ 
                                                         (0x1fffU 
                                                          & ((((IData)(1U) 
                                                               << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_rid)) 
                                                              >> 3U) 
                                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_h52ee47fe__0)))) 
                                                        << 3U)) 
                                                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_3_count)))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:664: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 664, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:666: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 666, "");
    }
    if (VL_UNLIKELY(((~ (IData)(vlSelf->reset)) & (0U 
                                                   == 
                                                   ((8U 
                                                     & ((~ 
                                                         (0x1fffU 
                                                          & ((((IData)(1U) 
                                                               << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_bid)) 
                                                              >> 3U) 
                                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awFIFOMap_15_T_4)))) 
                                                        << 3U)) 
                                                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_3_count)))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:676: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 676, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:678: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 678, "");
    }
    if (VL_UNLIKELY(((~ (IData)(vlSelf->reset)) & (0U 
                                                   == 
                                                   ((8U 
                                                     & ((~ 
                                                         (0xfffU 
                                                          & ((((IData)(1U) 
                                                               << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_rid)) 
                                                              >> 4U) 
                                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_h52ee47fe__0)))) 
                                                        << 3U)) 
                                                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_4_count)))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:688: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 688, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:690: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 690, "");
    }
    if (VL_UNLIKELY(((~ (IData)(vlSelf->reset)) & (0U 
                                                   == 
                                                   ((8U 
                                                     & ((~ 
                                                         (0xfffU 
                                                          & ((((IData)(1U) 
                                                               << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_bid)) 
                                                              >> 4U) 
                                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awFIFOMap_15_T_4)))) 
                                                        << 3U)) 
                                                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_4_count)))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:700: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 700, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:702: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 702, "");
    }
    if (VL_UNLIKELY(((~ (IData)(vlSelf->reset)) & (0U 
                                                   == 
                                                   ((8U 
                                                     & ((~ 
                                                         (0x7ffU 
                                                          & ((((IData)(1U) 
                                                               << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_rid)) 
                                                              >> 5U) 
                                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_h52ee47fe__0)))) 
                                                        << 3U)) 
                                                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_5_count)))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:712: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 712, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:714: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 714, "");
    }
    if (VL_UNLIKELY(((~ (IData)(vlSelf->reset)) & (0U 
                                                   == 
                                                   ((8U 
                                                     & ((~ 
                                                         (0x7ffU 
                                                          & ((((IData)(1U) 
                                                               << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_bid)) 
                                                              >> 5U) 
                                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awFIFOMap_15_T_4)))) 
                                                        << 3U)) 
                                                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_5_count)))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:724: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 724, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:726: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 726, "");
    }
    if (VL_UNLIKELY(((~ (IData)(vlSelf->reset)) & (0U 
                                                   == 
                                                   ((8U 
                                                     & ((~ 
                                                         (0x3ffU 
                                                          & ((((IData)(1U) 
                                                               << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_rid)) 
                                                              >> 6U) 
                                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_h52ee47fe__0)))) 
                                                        << 3U)) 
                                                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_6_count)))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:736: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 736, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:738: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 738, "");
    }
    if (VL_UNLIKELY(((~ (IData)(vlSelf->reset)) & (0U 
                                                   == 
                                                   ((8U 
                                                     & ((~ 
                                                         (0x3ffU 
                                                          & ((((IData)(1U) 
                                                               << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_bid)) 
                                                              >> 6U) 
                                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awFIFOMap_15_T_4)))) 
                                                        << 3U)) 
                                                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_6_count)))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:748: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 748, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:750: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 750, "");
    }
    if (VL_UNLIKELY(((~ (IData)(vlSelf->reset)) & (0U 
                                                   == 
                                                   ((8U 
                                                     & ((~ 
                                                         (0x1ffU 
                                                          & ((((IData)(1U) 
                                                               << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_rid)) 
                                                              >> 7U) 
                                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_h52ee47fe__0)))) 
                                                        << 3U)) 
                                                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_7_count)))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:760: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 760, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:762: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 762, "");
    }
    if (VL_UNLIKELY(((~ (IData)(vlSelf->reset)) & (0U 
                                                   == 
                                                   ((8U 
                                                     & ((~ 
                                                         (0x1ffU 
                                                          & ((((IData)(1U) 
                                                               << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_bid)) 
                                                              >> 7U) 
                                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awFIFOMap_15_T_4)))) 
                                                        << 3U)) 
                                                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_7_count)))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:772: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 772, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:774: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 774, "");
    }
    if (VL_UNLIKELY(((~ (IData)(vlSelf->reset)) & (0U 
                                                   == 
                                                   ((8U 
                                                     & ((~ 
                                                         (0xffU 
                                                          & ((((IData)(1U) 
                                                               << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_rid)) 
                                                              >> 8U) 
                                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_h52ee47fe__0)))) 
                                                        << 3U)) 
                                                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_8_count)))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:784: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 784, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:786: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 786, "");
    }
    if (VL_UNLIKELY(((~ (IData)(vlSelf->reset)) & (0U 
                                                   == 
                                                   ((8U 
                                                     & ((~ 
                                                         (0xffU 
                                                          & ((((IData)(1U) 
                                                               << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_bid)) 
                                                              >> 8U) 
                                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awFIFOMap_15_T_4)))) 
                                                        << 3U)) 
                                                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_8_count)))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:796: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 796, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:798: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 798, "");
    }
    if (VL_UNLIKELY(((~ (IData)(vlSelf->reset)) & (0U 
                                                   == 
                                                   ((8U 
                                                     & ((~ 
                                                         (0x7fU 
                                                          & ((((IData)(1U) 
                                                               << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_rid)) 
                                                              >> 9U) 
                                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_h52ee47fe__0)))) 
                                                        << 3U)) 
                                                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_9_count)))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:808: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 808, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:810: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 810, "");
    }
    if (VL_UNLIKELY(((~ (IData)(vlSelf->reset)) & (0U 
                                                   == 
                                                   ((8U 
                                                     & ((~ 
                                                         (0x7fU 
                                                          & ((((IData)(1U) 
                                                               << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_bid)) 
                                                              >> 9U) 
                                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awFIFOMap_15_T_4)))) 
                                                        << 3U)) 
                                                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_9_count)))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:820: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 820, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:822: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 822, "");
    }
    if (VL_UNLIKELY(((~ (IData)(vlSelf->reset)) & (0U 
                                                   == 
                                                   ((8U 
                                                     & ((~ 
                                                         (0x3fU 
                                                          & ((((IData)(1U) 
                                                               << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_rid)) 
                                                              >> 0xaU) 
                                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_h52ee47fe__0)))) 
                                                        << 3U)) 
                                                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_10_count)))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:832: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 832, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:834: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 834, "");
    }
    if (VL_UNLIKELY(((~ (IData)(vlSelf->reset)) & (0U 
                                                   == 
                                                   ((8U 
                                                     & ((~ 
                                                         (0x3fU 
                                                          & ((((IData)(1U) 
                                                               << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_bid)) 
                                                              >> 0xaU) 
                                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awFIFOMap_15_T_4)))) 
                                                        << 3U)) 
                                                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_10_count)))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:844: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 844, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:846: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 846, "");
    }
    if (VL_UNLIKELY(((~ (IData)(vlSelf->reset)) & (0U 
                                                   == 
                                                   ((8U 
                                                     & ((~ 
                                                         (0x1fU 
                                                          & ((((IData)(1U) 
                                                               << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_rid)) 
                                                              >> 0xbU) 
                                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_h52ee47fe__0)))) 
                                                        << 3U)) 
                                                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_11_count)))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:856: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 856, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:858: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 858, "");
    }
    if (VL_UNLIKELY(((~ (IData)(vlSelf->reset)) & (0U 
                                                   == 
                                                   ((8U 
                                                     & ((~ 
                                                         (0x1fU 
                                                          & ((((IData)(1U) 
                                                               << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_bid)) 
                                                              >> 0xbU) 
                                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awFIFOMap_15_T_4)))) 
                                                        << 3U)) 
                                                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_11_count)))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:868: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 868, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:870: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 870, "");
    }
    if (VL_UNLIKELY(((~ (IData)(vlSelf->reset)) & (0U 
                                                   == 
                                                   ((8U 
                                                     & ((~ 
                                                         (0xfU 
                                                          & ((((IData)(1U) 
                                                               << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_rid)) 
                                                              >> 0xcU) 
                                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_h52ee47fe__0)))) 
                                                        << 3U)) 
                                                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_12_count)))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:880: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 880, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:882: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 882, "");
    }
    if (VL_UNLIKELY(((~ (IData)(vlSelf->reset)) & (0U 
                                                   == 
                                                   ((8U 
                                                     & ((~ 
                                                         (0xfU 
                                                          & ((((IData)(1U) 
                                                               << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_bid)) 
                                                              >> 0xcU) 
                                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awFIFOMap_15_T_4)))) 
                                                        << 3U)) 
                                                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_12_count)))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:892: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 892, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:894: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 894, "");
    }
    if (VL_UNLIKELY(((~ (IData)(vlSelf->reset)) & (0U 
                                                   == 
                                                   ((8U 
                                                     & ((~ 
                                                         (7U 
                                                          & ((((IData)(1U) 
                                                               << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_rid)) 
                                                              >> 0xdU) 
                                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_h52ee47fe__0)))) 
                                                        << 3U)) 
                                                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_13_count)))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:904: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 904, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:906: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 906, "");
    }
    if (VL_UNLIKELY(((~ (IData)(vlSelf->reset)) & (0U 
                                                   == 
                                                   ((8U 
                                                     & ((~ 
                                                         (7U 
                                                          & ((((IData)(1U) 
                                                               << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_bid)) 
                                                              >> 0xdU) 
                                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awFIFOMap_15_T_4)))) 
                                                        << 3U)) 
                                                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_13_count)))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:916: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 916, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:918: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 918, "");
    }
    if (VL_UNLIKELY(((~ (IData)(vlSelf->reset)) & (0U 
                                                   == 
                                                   ((8U 
                                                     & ((~ 
                                                         (3U 
                                                          & ((((IData)(1U) 
                                                               << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_rid)) 
                                                              >> 0xeU) 
                                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_h52ee47fe__0)))) 
                                                        << 3U)) 
                                                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_14_count)))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:928: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 928, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:930: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 930, "");
    }
    if (VL_UNLIKELY(((~ (IData)(vlSelf->reset)) & (0U 
                                                   == 
                                                   ((8U 
                                                     & ((~ 
                                                         (3U 
                                                          & ((((IData)(1U) 
                                                               << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_bid)) 
                                                              >> 0xeU) 
                                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awFIFOMap_15_T_4)))) 
                                                        << 3U)) 
                                                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_14_count)))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:940: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 940, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:942: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 942, "");
    }
    if (VL_UNLIKELY(((~ (IData)(vlSelf->reset)) & (0U 
                                                   == 
                                                   ((8U 
                                                     & ((~ 
                                                         ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_h52ee47fe__0) 
                                                          & (((IData)(1U) 
                                                              << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_rid)) 
                                                             >> 0xfU))) 
                                                        << 3U)) 
                                                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_15_count)))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:952: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 952, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:954: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 954, "");
    }
    if (VL_UNLIKELY(((~ (IData)(vlSelf->reset)) & (0U 
                                                   == 
                                                   ((8U 
                                                     & ((~ 
                                                         ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awFIFOMap_15_T_4) 
                                                          & (((IData)(1U) 
                                                              << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_bid)) 
                                                             >> 0xfU))) 
                                                        << 3U)) 
                                                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_15_count)))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:964: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 964, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:966: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 966, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__prefixOR_1)) 
                                 | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__winner_2_1)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:976: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:286\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 976, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:978: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 978, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ (((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__anyValid)) 
                                  | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__prefixOR_1)) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__winner_2_1))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:982: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:288\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 982, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:984: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 984, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__winner_3_0)) 
                                 | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__winner_3_1)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:988: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:286\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 988, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:990: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 990, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ (((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__anyValid_1)) 
                                  | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__winner_3_0)) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__winner_3_1))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:994: Assertion failed in %NysyxSoCFull.asic.axi4xbar: Assertion failed at Xbar.scala:288\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 994, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:996: Assertion failed in %NysyxSoCFull.asic.axi4xbar\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 996, "");
    }
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__idle_3 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__idle_3;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_31__DOT__full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_31__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_30__DOT__full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_30__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_29__DOT__full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_29__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_28__DOT__full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_28__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_27__DOT__full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_27__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_26__DOT__full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_26__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_25__DOT__full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_25__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_24__DOT__full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_24__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_23__DOT__full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_23__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_22__DOT__full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_22__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_21__DOT__full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_21__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_20__DOT__full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_20__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_19__DOT__full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_19__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_18__DOT__full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_18__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_17__DOT__full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_17__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_16__DOT__full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_16__DOT__full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__key_ready 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__key_ready;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__key_overflow 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__key_overflow;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__w_ptr 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__w_ptr;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__count 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__count;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__r_ptr 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__r_ptr;
    __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__fifo__v0 = 0U;
    __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__fifo__v1 = 0U;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_15 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_15;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_14 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_14;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_13 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_13;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_12 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_12;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_11 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_11;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_10 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_10;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_9 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_9;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_8 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_8;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_7 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_7;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_6 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_6;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_5 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_5;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_4 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_4;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_3 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_3;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_2 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_2;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_1 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_1;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_0 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_0;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__wrap 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__wrap;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__wrap_1 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__wrap_1;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__idle_2 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__idle_2;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q_1__DOT__full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q_1__DOT__full;
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT____VdfgTmp_h026b18ec__0))) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_0_count))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1764: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1764, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1766: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1766, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___arFIFOMap_15_T_1))) 
                                 | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_0_count)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1770: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:132\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1770, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1772: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1772, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7))) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_0_count))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1776: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1776, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1778: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1778, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awFIFOMap_15_T_1))) 
                                 | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_0_count)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1782: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:132\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1782, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1784: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1784, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x7fffU & ((((IData)(1U) 
                                                  << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                                 >> 1U) 
                                                & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT____VdfgTmp_h026b18ec__0)))) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_1_count))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1788: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1788, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1790: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1790, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x7fffU & ((((IData)(1U) 
                                                  << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                                                 >> 1U) 
                                                & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___arFIFOMap_15_T_1)))) 
                                 | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_1_count)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1794: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:132\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1794, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1796: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1796, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x7fffU & ((((IData)(1U) 
                                                  << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                                                 >> 1U) 
                                                & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7)))) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_1_count))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1800: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1800, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1802: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1802, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x7fffU & ((((IData)(1U) 
                                                  << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                                                 >> 1U) 
                                                & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awFIFOMap_15_T_1)))) 
                                 | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_1_count)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1806: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:132\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1806, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1808: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1808, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x3fffU & ((((IData)(1U) 
                                                  << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                                 >> 2U) 
                                                & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT____VdfgTmp_h026b18ec__0)))) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_2_count))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1812: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1812, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1814: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1814, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x3fffU & ((((IData)(1U) 
                                                  << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                                                 >> 2U) 
                                                & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___arFIFOMap_15_T_1)))) 
                                 | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_2_count)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1818: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:132\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1818, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1820: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1820, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x3fffU & ((((IData)(1U) 
                                                  << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                                                 >> 2U) 
                                                & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7)))) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_2_count))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1824: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1824, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1826: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1826, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x3fffU & ((((IData)(1U) 
                                                  << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                                                 >> 2U) 
                                                & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awFIFOMap_15_T_1)))) 
                                 | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_2_count)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1830: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:132\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1830, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1832: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1832, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x1fffU & ((((IData)(1U) 
                                                  << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                                 >> 3U) 
                                                & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT____VdfgTmp_h026b18ec__0)))) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_3_count))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1836: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1836, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1838: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1838, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x1fffU & ((((IData)(1U) 
                                                  << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                                                 >> 3U) 
                                                & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___arFIFOMap_15_T_1)))) 
                                 | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_3_count)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1842: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:132\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1842, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1844: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1844, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x1fffU & ((((IData)(1U) 
                                                  << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                                                 >> 3U) 
                                                & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7)))) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_3_count))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1848: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1848, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1850: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1850, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x1fffU & ((((IData)(1U) 
                                                  << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                                                 >> 3U) 
                                                & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awFIFOMap_15_T_1)))) 
                                 | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_3_count)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1854: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:132\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1854, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1856: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1856, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0xfffU & ((((IData)(1U) 
                                                 << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                                >> 4U) 
                                               & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT____VdfgTmp_h026b18ec__0)))) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_4_count))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1860: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1860, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1862: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1862, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0xfffU & ((((IData)(1U) 
                                                 << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                                                >> 4U) 
                                               & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___arFIFOMap_15_T_1)))) 
                                 | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_4_count)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1866: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:132\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1866, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1868: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1868, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0xfffU & ((((IData)(1U) 
                                                 << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                                                >> 4U) 
                                               & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7)))) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_4_count))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1872: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1872, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1874: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1874, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0xfffU & ((((IData)(1U) 
                                                 << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                                                >> 4U) 
                                               & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awFIFOMap_15_T_1)))) 
                                 | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_4_count)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1878: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:132\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1878, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1880: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1880, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x7ffU & ((((IData)(1U) 
                                                 << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                                >> 5U) 
                                               & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT____VdfgTmp_h026b18ec__0)))) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_5_count))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1884: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1884, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1886: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1886, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x7ffU & ((((IData)(1U) 
                                                 << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                                                >> 5U) 
                                               & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___arFIFOMap_15_T_1)))) 
                                 | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_5_count)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1890: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:132\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1890, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1892: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1892, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x7ffU & ((((IData)(1U) 
                                                 << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                                                >> 5U) 
                                               & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7)))) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_5_count))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1896: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1896, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1898: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1898, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x7ffU & ((((IData)(1U) 
                                                 << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                                                >> 5U) 
                                               & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awFIFOMap_15_T_1)))) 
                                 | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_5_count)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1902: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:132\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1902, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1904: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1904, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x3ffU & ((((IData)(1U) 
                                                 << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                                >> 6U) 
                                               & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT____VdfgTmp_h026b18ec__0)))) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_6_count))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1908: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1908, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1910: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1910, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x3ffU & ((((IData)(1U) 
                                                 << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                                                >> 6U) 
                                               & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___arFIFOMap_15_T_1)))) 
                                 | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_6_count)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1914: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:132\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1914, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1916: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1916, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x3ffU & ((((IData)(1U) 
                                                 << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                                                >> 6U) 
                                               & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7)))) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_6_count))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1920: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1920, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1922: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1922, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x3ffU & ((((IData)(1U) 
                                                 << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                                                >> 6U) 
                                               & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awFIFOMap_15_T_1)))) 
                                 | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_6_count)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1926: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:132\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1926, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1928: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1928, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x1ffU & ((((IData)(1U) 
                                                 << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                                >> 7U) 
                                               & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT____VdfgTmp_h026b18ec__0)))) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_7_count))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1932: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1932, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1934: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1934, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x1ffU & ((((IData)(1U) 
                                                 << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                                                >> 7U) 
                                               & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___arFIFOMap_15_T_1)))) 
                                 | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_7_count)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1938: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:132\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1938, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1940: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1940, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x1ffU & ((((IData)(1U) 
                                                 << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                                                >> 7U) 
                                               & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7)))) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_7_count))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1944: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1944, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1946: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1946, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x1ffU & ((((IData)(1U) 
                                                 << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                                                >> 7U) 
                                               & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awFIFOMap_15_T_1)))) 
                                 | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_7_count)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1950: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:132\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1950, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1952: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1952, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0xffU & ((((IData)(1U) 
                                                << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                               >> 8U) 
                                              & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT____VdfgTmp_h026b18ec__0)))) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_8_count))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1956: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1956, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1958: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1958, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0xffU & ((((IData)(1U) 
                                                << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                                               >> 8U) 
                                              & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___arFIFOMap_15_T_1)))) 
                                 | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_8_count)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1962: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:132\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1962, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1964: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1964, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0xffU & ((((IData)(1U) 
                                                << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                                               >> 8U) 
                                              & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7)))) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_8_count))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1968: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1968, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1970: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1970, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0xffU & ((((IData)(1U) 
                                                << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                                               >> 8U) 
                                              & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awFIFOMap_15_T_1)))) 
                                 | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_8_count)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1974: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:132\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1974, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1976: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1976, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x7fU & ((((IData)(1U) 
                                                << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                               >> 9U) 
                                              & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT____VdfgTmp_h026b18ec__0)))) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_9_count))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1980: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1980, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1982: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1982, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x7fU & ((((IData)(1U) 
                                                << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                                               >> 9U) 
                                              & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___arFIFOMap_15_T_1)))) 
                                 | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_9_count)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1986: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:132\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1986, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1988: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1988, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x7fU & ((((IData)(1U) 
                                                << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                                               >> 9U) 
                                              & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7)))) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_9_count))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1992: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1992, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1994: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1994, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x7fU & ((((IData)(1U) 
                                                << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                                               >> 9U) 
                                              & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awFIFOMap_15_T_1)))) 
                                 | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_9_count)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:1998: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:132\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 1998, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2000: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2000, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x3fU & ((((IData)(1U) 
                                                << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                               >> 0xaU) 
                                              & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT____VdfgTmp_h026b18ec__0)))) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_10_count))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2004: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2004, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2006: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2006, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x3fU & ((((IData)(1U) 
                                                << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                                               >> 0xaU) 
                                              & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___arFIFOMap_15_T_1)))) 
                                 | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_10_count)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2010: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:132\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2010, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2012: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2012, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x3fU & ((((IData)(1U) 
                                                << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                                               >> 0xaU) 
                                              & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7)))) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_10_count))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2016: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2016, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2018: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2018, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x3fU & ((((IData)(1U) 
                                                << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                                               >> 0xaU) 
                                              & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awFIFOMap_15_T_1)))) 
                                 | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_10_count)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2022: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:132\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2022, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2024: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2024, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x1fU & ((((IData)(1U) 
                                                << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                               >> 0xbU) 
                                              & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT____VdfgTmp_h026b18ec__0)))) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_11_count))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2028: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2028, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2030: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2030, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x1fU & ((((IData)(1U) 
                                                << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                                               >> 0xbU) 
                                              & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___arFIFOMap_15_T_1)))) 
                                 | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_11_count)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2034: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:132\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2034, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2036: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2036, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x1fU & ((((IData)(1U) 
                                                << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                                               >> 0xbU) 
                                              & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7)))) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_11_count))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2040: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2040, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2042: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2042, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0x1fU & ((((IData)(1U) 
                                                << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                                               >> 0xbU) 
                                              & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awFIFOMap_15_T_1)))) 
                                 | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_11_count)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2046: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:132\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2046, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2048: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2048, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0xfU & ((((IData)(1U) 
                                               << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                              >> 0xcU) 
                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT____VdfgTmp_h026b18ec__0)))) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_12_count))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2052: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2052, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2054: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2054, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0xfU & ((((IData)(1U) 
                                               << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                                              >> 0xcU) 
                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___arFIFOMap_15_T_1)))) 
                                 | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_12_count)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2058: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:132\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2058, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2060: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2060, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0xfU & ((((IData)(1U) 
                                               << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                                              >> 0xcU) 
                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7)))) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_12_count))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2064: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2064, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2066: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2066, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (0xfU & ((((IData)(1U) 
                                               << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                                              >> 0xcU) 
                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awFIFOMap_15_T_1)))) 
                                 | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_12_count)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2070: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:132\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2070, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2072: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2072, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (7U & ((((IData)(1U) 
                                             << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                            >> 0xdU) 
                                           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT____VdfgTmp_h026b18ec__0)))) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_13_count))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2076: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2076, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2078: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2078, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (7U & ((((IData)(1U) 
                                             << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                                            >> 0xdU) 
                                           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___arFIFOMap_15_T_1)))) 
                                 | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_13_count)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2082: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:132\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2082, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2084: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2084, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (7U & ((((IData)(1U) 
                                             << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                                            >> 0xdU) 
                                           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7)))) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_13_count))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2088: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2088, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2090: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2090, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (7U & ((((IData)(1U) 
                                             << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                                            >> 0xdU) 
                                           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awFIFOMap_15_T_1)))) 
                                 | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_13_count)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2094: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:132\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2094, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2096: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2096, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (3U & ((((IData)(1U) 
                                             << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                            >> 0xeU) 
                                           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT____VdfgTmp_h026b18ec__0)))) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_14_count))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2100: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2100, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2102: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2102, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (3U & ((((IData)(1U) 
                                             << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                                            >> 0xeU) 
                                           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___arFIFOMap_15_T_1)))) 
                                 | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_14_count)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2106: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:132\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2106, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2108: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2108, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (3U & ((((IData)(1U) 
                                             << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                                            >> 0xeU) 
                                           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7)))) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_14_count))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2112: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2112, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2114: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2114, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (3U & ((((IData)(1U) 
                                             << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                                            >> 0xeU) 
                                           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awFIFOMap_15_T_1)))) 
                                 | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_14_count)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2118: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:132\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2118, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2120: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2120, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT____VdfgTmp_h026b18ec__0) 
                                     & (((IData)(1U) 
                                         << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                        >> 0xfU))) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_15_count))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2124: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2124, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2126: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2126, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___arFIFOMap_15_T_1) 
                                     & (((IData)(1U) 
                                         << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                                        >> 0xfU))) 
                                 | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_15_count)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2130: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:132\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2130, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2132: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2132, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7) 
                                     & (((IData)(1U) 
                                         << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                                        >> 0xfU))) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_15_count))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2136: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:131\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2136, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2138: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2138, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awFIFOMap_15_T_1) 
                                     & (((IData)(1U) 
                                         << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                                        >> 0xfU))) 
                                 | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_15_count)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2142: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:132\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2142, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2144: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2144, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ (((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__prefixOR_1)) 
                                  | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__winner_3_1))) 
                                 & ((~ ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__prefixOR_1) 
                                        | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__winner_3_1))) 
                                    | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__winner_3_2))))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2150: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:286\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2150, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2152: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2152, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__anyValid)) 
                                   | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__prefixOR_1)) 
                                  | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__winner_3_1)) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__winner_3_2))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2156: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:288\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2156, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2158: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2158, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__winner_4_0)) 
                                 | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__winner_4_2)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2162: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:286\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2162, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2164: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2164, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ (((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__anyValid_1)) 
                                  | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__winner_4_0)) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__winner_4_2))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2168: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1: Assertion failed at Xbar.scala:288\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2168, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:2170: Assertion failed in %NysyxSoCFull.asic.axi4xbar_1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 2170, "");
    }
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__burstCntIn 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__burstCntIn;
    __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__ram_ext__DOT__Memory__v0 = 0U;
    __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__ram_ext__DOT__Memory__v0 = 0U;
    __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__ram_ext__DOT__Memory__v0 = 0U;
    __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT__Uregs__DOT__receiver__DOT__fifo_rx__DOT__rfifo__DOT__ram__v0 = 0U;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__wrap 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__wrap;
    __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__ram_ext__DOT__Memory__v0 = 0U;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__refill_cnt 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__refill_cnt;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__wrap 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__wrap;
    if (VL_UNLIKELY((((~ (IData)(vlSelf->reset)) & 
                      (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__empty))) 
                     & (0U != (0xffU & (IData)((vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT___ram_ext_R0_data 
                                                >> 3U))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:3305: Assertion failed in %NysyxSoCFull.asic.axi42apb: Assertion failed at AXI4ToAPB.scala:61\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 3305, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:3307: Assertion failed in %NysyxSoCFull.asic.axi42apb\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 3307, "");
    }
    if (VL_UNLIKELY((((~ (IData)(vlSelf->reset)) & 
                      (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__empty))) 
                     & (0U != (0xffU & (IData)((vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT___ram_ext_R0_data 
                                                >> 3U))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:3311: Assertion failed in %NysyxSoCFull.asic.axi42apb: Assertion failed at AXI4ToAPB.scala:62\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 3311, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:3313: Assertion failed in %NysyxSoCFull.asic.axi42apb\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 3313, "");
    }
    if (VL_UNLIKELY((((~ (IData)(vlSelf->reset)) & 
                      (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__empty))) 
                     & (2U < (7U & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT___ram_ext_R0_data)))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:3317: Assertion failed in %NysyxSoCFull.asic.axi42apb: Assertion failed at AXI4ToAPB.scala:64\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 3317, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:3319: Assertion failed in %NysyxSoCFull.asic.axi42apb\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 3319, "");
    }
    if (VL_UNLIKELY((((~ (IData)(vlSelf->reset)) & 
                      (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__empty))) 
                     & (2U < (7U & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT___ram_ext_R0_data)))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:3323: Assertion failed in %NysyxSoCFull.asic.axi42apb: Assertion failed at AXI4ToAPB.scala:65\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 3323, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:3325: Assertion failed in %NysyxSoCFull.asic.axi42apb\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 3325, "");
    }
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__wrap 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__wrap;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__wrap 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__wrap;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__idle_3 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__idle_3;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__w_full 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__w_full;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__wrap 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__wrap;
    __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT__Uregs__DOT__transmitter__DOT__fifo_tx__DOT__tfifo__DOT__ram__v0 = 0U;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__idle_4 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__idle_4;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__wrap_1 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__wrap_1;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__wrap_1 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__wrap_1;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__burstCntOut 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__burstCntOut;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__delayCnt 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__delayCnt;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_7 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_7;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_6 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_6;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_5 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_5;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_4 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_4;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_3 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_3;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_2 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_2;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_1 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_1;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_0 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_0;
    __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__ram_ext__DOT__Memory__v0 = 0U;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__wrap_1 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__wrap_1;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__wrap_1 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__wrap_1;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__valid_array_0 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__valid_array_0;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__wrap_1 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__wrap_1;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi42apb__DOT__state 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi42apb__DOT__state;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__state 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__state;
    __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__refile__DOT__ysyx_25020039_rf_ext__DOT__Memory__v0 = 0U;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__w_handshake_done 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__w_handshake_done;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__ar_handshake_done 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__ar_handshake_done;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__aw_handshake_done 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__aw_handshake_done;
    __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__state 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__state;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__unnamedblk1__DOT___GEN_4 
        = (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_15__DOT__full) 
            << 0xfU) | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_14__DOT__full) 
                         << 0xeU) | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_13__DOT__full) 
                                      << 0xdU) | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_12__DOT__full) 
                                                   << 0xcU) 
                                                  | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_11__DOT__full) 
                                                      << 0xbU) 
                                                     | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_10__DOT__full) 
                                                         << 0xaU) 
                                                        | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_9__DOT__full) 
                                                            << 9U) 
                                                           | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_8__DOT__full) 
                                                               << 8U) 
                                                              | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_7__DOT__full) 
                                                                  << 7U) 
                                                                 | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_6__DOT__full) 
                                                                     << 6U) 
                                                                    | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_5__DOT__full) 
                                                                        << 5U) 
                                                                       | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_4__DOT__full) 
                                                                           << 4U) 
                                                                          | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_3__DOT__full) 
                                                                              << 3U) 
                                                                             | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_2__DOT__full) 
                                                                                << 2U) 
                                                                                | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_1__DOT__full) 
                                                                                << 1U) 
                                                                                | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap__DOT__full))))))))))))))));
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__unnamedblk1__DOT___GEN_5 
        = (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_31__DOT__full) 
            << 0xfU) | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_30__DOT__full) 
                         << 0xeU) | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_29__DOT__full) 
                                      << 0xdU) | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_28__DOT__full) 
                                                   << 0xcU) 
                                                  | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_27__DOT__full) 
                                                      << 0xbU) 
                                                     | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_26__DOT__full) 
                                                         << 0xaU) 
                                                        | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_25__DOT__full) 
                                                            << 9U) 
                                                           | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_24__DOT__full) 
                                                               << 8U) 
                                                              | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_23__DOT__full) 
                                                                  << 7U) 
                                                                 | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_22__DOT__full) 
                                                                     << 6U) 
                                                                    | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_21__DOT__full) 
                                                                        << 5U) 
                                                                       | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_20__DOT__full) 
                                                                           << 4U) 
                                                                          | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_19__DOT__full) 
                                                                              << 3U) 
                                                                             | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_18__DOT__full) 
                                                                                << 2U) 
                                                                                | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_17__DOT__full) 
                                                                                << 1U) 
                                                                                | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_16__DOT__full))))))))))))))));
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rvalid)) 
                                 | ((IData)(ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__unnamedblk1__DOT___GEN_4) 
                                    >> (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:4375: Assertion failed in %NysyxSoCFull.asic.axi4yank.unnamedblk1: Assertion failed at UserYanker.scala:69\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 4375, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:4377: Assertion failed in %NysyxSoCFull.asic.axi4yank.unnamedblk1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 4377, "");
    }
    if (VL_UNLIKELY((1U & ((~ (IData)(vlSelf->reset)) 
                           & (~ ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_bvalid)) 
                                 | ((IData)(ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__unnamedblk1__DOT___GEN_5) 
                                    >> (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)))))))) {
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:4381: Assertion failed in %NysyxSoCFull.asic.axi4yank.unnamedblk1: Assertion failed at UserYanker.scala:98\n\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 4381, "");
        VL_WRITEF("[%0t] %%Error: ysyxSoCFull.v:4383: Assertion failed in %NysyxSoCFull.asic.axi4yank.unnamedblk1\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/build/ysyxSoCFull.v", 4383, "");
    }
    __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__ps2_clk_sync 
        = ((6U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__ps2_clk_sync) 
                  << 1U)) | (IData)(vlSelf->externalPins_ps2_clk));
    if (((0x14U <= (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lpsram__DOT__mpsram__DOT__u0__DOT__MR__DOT__counter)) 
         & (0x1bU >= (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lpsram__DOT__mpsram__DOT__u0__DOT__MR__DOT__counter)))) {
        if (vlSelf->ysyxSoCFull__DOT__asic__DOT__lpsram__DOT__mpsram__DOT__u0__DOT__mr_sck) {
            vlSelf->__Vdlyvval__ysyxSoCFull__DOT__asic__DOT__lpsram__DOT__mpsram__DOT__u0__DOT__MR__DOT__data__v0 
                = ((0xf0U & (vlSelf->ysyxSoCFull__DOT__asic__DOT__lpsram__DOT__mpsram__DOT__u0__DOT__MR__DOT__data
                             [(3U & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lpsram__DOT__mpsram__DOT__u0__DOT__MR__DOT__counter) 
                                      >> 1U) - (IData)(2U)))] 
                             << 4U)) | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lpsram__DOT__mpsram__DOT__u0__DOT__mr_din));
            vlSelf->__Vdlyvset__ysyxSoCFull__DOT__asic__DOT__lpsram__DOT__mpsram__DOT__u0__DOT__MR__DOT__data__v0 = 1U;
            vlSelf->__Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__lpsram__DOT__mpsram__DOT__u0__DOT__MR__DOT__data__v0 
                = (3U & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lpsram__DOT__mpsram__DOT__u0__DOT__MR__DOT__counter) 
                          >> 1U) - (IData)(2U)));
        }
    }
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__do_enq) {
        __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__ram_ext__DOT__Memory__v0 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT____Vcellinp__awIn_0__io_enq_bits;
        __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__ram_ext__DOT__Memory__v0 = 1U;
        __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__ram_ext__DOT__Memory__v0 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__wrap;
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_15_count 
        = (1U & ((~ (IData)(vlSelf->reset)) & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_15_count) 
                                                + ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___arFIFOMap_15_T_1) 
                                                   & (((IData)(1U) 
                                                       << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                                                      >> 0xfU))) 
                                               - ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT____VdfgTmp_h026b18ec__0) 
                                                  & (((IData)(1U) 
                                                      << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                                     >> 0xfU)))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_14_count 
        = (1U & ((~ (IData)(vlSelf->reset)) & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_14_count) 
                                                + (3U 
                                                   & ((((IData)(1U) 
                                                        << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                                                       >> 0xeU) 
                                                      & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___arFIFOMap_15_T_1)))) 
                                               - (3U 
                                                  & ((((IData)(1U) 
                                                       << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                                      >> 0xeU) 
                                                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT____VdfgTmp_h026b18ec__0))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_5_count 
        = (1U & ((~ (IData)(vlSelf->reset)) & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_5_count) 
                                                + (0x7ffU 
                                                   & ((((IData)(1U) 
                                                        << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                                                       >> 5U) 
                                                      & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___arFIFOMap_15_T_1)))) 
                                               - (0x7ffU 
                                                  & ((((IData)(1U) 
                                                       << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                                      >> 5U) 
                                                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT____VdfgTmp_h026b18ec__0))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_3_count 
        = (1U & ((~ (IData)(vlSelf->reset)) & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_3_count) 
                                                + (0x1fffU 
                                                   & ((((IData)(1U) 
                                                        << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                                                       >> 3U) 
                                                      & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___arFIFOMap_15_T_1)))) 
                                               - (0x1fffU 
                                                  & ((((IData)(1U) 
                                                       << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                                      >> 3U) 
                                                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT____VdfgTmp_h026b18ec__0))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_2_count 
        = (1U & ((~ (IData)(vlSelf->reset)) & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_2_count) 
                                                + (0x3fffU 
                                                   & ((((IData)(1U) 
                                                        << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                                                       >> 2U) 
                                                      & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___arFIFOMap_15_T_1)))) 
                                               - (0x3fffU 
                                                  & ((((IData)(1U) 
                                                       << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                                      >> 2U) 
                                                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT____VdfgTmp_h026b18ec__0))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_1_count 
        = (1U & ((~ (IData)(vlSelf->reset)) & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_1_count) 
                                                + (0x7fffU 
                                                   & ((((IData)(1U) 
                                                        << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                                                       >> 1U) 
                                                      & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___arFIFOMap_15_T_1)))) 
                                               - (0x7fffU 
                                                  & ((((IData)(1U) 
                                                       << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                                      >> 1U) 
                                                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT____VdfgTmp_h026b18ec__0))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_0_count 
        = (1U & ((~ (IData)(vlSelf->reset)) & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_0_count) 
                                                + (
                                                   ((IData)(1U) 
                                                    << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                                                   & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___arFIFOMap_15_T_1))) 
                                               - (((IData)(1U) 
                                                   << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                                  & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT____VdfgTmp_h026b18ec__0)))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_4_count 
        = (1U & ((~ (IData)(vlSelf->reset)) & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_4_count) 
                                                + (0xfffU 
                                                   & ((((IData)(1U) 
                                                        << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                                                       >> 4U) 
                                                      & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___arFIFOMap_15_T_1)))) 
                                               - (0xfffU 
                                                  & ((((IData)(1U) 
                                                       << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                                      >> 4U) 
                                                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT____VdfgTmp_h026b18ec__0))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_6_count 
        = (1U & ((~ (IData)(vlSelf->reset)) & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_6_count) 
                                                + (0x3ffU 
                                                   & ((((IData)(1U) 
                                                        << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                                                       >> 6U) 
                                                      & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___arFIFOMap_15_T_1)))) 
                                               - (0x3ffU 
                                                  & ((((IData)(1U) 
                                                       << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                                      >> 6U) 
                                                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT____VdfgTmp_h026b18ec__0))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_7_count 
        = (1U & ((~ (IData)(vlSelf->reset)) & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_7_count) 
                                                + (0x1ffU 
                                                   & ((((IData)(1U) 
                                                        << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                                                       >> 7U) 
                                                      & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___arFIFOMap_15_T_1)))) 
                                               - (0x1ffU 
                                                  & ((((IData)(1U) 
                                                       << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                                      >> 7U) 
                                                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT____VdfgTmp_h026b18ec__0))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_8_count 
        = (1U & ((~ (IData)(vlSelf->reset)) & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_8_count) 
                                                + (0xffU 
                                                   & ((((IData)(1U) 
                                                        << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                                                       >> 8U) 
                                                      & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___arFIFOMap_15_T_1)))) 
                                               - (0xffU 
                                                  & ((((IData)(1U) 
                                                       << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                                      >> 8U) 
                                                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT____VdfgTmp_h026b18ec__0))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_9_count 
        = (1U & ((~ (IData)(vlSelf->reset)) & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_9_count) 
                                                + (0x7fU 
                                                   & ((((IData)(1U) 
                                                        << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                                                       >> 9U) 
                                                      & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___arFIFOMap_15_T_1)))) 
                                               - (0x7fU 
                                                  & ((((IData)(1U) 
                                                       << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                                      >> 9U) 
                                                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT____VdfgTmp_h026b18ec__0))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_10_count 
        = (1U & ((~ (IData)(vlSelf->reset)) & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_10_count) 
                                                + (0x3fU 
                                                   & ((((IData)(1U) 
                                                        << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                                                       >> 0xaU) 
                                                      & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___arFIFOMap_15_T_1)))) 
                                               - (0x3fU 
                                                  & ((((IData)(1U) 
                                                       << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                                      >> 0xaU) 
                                                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT____VdfgTmp_h026b18ec__0))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_11_count 
        = (1U & ((~ (IData)(vlSelf->reset)) & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_11_count) 
                                                + (0x1fU 
                                                   & ((((IData)(1U) 
                                                        << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                                                       >> 0xbU) 
                                                      & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___arFIFOMap_15_T_1)))) 
                                               - (0x1fU 
                                                  & ((((IData)(1U) 
                                                       << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                                      >> 0xbU) 
                                                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT____VdfgTmp_h026b18ec__0))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_12_count 
        = (1U & ((~ (IData)(vlSelf->reset)) & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_12_count) 
                                                + (0xfU 
                                                   & ((((IData)(1U) 
                                                        << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                                                       >> 0xcU) 
                                                      & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___arFIFOMap_15_T_1)))) 
                                               - (0xfU 
                                                  & ((((IData)(1U) 
                                                       << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                                      >> 0xcU) 
                                                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT____VdfgTmp_h026b18ec__0))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_13_count 
        = (1U & ((~ (IData)(vlSelf->reset)) & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__arFIFOMap_13_count) 
                                                + (7U 
                                                   & ((((IData)(1U) 
                                                        << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                                                       >> 0xdU) 
                                                      & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___arFIFOMap_15_T_1)))) 
                                               - (7U 
                                                  & ((((IData)(1U) 
                                                       << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                                      >> 0xdU) 
                                                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT____VdfgTmp_h026b18ec__0))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__latched 
        = ((~ (IData)(vlSelf->reset)) & ((~ ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__in_0_awready) 
                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__in_0_awvalid))) 
                                         & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awIn_0_io_enq_ready) 
                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0_io_enq_valid)) 
                                            | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__latched))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_15_count 
        = (1U & ((~ (IData)(vlSelf->reset)) & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_15_count) 
                                                + ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awFIFOMap_15_T_1) 
                                                   & (((IData)(1U) 
                                                       << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                                                      >> 0xfU))) 
                                               - ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7) 
                                                  & (((IData)(1U) 
                                                      << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                                                     >> 0xfU)))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_14_count 
        = (1U & ((~ (IData)(vlSelf->reset)) & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_14_count) 
                                                + (3U 
                                                   & ((((IData)(1U) 
                                                        << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                                                       >> 0xeU) 
                                                      & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awFIFOMap_15_T_1)))) 
                                               - (3U 
                                                  & ((((IData)(1U) 
                                                       << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                                                      >> 0xeU) 
                                                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_5_count 
        = (1U & ((~ (IData)(vlSelf->reset)) & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_5_count) 
                                                + (0x7ffU 
                                                   & ((((IData)(1U) 
                                                        << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                                                       >> 5U) 
                                                      & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awFIFOMap_15_T_1)))) 
                                               - (0x7ffU 
                                                  & ((((IData)(1U) 
                                                       << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                                                      >> 5U) 
                                                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_4_count 
        = (1U & ((~ (IData)(vlSelf->reset)) & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_4_count) 
                                                + (0xfffU 
                                                   & ((((IData)(1U) 
                                                        << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                                                       >> 4U) 
                                                      & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awFIFOMap_15_T_1)))) 
                                               - (0xfffU 
                                                  & ((((IData)(1U) 
                                                       << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                                                      >> 4U) 
                                                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_3_count 
        = (1U & ((~ (IData)(vlSelf->reset)) & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_3_count) 
                                                + (0x1fffU 
                                                   & ((((IData)(1U) 
                                                        << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                                                       >> 3U) 
                                                      & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awFIFOMap_15_T_1)))) 
                                               - (0x1fffU 
                                                  & ((((IData)(1U) 
                                                       << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                                                      >> 3U) 
                                                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_2_count 
        = (1U & ((~ (IData)(vlSelf->reset)) & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_2_count) 
                                                + (0x3fffU 
                                                   & ((((IData)(1U) 
                                                        << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                                                       >> 2U) 
                                                      & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awFIFOMap_15_T_1)))) 
                                               - (0x3fffU 
                                                  & ((((IData)(1U) 
                                                       << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                                                      >> 2U) 
                                                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_1_count 
        = (1U & ((~ (IData)(vlSelf->reset)) & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_1_count) 
                                                + (0x7fffU 
                                                   & ((((IData)(1U) 
                                                        << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                                                       >> 1U) 
                                                      & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awFIFOMap_15_T_1)))) 
                                               - (0x7fffU 
                                                  & ((((IData)(1U) 
                                                       << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                                                      >> 1U) 
                                                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_0_count 
        = (1U & ((~ (IData)(vlSelf->reset)) & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_0_count) 
                                                + (
                                                   ((IData)(1U) 
                                                    << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                                                   & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awFIFOMap_15_T_1))) 
                                               - (((IData)(1U) 
                                                   << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                                                  & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7)))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_6_count 
        = (1U & ((~ (IData)(vlSelf->reset)) & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_6_count) 
                                                + (0x3ffU 
                                                   & ((((IData)(1U) 
                                                        << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                                                       >> 6U) 
                                                      & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awFIFOMap_15_T_1)))) 
                                               - (0x3ffU 
                                                  & ((((IData)(1U) 
                                                       << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                                                      >> 6U) 
                                                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_7_count 
        = (1U & ((~ (IData)(vlSelf->reset)) & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_7_count) 
                                                + (0x1ffU 
                                                   & ((((IData)(1U) 
                                                        << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                                                       >> 7U) 
                                                      & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awFIFOMap_15_T_1)))) 
                                               - (0x1ffU 
                                                  & ((((IData)(1U) 
                                                       << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                                                      >> 7U) 
                                                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_8_count 
        = (1U & ((~ (IData)(vlSelf->reset)) & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_8_count) 
                                                + (0xffU 
                                                   & ((((IData)(1U) 
                                                        << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                                                       >> 8U) 
                                                      & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awFIFOMap_15_T_1)))) 
                                               - (0xffU 
                                                  & ((((IData)(1U) 
                                                       << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                                                      >> 8U) 
                                                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_9_count 
        = (1U & ((~ (IData)(vlSelf->reset)) & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_9_count) 
                                                + (0x7fU 
                                                   & ((((IData)(1U) 
                                                        << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                                                       >> 9U) 
                                                      & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awFIFOMap_15_T_1)))) 
                                               - (0x7fU 
                                                  & ((((IData)(1U) 
                                                       << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                                                      >> 9U) 
                                                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_10_count 
        = (1U & ((~ (IData)(vlSelf->reset)) & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_10_count) 
                                                + (0x3fU 
                                                   & ((((IData)(1U) 
                                                        << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                                                       >> 0xaU) 
                                                      & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awFIFOMap_15_T_1)))) 
                                               - (0x3fU 
                                                  & ((((IData)(1U) 
                                                       << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                                                      >> 0xaU) 
                                                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_11_count 
        = (1U & ((~ (IData)(vlSelf->reset)) & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_11_count) 
                                                + (0x1fU 
                                                   & ((((IData)(1U) 
                                                        << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                                                       >> 0xbU) 
                                                      & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awFIFOMap_15_T_1)))) 
                                               - (0x1fU 
                                                  & ((((IData)(1U) 
                                                       << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                                                      >> 0xbU) 
                                                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_12_count 
        = (1U & ((~ (IData)(vlSelf->reset)) & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_12_count) 
                                                + (0xfU 
                                                   & ((((IData)(1U) 
                                                        << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                                                       >> 0xcU) 
                                                      & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awFIFOMap_15_T_1)))) 
                                               - (0xfU 
                                                  & ((((IData)(1U) 
                                                       << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                                                      >> 0xcU) 
                                                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_13_count 
        = (1U & ((~ (IData)(vlSelf->reset)) & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awFIFOMap_13_count) 
                                                + (7U 
                                                   & ((((IData)(1U) 
                                                        << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                                                       >> 0xdU) 
                                                      & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awFIFOMap_15_T_1)))) 
                                               - (7U 
                                                  & ((((IData)(1U) 
                                                       << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                                                      >> 0xdU) 
                                                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7))))));
    if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT____Vcellinp__mem_ext__W0_en) 
         & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_wstrb))) {
        __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v0 
            = (0xffU & vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_wdata);
        __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v0 = 1U;
        __Vdlyvlsb__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v0 = 0U;
        __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v0 
            = (0x7ffU & (vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awaddr 
                         >> 2U));
    }
    if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT____Vcellinp__mem_ext__W0_en) 
         & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_wstrb) 
            >> 1U))) {
        __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v1 
            = (0xffU & (vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_wdata 
                        >> 8U));
        __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v1 = 1U;
        __Vdlyvlsb__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v1 = 8U;
        __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v1 
            = (0x7ffU & (vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awaddr 
                         >> 2U));
    }
    if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT____Vcellinp__mem_ext__W0_en) 
         & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_wstrb) 
            >> 2U))) {
        __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v2 
            = (0xffU & (vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_wdata 
                        >> 0x10U));
        __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v2 = 1U;
        __Vdlyvlsb__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v2 = 0x10U;
        __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v2 
            = (0x7ffU & (vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awaddr 
                         >> 2U));
    }
    if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT____Vcellinp__mem_ext__W0_en) 
         & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_wstrb) 
            >> 3U))) {
        __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v3 
            = (vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_wdata 
               >> 0x18U);
        __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v3 = 1U;
        __Vdlyvlsb__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v3 = 0x18U;
        __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v3 
            = (0x7ffU & (vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awaddr 
                         >> 2U));
    }
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__do_enq) {
        __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__ram_ext__DOT__Memory__v0 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____Vcellinp__awIn_0__io_enq_bits;
        __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__ram_ext__DOT__Memory__v0 = 1U;
        __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__ram_ext__DOT__Memory__v0 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__wrap;
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__wbeats_latched 
        = ((~ (IData)(vlSelf->reset)) & ((~ ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_awready) 
                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awvalid))) 
                                         & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__wbeats_valid) 
                                             & (0U 
                                                == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__w_counter))) 
                                            | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__wbeats_latched))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__latched 
        = ((~ (IData)(vlSelf->reset)) & ((~ ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__in_0_awready) 
                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__in_0_awvalid))) 
                                         & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awIn_0_io_enq_ready) 
                                             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0_io_enq_valid)) 
                                            | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__latched))));
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__do_enq) {
        __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__ram_ext__DOT__Memory__v0 
            = (((QData)((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_wdata)) 
                << 4U) | (QData)((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_wstrb)));
        __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__ram_ext__DOT__Memory__v0 = 1U;
        __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__ram_ext__DOT__Memory__v0 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__wrap;
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__is_write 
        = ((~ (IData)(vlSelf->reset)) & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4delay_delayer_io_out_awvalid) 
                                         | ((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__state)) 
                                            & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__is_write))));
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__do_enq) {
        __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__ram_ext__DOT__Memory__v0 
            = (((QData)((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                << 0x2bU) | (((QData)((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awaddr)) 
                              << 0xbU) | (QData)((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___deq_q_1_io_deq_bits_size))));
        __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__ram_ext__DOT__Memory__v0 = 1U;
        __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__ram_ext__DOT__Memory__v0 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__wrap;
    }
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__do_enq) {
        __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__ram_ext__DOT__Memory__v0 
            = (((QData)((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                << 0x2bU) | (((QData)((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_araddr)) 
                              << 0xbU) | (QData)((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___deq_q_io_deq_bits_size))));
        __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__ram_ext__DOT__Memory__v0 = 1U;
        __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__ram_ext__DOT__Memory__v0 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__wrap;
    }
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT__Uregs__DOT__rf_push_pulse) {
        __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT__Uregs__DOT__receiver__DOT__fifo_rx__DOT__rfifo__DOT__ram__v0 
            = (0xffU & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT__Uregs__DOT__receiver__DOT__rf_data_in) 
                        >> 3U));
        __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT__Uregs__DOT__receiver__DOT__fifo_rx__DOT__rfifo__DOT__ram__v0 = 1U;
        __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT__Uregs__DOT__receiver__DOT__fifo_rx__DOT__rfifo__DOT__ram__v0 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT__Uregs__DOT__receiver__DOT__fifo_rx__DOT__top;
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__is_read 
        = ((~ (IData)(vlSelf->reset)) & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4delay_delayer_io_out_arvalid) 
                                         | ((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__state)) 
                                            & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__is_read))));
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__do_enq) {
        __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__ram_ext__DOT__Memory__v0 
            = (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi42apb__DOT__bid_reg) 
                << 2U) | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi42apb_auto_in_bresp));
        __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__ram_ext__DOT__Memory__v0 = 1U;
        __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__ram_ext__DOT__Memory__v0 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__wrap;
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__r_full 
        = ((~ (IData)(vlSelf->reset)) & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__ren) 
                                         | ((~ ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_1_auto_anon_out_2_rready) 
                                                & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__r_full))) 
                                            & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__r_full))));
    __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__w_full 
        = ((~ (IData)(vlSelf->reset)) & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT___GEN) 
                                         | ((~ ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_1_auto_anon_out_2_bready) 
                                                & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__w_full))) 
                                            & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__w_full))));
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT__Uregs__DOT__tf_push) {
        __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT__Uregs__DOT__transmitter__DOT__fifo_tx__DOT__tfifo__DOT__ram__v0 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT____Vcellinp__Uregs__wb_dat_i;
        __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT__Uregs__DOT__transmitter__DOT__fifo_tx__DOT__tfifo__DOT__ram__v0 = 1U;
        __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT__Uregs__DOT__transmitter__DOT__fifo_tx__DOT__tfifo__DOT__ram__v0 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT__Uregs__DOT__transmitter__DOT__fifo_tx__DOT__top;
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__lmrom__DOT__state 
        = (1U & ((~ (IData)(vlSelf->reset)) & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lmrom__DOT__state)
                                                ? (~ 
                                                   (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_rready) 
                                                     & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__idle_3)
                                                         ? 
                                                        ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__readys_readys) 
                                                         >> 1U)
                                                         : (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__state_3_1))) 
                                                    & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lmrom__DOT__state)))
                                                : (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lmrom__DOT___nodeIn_rid_T))));
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__do_enq) {
        __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__ram_ext__DOT__Memory__v0 
            = (((QData)((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi42apb__DOT__rid_reg)) 
                << 0x23U) | (((QData)((IData)(((1U 
                                                == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi42apb__DOT__state))
                                                ? vlSelf->ysyxSoCFull__DOT__asic__DOT__apbdelay_delayer__DOT__delay_prdata
                                                : vlSelf->ysyxSoCFull__DOT__asic__DOT__axi42apb__DOT__nodeIn_rdata_r))) 
                              << 3U) | (QData)((IData)(
                                                       (1U 
                                                        | ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi42apb_auto_in_bresp) 
                                                           << 1U))))));
        __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__ram_ext__DOT__Memory__v0 = 1U;
        __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__ram_ext__DOT__Memory__v0 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__wrap;
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__clint__DOT__state 
        = (1U & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT____Vcellinp__cpu__reset)) 
                 & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__clint__DOT__state)
                     ? (~ ((0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT__state))
                            ? ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT____VdfgTmp_hf6e21050__0) 
                               & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_dmem_rready))
                            : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT____VdfgTmp_h3347b676__0) 
                               & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_dmem_rready))))
                     : ((0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT__state))
                         ? ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT____VdfgTmp_hf6e21050__0) 
                            & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_dmem_arvalid))
                         : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT____VdfgTmp_h3347b676__0) 
                            & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_dmem_arvalid))))));
    if ((((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_signals_wbu_reg_write) 
          & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___wbu_io_state_write_en)) 
         & (0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_waddr)))) {
        __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__refile__DOT__ysyx_25020039_rf_ext__DOT__Memory__v0 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___wbu_io_refile_wdata;
        __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__refile__DOT__ysyx_25020039_rf_ext__DOT__Memory__v0 = 1U;
        __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__refile__DOT__ysyx_25020039_rf_ext__DOT__Memory__v0 
            = (0xfU & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_waddr));
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT__reg_dat8_w_reg 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT__reg_dat8_w;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT___R0_en_d0 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__ren;
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT__lmrom__DOT___nodeIn_rid_T) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__lmrom__DOT__nodeIn_rdata_r 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT__lmrom__DOT___mrom_rdata;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__lmrom__DOT__nodeIn_rid_r 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid;
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__r_sel1 
        = (0x7800U == (0x7fffU & (vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_araddr 
                                  >> 0xdU)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu__DOT__first_cycle 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT____Vcellinp__cpu__reset;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT___R0_addr_d0 
        = (0x7ffU & (vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_araddr 
                     >> 2U));
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__ren) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__r_id 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid;
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_valid_r 
        = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT____Vcellinp__cpu__reset)) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___lsu_io_out_valid));
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__rdata_REG) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__rdata_r0 
            = (0xffU & vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT___mem_ext_R0_data);
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__rdata_r1 
            = (0xffU & (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT___mem_ext_R0_data 
                        >> 8U));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__rdata_r2 
            = (0xffU & (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT___mem_ext_R0_data 
                        >> 0x10U));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__rdata_r3 
            = (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT___mem_ext_R0_data 
               >> 0x18U);
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__w_sel1 
        = (0x7800U == (0x7fffU & (vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awaddr 
                                  >> 0xdU)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu_reset_chain__DOT__output_chain__DOT__sync_0 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu_reset_chain__DOT__output_chain__DOT__sync_1;
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_15__DOT__unnamedblk1__DOT__do_enq 
        = (((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_15__DOT__full)) 
            & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_2)) 
           & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
              >> 0xfU));
    if (ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_15__DOT__unnamedblk1__DOT__do_enq) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_15__DOT__ram_real_last 
            = (1U & (~ (IData)((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len)))));
    }
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_14__DOT__unnamedblk1__DOT__do_enq 
        = (3U & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_14__DOT__full)) 
                 & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_2) 
                    & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                       >> 0xeU))));
    if (ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_14__DOT__unnamedblk1__DOT__do_enq) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_14__DOT__ram_real_last 
            = (1U & (~ (IData)((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len)))));
    }
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_13__DOT__unnamedblk1__DOT__do_enq 
        = (7U & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_13__DOT__full)) 
                 & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_2) 
                    & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                       >> 0xdU))));
    if (ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_13__DOT__unnamedblk1__DOT__do_enq) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_13__DOT__ram_real_last 
            = (1U & (~ (IData)((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len)))));
    }
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_12__DOT__unnamedblk1__DOT__do_enq 
        = (0xfU & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_12__DOT__full)) 
                   & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_2) 
                      & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                         >> 0xcU))));
    if (ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_12__DOT__unnamedblk1__DOT__do_enq) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_12__DOT__ram_real_last 
            = (1U & (~ (IData)((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len)))));
    }
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_11__DOT__unnamedblk1__DOT__do_enq 
        = (0x1fU & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_11__DOT__full)) 
                    & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_2) 
                       & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                          >> 0xbU))));
    if (ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_11__DOT__unnamedblk1__DOT__do_enq) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_11__DOT__ram_real_last 
            = (1U & (~ (IData)((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len)))));
    }
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_10__DOT__unnamedblk1__DOT__do_enq 
        = (0x3fU & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_10__DOT__full)) 
                    & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_2) 
                       & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                          >> 0xaU))));
    if (ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_10__DOT__unnamedblk1__DOT__do_enq) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_10__DOT__ram_real_last 
            = (1U & (~ (IData)((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len)))));
    }
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_9__DOT__unnamedblk1__DOT__do_enq 
        = (0x7fU & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_9__DOT__full)) 
                    & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_2) 
                       & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                          >> 9U))));
    if (ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_9__DOT__unnamedblk1__DOT__do_enq) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_9__DOT__ram_real_last 
            = (1U & (~ (IData)((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len)))));
    }
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_8__DOT__unnamedblk1__DOT__do_enq 
        = (0xffU & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_8__DOT__full)) 
                    & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_2) 
                       & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                          >> 8U))));
    if (ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_8__DOT__unnamedblk1__DOT__do_enq) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_8__DOT__ram_real_last 
            = (1U & (~ (IData)((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len)))));
    }
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_7__DOT__unnamedblk1__DOT__do_enq 
        = (0x1ffU & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_7__DOT__full)) 
                     & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_2) 
                        & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                           >> 7U))));
    if (ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_7__DOT__unnamedblk1__DOT__do_enq) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_7__DOT__ram_real_last 
            = (1U & (~ (IData)((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len)))));
    }
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_6__DOT__unnamedblk1__DOT__do_enq 
        = (0x3ffU & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_6__DOT__full)) 
                     & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_2) 
                        & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                           >> 6U))));
    if (ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_6__DOT__unnamedblk1__DOT__do_enq) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_6__DOT__ram_real_last 
            = (1U & (~ (IData)((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len)))));
    }
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_5__DOT__unnamedblk1__DOT__do_enq 
        = (0x7ffU & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_5__DOT__full)) 
                     & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_2) 
                        & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                           >> 5U))));
    if (ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_5__DOT__unnamedblk1__DOT__do_enq) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_5__DOT__ram_real_last 
            = (1U & (~ (IData)((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len)))));
    }
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_4__DOT__unnamedblk1__DOT__do_enq 
        = (0xfffU & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_4__DOT__full)) 
                     & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_2) 
                        & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                           >> 4U))));
    if (ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_4__DOT__unnamedblk1__DOT__do_enq) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_4__DOT__ram_real_last 
            = (1U & (~ (IData)((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len)))));
    }
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_3__DOT__unnamedblk1__DOT__do_enq 
        = (0x1fffU & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_3__DOT__full)) 
                      & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_2) 
                         & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                            >> 3U))));
    if (ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_3__DOT__unnamedblk1__DOT__do_enq) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_3__DOT__ram_real_last 
            = (1U & (~ (IData)((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len)))));
    }
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_2__DOT__unnamedblk1__DOT__do_enq 
        = (0x3fffU & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_2__DOT__full)) 
                      & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_2) 
                         & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                            >> 2U))));
    if (ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_2__DOT__unnamedblk1__DOT__do_enq) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_2__DOT__ram_real_last 
            = (1U & (~ (IData)((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len)))));
    }
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_1__DOT__unnamedblk1__DOT__do_enq 
        = (0x7fffU & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_1__DOT__full)) 
                      & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_2) 
                         & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)) 
                            >> 1U))));
    if (ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_1__DOT__unnamedblk1__DOT__do_enq) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_1__DOT__ram_real_last 
            = (1U & (~ (IData)((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len)))));
    }
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap__DOT__unnamedblk1__DOT__do_enq 
        = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap__DOT__full)) 
           & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_2) 
              & ((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid))));
    if (ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap__DOT__unnamedblk1__DOT__do_enq) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap__DOT__ram_real_last 
            = (1U & (~ (IData)((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len)))));
    }
    if ((((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT___GEN_3)) 
          & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT___GEN_4)) 
         & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___xbar_io_imem_rlast))) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__ysyx_25020039_tag_array 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__raw_addr;
    }
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT___GEN) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__w_id 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid;
    }
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___arFIFOMap_15_T_1) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_0_last 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__requestARIO_0_1;
    }
    ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT___GEN_0 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_arready) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___deq_q_io_deq_valid));
    if (ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT___GEN_0) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk3__DOT___inc_addr_T_1 
            = (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__addr 
               + (0xffffU & ((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___deq_q_io_deq_bits_size))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk3__DOT___wrapMask_T_1 
            = (0x7fffffU & ((0xffU | ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___deq_q_io_deq_bits_len) 
                                      << 8U)) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___deq_q_io_deq_bits_size)));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk3__DOT___mux_addr_T_1 
            = (~ vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___deq_q_io_deq_bits_addr);
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__r_len 
            = (0xffU & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len) 
                        - (IData)(1U)));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__r_addr 
            = ((0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___deq_q_io_deq_bits_burst))
                ? vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___deq_q_io_deq_bits_addr
                : ((2U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___deq_q_io_deq_bits_burst))
                    ? ((0x7fffU & (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk3__DOT___inc_addr_T_1 
                                   & (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk3__DOT___wrapMask_T_1 
                                      >> 8U))) | (~ 
                                                  ((0xffff8000U 
                                                    & vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk3__DOT___mux_addr_T_1) 
                                                   | (0x7fffU 
                                                      & (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk3__DOT___mux_addr_T_1 
                                                         | (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk3__DOT___wrapMask_T_1 
                                                            >> 8U))))))
                    : vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk3__DOT___inc_addr_T_1));
    }
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q__DOT__do_enq) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q__DOT__ram 
            = (((QData)((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4delay_delayer_io_out_araddr)) 
                << 0xdU) | (QData)((IData)((((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4delay_delayer_io_out_arsize) 
                                             << 2U) 
                                            | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4delay_delayer_io_out_arburst)))));
    }
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awFIFOMap_15_T_1) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_0_last 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__requestAWIO_0_1;
    }
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_31__DOT__unnamedblk1__DOT__do_enq 
        = (((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_31__DOT__full)) 
            & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_8)) 
           & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
              >> 0xfU));
    if (ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_31__DOT__unnamedblk1__DOT__do_enq) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_31__DOT__ram_real_last 
            = (1U & (~ (IData)((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len_1)))));
    }
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_30__DOT__unnamedblk1__DOT__do_enq 
        = (3U & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_30__DOT__full)) 
                 & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_8) 
                    & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                       >> 0xeU))));
    if (ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_30__DOT__unnamedblk1__DOT__do_enq) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_30__DOT__ram_real_last 
            = (1U & (~ (IData)((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len_1)))));
    }
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_29__DOT__unnamedblk1__DOT__do_enq 
        = (7U & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_29__DOT__full)) 
                 & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_8) 
                    & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                       >> 0xdU))));
    if (ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_29__DOT__unnamedblk1__DOT__do_enq) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_29__DOT__ram_real_last 
            = (1U & (~ (IData)((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len_1)))));
    }
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_28__DOT__unnamedblk1__DOT__do_enq 
        = (0xfU & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_28__DOT__full)) 
                   & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_8) 
                      & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                         >> 0xcU))));
    if (ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_28__DOT__unnamedblk1__DOT__do_enq) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_28__DOT__ram_real_last 
            = (1U & (~ (IData)((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len_1)))));
    }
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_27__DOT__unnamedblk1__DOT__do_enq 
        = (0x1fU & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_27__DOT__full)) 
                    & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_8) 
                       & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                          >> 0xbU))));
    if (ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_27__DOT__unnamedblk1__DOT__do_enq) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_27__DOT__ram_real_last 
            = (1U & (~ (IData)((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len_1)))));
    }
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_26__DOT__unnamedblk1__DOT__do_enq 
        = (0x3fU & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_26__DOT__full)) 
                    & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_8) 
                       & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                          >> 0xaU))));
    if (ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_26__DOT__unnamedblk1__DOT__do_enq) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_26__DOT__ram_real_last 
            = (1U & (~ (IData)((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len_1)))));
    }
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_25__DOT__unnamedblk1__DOT__do_enq 
        = (0x7fU & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_25__DOT__full)) 
                    & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_8) 
                       & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                          >> 9U))));
    if (ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_25__DOT__unnamedblk1__DOT__do_enq) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_25__DOT__ram_real_last 
            = (1U & (~ (IData)((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len_1)))));
    }
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_24__DOT__unnamedblk1__DOT__do_enq 
        = (0xffU & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_24__DOT__full)) 
                    & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_8) 
                       & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                          >> 8U))));
    if (ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_24__DOT__unnamedblk1__DOT__do_enq) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_24__DOT__ram_real_last 
            = (1U & (~ (IData)((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len_1)))));
    }
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_23__DOT__unnamedblk1__DOT__do_enq 
        = (0x1ffU & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_23__DOT__full)) 
                     & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_8) 
                        & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                           >> 7U))));
    if (ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_23__DOT__unnamedblk1__DOT__do_enq) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_23__DOT__ram_real_last 
            = (1U & (~ (IData)((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len_1)))));
    }
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_22__DOT__unnamedblk1__DOT__do_enq 
        = (0x3ffU & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_22__DOT__full)) 
                     & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_8) 
                        & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                           >> 6U))));
    if (ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_22__DOT__unnamedblk1__DOT__do_enq) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_22__DOT__ram_real_last 
            = (1U & (~ (IData)((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len_1)))));
    }
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_21__DOT__unnamedblk1__DOT__do_enq 
        = (0x7ffU & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_21__DOT__full)) 
                     & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_8) 
                        & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                           >> 5U))));
    if (ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_21__DOT__unnamedblk1__DOT__do_enq) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_21__DOT__ram_real_last 
            = (1U & (~ (IData)((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len_1)))));
    }
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_20__DOT__unnamedblk1__DOT__do_enq 
        = (0xfffU & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_20__DOT__full)) 
                     & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_8) 
                        & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                           >> 4U))));
    if (ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_20__DOT__unnamedblk1__DOT__do_enq) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_20__DOT__ram_real_last 
            = (1U & (~ (IData)((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len_1)))));
    }
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_19__DOT__unnamedblk1__DOT__do_enq 
        = (0x1fffU & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_19__DOT__full)) 
                      & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_8) 
                         & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                            >> 3U))));
    if (ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_19__DOT__unnamedblk1__DOT__do_enq) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_19__DOT__ram_real_last 
            = (1U & (~ (IData)((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len_1)))));
    }
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_18__DOT__unnamedblk1__DOT__do_enq 
        = (0x3fffU & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_18__DOT__full)) 
                      & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_8) 
                         & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                            >> 2U))));
    if (ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_18__DOT__unnamedblk1__DOT__do_enq) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_18__DOT__ram_real_last 
            = (1U & (~ (IData)((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len_1)))));
    }
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_17__DOT__unnamedblk1__DOT__do_enq 
        = (0x7fffU & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_17__DOT__full)) 
                      & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_8) 
                         & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)) 
                            >> 1U))));
    if (ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_17__DOT__unnamedblk1__DOT__do_enq) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_17__DOT__ram_real_last 
            = (1U & (~ (IData)((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len_1)))));
    }
    ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_16__DOT__unnamedblk1__DOT__do_enq 
        = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_16__DOT__full)) 
           & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_8) 
              & ((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid))));
    if (ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_16__DOT__unnamedblk1__DOT__do_enq) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_16__DOT__ram_real_last 
            = (1U & (~ (IData)((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len_1)))));
    }
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q_1__DOT__do_enq) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q_1__DOT__ram 
            = (((QData)((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4delay_delayer_io_out_awaddr)) 
                << 0xdU) | (QData)((IData)((((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4delay_delayer_io_out_awsize) 
                                             << 2U) 
                                            | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4delay_delayer_io_out_awburst)))));
    }
    ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT___GEN_1 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__in_awready) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___deq_q_1_io_deq_valid));
    if (vlSelf->reset) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_13_count = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_12_count = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_11_count = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_10_count = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_9_count = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_8_count = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_7_count = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_6_count = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_1_count = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_3_count = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_4_count = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_5_count = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_2_count = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_14_count = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_15_count = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_13_count = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_12_count = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_11_count = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_10_count = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_9_count = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_8_count = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_7_count = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_6_count = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_4_count = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_1_count = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_2_count = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_3_count = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_5_count = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_14_count = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_15_count = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__y_cnt = 1U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__counter = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__in_wdeq_q__DOT__full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__wrap = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__wrap_1 = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_0_count = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q__DOT__full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__x_cnt = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__vga_apb_state = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_0_count = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__count = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__w_ptr = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__r_ptr = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__key_overflow = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__key_ready = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_0 = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_1 = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_2 = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_3 = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_4 = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_5 = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_6 = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_7 = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_8 = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_9 = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_10 = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_11 = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_12 = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_13 = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_14 = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_15 = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__wrap = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__wrap_1 = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q_1__DOT__full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__burstCntIn = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__wrap = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__wrap = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__wrap = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__wrap = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__wrap = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi42apb__DOT__state = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__maybe_full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_15__DOT__full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_14__DOT__full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_13__DOT__full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_12__DOT__full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_11__DOT__full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_10__DOT__full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_9__DOT__full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_8__DOT__full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_7__DOT__full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_6__DOT__full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_5__DOT__full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_4__DOT__full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_3__DOT__full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_2__DOT__full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_1__DOT__full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap__DOT__full = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__busy = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__maybe_full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__idle_3 = 1U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__readys_mask_1 = 3U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__state_3_0 = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__state_3_1 = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_31__DOT__full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_30__DOT__full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_29__DOT__full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_28__DOT__full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_27__DOT__full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_26__DOT__full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_25__DOT__full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_24__DOT__full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_23__DOT__full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_22__DOT__full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_21__DOT__full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_20__DOT__full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_19__DOT__full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_18__DOT__full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_17__DOT__full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_16__DOT__full = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__busy_1 = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__idle_2 = 1U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__readys_mask = 3U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__state_2_0 = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__state_2_1 = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__wrap_1 = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__maybe_full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__wrap_1 = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__maybe_full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__delayCnt = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__rdata_0 = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__rdata_1 = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__rdata_2 = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__rdata_3 = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__rdata_4 = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__rdata_5 = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__rdata_6 = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__rdata_7 = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__burstCntOut = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_0 = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_1 = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_2 = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_3 = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_4 = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_5 = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_6 = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_7 = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__wrap_1 = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__maybe_full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__wrap_1 = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__maybe_full = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__wrap_1 = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__maybe_full = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__w_counter = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__state = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__idle_3 = 1U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__readys_mask = 7U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__state_3_0 = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__state_3_1 = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__state_3_2 = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__idle_4 = 1U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__readys_mask_1 = 7U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__state_4_0 = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__state_4_2 = 0U;
    } else {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_13_count 
            = (7U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_13_count) 
                     - (7U & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_bid)) 
                               >> 0xdU) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awFIFOMap_15_T_4)))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_12_count 
            = (7U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_12_count) 
                     - (0xfU & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_bid)) 
                                 >> 0xcU) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awFIFOMap_15_T_4)))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_11_count 
            = (7U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_11_count) 
                     - (0x1fU & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_bid)) 
                                  >> 0xbU) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awFIFOMap_15_T_4)))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_10_count 
            = (7U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_10_count) 
                     - (0x3fU & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_bid)) 
                                  >> 0xaU) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awFIFOMap_15_T_4)))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_9_count 
            = (7U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_9_count) 
                     - (0x7fU & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_bid)) 
                                  >> 9U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awFIFOMap_15_T_4)))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_8_count 
            = (7U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_8_count) 
                     - (0xffU & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_bid)) 
                                  >> 8U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awFIFOMap_15_T_4)))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_7_count 
            = (7U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_7_count) 
                     - (0x1ffU & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_bid)) 
                                   >> 7U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awFIFOMap_15_T_4)))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_6_count 
            = (7U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_6_count) 
                     - (0x3ffU & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_bid)) 
                                   >> 6U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awFIFOMap_15_T_4)))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_1_count 
            = (7U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_1_count) 
                     - (0x7fffU & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_bid)) 
                                    >> 1U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awFIFOMap_15_T_4)))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_3_count 
            = (7U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_3_count) 
                     - (0x1fffU & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_bid)) 
                                    >> 3U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awFIFOMap_15_T_4)))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_4_count 
            = (7U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_4_count) 
                     - (0xfffU & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_bid)) 
                                   >> 4U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awFIFOMap_15_T_4)))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_5_count 
            = (7U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_5_count) 
                     - (0x7ffU & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_bid)) 
                                   >> 5U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awFIFOMap_15_T_4)))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_2_count 
            = (7U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_2_count) 
                     - (0x3fffU & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_bid)) 
                                    >> 2U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awFIFOMap_15_T_4)))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_14_count 
            = (7U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_14_count) 
                     - (3U & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_bid)) 
                               >> 0xeU) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awFIFOMap_15_T_4)))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_15_count 
            = (7U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_15_count) 
                     - (1U & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_bid)) 
                               >> 0xfU) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awFIFOMap_15_T_4)))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_13_count 
            = (7U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_13_count) 
                     - (7U & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_rid)) 
                               >> 0xdU) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_h52ee47fe__0)))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_12_count 
            = (7U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_12_count) 
                     - (0xfU & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_rid)) 
                                 >> 0xcU) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_h52ee47fe__0)))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_11_count 
            = (7U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_11_count) 
                     - (0x1fU & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_rid)) 
                                  >> 0xbU) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_h52ee47fe__0)))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_10_count 
            = (7U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_10_count) 
                     - (0x3fU & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_rid)) 
                                  >> 0xaU) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_h52ee47fe__0)))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_9_count 
            = (7U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_9_count) 
                     - (0x7fU & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_rid)) 
                                  >> 9U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_h52ee47fe__0)))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_8_count 
            = (7U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_8_count) 
                     - (0xffU & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_rid)) 
                                  >> 8U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_h52ee47fe__0)))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_7_count 
            = (7U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_7_count) 
                     - (0x1ffU & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_rid)) 
                                   >> 7U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_h52ee47fe__0)))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_6_count 
            = (7U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_6_count) 
                     - (0x3ffU & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_rid)) 
                                   >> 6U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_h52ee47fe__0)))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_4_count 
            = (7U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_4_count) 
                     - (0xfffU & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_rid)) 
                                   >> 4U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_h52ee47fe__0)))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_1_count 
            = (7U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_1_count) 
                     - (0x7fffU & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_rid)) 
                                    >> 1U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_h52ee47fe__0)))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_2_count 
            = (7U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_2_count) 
                     - (0x3fffU & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_rid)) 
                                    >> 2U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_h52ee47fe__0)))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_3_count 
            = (7U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_3_count) 
                     - (0x1fffU & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_rid)) 
                                    >> 3U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_h52ee47fe__0)))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_5_count 
            = (7U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_5_count) 
                     - (0x7ffU & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_rid)) 
                                   >> 5U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_h52ee47fe__0)))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_14_count 
            = (7U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_14_count) 
                     - (3U & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_rid)) 
                               >> 0xeU) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_h52ee47fe__0)))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_15_count 
            = (7U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_15_count) 
                     - (1U & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_rid)) 
                               >> 0xfU) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_h52ee47fe__0)))));
        if ((0x320U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__x_cnt))) {
            if ((0x20dU == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__y_cnt))) {
                __Vdly__ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__y_cnt = 1U;
                __Vdly__ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__x_cnt = 0U;
            } else {
                __Vdly__ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__y_cnt 
                    = (0x3ffU & ((IData)(1U) + (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__y_cnt)));
                __Vdly__ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__x_cnt = 1U;
            }
        } else if ((0U < (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__x_cnt))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__x_cnt 
                = (0x3ffU & ((IData)(1U) + (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__x_cnt)));
        } else if (vlSelf->ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__sync_reg) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__x_cnt = 1U;
        }
        vlSelf->ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__counter 
            = ((0x20dU == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__y_cnt))
                ? 0U : (0x1fffffU & ((IData)(vlSelf->externalPins_vga_valid)
                                      ? ((IData)(1U) 
                                         + vlSelf->ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__counter)
                                      : vlSelf->ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__counter)));
        if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__in_wdeq_q__DOT__do_enq) 
             != (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__in_wdeq_q__DOT__full) 
                  & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT____Vcellinp__in_wdeq_q__io_deq_ready)) 
                 & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___in_wdeq_q_io_deq_valid)))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__in_wdeq_q__DOT__full 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__in_wdeq_q__DOT__do_enq;
        }
        if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__do_enq) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__wrap 
                = (1U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__wrap) 
                         - (IData)(1U)));
        }
        if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__do_deq) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__wrap_1 
                = (1U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__wrap_1) 
                         - (IData)(1U)));
        }
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_0_count 
            = (7U & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__arFIFOMap_0_count) 
                      + (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___arFIFOMap_15_T_1)) 
                     - (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_rid)) 
                        & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_h52ee47fe__0))));
        if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q__DOT__do_enq) 
             != (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q__DOT__full) 
                  & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT____Vcellinp__deq_q__io_deq_ready)) 
                 & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___deq_q_io_deq_valid)))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q__DOT__full 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q__DOT__do_enq;
        }
        if ((0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__vga_apb_state))) {
            if (((IData)(((0x20000000U == (0x30000000U 
                                           & vlSelf->ysyxSoCFull__DOT__asic__DOT___axi42apb_auto_out_paddr)) 
                          & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___apbdelay_delayer_out_psel))) 
                 & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___apbdelay_delayer_out_pwrite))) {
                __Vdly__ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__vga_apb_state = 1U;
            }
        } else {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__vga_apb_state = 0U;
        }
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_0_count 
            = (7U & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_0_count) 
                      + (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awFIFOMap_15_T_1)) 
                     - (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_bid)) 
                        & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awFIFOMap_15_T_4))));
        if (vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__key_ready) {
            if ((1U & (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__next_read_en)))) {
                __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__fifo__v0 = 1U;
                __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__fifo__v0 
                    = (0x7fU & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__r_ptr) 
                                - (IData)(1U)));
                __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__r_ptr 
                    = (0x7fU & ((IData)(1U) + (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__r_ptr)));
                if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__w_ptr) 
                     == (0x7fU & ((IData)(1U) + (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__r_ptr))))) {
                    __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__key_ready = 0U;
                }
            }
        }
        if ((IData)((4U == (6U & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__ps2_clk_sync))))) {
            if ((0xaU == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__count))) {
                if ((((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__buffer)) 
                      & (IData)(vlSelf->externalPins_ps2_data)) 
                     & VL_REDXOR_32((0x1ffU & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__buffer) 
                                               >> 1U))))) {
                    __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__fifo__v1 
                        = (0xffU & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__buffer) 
                                    >> 1U));
                    __Vdlyvset__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__fifo__v1 = 1U;
                    __Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__fifo__v1 
                        = vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__w_ptr;
                    __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__key_ready = 1U;
                    __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__w_ptr 
                        = (0x7fU & ((IData)(1U) + (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__w_ptr)));
                    __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__key_overflow 
                        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__key_overflow) 
                           | ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__r_ptr) 
                              == (0x7fU & ((IData)(1U) 
                                           + (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__w_ptr)))));
                }
                __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__count = 0U;
            } else {
                vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT____Vlvbound_h1a91ade8__0 
                    = vlSelf->externalPins_ps2_data;
                if ((9U >= (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__count))) {
                    vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__buffer 
                        = (((~ ((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__count))) 
                            & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__buffer)) 
                           | (0x3ffU & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT____Vlvbound_h1a91ade8__0) 
                                        << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__count))));
                }
                __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__count 
                    = (0xfU & ((IData)(1U) + (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__count)));
            }
        }
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk2__DOT___GEN_2 
            = (0xffffU & ((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk2__DOT___GEN_3 
            = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_bready) 
               & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_bvalid));
        if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk2__DOT___GEN_2) 
             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk2__DOT___GEN_3))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_0 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_becho_real_last)
                    ? 0U : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_0) 
                            | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_bresp)));
        }
        if ((((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk2__DOT___GEN_2) 
              >> 1U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk2__DOT___GEN_3))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_1 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_becho_real_last)
                    ? 0U : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_1) 
                            | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_bresp)));
        }
        if ((((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk2__DOT___GEN_2) 
              >> 2U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk2__DOT___GEN_3))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_2 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_becho_real_last)
                    ? 0U : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_2) 
                            | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_bresp)));
        }
        if ((((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk2__DOT___GEN_2) 
              >> 3U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk2__DOT___GEN_3))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_3 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_becho_real_last)
                    ? 0U : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_3) 
                            | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_bresp)));
        }
        if ((((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk2__DOT___GEN_2) 
              >> 4U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk2__DOT___GEN_3))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_4 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_becho_real_last)
                    ? 0U : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_4) 
                            | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_bresp)));
        }
        if ((((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk2__DOT___GEN_2) 
              >> 5U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk2__DOT___GEN_3))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_5 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_becho_real_last)
                    ? 0U : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_5) 
                            | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_bresp)));
        }
        if ((((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk2__DOT___GEN_2) 
              >> 6U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk2__DOT___GEN_3))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_6 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_becho_real_last)
                    ? 0U : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_6) 
                            | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_bresp)));
        }
        if ((((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk2__DOT___GEN_2) 
              >> 7U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk2__DOT___GEN_3))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_7 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_becho_real_last)
                    ? 0U : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_7) 
                            | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_bresp)));
        }
        if ((((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk2__DOT___GEN_2) 
              >> 8U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk2__DOT___GEN_3))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_8 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_becho_real_last)
                    ? 0U : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_8) 
                            | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_bresp)));
        }
        if ((((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk2__DOT___GEN_2) 
              >> 9U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk2__DOT___GEN_3))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_9 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_becho_real_last)
                    ? 0U : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_9) 
                            | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_bresp)));
        }
        if ((((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk2__DOT___GEN_2) 
              >> 0xaU) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk2__DOT___GEN_3))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_10 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_becho_real_last)
                    ? 0U : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_10) 
                            | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_bresp)));
        }
        if ((((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk2__DOT___GEN_2) 
              >> 0xbU) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk2__DOT___GEN_3))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_11 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_becho_real_last)
                    ? 0U : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_11) 
                            | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_bresp)));
        }
        if ((((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk2__DOT___GEN_2) 
              >> 0xcU) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk2__DOT___GEN_3))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_12 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_becho_real_last)
                    ? 0U : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_12) 
                            | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_bresp)));
        }
        if ((((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk2__DOT___GEN_2) 
              >> 0xdU) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk2__DOT___GEN_3))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_13 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_becho_real_last)
                    ? 0U : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_13) 
                            | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_bresp)));
        }
        if ((((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk2__DOT___GEN_2) 
              >> 0xeU) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk2__DOT___GEN_3))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_14 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_becho_real_last)
                    ? 0U : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_14) 
                            | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_bresp)));
        }
        if ((((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk2__DOT___GEN_2) 
              >> 0xfU) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk2__DOT___GEN_3))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_15 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_becho_real_last)
                    ? 0U : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_15) 
                            | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_bresp)));
        }
        if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__do_enq) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__wrap 
                = (1U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__wrap) 
                         - (IData)(1U)));
        }
        if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__do_deq) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__wrap_1 
                = (1U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__wrap_1) 
                         - (IData)(1U)));
        }
        if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q_1__DOT__do_enq) 
             != (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q_1__DOT__full) 
                  & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT____Vcellinp__deq_q_1__io_deq_ready)) 
                 & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___deq_q_1_io_deq_valid)))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q_1__DOT__full 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q_1__DOT__do_enq;
        }
        if (vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4delay_delayer_io_out_arvalid) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__burstCntIn = 0U;
        } else if ((((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4delay_delayer_io_out_rready) 
                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4delay_delayer_io_in_rvalid)) 
                    & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__burstCntIn) 
                       < (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__burstLenR)))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__burstCntIn 
                = (0xffU & ((IData)(1U) + (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__burstCntIn)));
        }
        if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__do_enq) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__wrap 
                = (1U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__wrap) 
                         - (IData)(1U)));
        }
        if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__do_enq) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__wrap 
                = (1U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__wrap) 
                         - (IData)(1U)));
        }
        if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__do_enq) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__wrap 
                = (1U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__wrap) 
                         - (IData)(1U)));
        }
        if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__do_enq) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__wrap 
                = (1U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__wrap) 
                         - (IData)(1U)));
        }
        if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__do_enq) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__wrap 
                = (1U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__wrap) 
                         - (IData)(1U)));
        }
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi42apb__DOT__unnamedblk1__DOT___GEN 
            = (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi42apb__DOT__state) 
                << 6U) | ((0x20U & ((~ (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4buf_auto_out_rready) 
                                         & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi42apb_auto_in_rvalid)) 
                                        | ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4buf_auto_out_bready) 
                                           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi42apb_auto_in_bvalid)))) 
                                    << 5U)) | ((((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__apbdelay_delayer__DOT__delay_pready)
                                                  ? 
                                                 (2U 
                                                  & ((~ 
                                                      (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4buf_auto_out_rready) 
                                                        & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi42apb_auto_in_rvalid)) 
                                                       | ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4buf_auto_out_bready) 
                                                          & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi42apb_auto_in_bvalid)))) 
                                                     << 1U))
                                                  : 1U) 
                                                << 2U) 
                                               | (1U 
                                                  & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__empty)) 
                                                     | ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__empty)) 
                                                        & (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__empty))))))));
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi42apb__DOT__state 
            = (3U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi42apb__DOT__unnamedblk1__DOT___GEN) 
                     >> (7U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi42apb__DOT__state) 
                               << 1U))));
        if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__do_enq) 
             != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__do_deq))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__maybe_full 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__do_enq;
        }
        if (((IData)(ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_15__DOT__unnamedblk1__DOT__do_enq) 
             != ((((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_1) 
                   & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_rlast)) 
                  & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_15__DOT__full)) 
                 & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                    >> 0xfU)))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_15__DOT__full 
                = ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_15__DOT__unnamedblk1__DOT__do_enq;
        }
        if (((IData)(ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_14__DOT__unnamedblk1__DOT__do_enq) 
             != (3U & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_1) 
                        & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                            >> 0xeU) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_rlast))) 
                       & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_14__DOT__full))))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_14__DOT__full 
                = ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_14__DOT__unnamedblk1__DOT__do_enq;
        }
        if (((IData)(ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_13__DOT__unnamedblk1__DOT__do_enq) 
             != (7U & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_1) 
                        & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                            >> 0xdU) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_rlast))) 
                       & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_13__DOT__full))))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_13__DOT__full 
                = ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_13__DOT__unnamedblk1__DOT__do_enq;
        }
        if (((IData)(ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_12__DOT__unnamedblk1__DOT__do_enq) 
             != (0xfU & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_1) 
                          & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                              >> 0xcU) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_rlast))) 
                         & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_12__DOT__full))))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_12__DOT__full 
                = ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_12__DOT__unnamedblk1__DOT__do_enq;
        }
        if (((IData)(ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_11__DOT__unnamedblk1__DOT__do_enq) 
             != (0x1fU & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_1) 
                           & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                               >> 0xbU) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_rlast))) 
                          & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_11__DOT__full))))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_11__DOT__full 
                = ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_11__DOT__unnamedblk1__DOT__do_enq;
        }
        if (((IData)(ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_10__DOT__unnamedblk1__DOT__do_enq) 
             != (0x3fU & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_1) 
                           & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                               >> 0xaU) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_rlast))) 
                          & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_10__DOT__full))))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_10__DOT__full 
                = ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_10__DOT__unnamedblk1__DOT__do_enq;
        }
        if (((IData)(ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_9__DOT__unnamedblk1__DOT__do_enq) 
             != (0x7fU & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_1) 
                           & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                               >> 9U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_rlast))) 
                          & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_9__DOT__full))))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_9__DOT__full 
                = ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_9__DOT__unnamedblk1__DOT__do_enq;
        }
        if (((IData)(ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_8__DOT__unnamedblk1__DOT__do_enq) 
             != (0xffU & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_1) 
                           & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                               >> 8U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_rlast))) 
                          & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_8__DOT__full))))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_8__DOT__full 
                = ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_8__DOT__unnamedblk1__DOT__do_enq;
        }
        if (((IData)(ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_7__DOT__unnamedblk1__DOT__do_enq) 
             != (0x1ffU & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_1) 
                            & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                >> 7U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_rlast))) 
                           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_7__DOT__full))))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_7__DOT__full 
                = ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_7__DOT__unnamedblk1__DOT__do_enq;
        }
        if (((IData)(ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_6__DOT__unnamedblk1__DOT__do_enq) 
             != (0x3ffU & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_1) 
                            & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                >> 6U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_rlast))) 
                           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_6__DOT__full))))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_6__DOT__full 
                = ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_6__DOT__unnamedblk1__DOT__do_enq;
        }
        if (((IData)(ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_5__DOT__unnamedblk1__DOT__do_enq) 
             != (0x7ffU & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_1) 
                            & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                >> 5U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_rlast))) 
                           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_5__DOT__full))))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_5__DOT__full 
                = ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_5__DOT__unnamedblk1__DOT__do_enq;
        }
        if (((IData)(ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_4__DOT__unnamedblk1__DOT__do_enq) 
             != (0xfffU & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_1) 
                            & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                >> 4U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_rlast))) 
                           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_4__DOT__full))))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_4__DOT__full 
                = ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_4__DOT__unnamedblk1__DOT__do_enq;
        }
        if (((IData)(ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_3__DOT__unnamedblk1__DOT__do_enq) 
             != (0x1fffU & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_1) 
                             & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                 >> 3U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_rlast))) 
                            & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_3__DOT__full))))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_3__DOT__full 
                = ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_3__DOT__unnamedblk1__DOT__do_enq;
        }
        if (((IData)(ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_2__DOT__unnamedblk1__DOT__do_enq) 
             != (0x3fffU & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_1) 
                             & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                 >> 2U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_rlast))) 
                            & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_2__DOT__full))))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_2__DOT__full 
                = ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_2__DOT__unnamedblk1__DOT__do_enq;
        }
        if (((IData)(ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_1__DOT__unnamedblk1__DOT__do_enq) 
             != (0x7fffU & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_1) 
                             & ((((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                                 >> 1U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_rlast))) 
                            & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_1__DOT__full))))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_1__DOT__full 
                = ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_1__DOT__unnamedblk1__DOT__do_enq;
        }
        if (((IData)(ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap__DOT__unnamedblk1__DOT__do_enq) 
             != (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_1) 
                  & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid)) 
                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_rlast))) 
                 & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap__DOT__full)))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap__DOT__full 
                = ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap__DOT__unnamedblk1__DOT__do_enq;
        }
        if (ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT___GEN_0) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__busy 
                = (0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len));
        }
        if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__do_enq) 
             != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__do_deq))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__maybe_full 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__do_enq;
        }
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__idle_3 
            = (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___cpu_auto_master_out_bready) 
                & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_bvalid)) 
               | ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__anyValid_1)) 
                  & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__idle_3)));
        if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__idle_3) 
             & (0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__readys_valid_1)))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__unnamedblk2__DOT___readys_mask_T_5 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__readys_readys_1) 
                   & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__readys_valid_1));
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__readys_mask_1 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__unnamedblk2__DOT___readys_mask_T_5) 
                   | (2U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__unnamedblk2__DOT___readys_mask_T_5) 
                            << 1U)));
        }
        if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__idle_3) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__state_3_0 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__winner_3_0;
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__state_3_1 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__winner_3_1;
        }
        if (((IData)(ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_31__DOT__unnamedblk1__DOT__do_enq) 
             != (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7) 
                  & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_31__DOT__full)) 
                 & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                    >> 0xfU)))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_31__DOT__full 
                = ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_31__DOT__unnamedblk1__DOT__do_enq;
        }
        if (((IData)(ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_30__DOT__unnamedblk1__DOT__do_enq) 
             != (3U & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7) 
                        & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                           >> 0xeU)) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_30__DOT__full))))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_30__DOT__full 
                = ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_30__DOT__unnamedblk1__DOT__do_enq;
        }
        if (((IData)(ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_29__DOT__unnamedblk1__DOT__do_enq) 
             != (7U & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7) 
                        & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                           >> 0xdU)) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_29__DOT__full))))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_29__DOT__full 
                = ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_29__DOT__unnamedblk1__DOT__do_enq;
        }
        if (((IData)(ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_28__DOT__unnamedblk1__DOT__do_enq) 
             != (0xfU & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7) 
                          & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                             >> 0xcU)) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_28__DOT__full))))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_28__DOT__full 
                = ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_28__DOT__unnamedblk1__DOT__do_enq;
        }
        if (((IData)(ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_27__DOT__unnamedblk1__DOT__do_enq) 
             != (0x1fU & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7) 
                           & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                              >> 0xbU)) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_27__DOT__full))))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_27__DOT__full 
                = ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_27__DOT__unnamedblk1__DOT__do_enq;
        }
        if (((IData)(ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_26__DOT__unnamedblk1__DOT__do_enq) 
             != (0x3fU & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7) 
                           & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                              >> 0xaU)) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_26__DOT__full))))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_26__DOT__full 
                = ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_26__DOT__unnamedblk1__DOT__do_enq;
        }
        if (((IData)(ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_25__DOT__unnamedblk1__DOT__do_enq) 
             != (0x7fU & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7) 
                           & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                              >> 9U)) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_25__DOT__full))))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_25__DOT__full 
                = ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_25__DOT__unnamedblk1__DOT__do_enq;
        }
        if (((IData)(ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_24__DOT__unnamedblk1__DOT__do_enq) 
             != (0xffU & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7) 
                           & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                              >> 8U)) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_24__DOT__full))))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_24__DOT__full 
                = ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_24__DOT__unnamedblk1__DOT__do_enq;
        }
        if (((IData)(ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_23__DOT__unnamedblk1__DOT__do_enq) 
             != (0x1ffU & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7) 
                            & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                               >> 7U)) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_23__DOT__full))))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_23__DOT__full 
                = ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_23__DOT__unnamedblk1__DOT__do_enq;
        }
        if (((IData)(ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_22__DOT__unnamedblk1__DOT__do_enq) 
             != (0x3ffU & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7) 
                            & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                               >> 6U)) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_22__DOT__full))))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_22__DOT__full 
                = ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_22__DOT__unnamedblk1__DOT__do_enq;
        }
        if (((IData)(ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_21__DOT__unnamedblk1__DOT__do_enq) 
             != (0x7ffU & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7) 
                            & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                               >> 5U)) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_21__DOT__full))))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_21__DOT__full 
                = ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_21__DOT__unnamedblk1__DOT__do_enq;
        }
        if (((IData)(ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_20__DOT__unnamedblk1__DOT__do_enq) 
             != (0xfffU & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7) 
                            & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                               >> 4U)) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_20__DOT__full))))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_20__DOT__full 
                = ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_20__DOT__unnamedblk1__DOT__do_enq;
        }
        if (((IData)(ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_19__DOT__unnamedblk1__DOT__do_enq) 
             != (0x1fffU & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7) 
                             & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                                >> 3U)) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_19__DOT__full))))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_19__DOT__full 
                = ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_19__DOT__unnamedblk1__DOT__do_enq;
        }
        if (((IData)(ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_18__DOT__unnamedblk1__DOT__do_enq) 
             != (0x3fffU & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7) 
                             & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                                >> 2U)) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_18__DOT__full))))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_18__DOT__full 
                = ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_18__DOT__unnamedblk1__DOT__do_enq;
        }
        if (((IData)(ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_17__DOT__unnamedblk1__DOT__do_enq) 
             != (0x7fffU & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7) 
                             & (((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)) 
                                >> 1U)) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_17__DOT__full))))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_17__DOT__full 
                = ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_17__DOT__unnamedblk1__DOT__do_enq;
        }
        if (((IData)(ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_16__DOT__unnamedblk1__DOT__do_enq) 
             != (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT___GEN_7) 
                  & ((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid))) 
                 & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_16__DOT__full)))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_16__DOT__full 
                = ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_16__DOT__unnamedblk1__DOT__do_enq;
        }
        if (ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT___GEN_1) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__busy_1 
                = (0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len_1));
        }
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__idle_2 
            = (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___cpu_auto_master_out_rready) 
                & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_rvalid)) 
               | ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__anyValid)) 
                  & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__idle_2)));
        if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__idle_2) 
             & (0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__readys_valid)))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__unnamedblk1__DOT___readys_mask_T 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__readys_readys) 
                   & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__readys_valid));
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__readys_mask 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__unnamedblk1__DOT___readys_mask_T) 
                   | (2U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__unnamedblk1__DOT___readys_mask_T) 
                            << 1U)));
        }
        if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__idle_2) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__state_2_0 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__prefixOR_1;
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__state_2_1 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__winner_2_1;
        }
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__unnamedblk1__DOT__do_deq 
            = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi42apb_auto_in_awready) 
               & (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__empty)));
        if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__unnamedblk1__DOT__do_deq) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__wrap_1 
                = (1U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__wrap_1) 
                         - (IData)(1U)));
        }
        if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__do_enq) 
             != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__unnamedblk1__DOT__do_deq))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__maybe_full 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__do_enq;
        }
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__unnamedblk1__DOT__do_deq 
            = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi42apb_auto_in_awready) 
               & (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__empty)));
        if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__unnamedblk1__DOT__do_deq) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__wrap_1 
                = (1U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__wrap_1) 
                         - (IData)(1U)));
        }
        if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__do_enq) 
             != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__unnamedblk1__DOT__do_deq))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__maybe_full 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__do_enq;
        }
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_2 
            = (1U == vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__nextState);
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_12 
            = (0U == vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__nextState);
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_3 
            = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_2) 
               & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___lsdram_axi_auto_in_rvalid));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_4 
            = (1U & ((0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__state)) 
                     | (~ ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_3) 
                           & (0U == (7U & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__burstCntOut)))))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_5 
            = (1U & ((0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__state)) 
                     | (~ ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_3) 
                           & (1U == (7U & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__burstCntOut)))))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_6 
            = (1U & ((0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__state)) 
                     | (~ ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_3) 
                           & (2U == (7U & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__burstCntOut)))))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_7 
            = (1U & ((0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__state)) 
                     | (~ ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_3) 
                           & (3U == (7U & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__burstCntOut)))))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_8 
            = (1U & ((0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__state)) 
                     | (~ ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_3) 
                           & (4U == (7U & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__burstCntOut)))))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_9 
            = (1U & ((0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__state)) 
                     | (~ ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_3) 
                           & (5U == (7U & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__burstCntOut)))))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_10 
            = (1U & ((0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__state)) 
                     | (~ ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_3) 
                           & (6U == (7U & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__burstCntOut)))))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_11 
            = (1U & ((0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__state)) 
                     | (~ ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_3) 
                           & (7U == (7U & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__burstCntOut)))))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_13 
            = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_2) 
               | (2U == vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__nextState));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_14 
            = (3U == vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__nextState);
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___singleBurstDelay_7_T 
            = (3U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__state));
        if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_12) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__delayCnt = 0U;
        } else if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_13) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__delayCnt 
                = (0xffffU & ((IData)(5U) + (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__delayCnt)));
        } else if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_14) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__delayCnt 
                = (0xffffU & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___singleBurstDelay_7_T)
                               ? (0x7fffU & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__delayCnt) 
                                              >> 1U) 
                                             - (IData)(1U)))
                               : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__delayCnt) 
                                  - (IData)(1U))));
        }
        if ((1U & (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_4)))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__rdata_0 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT___lsdram_axi_auto_in_rdata;
        }
        if ((1U & (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_5)))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__rdata_1 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT___lsdram_axi_auto_in_rdata;
        }
        if ((1U & (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_6)))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__rdata_2 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT___lsdram_axi_auto_in_rdata;
        }
        if ((1U & (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_7)))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__rdata_3 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT___lsdram_axi_auto_in_rdata;
        }
        if ((1U & (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_8)))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__rdata_4 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT___lsdram_axi_auto_in_rdata;
        }
        if ((1U & (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_9)))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__rdata_5 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT___lsdram_axi_auto_in_rdata;
        }
        if ((1U & (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_10)))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__rdata_6 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT___lsdram_axi_auto_in_rdata;
        }
        if ((1U & (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_11)))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__rdata_7 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT___lsdram_axi_auto_in_rdata;
        }
        if ((0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__state))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__burstCntOut = 0U;
        } else if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_3) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__burstCntOut 
                = (0xffU & ((IData)(1U) + (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__burstCntOut)));
        }
        if ((1U & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_12) 
                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_13)) 
                   | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_14))))) {
            if ((1U & (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_4)))) {
                __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_0 
                    = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__delayCnt;
            }
            if ((1U & (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_5)))) {
                __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_1 
                    = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__delayCnt;
            }
            if ((1U & (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_6)))) {
                __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_2 
                    = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__delayCnt;
            }
            if ((1U & (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_7)))) {
                __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_3 
                    = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__delayCnt;
            }
            if ((1U & (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_8)))) {
                __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_4 
                    = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__delayCnt;
            }
            if ((1U & (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_9)))) {
                __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_5 
                    = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__delayCnt;
            }
            if ((1U & (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_10)))) {
                __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_6 
                    = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__delayCnt;
            }
            if ((1U & (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___GEN_11)))) {
                __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_7 
                    = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__delayCnt;
            }
        } else {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT__unnamedblk2__DOT___singleBurstDelay_0_T_1 
                = (0xffffU & ((IData)(5U) + (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_0)));
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT__unnamedblk2__DOT___singleBurstDelay_1_T_1 
                = (0xffffU & ((IData)(5U) + (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_1)));
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT__unnamedblk2__DOT___singleBurstDelay_2_T_1 
                = (0xffffU & ((IData)(5U) + (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_2)));
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT__unnamedblk2__DOT___singleBurstDelay_3_T_1 
                = (0xffffU & ((IData)(5U) + (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_3)));
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT__unnamedblk2__DOT___singleBurstDelay_4_T_1 
                = (0xffffU & ((IData)(5U) + (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_4)));
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT__unnamedblk2__DOT___singleBurstDelay_5_T_1 
                = (0xffffU & ((IData)(5U) + (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_5)));
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT__unnamedblk2__DOT___singleBurstDelay_6_T_1 
                = (0xffffU & ((IData)(5U) + (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_6)));
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT__unnamedblk2__DOT___singleBurstDelay_7_T_1 
                = (0xffffU & ((IData)(5U) + (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_7)));
            if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT___singleBurstDelay_7_T) {
                __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_0 
                    = (0xffffU & (0x7fffU & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT__unnamedblk2__DOT___singleBurstDelay_0_T_1) 
                                              >> 1U) 
                                             - (IData)(1U))));
                __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_1 
                    = (0xffffU & (0x7fffU & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT__unnamedblk2__DOT___singleBurstDelay_1_T_1) 
                                              >> 1U) 
                                             - (IData)(1U))));
                __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_2 
                    = (0xffffU & (0x7fffU & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT__unnamedblk2__DOT___singleBurstDelay_2_T_1) 
                                              >> 1U) 
                                             - (IData)(1U))));
                __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_3 
                    = (0xffffU & (0x7fffU & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT__unnamedblk2__DOT___singleBurstDelay_3_T_1) 
                                              >> 1U) 
                                             - (IData)(1U))));
                __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_4 
                    = (0xffffU & (0x7fffU & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT__unnamedblk2__DOT___singleBurstDelay_4_T_1) 
                                              >> 1U) 
                                             - (IData)(1U))));
                __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_5 
                    = (0xffffU & (0x7fffU & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT__unnamedblk2__DOT___singleBurstDelay_5_T_1) 
                                              >> 1U) 
                                             - (IData)(1U))));
                __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_6 
                    = (0xffffU & (0x7fffU & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT__unnamedblk2__DOT___singleBurstDelay_6_T_1) 
                                              >> 1U) 
                                             - (IData)(1U))));
                __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_7 
                    = (0xffffU & (0x7fffU & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__unnamedblk1__DOT__unnamedblk2__DOT___singleBurstDelay_7_T_1) 
                                              >> 1U) 
                                             - (IData)(1U))));
            } else {
                __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_0 
                    = (0xffffU & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_0) 
                                  - (IData)(1U)));
                __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_1 
                    = (0xffffU & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_1) 
                                  - (IData)(1U)));
                __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_2 
                    = (0xffffU & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_2) 
                                  - (IData)(1U)));
                __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_3 
                    = (0xffffU & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_3) 
                                  - (IData)(1U)));
                __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_4 
                    = (0xffffU & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_4) 
                                  - (IData)(1U)));
                __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_5 
                    = (0xffffU & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_5) 
                                  - (IData)(1U)));
                __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_6 
                    = (0xffffU & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_6) 
                                  - (IData)(1U)));
                __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_7 
                    = (0xffffU & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_7) 
                                  - (IData)(1U)));
            }
        }
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__unnamedblk1__DOT__do_deq 
            = (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_rready) 
                & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__idle_3)
                    ? (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__readys_readys)
                    : (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__state_3_0))) 
               & (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__empty)));
        if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__unnamedblk1__DOT__do_deq) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__wrap_1 
                = (1U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__wrap_1) 
                         - (IData)(1U)));
        }
        if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__do_enq) 
             != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__unnamedblk1__DOT__do_deq))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__maybe_full 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__do_enq;
        }
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__unnamedblk1__DOT__do_deq 
            = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi42apb_auto_in_arready) 
               & (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__empty)));
        if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__unnamedblk1__DOT__do_deq) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__wrap_1 
                = (1U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__wrap_1) 
                         - (IData)(1U)));
        }
        if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__do_enq) 
             != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__unnamedblk1__DOT__do_deq))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__maybe_full 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__do_enq;
        }
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__unnamedblk1__DOT__do_deq 
            = (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_bready) 
                & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__idle_4)
                    ? (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__readys_readys_1)
                    : (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__state_4_0))) 
               & (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__empty)));
        if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__unnamedblk1__DOT__do_deq) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__wrap_1 
                = (1U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__wrap_1) 
                         - (IData)(1U)));
        }
        if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__do_enq) 
             != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__unnamedblk1__DOT__do_deq))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__maybe_full 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__do_enq;
        }
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__w_counter 
            = (0x1ffU & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__w_todo) 
                         - ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_wready) 
                            & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_wvalid))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__state 
            = (3U & vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__nextState);
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__idle_3 
            = (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_rready) 
                & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rvalid)) 
               | ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__anyValid)) 
                  & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__idle_3)));
        if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__idle_3) 
             & (0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__readys_valid)))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__unnamedblk1__DOT___readys_mask_T 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__readys_readys) 
                   & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__readys_valid));
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__unnamedblk1__DOT___readys_mask_T_3 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__unnamedblk1__DOT___readys_mask_T) 
                   | (6U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__unnamedblk1__DOT___readys_mask_T) 
                            << 1U)));
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__readys_mask 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__unnamedblk1__DOT___readys_mask_T_3) 
                   | (4U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__unnamedblk1__DOT___readys_mask_T_3) 
                            << 2U)));
        }
        if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__idle_3) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__state_3_0 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__prefixOR_1;
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__state_3_1 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__winner_3_1;
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__state_3_2 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__winner_3_2;
        }
        __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__idle_4 
            = (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_bready) 
                & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_bvalid)) 
               | ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__anyValid_1)) 
                  & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__idle_4)));
        if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__idle_4) 
             & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__w_full) 
                | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__empty))))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__unnamedblk2__DOT___readys_mask_T_8 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__readys_readys_1) 
                   & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__readys_valid_1));
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__unnamedblk2__DOT___readys_mask_T_11 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__unnamedblk2__DOT___readys_mask_T_8) 
                   | (6U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__unnamedblk2__DOT___readys_mask_T_8) 
                            << 1U)));
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__readys_mask_1 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__unnamedblk2__DOT___readys_mask_T_11) 
                   | (4U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__unnamedblk2__DOT___readys_mask_T_11) 
                            << 2U)));
        }
        if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__idle_4) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__state_4_0 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__winner_4_0;
            vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__state_4_2 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__winner_4_2;
        }
    }
    if (ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT___GEN_1) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk4__DOT___inc_addr_T_3 
            = (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__addr_1 
               + (0xffffU & ((IData)(1U) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___deq_q_1_io_deq_bits_size))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk4__DOT___wrapMask_T_3 
            = (0x7fffffU & ((0xffU | ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___deq_q_1_io_deq_bits_len) 
                                      << 8U)) << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___deq_q_1_io_deq_bits_size)));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk4__DOT___mux_addr_T_6 
            = (~ vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___deq_q_1_io_deq_bits_addr);
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__r_len_1 
            = (0xffU & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len_1) 
                        - (IData)(1U)));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__r_addr_1 
            = ((0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___deq_q_1_io_deq_bits_burst))
                ? vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___deq_q_1_io_deq_bits_addr
                : ((2U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___deq_q_1_io_deq_bits_burst))
                    ? ((0x7fffU & (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk4__DOT___inc_addr_T_3 
                                   & (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk4__DOT___wrapMask_T_3 
                                      >> 8U))) | (~ 
                                                  ((0xffff8000U 
                                                    & vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk4__DOT___mux_addr_T_6) 
                                                   | (0x7fffU 
                                                      & (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk4__DOT___mux_addr_T_6 
                                                         | (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk4__DOT___wrapMask_T_3 
                                                            >> 8U))))))
                    : vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__unnamedblk1__DOT__unnamedblk4__DOT___inc_addr_T_3));
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__is_fencei 
        = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT____Vcellinp__cpu__reset)) 
           & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___idu_io_ifu_signals_valid) 
              & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___icache_io_fencei_ready)));
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT___axi42apb_auto_in_arready) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi42apb__DOT__araddr_reg_r 
            = (IData)((vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT___ram_ext_R0_data 
                       >> 0xbU));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi42apb__DOT__rid_reg 
            = (0xfU & (IData)((vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT___ram_ext_R0_data 
                               >> 0x2bU)));
    }
    if ((1U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi42apb__DOT__state))) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi42apb__DOT__resp_hold_r 
            = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__apbdelay_delayer__DOT__delay_pslverr) 
               << 1U);
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi42apb__DOT__nodeIn_rdata_r 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT__apbdelay_delayer__DOT__delay_prdata;
    }
    if (__Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__ram_ext__DOT__Memory__v0) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__ram_ext__DOT__Memory[__Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__ram_ext__DOT__Memory__v0] 
            = __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__ram_ext__DOT__Memory__v0;
    }
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__in_wdeq_q__DOT__do_enq) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__in_wdeq_q__DOT__ram 
            = (((QData)((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lsdram_axi__DOT__msdram__DOT__u_sdram_axi__DOT__u_core__DOT__ram_write_data_w)) 
                << 5U) | (QData)((IData)((((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4delay_delayer_io_out_wstrb) 
                                           << 1U) | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4delay_delayer_io_out_wlast)))));
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__in_wdeq_q__DOT__full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__in_wdeq_q__DOT__full;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__wrap 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__wrap;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__wrap_1 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__wrap_1;
    if (__Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v0) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory[__Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v0] 
            = (((~ ((IData)(0xffU) << (IData)(__Vdlyvlsb__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v0))) 
                & vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory
                [__Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v0]) 
               | (0xffffffffULL & ((IData)(__Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v0) 
                                   << (IData)(__Vdlyvlsb__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v0))));
    }
    if (__Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v1) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory[__Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v1] 
            = (((~ ((IData)(0xffU) << (IData)(__Vdlyvlsb__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v1))) 
                & vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory
                [__Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v1]) 
               | (0xffffffffULL & ((IData)(__Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v1) 
                                   << (IData)(__Vdlyvlsb__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v1))));
    }
    if (__Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v2) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory[__Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v2] 
            = (((~ ((IData)(0xffU) << (IData)(__Vdlyvlsb__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v2))) 
                & vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory
                [__Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v2]) 
               | (0xffffffffULL & ((IData)(__Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v2) 
                                   << (IData)(__Vdlyvlsb__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v2))));
    }
    if (__Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v3) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory[__Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v3] 
            = (((~ ((IData)(0xffU) << (IData)(__Vdlyvlsb__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v3))) 
                & vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory
                [__Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v3]) 
               | (0xffffffffULL & ((IData)(__Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v3) 
                                   << (IData)(__Vdlyvlsb__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory__v3))));
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q__DOT__full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q__DOT__full;
    if (__Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__ram_ext__DOT__Memory__v0) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__ram_ext__DOT__Memory[__Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__ram_ext__DOT__Memory__v0] 
            = __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__ram_ext__DOT__Memory__v0;
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__x_cnt 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__x_cnt;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__y_cnt 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__y_cnt;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__vga_apb_state 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__vga_apb_state;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__count 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__count;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__w_ptr 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__w_ptr;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__key_overflow 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__key_overflow;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__key_ready 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__key_ready;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__ps2_clk_sync 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__ps2_clk_sync;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__r_ptr 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__r_ptr;
    if (__Vdlyvset__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__fifo__v0) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__fifo[__Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__fifo__v0] = 0U;
    }
    if (__Vdlyvset__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__fifo__v1) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__fifo[__Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__fifo__v1] 
            = __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__lkeyboard__DOT__mps2__DOT__u_ps2_keyboard__DOT__fifo__v1;
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_0 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_0;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_1 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_1;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_2 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_2;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_3 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_3;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_4 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_4;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_5 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_5;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_6 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_6;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_7 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_7;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_8 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_8;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_9 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_9;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_10 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_10;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_11 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_11;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_12 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_12;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_13 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_13;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_14 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_14;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_15 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__error_15;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__wrap 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__wrap;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__wrap_1 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__wrap_1;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q_1__DOT__full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q_1__DOT__full;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__burstCntIn 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__burstCntIn;
    if (__Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__ram_ext__DOT__Memory__v0) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__ram_ext__DOT__Memory[__Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__ram_ext__DOT__Memory__v0] 
            = __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__ram_ext__DOT__Memory__v0;
    }
    if (__Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__ram_ext__DOT__Memory__v0) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__ram_ext__DOT__Memory[__Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__ram_ext__DOT__Memory__v0] 
            = __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__ram_ext__DOT__Memory__v0;
    }
    if (__Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__ram_ext__DOT__Memory__v0) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__ram_ext__DOT__Memory[__Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__ram_ext__DOT__Memory__v0] 
            = __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__ram_ext__DOT__Memory__v0;
    }
    if (__Vdlyvset__ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT__Uregs__DOT__receiver__DOT__fifo_rx__DOT__rfifo__DOT__ram__v0) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT__Uregs__DOT__receiver__DOT__fifo_rx__DOT__rfifo__DOT__ram[__Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT__Uregs__DOT__receiver__DOT__fifo_rx__DOT__rfifo__DOT__ram__v0] 
            = __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT__Uregs__DOT__receiver__DOT__fifo_rx__DOT__rfifo__DOT__ram__v0;
    }
    if (__Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__ram_ext__DOT__Memory__v0) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__ram_ext__DOT__Memory[__Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__ram_ext__DOT__Memory__v0] 
            = __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__ram_ext__DOT__Memory__v0;
    }
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT____Vcellinp__cpu__reset) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__clint__DOT__mtime = 0ULL;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu__DOT__state = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__refill_cnt = 0U;
    } else {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__clint__DOT__mtime 
            = (1ULL + vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__clint__DOT__mtime);
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu__DOT__state 
            = ((2U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu__DOT__state))
                ? (2U & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___icache_io_in_rvalid)) 
                         << 1U)) : ((1U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu__DOT__state))
                                     ? ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu__DOT__work)
                                         ? 0U : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT____Vcellinp__ifu__io_is_flush)
                                                  ? 2U
                                                  : 1U))
                                     : (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu__DOT___io_imem_arvalid_T) 
                                         & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu__DOT__idle))
                                         ? (1U & (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu__DOT__work)))
                                         : 0U)));
        if ((0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__state))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__refill_cnt 
                = ((((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT___GEN_0) 
                     | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___ifu_io_imem_arvalid))) 
                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__hit)) 
                   & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__refill_cnt));
        } else if ((1U & (~ ((1U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__state)) 
                             | (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT___GEN_4)))))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__refill_cnt 
                = (1U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__refill_cnt) 
                         - (IData)(1U)));
        }
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__refill_cnt 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__refill_cnt;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__wrap 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__wrap;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__wrap 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__wrap;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__wrap 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__wrap;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__wrap 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__wrap;
    if (__Vdlyvset__ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT__Uregs__DOT__transmitter__DOT__fifo_tx__DOT__tfifo__DOT__ram__v0) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT__Uregs__DOT__transmitter__DOT__fifo_tx__DOT__tfifo__DOT__ram[__Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT__Uregs__DOT__transmitter__DOT__fifo_tx__DOT__tfifo__DOT__ram__v0] 
            = __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__luart__DOT__muart__DOT__Uregs__DOT__transmitter__DOT__fifo_tx__DOT__tfifo__DOT__ram__v0;
    }
    if (__Vdlyvset__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__ram_ext__DOT__Memory__v0) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__ram_ext__DOT__Memory[__Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__ram_ext__DOT__Memory__v0] 
            = __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__ram_ext__DOT__Memory__v0;
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__wrap 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__wrap;
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT____Vcellinp__cpu__reset) {
        __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__valid_array_0 = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__state = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__valid_array_0 
            = __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__valid_array_0;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__state = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__ar_handshake_done = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__aw_handshake_done = 0U;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__w_handshake_done = 0U;
    } else {
        if ((1U & (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT___GEN_3)))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__valid_array_0 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT___GEN_2)
                    ? ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT___GEN_5) 
                       | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__valid_array_0))
                    : ((~ (IData)((3U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__state)))) 
                       & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__valid_array_0)));
        }
        if (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT___GEN) {
            if (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT___GEN_0) {
                __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__state = 3U;
            } else if ((1U & (~ ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___ifu_io_imem_arvalid)) 
                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__hit))))) {
                __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__state = 1U;
            }
        } else if (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT___GEN_1) {
            if (((0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT__state))
                  ? ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_imem_arvalid) 
                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_arready))
                  : ((1U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT__state)) 
                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_arready)))) {
                __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__state = 2U;
            }
        } else if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT___GEN_2)
                     ? (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT___GEN_5)
                     : (3U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__state)))) {
            __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__state = 0U;
        }
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__valid_array_0 
            = __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__valid_array_0;
        __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__state 
            = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT___io_dmem_bready_T_2)
                ? (2U & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT___ready_T)) 
                         << 1U)) : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT___io_dmem_bready_T)
                                     ? ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__ready)
                                         ? 0U : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__is_irq)
                                                  ? 2U
                                                  : 1U))
                                     : ((((~ (IData)(
                                                     (0U 
                                                      != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__state)))) 
                                          & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__idle)) 
                                         & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__is_load) 
                                             & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__ar_handshake_done) 
                                                | ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_dmem_arvalid) 
                                                   & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___xbar_io_dmem_arready)))) 
                                            | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__is_store) 
                                                & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__aw_handshake_done) 
                                                   | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT___aw_complete_T))) 
                                               & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__w_handshake_done) 
                                                  | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT___w_complete_T)))))
                                         ? (1U & (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__ready)))
                                         : 0U)));
        __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__ar_handshake_done 
            = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT___GEN_1)) 
               & ((((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT___GEN_0) 
                    & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_dmem_arvalid)) 
                   & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___xbar_io_dmem_arready)) 
                  | ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT___GEN)) 
                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__ar_handshake_done))));
        __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__aw_handshake_done 
            = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT___GEN_1)) 
               & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT___GEN_0) 
                   & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT___aw_complete_T)) 
                  | ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT___GEN)) 
                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__aw_handshake_done))));
        __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__w_handshake_done 
            = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT___GEN_1)) 
               & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT___GEN_0) 
                   & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT___w_complete_T)) 
                  | ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT___GEN)) 
                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__w_handshake_done))));
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__w_handshake_done 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__w_handshake_done;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__ar_handshake_done 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__ar_handshake_done;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__aw_handshake_done 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__aw_handshake_done;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__state 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__state;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_15__DOT__full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_15__DOT__full;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_14__DOT__full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_14__DOT__full;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_13__DOT__full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_13__DOT__full;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_12__DOT__full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_12__DOT__full;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_11__DOT__full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_11__DOT__full;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_10__DOT__full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_10__DOT__full;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_9__DOT__full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_9__DOT__full;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_8__DOT__full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_8__DOT__full;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_7__DOT__full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_7__DOT__full;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_6__DOT__full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_6__DOT__full;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_5__DOT__full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_5__DOT__full;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_4__DOT__full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_4__DOT__full;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_3__DOT__full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_3__DOT__full;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_2__DOT__full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_2__DOT__full;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_1__DOT__full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_1__DOT__full;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap__DOT__full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap__DOT__full;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__idle_3 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__idle_3;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_31__DOT__full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_31__DOT__full;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_30__DOT__full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_30__DOT__full;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_29__DOT__full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_29__DOT__full;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_28__DOT__full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_28__DOT__full;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_27__DOT__full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_27__DOT__full;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_26__DOT__full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_26__DOT__full;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_25__DOT__full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_25__DOT__full;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_24__DOT__full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_24__DOT__full;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_23__DOT__full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_23__DOT__full;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_22__DOT__full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_22__DOT__full;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_21__DOT__full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_21__DOT__full;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_20__DOT__full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_20__DOT__full;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_19__DOT__full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_19__DOT__full;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_18__DOT__full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_18__DOT__full;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_17__DOT__full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_17__DOT__full;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_16__DOT__full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_16__DOT__full;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__idle_2 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__idle_2;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__wrap_1 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__wrap_1;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__wrap_1 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__wrap_1;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__burstCntOut 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__burstCntOut;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__delayCnt 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__delayCnt;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_0 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_0;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_1 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_1;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_2 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_2;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_3 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_3;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_4 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_4;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_5 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_5;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_6 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_6;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_7 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__singleBurstDelay_7;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__wrap_1 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__wrap_1;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__wrap_1 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__wrap_1;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__wrap_1 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__wrap_1;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__ptr_match 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__wrap) 
           == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__wrap_1));
    vlSelf->externalPins_vga_hsync = (0x60U < (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__x_cnt));
    vlSelf->externalPins_vga_vsync = (2U < (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__y_cnt));
    vlSelf->externalPins_vga_valid = ((0x90U < (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__x_cnt)) 
                                      & ((0x310U >= (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__x_cnt)) 
                                         & ((0x23U 
                                             < (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__y_cnt)) 
                                            & (0x203U 
                                               >= (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lvga__DOT__mvga__DOT__y_cnt)))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__ptr_match 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__wrap) 
           == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__wrap_1));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu__DOT___io_imem_arvalid_T 
        = (0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu__DOT__state));
    if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4delay_delayer_io_in_arready) 
         & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4delay_delayer_io_out_arvalid))) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4delay_delayer__DOT__burstLenR = 0U;
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT___io_dmem_bready_T_2 
        = (2U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__state));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT___io_dmem_bready_T 
        = (1U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__state));
    vlSelf->__VdfgTmp_hf132a334__0 = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory
        [vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT___R0_addr_d0];
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__rdata_REG 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__ren;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu_reset_chain__DOT__output_chain__DOT__sync_1 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu_reset_chain__DOT__output_chain__DOT__sync_2;
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT____Vcellinp__cpu__reset) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_mem_read = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_valid_r = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__in_addr = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_valid_r = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_signals_wbu_reg_write_sel = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_jump = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu__DOT__pc_reg = 0x30000000U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_signals_lsu_mem_wmask = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_signals_lsu_mem_rd = 0U;
    } else {
        if (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___lsu_io_out_valid) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_mem_read 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___lsu_io_out_bits_mem_read;
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_signals_wbu_reg_write_sel 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_signals_wbu_reg_write_sel;
        }
        if (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___idu_io_in_ready) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_valid_r 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___ifu_io_out_valid;
        }
        if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___ifu_io_imem_arvalid) 
             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___icache_io_in_arready))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__in_addr 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___ifu_io_out_bits_pc;
        }
        if (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___exu_io_in_ready) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_valid_r 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___idu_io_out_valid;
        }
        if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___idu_io_out_valid) 
             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___exu_io_in_ready))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_jump 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___idu_io_out_bits_signals_exu_jump;
        }
        if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu_io_in_valid_r) 
             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___ifu_io_in_ready))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu__DOT__pc_reg 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu_io_in_bits_r_next_pc;
        }
        if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___exu_io_out_valid) 
             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___lsu_io_in_ready))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_signals_lsu_mem_wmask 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_lsu_mem_wmask;
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_signals_lsu_mem_rd 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_lsu_mem_rd;
        }
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_dmem_arsize 
        = ((5U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_signals_lsu_mem_rd))
            ? 1U : ((4U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_signals_lsu_mem_rd))
                     ? 0U : ((3U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_signals_lsu_mem_rd))
                              ? 2U : ((2U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_signals_lsu_mem_rd))
                                       ? 1U : ((1U 
                                                != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_signals_lsu_mem_rd)) 
                                               << 1U)))));
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT____Vcellinp__cpu__reset) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_alu_control = 0U;
    } else if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___idu_io_out_valid) 
                & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___exu_io_in_ready))) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_alu_control 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___idu_io_out_bits_signals_exu_alu_control;
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT__is_sub 
        = ((1U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_alu_control)) 
           | ((3U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_alu_control)) 
              | (5U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_alu_control))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__ptr_match 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__wrap) 
           == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__wrap_1));
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT___axi42apb_auto_in_awready) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi42apb__DOT__wdata_reg_r 
            = (IData)((vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT___ram_ext_R0_data 
                       >> 4U));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi42apb__DOT__wstrb_reg_r 
            = (0xfU & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT___ram_ext_R0_data));
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT___ram_ext_R0_data 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__ram_ext__DOT__Memory
        [vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__wrap_1];
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__ptr_match 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__wrap) 
           == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__wrap_1));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT___ram_ext_R0_data 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__ram_ext__DOT__Memory
        [vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__wrap_1];
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__ptr_match 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__wrap) 
           == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__wrap_1));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__ptr_match 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__wrap) 
           == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__wrap_1));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT___ram_ext_R0_data 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__ram_ext__DOT__Memory
        [vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__wrap_1];
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__ptr_match 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__wrap) 
           == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__wrap_1));
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT____Vcellinp__cpu__reset) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_alu_srcB = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_alu_result = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_valid_r = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_signals_lsu_mem_valid = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_signals_lsu_mem_write = 0U;
    } else {
        if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___idu_io_out_valid) 
             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___exu_io_in_ready))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_alu_srcB 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_13)
                    ? 1U : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_11)
                             ? 0U : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_18)
                                      ? 1U : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_3)
                                               ? 0U
                                               : (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_14)))));
        }
        if (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___lsu_io_out_valid) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_alu_result 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_alu_result;
        }
        if (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___lsu_io_in_ready) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_valid_r 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___exu_io_out_valid;
        }
        if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___exu_io_out_valid) 
             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___lsu_io_in_ready))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_signals_lsu_mem_valid 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_lsu_mem_valid;
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_signals_lsu_mem_write 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_lsu_mem_write;
        }
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__is_load 
        = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_signals_lsu_mem_write)) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_signals_lsu_mem_valid));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__is_store 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_signals_lsu_mem_valid) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_signals_lsu_mem_write));
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT___axi42apb_auto_in_awready) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi42apb__DOT__awaddr_reg_r 
            = (IData)((vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT___ram_ext_R0_data 
                       >> 0xbU));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi42apb__DOT__bid_reg 
            = (0xfU & (IData)((vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT___ram_ext_R0_data 
                               >> 0x2bU)));
    }
    if ((0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi42apb__DOT__state))) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi42apb__DOT__is_write_r 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT___axi42apb_auto_in_awready;
    }
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT____Vcellinp__cpu__reset) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_is_ebreak = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_signals_wbu_csr_write = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_signals_wbu_csr_sel = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_imm_ext = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_rd2 = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_rd1 = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_alu_srcA = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_csr_rd1 = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT__state = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_signals_wbu_reg_write = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_waddr = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_signals_wbu_reg_write_sel = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu_io_in_bits_r_next_pc = 0x30000000U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu_io_in_valid_r = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_lsu_mem_wmask = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_lsu_mem_rd = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_alu_result = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_lsu_mem_valid = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_lsu_mem_write = 0U;
    } else {
        if (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___lsu_io_out_valid) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_is_ebreak 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_is_ebreak;
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_signals_wbu_csr_write 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_signals_wbu_csr_write;
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_signals_wbu_csr_sel 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_signals_wbu_csr_sel;
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_rd1 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_rd1;
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_csr_rd1 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_csr_rd1;
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_signals_wbu_reg_write 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_signals_wbu_reg_write;
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_waddr 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_waddr;
        }
        if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___exu_io_out_valid) 
             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___lsu_io_in_ready))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_imm_ext 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_imm_ext;
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_rd2 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_rd2_r;
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_signals_wbu_reg_write_sel 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_wbu_reg_write_sel;
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_alu_result 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT__casez_tmp;
        }
        if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___idu_io_out_valid) 
             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___exu_io_in_ready))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_alu_srcA 
                = ((0x37U == (0x7fU & vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst))
                    ? 2U : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_17)
                             ? 1U : (2U & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_15)) 
                                           << 1U))));
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_lsu_mem_wmask 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_5)
                    ? 1U : ((0x123U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN))
                             ? 0xfU : ((0xa3U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN))
                                        ? 3U : ((1U 
                                                 & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_8)) 
                                                    | (0x23U 
                                                       == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN))))
                                                 ? 1U
                                                 : 0xfU))));
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_lsu_mem_rd 
                = (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT____VdfgTmp_h951ab88b__0) 
                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_9))
                    ? 0U : ((0x103U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN))
                             ? 3U : ((0x83U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN))
                                      ? 2U : ((3U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN))
                                               ? 1U
                                               : ((0x203U 
                                                   == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN))
                                                   ? 4U
                                                   : 
                                                  ((0x283U 
                                                    == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN))
                                                    ? 5U
                                                    : 0U))))));
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_lsu_mem_valid 
                = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_5)) 
                   & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_14));
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_lsu_mem_write 
                = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_5)) 
                   & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_9));
        }
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT__state 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT__casez_tmp;
        if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___ifu_io_pc_valid) 
             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___ifu_io_in_ready))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu_io_in_bits_r_next_pc 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___ifu_io_pc_bits_next_pc;
        }
        if (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___ifu_io_in_ready) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu_io_in_valid_r 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___ifu_io_pc_valid;
        }
    }
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q__DOT__full) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___deq_q_io_deq_bits_len 
            = (0xffU & (IData)((vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q__DOT__ram 
                                >> 5U)));
        vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid 
            = (0xfU & (IData)((vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q__DOT__ram 
                               >> 0x2dU)));
    } else {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___deq_q_io_deq_bits_len = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid = 0U;
    }
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q_1__DOT__full) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___deq_q_1_io_deq_bits_len 
            = (0xffU & (IData)((vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q_1__DOT__ram 
                                >> 5U)));
        vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid 
            = (0xfU & (IData)((vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q_1__DOT__ram 
                               >> 0x2dU)));
    } else {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___deq_q_1_io_deq_bits_len = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid = 0U;
    }
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_is_ebreak) {
        VysyxSoCFull___024root____Vdpiimwrap_ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu__DOT__ebreak__DOT__sim_exit_TOP();
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awIn_0_io_enq_ready 
        = (1U & (~ ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__ptr_match) 
                    & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__maybe_full))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__empty 
        = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__maybe_full)) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__ptr_match));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__empty 
        = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__maybe_full)) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__ptr_match));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awIn_0_io_enq_ready 
        = (1U & (~ ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__ptr_match) 
                    & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__maybe_full))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT___ram_ext_R0_data 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__ram_ext__DOT__Memory
        [vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__wrap_1];
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT___ram_ext_R0_data 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__ram_ext__DOT__Memory
        [vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__wrap_1];
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi42apb__DOT__state 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi42apb__DOT__state;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT___mem_ext_R0_data 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT___R0_en_d0)
            ? vlSelf->__VdfgTmp_hf132a334__0 : 0U);
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__busy)
            ? (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__r_len)
            : (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___deq_q_io_deq_bits_len));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT____VdfgTmp_h73aa1639__0 
        = (1U & (((0x8000U & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_15__DOT__full)) 
                              << 0xfU)) | ((0x4000U 
                                            & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_14__DOT__full)) 
                                               << 0xeU)) 
                                           | ((0x2000U 
                                               & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_13__DOT__full)) 
                                                  << 0xdU)) 
                                              | ((0x1000U 
                                                  & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_12__DOT__full)) 
                                                     << 0xcU)) 
                                                 | ((0x800U 
                                                     & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_11__DOT__full)) 
                                                        << 0xbU)) 
                                                    | ((0x400U 
                                                        & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_10__DOT__full)) 
                                                           << 0xaU)) 
                                                       | ((0x200U 
                                                           & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_9__DOT__full)) 
                                                              << 9U)) 
                                                          | ((0x100U 
                                                              & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_8__DOT__full)) 
                                                                 << 8U)) 
                                                             | ((0x80U 
                                                                 & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_7__DOT__full)) 
                                                                    << 7U)) 
                                                                | ((0x40U 
                                                                    & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_6__DOT__full)) 
                                                                       << 6U)) 
                                                                   | ((0x20U 
                                                                       & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_5__DOT__full)) 
                                                                          << 5U)) 
                                                                      | ((0x10U 
                                                                          & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_4__DOT__full)) 
                                                                             << 4U)) 
                                                                         | ((8U 
                                                                             & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_3__DOT__full)) 
                                                                                << 3U)) 
                                                                            | ((4U 
                                                                                & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_2__DOT__full)) 
                                                                                << 2U)) 
                                                                               | ((2U 
                                                                                & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_1__DOT__full)) 
                                                                                << 1U)) 
                                                                                | (1U 
                                                                                & (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap__DOT__full)))))))))))))))))) 
                 >> (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_arid)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__len_1 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__busy_1)
            ? (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__r_len_1)
            : (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___deq_q_1_io_deq_bits_len));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT____VdfgTmp_he7a0eac8__0 
        = (1U & (((0x8000U & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_31__DOT__full)) 
                              << 0xfU)) | ((0x4000U 
                                            & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_30__DOT__full)) 
                                               << 0xeU)) 
                                           | ((0x2000U 
                                               & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_29__DOT__full)) 
                                                  << 0xdU)) 
                                              | ((0x1000U 
                                                  & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_28__DOT__full)) 
                                                     << 0xcU)) 
                                                 | ((0x800U 
                                                     & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_27__DOT__full)) 
                                                        << 0xbU)) 
                                                    | ((0x400U 
                                                        & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_26__DOT__full)) 
                                                           << 0xaU)) 
                                                       | ((0x200U 
                                                           & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_25__DOT__full)) 
                                                              << 9U)) 
                                                          | ((0x100U 
                                                              & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_24__DOT__full)) 
                                                                 << 8U)) 
                                                             | ((0x80U 
                                                                 & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_23__DOT__full)) 
                                                                    << 7U)) 
                                                                | ((0x40U 
                                                                    & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_22__DOT__full)) 
                                                                       << 6U)) 
                                                                   | ((0x20U 
                                                                       & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_21__DOT__full)) 
                                                                          << 5U)) 
                                                                      | ((0x10U 
                                                                          & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_20__DOT__full)) 
                                                                             << 4U)) 
                                                                         | ((8U 
                                                                             & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_19__DOT__full)) 
                                                                                << 3U)) 
                                                                            | ((4U 
                                                                                & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_18__DOT__full)) 
                                                                                << 2U)) 
                                                                               | ((2U 
                                                                                & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_17__DOT__full)) 
                                                                                << 1U)) 
                                                                                | (1U 
                                                                                & (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_16__DOT__full)))))))))))))))))) 
                 >> (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awid)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4buf_auto_in_awready 
        = (1U & (~ ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__ptr_match) 
                    & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__maybe_full))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__empty 
        = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__maybe_full)) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__ptr_match));
    vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4buf_auto_in_wready 
        = (1U & (~ ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__ptr_match) 
                    & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__maybe_full))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__empty 
        = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__maybe_full)) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__ptr_match));
    vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4buf_auto_out_rready 
        = (1U & (~ ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__ptr_match) 
                    & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__maybe_full))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__empty 
        = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__maybe_full)) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__ptr_match));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__idle_3 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__idle_3;
    vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4buf_auto_in_arready 
        = (1U & (~ ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__ptr_match) 
                    & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__maybe_full))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__empty 
        = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__maybe_full)) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__ptr_match));
    vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4buf_auto_out_bready 
        = (1U & (~ ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__ptr_match) 
                    & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__maybe_full))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__idle_4 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__idle_4;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__empty 
        = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__maybe_full)) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__ptr_match));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__w_full 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__w_full;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_dmem_rready 
        = (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__is_load) 
            & (1U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__state))) 
           | (2U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__state)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_dmem_bready 
        = (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__is_store) 
            & (1U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__state))) 
           | (2U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__state)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___anonIn_awready_T 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awIn_0_io_enq_ready) 
           | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__latched));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___in_awready_T 
        = ((0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__w_counter)) 
           | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__wbeats_latched));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT___GEN_5 
        = ((1U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT__state)) 
           | (2U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT__state)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT____VdfgTmp_hb30a1854__0 
        = ((1U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT__state)) 
           & (2U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT__state)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu_reset_chain__DOT__output_chain__DOT__sync_2 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu_reset_chain__DOT__output_chain__DOT__sync_3;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__anyValid 
        = (1U & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__empty)) 
                 | ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__r_full) 
                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lmrom__DOT__state))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__readys_valid 
        = (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__r_full) 
            << 2U) | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lmrom__DOT__state) 
                       << 1U) | (1U & (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__empty)))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT___axi42apb_auto_in_arready 
        = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_ardeq_q__DOT__empty)) 
           & (0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi42apb__DOT__state)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT___data_offset_T 
        = (7U & (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_alu_result 
                 - (4U & vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_alu_result)));
    vlSelf->__VdfgTmp_h0842f2a7__0 = ((0U != (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_alu_result 
                                              >> 0x19U)) 
                                      & (0x2010000U 
                                         > vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_alu_result));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__anyValid_1 
        = (1U & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__empty)) 
                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__w_full)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__readys_valid_1 
        = (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__w_full) 
            << 2U) | (1U & (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__empty))));
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT____Vcellinp__cpu__reset) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_is_ebreak = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_signals_wbu_csr_write = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_signals_wbu_csr_sel = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_imm_ext = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_rd2_r = 0U;
    } else {
        if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___exu_io_out_valid) 
             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___lsu_io_in_ready))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_is_ebreak 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_is_ebreak;
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_signals_wbu_csr_write 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_wbu_csr_write;
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_signals_wbu_csr_sel 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_wbu_csr_sel;
        }
        if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___idu_io_out_valid) 
             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___exu_io_in_ready))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_imm_ext 
                = ((4U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT___controller_io_signals_idu_imm_type))
                    ? (((- (IData)((vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst 
                                    >> 0x1fU))) << 0xcU) 
                       | ((0x800U & (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst 
                                     << 4U)) | ((0x7e0U 
                                                 & (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst 
                                                    >> 0x14U)) 
                                                | (0x1eU 
                                                   & (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst 
                                                      >> 7U)))))
                    : ((3U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT___controller_io_signals_idu_imm_type))
                        ? (((- (IData)((vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst 
                                        >> 0x1fU))) 
                            << 0xcU) | ((0xfe0U & (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst 
                                                   >> 0x14U)) 
                                        | (0x1fU & 
                                           (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst 
                                            >> 7U))))
                        : ((2U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT___controller_io_signals_idu_imm_type))
                            ? (((- (IData)((vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst 
                                            >> 0x1fU))) 
                                << 0x14U) | ((0xff000U 
                                              & vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst) 
                                             | ((0x800U 
                                                 & (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst 
                                                    >> 9U)) 
                                                | (0x7feU 
                                                   & (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst 
                                                      >> 0x14U)))))
                            : ((1U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT___controller_io_signals_idu_imm_type))
                                ? (0xfffff000U & vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst)
                                : (((- (IData)((vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst 
                                                >> 0x1fU))) 
                                    << 0xcU) | (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst 
                                                >> 0x14U))))));
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_rd2_r 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_forward_rd2)
                    ? vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_forward_data
                    : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_forward_rd2)
                        ? vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__casez_tmp
                        : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_raw_rs2)
                            ? vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___wbu_io_refile_wdata
                            : ((0U == (0x1fU & (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst 
                                                >> 0x14U)))
                                ? 0U : vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__refile__DOT__ysyx_25020039_rf_ext__DOT__Memory
                               [(0xfU & (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst 
                                         >> 0x14U))]))));
        }
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT____Vcellinp__alu__io_B 
        = ((1U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_alu_srcB))
            ? vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_imm_ext
            : vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_rd2_r);
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT____VdfgTmp_h3347b676__0 
        = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT___GEN_5)) 
           & (3U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT__state)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___readys_filter_T_1 
        = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__readys_mask)) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__readys_valid));
    vlSelf->ysyxSoCFull__DOT__asic__DOT___axi42apb_auto_in_awready 
        = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi42apb_auto_in_arready)) 
           & ((0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi42apb__DOT__state)) 
              & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_awdeq_q__DOT__empty)) 
                 & (~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT__empty)))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT___axi42apb_auto_out_pwrite 
        = ((0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi42apb__DOT__state))
            ? (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi42apb_auto_in_awready)
            : (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi42apb__DOT__is_write_r));
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT___axi42apb_auto_in_awready) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT___axi42apb_auto_out_pwdata 
            = (IData)((vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT___ram_ext_R0_data 
                       >> 4U));
        vlSelf->__VdfgTmp_h7fb30c92__0 = (0xfU & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeOut_wdeq_q__DOT___ram_ext_R0_data));
    } else {
        vlSelf->ysyxSoCFull__DOT__asic__DOT___axi42apb_auto_out_pwdata 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi42apb__DOT__wdata_reg_r;
        vlSelf->__VdfgTmp_h7fb30c92__0 = (0xfU & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi42apb__DOT__wstrb_reg_r));
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___readys_filter_T_3 
        = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__readys_mask_1)) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__readys_valid_1));
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT____Vcellinp__cpu__reset) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_rd1 = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_csr_rd1 = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_signals_wbu_reg_write = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_waddr = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_wbu_reg_write_sel = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_is_ebreak = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_wbu_csr_write = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_wbu_csr_sel = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_rd1_r = 0U;
    } else {
        if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___exu_io_out_valid) 
             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___lsu_io_in_ready))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_rd1 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_rd1_r;
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_csr_rd1 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_csr_rd1;
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_signals_wbu_reg_write 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_wbu_reg_write;
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_waddr 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_waddr;
        }
        if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___idu_io_out_valid) 
             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___exu_io_in_ready))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_wbu_reg_write_sel 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_1)
                    ? 1U : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_12)
                             ? 3U : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_11)
                                      ? 0U : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_4)
                                               ? 1U
                                               : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_9)
                                                   ? 0U
                                                   : 
                                                  ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_8)
                                                    ? 4U
                                                    : 
                                                   ((1U 
                                                     & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_19)) 
                                                        | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_7)))
                                                     ? 0U
                                                     : 5U)))))));
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_is_ebreak 
                = (0x100073U == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst);
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_wbu_csr_write 
                = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_16)) 
                   & ((0x73U == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst) 
                      | ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_6)) 
                         & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_19))));
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_wbu_csr_sel 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_16)
                    ? 0U : ((0x73U == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst)
                             ? 3U : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_6)
                                      ? 0U : ((0xf3U 
                                               == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN))
                                               ? 1U
                                               : ((0x173U 
                                                   == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                                                  << 1U)))));
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_rd1_r 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_forward_rd1)
                    ? vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_forward_data
                    : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_forward_rd1)
                        ? vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__casez_tmp
                        : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_raw_rs1)
                            ? vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___wbu_io_refile_wdata
                            : ((0U == (0x1fU & (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst 
                                                >> 0xfU)))
                                ? 0U : vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__refile__DOT__ysyx_25020039_rf_ext__DOT__Memory
                               [(0xfU & (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst 
                                         >> 0xfU))]))));
        }
    }
    if (__Vdlyvset__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__refile__DOT__ysyx_25020039_rf_ext__DOT__Memory__v0) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__refile__DOT__ysyx_25020039_rf_ext__DOT__Memory[__Vdlyvdim0__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__refile__DOT__ysyx_25020039_rf_ext__DOT__Memory__v0] 
            = __Vdlyvval__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__refile__DOT__ysyx_25020039_rf_ext__DOT__Memory__v0;
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu_reset_chain__DOT__output_chain__DOT__sync_3 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu_reset_chain__DOT__output_chain__DOT__sync_4;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___GEN_1 
        = (((0xcU & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___readys_filter_T_1) 
                     << 2U)) | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__r_full) 
                                 << 1U) | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lmrom__DOT__state))) 
           | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___readys_filter_T_1) 
               << 1U) | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__r_full)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT___axi42apb_auto_out_psel 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi42apb_auto_in_arready) 
           | ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi42apb_auto_in_awready) 
              | (1U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi42apb__DOT__state))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___GEN_2 
        = (((0xcU & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___readys_filter_T_3) 
                     << 2U)) | ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__w_full) 
                                << 1U)) | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___readys_filter_T_3) 
                                            << 1U) 
                                           | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__w_full)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__readys_unready 
        = (((0x10U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___readys_filter_T_1) 
                      << 2U)) | ((8U & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___GEN_1)) 
                                 | (7U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___GEN_1) 
                                          | ((4U & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___readys_filter_T_1)) 
                                             | (3U 
                                                & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___GEN_1) 
                                                   >> 2U))))))) 
           | ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__readys_mask) 
              << 3U));
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT___axi42apb_auto_out_pwrite) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__lpsram__DOT__mpsram__DOT__u0__DOT____VdfgTmp_h1138d757__0 
            = (1U & ((IData)(vlSelf->__VdfgTmp_h7fb30c92__0) 
                     >> 2U));
        vlSelf->__VdfgTmp_h4d523885__0 = (1U & (IData)(vlSelf->__VdfgTmp_h7fb30c92__0));
        vlSelf->__VdfgTmp_h428888aa__0 = ((IData)(vlSelf->__VdfgTmp_h7fb30c92__0) 
                                          >> 3U);
        vlSelf->ysyxSoCFull__DOT__asic__DOT___axi42apb_auto_out_pstrb 
            = vlSelf->__VdfgTmp_h7fb30c92__0;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__lpsram__DOT__mpsram__DOT__u0__DOT____VdfgTmp_h1164a660__0 
            = (1U & ((IData)(vlSelf->__VdfgTmp_h7fb30c92__0) 
                     >> 1U));
    } else {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__lpsram__DOT__mpsram__DOT__u0__DOT____VdfgTmp_h1138d757__0 = 0U;
        vlSelf->__VdfgTmp_h4d523885__0 = 0U;
        vlSelf->__VdfgTmp_h428888aa__0 = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT___axi42apb_auto_out_pstrb = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__lpsram__DOT__mpsram__DOT__u0__DOT____VdfgTmp_h1164a660__0 = 0U;
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__readys_unready_1 
        = (((0x10U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___readys_filter_T_3) 
                      << 2U)) | ((8U & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___GEN_2)) 
                                 | (7U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___GEN_2) 
                                          | ((4U & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___readys_filter_T_3)) 
                                             | (3U 
                                                & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___GEN_2) 
                                                   >> 2U))))))) 
           | ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__readys_mask_1) 
              << 3U));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu_reset_chain__DOT__output_chain__DOT__sync_4 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu_reset_chain__DOT__output_chain__DOT__sync_5;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__readys_readys 
        = (7U & (~ (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__readys_unready) 
                     >> 3U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__readys_unready))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__lpsram__DOT__mpsram__DOT__u0__DOT__size 
        = ((1U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi42apb_auto_out_pstrb))
            ? 1U : ((2U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi42apb_auto_out_pstrb))
                     ? 1U : ((4U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi42apb_auto_out_pstrb))
                              ? 1U : ((8U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi42apb_auto_out_pstrb))
                                       ? 1U : ((3U 
                                                == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi42apb_auto_out_pstrb))
                                                ? 2U
                                                : (
                                                   (0xcU 
                                                    == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi42apb_auto_out_pstrb))
                                                    ? 2U
                                                    : 4U))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__readys_readys_1 
        = (7U & (~ (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__readys_unready_1) 
                     >> 3U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__readys_unready_1))));
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT____Vcellinp__cpu__reset) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_csr_rd1 = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_wbu_reg_write = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_waddr = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__csr__DOT__rf_mtvec = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__csr__DOT__rf_mepc = 0U;
    } else {
        if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___idu_io_out_valid) 
             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___exu_io_in_ready))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_csr_rd1 
                = ((0xf12U == (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst 
                               >> 0x14U)) ? 0x25020039U
                    : ((0xf11U == (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst 
                                   >> 0x14U)) ? 0x79737978U
                        : ((0x342U == (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst 
                                       >> 0x14U)) ? 0xbU
                            : ((0x341U == (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst 
                                           >> 0x14U))
                                ? vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__csr__DOT__rf_mepc
                                : ((0x305U == (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst 
                                               >> 0x14U))
                                    ? vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__csr__DOT__rf_mtvec
                                    : ((0x300U == (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst 
                                                   >> 0x14U))
                                        ? 0x1800U : 0U))))));
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_wbu_reg_write 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_13) 
                   | ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_11)) 
                      & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_4) 
                         | ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_9)) 
                            & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_8) 
                               | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT____VdfgTmp_h22d8ad4d__0))))));
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_waddr 
                = (0x1fU & (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst 
                            >> 7U));
        }
        if ((1U & (~ ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__is_irq) 
                      | (~ ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___wbu_io_csr_wen) 
                            & (0x305U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_csr_waddr)))))))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__csr__DOT__rf_mtvec 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu__DOT__casez_tmp;
        }
        if (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__is_irq) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__csr__DOT__rf_mepc 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__csr_io_irq_pc_REG;
        } else if ((1U & (~ (((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___wbu_io_csr_wen)) 
                              | (0x305U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_csr_waddr))) 
                             | (0x341U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_csr_waddr)))))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__csr__DOT__rf_mepc 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu__DOT__casez_tmp;
        }
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__is_irq 
        = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT____Vcellinp__cpu__reset)) 
           & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___wbu_io_state_write_en) 
              & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_state_state)));
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT____Vcellinp__cpu__reset) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__csr_io_irq_pc_REG = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_csr_waddr = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_pc = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_csr_waddr = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_state_state = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_pc = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_csr_waddr = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_state_state = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_pc = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_state_state = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_pc = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst = 0U;
    } else {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__csr_io_irq_pc_REG 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_pc;
        if (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___lsu_io_out_valid) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_csr_waddr 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_csr_waddr;
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_pc 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_pc;
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_state_state 
                = (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___xbar_io_dmem_rvalid) 
                    & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT___GEN_4)) 
                       & (0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_rresp)))) 
                   | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___xbar_io_dmem_bvalid) 
                       & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT___GEN_4)) 
                          & (0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_in_bresp)))) 
                      | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_state_state)));
        }
        if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___exu_io_out_valid) 
             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___lsu_io_in_ready))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_csr_waddr 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_csr_waddr;
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_pc 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_pc;
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_state_state 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_state_state;
        }
        if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___idu_io_out_valid) 
             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___exu_io_in_ready))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_csr_waddr 
                = (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst 
                   >> 0x14U);
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_pc 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_pc;
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_state_state 
                = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_16)) 
                   & (0x73U == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst));
        }
        if (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___ifu_io_out_valid) 
             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___idu_io_in_ready))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_pc 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___ifu_io_out_bits_pc;
            vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst 
                = ((0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__state))
                    ? ((1U & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT____VdfgTmp_h51929d94__0)) 
                              | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT___GEN_0)))
                        ? 0U : vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__ysyx_25020039_data_array)
                    : ((1U & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT____VdfgTmp_hdc72c3c2__0)) 
                              | (1U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__state))))
                        ? 0U : vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___xbar_io_imem_rdata));
        }
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__winner_3_1 
        = (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__readys_readys) 
            >> 1U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lmrom__DOT__state));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__prefixOR_1 
        = (1U & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__empty)) 
                 & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__readys_readys)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__winner_3_2 
        = (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__readys_readys) 
            >> 2U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__r_full));
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__idle_3) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rvalid 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__anyValid;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__muxState_3_1 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__winner_3_1;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__muxState_3_0 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__prefixOR_1;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__muxState_3_2 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__winner_3_2;
    } else {
        vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rvalid 
            = (((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT__empty)) 
                & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__state_3_0)) 
               | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__state_3_1) 
                   & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lmrom__DOT__state)) 
                  | ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__r_full) 
                     & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__state_3_2))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__muxState_3_1 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__state_3_1;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__muxState_3_0 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__state_3_0;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__muxState_3_2 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__state_3_2;
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__winner_4_0 
        = (1U & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__empty)) 
                 & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__readys_readys_1)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__winner_4_2 
        = (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__readys_readys_1) 
            >> 2U) & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__w_full));
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__idle_4) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_bvalid 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__anyValid_1;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__muxState_4_0 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__winner_4_0;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__muxState_4_2 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__winner_4_2;
    } else {
        vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_bvalid 
            = (((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT__empty)) 
                & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__state_4_0)) 
               | ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__w_full) 
                  & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__state_4_2)));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__muxState_4_0 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__state_4_0;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__muxState_4_2 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__state_4_2;
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu_reset_chain__DOT__output_chain__DOT__sync_5 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu_reset_chain__DOT__output_chain__DOT__sync_6;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT___GEN 
        = ((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__state)) 
           | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__is_irq));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_valid 
        = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__is_irq)) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_valid_r));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_valid 
        = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__is_irq)) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_valid_r));
    vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_rlast 
        = (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__muxState_3_0) 
            & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT___ram_ext_R0_data)) 
           | ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__muxState_3_1) 
              | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__muxState_3_2)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_rid 
        = (0xfU & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__muxState_3_0)
                     ? (IData)((vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_rdeq_q__DOT___ram_ext_R0_data 
                                >> 0x23U)) : 0U) | 
                   (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__muxState_3_1)
                      ? (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lmrom__DOT__nodeIn_rid_r)
                      : 0U) | ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__muxState_3_2)
                                ? (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__r_id)
                                : 0U))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_bresp 
        = (3U & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__muxState_4_0)
                   ? (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT___ram_ext_R0_data)
                   : 0U) | ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__muxState_4_2)
                             ? ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__w_sel1)
                                 ? 0U : 3U) : 0U)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid 
        = (0xfU & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__muxState_4_0)
                     ? ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4buf__DOT__nodeIn_bdeq_q__DOT___ram_ext_R0_data) 
                        >> 2U) : 0U) | ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__muxState_4_2)
                                         ? (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__w_id)
                                         : 0U)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___wbu_io_state_write_en 
        = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__is_irq)) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_valid));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__idle 
        = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__is_irq)) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_valid));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu_reset_chain__DOT__output_chain__DOT__sync_6 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu_reset_chain__DOT__output_chain__DOT__sync_7;
    vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_becho_real_last 
        = (1U & ((((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_31__DOT__ram_real_last) 
                   << 0xfU) | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_30__DOT__ram_real_last) 
                                << 0xeU) | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_29__DOT__ram_real_last) 
                                             << 0xdU) 
                                            | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_28__DOT__ram_real_last) 
                                                << 0xcU) 
                                               | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_27__DOT__ram_real_last) 
                                                   << 0xbU) 
                                                  | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_26__DOT__ram_real_last) 
                                                      << 0xaU) 
                                                     | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_25__DOT__ram_real_last) 
                                                         << 9U) 
                                                        | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_24__DOT__ram_real_last) 
                                                            << 8U) 
                                                           | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_23__DOT__ram_real_last) 
                                                               << 7U) 
                                                              | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_22__DOT__ram_real_last) 
                                                                  << 6U) 
                                                                 | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_21__DOT__ram_real_last) 
                                                                     << 5U) 
                                                                    | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_20__DOT__ram_real_last) 
                                                                        << 4U) 
                                                                       | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_19__DOT__ram_real_last) 
                                                                           << 3U) 
                                                                          | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_18__DOT__ram_real_last) 
                                                                              << 2U) 
                                                                             | (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_17__DOT__ram_real_last) 
                                                                                << 1U) 
                                                                                | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT__Queue1_BundleMap_16__DOT__ram_real_last)))))))))))))))) 
                 >> (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bid)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu__DOT__casez_tmp 
        = ((0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_signals_wbu_csr_sel))
            ? 0U : ((1U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_signals_wbu_csr_sel))
                     ? vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_rd1
                     : ((2U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_signals_wbu_csr_sel))
                         ? (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_rd1 
                            | vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_csr_rd1)
                         : vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_pc)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___wbu_io_refile_wdata 
        = ((5U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_signals_wbu_reg_write_sel))
            ? vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_csr_rd1
            : ((4U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_signals_wbu_reg_write_sel))
                ? vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_mem_read
                : ((3U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_signals_wbu_reg_write_sel))
                    ? ((IData)(4U) + vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_pc)
                    : vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_alu_result)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___wbu_io_csr_wen 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_signals_wbu_csr_write) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___wbu_io_state_write_en));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_dmem_arvalid 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__is_load) 
           & ((~ (IData)((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__state)))) 
              & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__ar_handshake_done)) 
                 & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__idle))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT___GEN_0 
        = ((~ (IData)((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__state)))) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__idle));
    vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_in_bvalid 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_becho_real_last) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_in_bvalid));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT____VdfgTmp_h186720f0__0 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__is_store) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT___GEN_0));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___exu_io_out_valid 
        = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__is_irq)) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_valid_r));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu_reset_chain__DOT__output_chain__DOT__sync_7 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu_reset_chain__DOT__output_chain__DOT__sync_8;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_dmem_wvalid 
        = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__w_handshake_done)) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT____VdfgTmp_h186720f0__0));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_dmem_awvalid 
        = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT__aw_handshake_done)) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT____VdfgTmp_h186720f0__0));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT__is_dmem 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_dmem_arvalid) 
           | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_dmem_awvalid));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu_reset_chain__DOT__output_chain__DOT__sync_8 
        = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu_reset_chain__DOT__output_chain__DOT__sync_9;
    vlSelf->__VdfgTmp_h058c4e0a__0 = ((1U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_alu_srcA))
                                       ? vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_pc
                                       : vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_rd1_r);
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT___GEN 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT__is_dmem) 
           & (IData)(vlSelf->__VdfgTmp_h0842f2a7__0));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___ifu_io_out_bits_pc 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu_io_in_valid_r)
            ? vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu_io_in_bits_r_next_pc
            : vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu__DOT__pc_reg);
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT____Vcellinp__alu__io_A 
        = ((2U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_alu_srcA))
            ? 0U : vlSelf->__VdfgTmp_h058c4e0a__0);
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT____VdfgTmp_h09e3b85b__0 
        = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT___GEN)) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT__is_dmem));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT___GEN_0 
        = (1U & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT__is_dmem)) 
                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT___GEN)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT____Vcellinp__cpu__reset 
        = ((IData)(vlSelf->reset) | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu_reset_chain__DOT__output_chain__DOT__sync_0));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu_reset_chain__DOT__output_chain__DOT__sync_9 
        = vlSelf->reset;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT___adder_result_ext_T_1 
        = (0x1ffffffffULL & ((QData)((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT____Vcellinp__alu__io_A)) 
                             + ((QData)((IData)(((- (IData)((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT__is_sub))) 
                                                 ^ vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT____Vcellinp__alu__io_B))) 
                                + (QData)((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT__is_sub)))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT___shin_T_9 
        = ((vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT____Vcellinp__alu__io_A 
            >> 0x10U) | (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT____Vcellinp__alu__io_A 
                         << 0x10U));
    if ((((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT___GEN_3)) 
          & (2U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__state))) 
         & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___xbar_io_imem_rvalid))) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__ysyx_25020039_data_array 
            = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___xbar_io_imem_rdata;
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___wbu_raw_rs1_T 
        = ((0x1fU & (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst 
                     >> 0xfU)) == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_waddr));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___wbu_raw_rs2_T 
        = ((0x1fU & (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst 
                     >> 0x14U)) == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_waddr));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___exu_raw_rs1_T 
        = ((0x1fU & (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst 
                     >> 0xfU)) == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_waddr));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___exu_raw_rs2_T 
        = ((0x1fU & (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst 
                     >> 0x14U)) == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_waddr));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___lsu_raw_rs1_T 
        = ((0x1fU & (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst 
                     >> 0xfU)) == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_waddr));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___lsu_raw_rs2_T 
        = ((0x1fU & (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst 
                     >> 0x14U)) == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_waddr));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_17 
        = ((0x17U == (0x7fU & vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst)) 
           | (0x6fU == (0x7fU & vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_6 
        = ((0x100073U == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst) 
           | (0x30200073U == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_1 
        = ((0x37U == (0x7fU & vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst)) 
           | (0x17U == (0x7fU & vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN 
        = ((0x380U & (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst 
                      >> 5U)) | (0x7fU & vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT___shin_T_19 
        = ((0xff00ffU & (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT___shin_T_9 
                         >> 8U)) | (0xff00ff00U & (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT___shin_T_9 
                                                   << 8U)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__state 
        = __Vdly__ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__state;
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_7 
        = ((0x73U == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst) 
           | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_6));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT____VdfgTmp_h5669e553__0 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_1) 
           | (0x6fU == (0x7fU & vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_12 
        = ((0x6fU == (0x7fU & vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst)) 
           | (0x67U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_8 
        = ((0x103U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
           | ((0x83U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
              | ((3U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                 | ((0x203U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                    | (0x283U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT____VdfgTmp_h035fe50d__0 
        = ((0x63U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
           | (0xe3U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_9 
        = ((0x123U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
           | ((0xa3U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
              | (0x23U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_19 
        = ((0xf3U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
           | (0x173U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_10 
        = ((0x363U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
           | (0x3e3U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0 
        = ((0x1fc00U & (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst 
                        >> 0xfU)) | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT___shin_T_29 
        = ((0xf0f0f0fU & (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT___shin_T_19 
                          >> 4U)) | (0xf0f0f0f0U & 
                                     (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT___shin_T_19 
                                      << 4U)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT___GEN_2 
        = (2U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__state));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT___GEN 
        = (0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__state));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT___GEN_1 
        = (1U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__state));
    if ((0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__state))) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_imem_arburst = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_imem_arsize = 0U;
    } else {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_imem_arburst 
            = (1U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__state));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_imem_arsize 
            = ((1U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__state)) 
               << 1U);
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT___GEN_3 
        = ((0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__state)) 
           | (1U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__state)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_imem_arvalid 
        = ((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__state)) 
           & (1U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__state)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_13 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_1) 
           | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_12));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_14 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_9) 
           | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_8));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT____VdfgTmp_h22d8ad4d__0 
        = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_7)) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_19));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT____VdfgTmp_h9c4c2914__0 
        = ((0x263U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
           | ((0x2e3U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
              | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_10)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_18 
        = ((0x13U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
           | ((0x113U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
              | ((0x193U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                 | ((0x213U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                    | ((0x313U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                       | ((0x393U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                          | ((0x93U == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
                             | ((0x293U == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
                                | (0x8293U == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0)))))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_3 
        = ((0x33U == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
           | ((0x8033U == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
              | ((0xb3U == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
                 | ((0x133U == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
                    | ((0x1b3U == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
                       | ((0x233U == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
                          | ((0x2b3U == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
                             | ((0x82b3U == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
                                | ((0x333U == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
                                   | (0x3b3U == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0))))))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT___shin_T_39 
        = ((0x33333333U & (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT___shin_T_29 
                           >> 2U)) | (0xccccccccU & 
                                      (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT___shin_T_29 
                                       << 2U)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___icache_io_fencei_ready 
        = ((~ ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT___GEN_3) 
               | (2U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__state)))) 
           & (3U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__state)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_imem_rready 
        = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT___GEN_3)) 
           & (2U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__state)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT____VdfgTmp_hf6e21050__0 
        = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_imem_arvalid)) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT___GEN));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___clint_io_rdata 
        = ((0x2000000U == ((1U & ((0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT__state))
                                   ? ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT___GEN)) 
                                      | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_imem_arvalid))
                                   : ((3U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT__state)) 
                                      | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT___GEN_5))))
                            ? 0U : vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_alu_result))
            ? (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__clint__DOT__mtime)
            : (IData)((vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__clint__DOT__mtime 
                       >> 0x20U)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4delay_delayer_io_out_arsize 
        = ((0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT__state))
            ? ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_imem_arvalid)
                ? (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_imem_arsize)
                : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT___GEN_0)
                    ? 0U : (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_dmem_arsize)))
            : ((1U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT__state))
                ? (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_imem_arsize)
                : ((2U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT__state))
                    ? (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_dmem_arsize)
                    : 0U)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___deq_q_io_deq_bits_size 
        = (7U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q__DOT__full)
                  ? (IData)((vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q__DOT__ram 
                             >> 2U)) : (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4delay_delayer_io_out_arsize)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT___GEN_2 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_imem_arvalid) 
           | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT___GEN));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___idu_io_out_bits_signals_exu_alu_control 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_13)
            ? 0U : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT____VdfgTmp_h035fe50d__0)
                     ? 1U : (((0x263U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                              | (0x2e3U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)))
                              ? 3U : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_10)
                                       ? 5U : ((0x13U 
                                                == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN))
                                                ? 0U
                                                : (
                                                   (0x113U 
                                                    == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN))
                                                    ? 3U
                                                    : 
                                                   ((0x193U 
                                                     == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN))
                                                     ? 5U
                                                     : 
                                                    ((0x213U 
                                                      == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN))
                                                      ? 4U
                                                      : 
                                                     ((0x313U 
                                                       == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN))
                                                       ? 6U
                                                       : 
                                                      ((0x393U 
                                                        == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN))
                                                        ? 2U
                                                        : 
                                                       ((0x93U 
                                                         == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0)
                                                         ? 9U
                                                         : 
                                                        ((0x293U 
                                                          == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0)
                                                          ? 8U
                                                          : 
                                                         ((0x8293U 
                                                           == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0)
                                                           ? 7U
                                                           : 
                                                          ((0x33U 
                                                            == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0)
                                                            ? 0U
                                                            : 
                                                           ((0x8033U 
                                                             == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0)
                                                             ? 1U
                                                             : 
                                                            ((0xb3U 
                                                              == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0)
                                                              ? 9U
                                                              : 
                                                             ((0x133U 
                                                               == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0)
                                                               ? 3U
                                                               : 
                                                              ((0x1b3U 
                                                                == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0)
                                                                ? 5U
                                                                : 
                                                               ((0x233U 
                                                                 == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0)
                                                                 ? 4U
                                                                 : 
                                                                ((0x2b3U 
                                                                  == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0)
                                                                  ? 8U
                                                                  : 
                                                                 ((0x82b3U 
                                                                   == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0)
                                                                   ? 7U
                                                                   : 
                                                                  ((0x333U 
                                                                    == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0)
                                                                    ? 6U
                                                                    : 
                                                                   ((0x3b3U 
                                                                     == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0)
                                                                     ? 2U
                                                                     : 
                                                                    ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_14)
                                                                      ? 0U
                                                                      : 0xaU))))))))))))))))))))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_15 
        = ((0x67U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
           | ((0x63U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
              | ((0xe3U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                 | ((0x263U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                    | ((0x2e3U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                       | ((0x363U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                          | ((0x3e3U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                             | ((0x13U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                                | ((0x113U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                                   | ((0x193U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                                      | ((0x213U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                                         | ((0x313U 
                                             == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                                            | ((0x393U 
                                                == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                                               | ((0x93U 
                                                   == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
                                                  | ((0x293U 
                                                      == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
                                                     | ((0x8293U 
                                                         == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
                                                        | ((0x33U 
                                                            == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
                                                           | ((0x8033U 
                                                               == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
                                                              | ((0xb3U 
                                                                  == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
                                                                 | ((0x133U 
                                                                     == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
                                                                    | ((0x1b3U 
                                                                        == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
                                                                       | ((0x233U 
                                                                           == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
                                                                          | ((0x2b3U 
                                                                              == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
                                                                             | ((0x82b3U 
                                                                                == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
                                                                                | ((0x333U 
                                                                                == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
                                                                                | ((0x3b3U 
                                                                                == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
                                                                                | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_14)))))))))))))))))))))))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_11 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT____VdfgTmp_h035fe50d__0) 
           | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT____VdfgTmp_h9c4c2914__0));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT____VdfgTmp_h1c548fb4__0 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT____VdfgTmp_h5669e553__0) 
           | ((0x67U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
              | ((0x63U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                 | ((0xe3U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                    | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT____VdfgTmp_h9c4c2914__0)))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_4 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_18) 
           | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_3));
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_1) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___idu_io_out_bits_signals_exu_jump = 0xfU;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT___controller_io_signals_idu_imm_type = 1U;
    } else if ((0x6fU == (0x7fU & vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst))) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___idu_io_out_bits_signals_exu_jump = 8U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT___controller_io_signals_idu_imm_type = 2U;
    } else if ((0x67U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN))) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___idu_io_out_bits_signals_exu_jump = 9U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT___controller_io_signals_idu_imm_type = 0U;
    } else {
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___idu_io_out_bits_signals_exu_jump 
            = ((0x63U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN))
                ? 0U : ((0xe3U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN))
                         ? 1U : ((0x263U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN))
                                  ? 4U : ((0x2e3U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN))
                                           ? 5U : (
                                                   (0x363U 
                                                    == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN))
                                                    ? 6U
                                                    : 
                                                   ((0x3e3U 
                                                     == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN))
                                                     ? 7U
                                                     : 
                                                    (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_18) 
                                                      | ((0x33U 
                                                          == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
                                                         | ((0x8033U 
                                                             == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
                                                            | ((0xb3U 
                                                                == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
                                                               | ((0x133U 
                                                                   == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
                                                                  | ((0x1b3U 
                                                                      == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
                                                                     | ((0x233U 
                                                                         == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
                                                                        | ((0x2b3U 
                                                                            == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
                                                                           | ((0x82b3U 
                                                                               == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
                                                                              | ((0x333U 
                                                                                == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
                                                                                | ((0x3b3U 
                                                                                == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
                                                                                | ((0x123U 
                                                                                == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                                                                                | ((0xa3U 
                                                                                == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                                                                                | ((0x23U 
                                                                                == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                                                                                | ((0x103U 
                                                                                == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                                                                                | ((0x83U 
                                                                                == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                                                                                | ((3U 
                                                                                == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                                                                                | ((0x203U 
                                                                                == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                                                                                | ((0x283U 
                                                                                == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                                                                                | ((0x73U 
                                                                                == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst) 
                                                                                | ((0x30200073U 
                                                                                != vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst) 
                                                                                | (0x100073U 
                                                                                == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst))))))))))))))))))))))
                                                      ? 0xfU
                                                      : 0xaU)))))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT___controller_io_signals_idu_imm_type 
            = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_11)
                ? 4U : ((1U & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_9)) 
                               | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_4)))
                         ? 0U : 3U));
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT___shift_right_logic_T_5 
        = (0x1ffffffffULL & VL_SHIFTRS_QQI(33,33,5, 
                                           (((QData)((IData)(
                                                             ((7U 
                                                               == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_alu_control)) 
                                                              & ((2U 
                                                                  != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_alu_srcA)) 
                                                                 & (vlSelf->__VdfgTmp_h058c4e0a__0 
                                                                    >> 0x1fU))))) 
                                             << 0x20U) 
                                            | (QData)((IData)(
                                                              ((9U 
                                                                == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_alu_control))
                                                                ? 
                                                               ((0x55555555U 
                                                                 & (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT___shin_T_39 
                                                                    >> 1U)) 
                                                                | (0xaaaaaaaaU 
                                                                   & (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT___shin_T_39 
                                                                      << 1U)))
                                                                : vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT____Vcellinp__alu__io_A)))), 
                                           (0x1fU & vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT____Vcellinp__alu__io_B)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT____VdfgTmp_ha9dd3dc1__0 
        = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT___GEN_2)) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT__is_dmem));
    if ((0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT__state))) {
        if (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_imem_arvalid) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT___cpu_auto_master_out_arvalid 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_imem_arvalid;
            vlSelf->ysyxSoCFull__DOT__asic__DOT___cpu_auto_master_out_rready 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_imem_rready;
        } else {
            vlSelf->ysyxSoCFull__DOT__asic__DOT___cpu_auto_master_out_arvalid 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT____VdfgTmp_h09e3b85b__0) 
                   & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_dmem_arvalid));
            vlSelf->ysyxSoCFull__DOT__asic__DOT___cpu_auto_master_out_rready 
                = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT____VdfgTmp_h09e3b85b__0) 
                   & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_dmem_rready));
        }
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT___GEN_4 
            = (1U & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT__is_dmem)) 
                     | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT___GEN_2)));
        vlSelf->ysyxSoCFull__DOT__asic__DOT___cpu_auto_master_out_bready 
            = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT____VdfgTmp_ha9dd3dc1__0) 
               & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_dmem_bready));
        vlSelf->ysyxSoCFull__DOT__asic__DOT___cpu_auto_master_out_wvalid 
            = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT____VdfgTmp_ha9dd3dc1__0) 
               & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_dmem_wvalid));
        vlSelf->ysyxSoCFull__DOT__asic__DOT___cpu_auto_master_out_awvalid 
            = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT____VdfgTmp_ha9dd3dc1__0) 
               & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_dmem_awvalid));
    } else {
        if ((1U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT__state))) {
            vlSelf->ysyxSoCFull__DOT__asic__DOT___cpu_auto_master_out_arvalid 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_imem_arvalid;
            vlSelf->ysyxSoCFull__DOT__asic__DOT___cpu_auto_master_out_rready 
                = vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_imem_rready;
        } else {
            vlSelf->ysyxSoCFull__DOT__asic__DOT___cpu_auto_master_out_arvalid 
                = ((2U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT__state)) 
                   & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_dmem_arvalid));
            vlSelf->ysyxSoCFull__DOT__asic__DOT___cpu_auto_master_out_rready 
                = ((2U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT__state)) 
                   & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_dmem_rready));
        }
        vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT___GEN_4 
            = (1U & ((2U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT__state)) 
                     | (1U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT__state))));
        vlSelf->ysyxSoCFull__DOT__asic__DOT___cpu_auto_master_out_bready 
            = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT____VdfgTmp_hb30a1854__0) 
               & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_dmem_bready));
        vlSelf->ysyxSoCFull__DOT__asic__DOT___cpu_auto_master_out_wvalid 
            = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT____VdfgTmp_hb30a1854__0) 
               & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_dmem_wvalid));
        vlSelf->ysyxSoCFull__DOT__asic__DOT___cpu_auto_master_out_awvalid 
            = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT____VdfgTmp_hb30a1854__0) 
               & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_dmem_awvalid));
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_16 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT____VdfgTmp_h5669e553__0) 
           | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_15));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT____VdfgTmp_h782d1a2f__0 
        = (((~ ((0x37U == (0x7fU & vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst)) 
                | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_17))) 
            & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_15) 
               | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT____VdfgTmp_h22d8ad4d__0))) 
           & (0U != (0x1fU & (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst 
                              >> 0xfU))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT____VdfgTmp_hd67933d9__0 
        = (((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_13)) 
            & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_11) 
               | ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_18)) 
                  & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_3) 
                     | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_9))))) 
           & (0U != (0x1fU & (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst 
                              >> 0x14U))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT____VdfgTmp_h951ab88b__0 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT____VdfgTmp_h1c548fb4__0) 
           | ((0x13U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
              | ((0x113U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                 | ((0x193U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                    | ((0x213U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                       | ((0x313U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                          | ((0x393U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                             | ((0x93U == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
                                | ((0x293U == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
                                   | ((0x8293U == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_0) 
                                      | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_3)))))))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_5 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT____VdfgTmp_h1c548fb4__0) 
           | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_4));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT___shift_result_T_20 
        = ((0xff00ffU & ((0xffff00U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT___shift_right_logic_T_5) 
                                       << 8U)) | (0xffU 
                                                  & (IData)(
                                                            (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT___shift_right_logic_T_5 
                                                             >> 0x18U))))) 
           | (0xff00ff00U & (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT___shift_right_logic_T_5) 
                              << 0x18U) | (0xffff00U 
                                           & ((IData)(
                                                      (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT___shift_right_logic_T_5 
                                                       >> 0x10U)) 
                                              << 8U)))));
    if (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT___GEN_4) {
        vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4delay_delayer_io_out_wstrb = 0U;
        vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4delay_delayer_io_out_awsize = 0U;
    } else {
        vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4delay_delayer_io_out_wstrb 
            = (0xfU & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_signals_lsu_mem_wmask) 
                       << (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu__DOT___data_offset_T)));
        vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4delay_delayer_io_out_awsize 
            = ((0xfU == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_signals_lsu_mem_wmask))
                ? 2U : ((3U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_signals_lsu_mem_wmask))
                         ? 1U : ((1U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_signals_lsu_mem_wmask)) 
                                 << 1U)));
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___deq_q_1_io_deq_bits_size 
        = (7U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q_1__DOT__full)
                  ? (IData)((vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q_1__DOT__ram 
                             >> 2U)) : (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4delay_delayer_io_out_awsize)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_hafa17882__0 
        = (1U & (~ ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT___GEN_4)) 
                    & (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_alu_result 
                       >> 0x1fU))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___requestAWIO_T_11 
        = (0xfU & (0xaU ^ ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT___GEN_4)
                            ? 0U : (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_alu_result 
                                    >> 0x1cU))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT___shift_result_T_30 
        = ((0xf0f0f0fU & (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT___shift_result_T_20 
                          >> 4U)) | (0xf0f0f0f0U & 
                                     (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT___shift_result_T_20 
                                      << 4U)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0_io_enq_valid 
        = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__latched)) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___cpu_auto_master_out_awvalid));
    vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_wstrb 
        = (0xfU & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__in_wdeq_q__DOT__full)
                    ? (IData)((vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__in_wdeq_q__DOT__ram 
                               >> 1U)) : (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4delay_delayer_io_out_wstrb)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__requestAWIO_0_0 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_hafa17882__0) 
           | (0U == (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_hafa17882__0) 
                      << 2U) | ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT___GEN_4)
                                 ? 0U : (3U & (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_alu_result 
                                               >> 0x1cU))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__requestAWIO_0_1 
        = (0U == ((4U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___requestAWIO_T_11) 
                         >> 1U)) | (3U & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___requestAWIO_T_11))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT___shift_result_T_40 
        = ((0x33333333U & (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT___shift_result_T_30 
                           >> 2U)) | (0xccccccccU & 
                                      (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT___shift_result_T_30 
                                       << 2U)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awIn_0_io_deq_valid 
        = (1U & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__empty)) 
                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0_io_enq_valid)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____Vcellinp__awIn_0__io_enq_bits 
        = (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__requestAWIO_0_1) 
            << 1U) | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__requestAWIO_0_0));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_h1a379823__0 
        = (((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awIn_0_io_enq_ready) 
            | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__latched)) 
           & (((0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_0_count)) 
               | ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_0_last) 
                  == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__requestAWIO_0_1))) 
              & (7U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awFIFOMap_0_count))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT__casez_tmp 
        = ((8U & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_alu_control))
            ? ((4U & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_alu_control))
                ? 0U : ((2U & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_alu_control))
                         ? 0U : ((1U & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_alu_control))
                                  ? ((0x55555555U & 
                                      (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT___shift_result_T_40 
                                       >> 1U)) | (0xaaaaaaaaU 
                                                  & (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT___shift_result_T_40 
                                                     << 1U)))
                                  : (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT___shift_right_logic_T_5))))
            : ((4U & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_alu_control))
                ? ((2U & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_alu_control))
                    ? ((1U & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_alu_control))
                        ? (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT___shift_right_logic_T_5)
                        : (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT____Vcellinp__alu__io_A 
                           | vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT____Vcellinp__alu__io_B))
                    : ((1U & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_alu_control))
                        ? (1U & (~ (IData)((vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT___adder_result_ext_T_1 
                                            >> 0x20U))))
                        : (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT____Vcellinp__alu__io_A 
                           ^ vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT____Vcellinp__alu__io_B)))
                : ((2U & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_alu_control))
                    ? ((1U & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_alu_control))
                        ? (1U & ((IData)((vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT___adder_result_ext_T_1 
                                          >> 0x1fU)) 
                                 ^ (((vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT____Vcellinp__alu__io_A 
                                      >> 0x1fU) != 
                                     (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT____Vcellinp__alu__io_B 
                                      >> 0x1fU)) & 
                                    ((vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT____Vcellinp__alu__io_A 
                                      >> 0x1fU) != 
                                     (1U & (IData)(
                                                   (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT___adder_result_ext_T_1 
                                                    >> 0x1fU)))))))
                        : (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT____Vcellinp__alu__io_A 
                           & vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT____Vcellinp__alu__io_B))
                    : (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT___adder_result_ext_T_1))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__in_0_wvalid 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awIn_0_io_deq_valid) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___cpu_auto_master_out_wvalid));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awIn_0_io_deq_bits 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__empty)
            ? (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____Vcellinp__awIn_0__io_enq_bits)
            : vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__ram_ext__DOT__Memory
           [vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__awIn_0__DOT__wrap_1]);
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__in_0_awvalid 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___cpu_auto_master_out_awvalid) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT____VdfgTmp_h1a379823__0));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_forward_data 
        = ((5U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_wbu_reg_write_sel))
            ? vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_csr_rd1
            : ((3U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_wbu_reg_write_sel))
                ? ((IData)(4U) + vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_pc)
                : ((2U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_wbu_reg_write_sel))
                    ? vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_imm_ext
                    : ((1U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_wbu_reg_write_sel))
                        ? vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT__casez_tmp
                        : 0U))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___exu_io_pc_bits_pc_src 
        = ((0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_jump))
            ? (0U == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT__casez_tmp)
            : ((1U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_jump))
                ? (0U != vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT__casez_tmp)
                : ((5U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_jump))
                    ? (1U & (~ vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT__casez_tmp))
                    : ((4U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_jump))
                        ? (1U & vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT__casez_tmp)
                        : ((6U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_jump))
                            ? (1U & vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT__casez_tmp)
                            : ((7U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_jump))
                                ? (1U & (~ vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT__casez_tmp))
                                : ((8U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_jump))
                                    ? 1U : ((9U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_jump))
                                             ? 2U : 
                                            ((0xaU 
                                              == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_jump)) 
                                             << 2U)))))))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_out_0_wvalid 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__in_0_wvalid) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT___awIn_0_io_deq_bits));
    vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_out_0_awvalid 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__in_0_awvalid) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar__DOT__requestAWIO_0_0));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__is_ch 
        = ((0xfU != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_exu_jump)) 
           & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___exu_io_out_valid) 
              & (0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___exu_io_pc_bits_pc_src))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___in_wdeq_q_io_deq_valid 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__in_wdeq_q__DOT__full) 
           | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_out_0_wvalid));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___deq_q_1_io_deq_valid 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__deq_q_1__DOT__full) 
           | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4xbar_auto_anon_out_0_awvalid));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_is_flush 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__is_ch) 
           | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__is_irq));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__wbeats_valid 
        = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__wbeats_latched)) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___deq_q_1_io_deq_valid));
    vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awvalid 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___deq_q_1_io_deq_valid) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___in_awready_T));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT____Vcellinp__ifu__io_is_flush 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_is_flush) 
           | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__is_fencei));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___idu_io_ifu_signals_valid 
        = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_is_flush)) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_valid_r));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__w_todo 
        = ((0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__w_counter))
            ? (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__wbeats_valid)
            : (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__w_counter));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT____VdfgTmp_ha2215a43__0 
        = ((0U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__w_counter)) 
           | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT__wbeats_valid));
    vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_out_awvalid 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_awvalid) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4yank__DOT____VdfgTmp_he7a0eac8__0));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___ifu_io_pc_bits_next_pc 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT____Vcellinp__ifu__io_is_flush)
            ? ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__is_irq)
                ? vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__csr__DOT__rf_mtvec
                : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__is_ch)
                    ? ((4U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___exu_io_pc_bits_pc_src))
                        ? vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__csr__DOT__rf_mepc
                        : ((2U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___exu_io_pc_bits_pc_src))
                            ? (0xfffffffeU & vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu__DOT__alu__DOT__casez_tmp)
                            : ((1U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___exu_io_pc_bits_pc_src))
                                ? (0xfffffffeU & (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_imm_ext 
                                                  + vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_pc))
                                : ((IData)(4U) + vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_pc))))
                    : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__is_fencei)
                        ? vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu_io_in_bits_r_next_pc
                        : 0U))) : ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu__DOT__first_cycle)
                                    ? 0x30000000U : 
                                   ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu_io_in_valid_r)
                                     ? ((IData)(4U) 
                                        + vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu_io_in_bits_r_next_pc)
                                     : ((IData)(4U) 
                                        + vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu__DOT__pc_reg))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_raw 
        = ((((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT____VdfgTmp_h782d1a2f__0) 
             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___wbu_raw_rs1_T)) 
            | ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT____VdfgTmp_hd67933d9__0) 
               & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___wbu_raw_rs2_T))) 
           & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_signals_wbu_reg_write) 
              & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___idu_io_ifu_signals_valid) 
                 & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_valid))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_raw 
        = ((((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT____VdfgTmp_h782d1a2f__0) 
             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___exu_raw_rs1_T)) 
            | ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT____VdfgTmp_hd67933d9__0) 
               & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___exu_raw_rs2_T))) 
           & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_wbu_reg_write) 
              & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___idu_io_ifu_signals_valid) 
                 & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___exu_io_out_valid))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_raw 
        = ((((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT____VdfgTmp_h782d1a2f__0) 
             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___lsu_raw_rs1_T)) 
            | ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT____VdfgTmp_hd67933d9__0) 
               & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___lsu_raw_rs2_T))) 
           & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_signals_wbu_reg_write) 
              & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___idu_io_ifu_signals_valid) 
                 & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_valid))));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT___GEN_0 
        = (((~ ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT____VdfgTmp_h951ab88b__0) 
                | ((0x123U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                   | ((0xa3U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                      | ((0x23U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                         | ((0x103U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                            | ((0x83U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                               | ((3U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                                  | ((0x203U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                                     | ((0x283U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN)) 
                                        | ((0x73U == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst) 
                                           | ((0x100073U 
                                               == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst) 
                                              | ((0x30200073U 
                                                  == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst) 
                                                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT__controller__DOT___GEN_19)))))))))))))) 
            & (0x100fU == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu_io_in_bits_r_inst)) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___idu_io_ifu_signals_valid));
    vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_wvalid 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT___in_wdeq_q_io_deq_valid) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4frag__DOT____VdfgTmp_ha2215a43__0));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__in_0_awvalid 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_out_awvalid) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___anonIn_awready_T));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0_io_enq_valid 
        = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__latched)) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4yank_auto_out_awvalid));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_raw_rs1 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_raw) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___wbu_raw_rs1_T));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_raw_rs2 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_raw) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___wbu_raw_rs2_T));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_raw_rs1 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_raw) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___exu_raw_rs1_T));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_raw_rs2 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_raw) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___exu_raw_rs2_T));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_raw_rs1 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_raw) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___lsu_raw_rs1_T));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_raw_rs2 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_raw) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___lsu_raw_rs2_T));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__idu__DOT____VdfgTmp_h891f061b__0 
        = (1U & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT___GEN_0)) 
                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___icache_io_fencei_ready)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___icache_io_in_arready 
        = ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT___GEN_0)) 
           & (0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__state)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awIn_0_io_deq_valid 
        = (1U & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0__DOT__empty)) 
                 | (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__awIn_0_io_enq_valid)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_forward_rd1 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_raw_rs1) 
           & (4U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_wbu_reg_write_sel)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_forward_rd2 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_raw_rs2) 
           & (4U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__exu_io_in_bits_r_signals_wbu_reg_write_sel)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu__DOT__idle 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu_io_in_valid_r) 
           & ((~ (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT____Vcellinp__ifu__io_is_flush)) 
              & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___icache_io_in_arready)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT__in_0_wvalid 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__axi4xbar_1__DOT___awIn_0_io_deq_valid) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT___axi4frag_auto_out_wvalid));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___ifu_io_imem_arvalid 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu__DOT__idle) 
           & (0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu__DOT__state)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__raw_addr 
        = (0x3fffffffU & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___ifu_io_imem_arvalid)
                           ? (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___ifu_io_out_bits_pc 
                              >> 2U) : (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__in_addr 
                                        >> 2U)));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__hit 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__valid_array_0) 
           & (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__ysyx_25020039_tag_array 
              == vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__raw_addr));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT___core_io_imem_araddr 
        = (((1U != (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__state)) 
            | (0U == (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__state)))
            ? 0U : (vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__raw_addr 
                    << 2U));
    vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT____VdfgTmp_h51929d94__0 
        = ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___ifu_io_imem_arvalid) 
           & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__hit));
}

VL_INLINE_OPT void VysyxSoCFull___024root___nba_sequent__TOP__2(VysyxSoCFull___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VysyxSoCFull__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VysyxSoCFull___024root___nba_sequent__TOP__2\n"); );
    // Body
    vlSelf->__Vdly__ysyxSoCFull__DOT__flash__DOT__counter 
        = vlSelf->ysyxSoCFull__DOT__flash__DOT__counter;
    vlSelf->__Vdly__ysyxSoCFull__DOT__flash__DOT__cmd 
        = vlSelf->ysyxSoCFull__DOT__flash__DOT__cmd;
    vlSelf->__Vdly__ysyxSoCFull__DOT__flash__DOT__state 
        = vlSelf->ysyxSoCFull__DOT__flash__DOT__state;
    vlSelf->__Vdly__ysyxSoCFull__DOT__flash__DOT__data 
        = vlSelf->ysyxSoCFull__DOT__flash__DOT__data;
    vlSelf->__Vdly__ysyxSoCFull__DOT__flash__DOT__addr 
        = vlSelf->ysyxSoCFull__DOT__flash__DOT__addr;
    if (vlSelf->ysyxSoCFull__DOT____Vcellinp__flash__ss) {
        vlSelf->__Vdly__ysyxSoCFull__DOT__flash__DOT__counter = 0U;
        vlSelf->__Vdly__ysyxSoCFull__DOT__flash__DOT__cmd = 0U;
    } else if ((0U == (IData)(vlSelf->ysyxSoCFull__DOT__flash__DOT__state))) {
        vlSelf->__Vdly__ysyxSoCFull__DOT__flash__DOT__counter 
            = (0xffU & ((7U > (IData)(vlSelf->ysyxSoCFull__DOT__flash__DOT__counter))
                         ? ((IData)(1U) + (IData)(vlSelf->ysyxSoCFull__DOT__flash__DOT__counter))
                         : 0U));
        vlSelf->__Vdly__ysyxSoCFull__DOT__flash__DOT__cmd 
            = ((0xfeU & ((IData)(vlSelf->ysyxSoCFull__DOT__flash__DOT__cmd) 
                         << 1U)) | (IData)(vlSelf->ysyxSoCFull__DOT___asic_spi_mosi));
    } else {
        vlSelf->__Vdly__ysyxSoCFull__DOT__flash__DOT__counter 
            = (0xffU & ((1U == (IData)(vlSelf->ysyxSoCFull__DOT__flash__DOT__state))
                         ? ((0x17U > (IData)(vlSelf->ysyxSoCFull__DOT__flash__DOT__counter))
                             ? ((IData)(1U) + (IData)(vlSelf->ysyxSoCFull__DOT__flash__DOT__counter))
                             : 0U) : ((IData)(1U) + (IData)(vlSelf->ysyxSoCFull__DOT__flash__DOT__counter))));
    }
    if (vlSelf->ysyxSoCFull__DOT____Vcellinp__flash__ss) {
        vlSelf->__Vdly__ysyxSoCFull__DOT__flash__DOT__state = 0U;
    } else if ((0U == (IData)(vlSelf->ysyxSoCFull__DOT__flash__DOT__state))) {
        vlSelf->__Vdly__ysyxSoCFull__DOT__flash__DOT__state 
            = ((7U == (IData)(vlSelf->ysyxSoCFull__DOT__flash__DOT__counter))
                ? 1U : (IData)(vlSelf->ysyxSoCFull__DOT__flash__DOT__state));
    } else if ((1U == (IData)(vlSelf->ysyxSoCFull__DOT__flash__DOT__state))) {
        vlSelf->__Vdly__ysyxSoCFull__DOT__flash__DOT__state 
            = ((3U != (IData)(vlSelf->ysyxSoCFull__DOT__flash__DOT__cmd))
                ? 3U : ((0x17U == (IData)(vlSelf->ysyxSoCFull__DOT__flash__DOT__counter))
                         ? 2U : (IData)(vlSelf->ysyxSoCFull__DOT__flash__DOT__state)));
    } else if (VL_LIKELY((2U == (IData)(vlSelf->ysyxSoCFull__DOT__flash__DOT__state)))) {
        vlSelf->__Vdly__ysyxSoCFull__DOT__flash__DOT__state 
            = vlSelf->ysyxSoCFull__DOT__flash__DOT__state;
    } else {
        VL_FWRITEF(0x80000002U,"Assertion failed: Unsupported command `%xh`, only support `03h` read command\n",
                   8,vlSelf->ysyxSoCFull__DOT__flash__DOT__cmd);
        vlSelf->__Vdly__ysyxSoCFull__DOT__flash__DOT__state 
            = vlSelf->ysyxSoCFull__DOT__flash__DOT__state;
        VL_WRITEF("[%0t] %%Error: flash.v:44: Assertion failed in %NysyxSoCFull.flash\n",
                  64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
        VL_STOP_MT("../ysyxSoC/perip/flash/flash.v", 44, "");
    }
    if (vlSelf->ysyxSoCFull__DOT____Vcellinp__flash__ss) {
        vlSelf->__Vdly__ysyxSoCFull__DOT__flash__DOT__data = 0U;
        vlSelf->__Vdly__ysyxSoCFull__DOT__flash__DOT__addr = 0U;
    } else {
        if ((2U == (IData)(vlSelf->ysyxSoCFull__DOT__flash__DOT__state))) {
            vlSelf->__Vdly__ysyxSoCFull__DOT__flash__DOT__data 
                = (((0U == (IData)(vlSelf->ysyxSoCFull__DOT__flash__DOT__counter))
                     ? vlSelf->ysyxSoCFull__DOT__flash__DOT__data_bswap
                     : vlSelf->ysyxSoCFull__DOT__flash__DOT__data) 
                   << 1U);
        }
        if (((1U == (IData)(vlSelf->ysyxSoCFull__DOT__flash__DOT__state)) 
             & (0x17U > (IData)(vlSelf->ysyxSoCFull__DOT__flash__DOT__counter)))) {
            vlSelf->__Vdly__ysyxSoCFull__DOT__flash__DOT__addr 
                = ((0xfffffeU & (vlSelf->ysyxSoCFull__DOT__flash__DOT__addr 
                                 << 1U)) | (IData)(vlSelf->ysyxSoCFull__DOT___asic_spi_mosi));
        }
    }
}

void VysyxSoCFull___024unit____Vdpiimwrap_flash_read_TOP____024unit(IData/*31:0*/ addr, IData/*31:0*/ &data);

VL_INLINE_OPT void VysyxSoCFull___024root___nba_sequent__TOP__6(VysyxSoCFull___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VysyxSoCFull__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VysyxSoCFull___024root___nba_sequent__TOP__6\n"); );
    // Init
    IData/*31:0*/ __Vtask_flash_read__5__data;
    __Vtask_flash_read__5__data = 0;
    // Body
    if (((1U == (IData)(vlSelf->ysyxSoCFull__DOT__flash__DOT__state)) 
         & (0x17U == (IData)(vlSelf->ysyxSoCFull__DOT__flash__DOT__counter)))) {
        if (VL_LIKELY((3U == (IData)(vlSelf->ysyxSoCFull__DOT__flash__DOT__cmd)))) {
            VysyxSoCFull___024unit____Vdpiimwrap_flash_read_TOP____024unit(
                                                                           ((0xfffffeU 
                                                                             & (vlSelf->ysyxSoCFull__DOT__flash__DOT__addr 
                                                                                << 1U)) 
                                                                            | (IData)(vlSelf->ysyxSoCFull__DOT___asic_spi_mosi)), __Vtask_flash_read__5__data);
            vlSelf->ysyxSoCFull__DOT__flash__DOT__rdata 
                = __Vtask_flash_read__5__data;
        } else {
            VL_FWRITEF(0x80000002U,"Assertion failed: Unsupport command `%xh`, only support `03h` read command\n",
                       8,vlSelf->ysyxSoCFull__DOT__flash__DOT__cmd);
            VL_WRITEF("[%0t] %%Error: flash.v:98: Assertion failed in %NysyxSoCFull.flash.flash_cmd_i\n",
                      64,VL_TIME_UNITED_Q(1),-9,vlSymsp->name());
            VL_STOP_MT("../ysyxSoC/perip/flash/flash.v", 98, "");
        }
    }
    vlSelf->ysyxSoCFull__DOT__flash__DOT__data_bswap 
        = ((vlSelf->ysyxSoCFull__DOT__flash__DOT__rdata 
            << 0x18U) | ((0xff0000U & (vlSelf->ysyxSoCFull__DOT__flash__DOT__rdata 
                                       << 8U)) | ((0xff00U 
                                                   & (vlSelf->ysyxSoCFull__DOT__flash__DOT__rdata 
                                                      >> 8U)) 
                                                  | (vlSelf->ysyxSoCFull__DOT__flash__DOT__rdata 
                                                     >> 0x18U))));
}
