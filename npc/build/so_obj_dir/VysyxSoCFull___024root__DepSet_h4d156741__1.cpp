// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See VysyxSoCFull.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "VysyxSoCFull__Syms.h"
#include "VysyxSoCFull___024root.h"

void VysyxSoCFull___024unit____Vdpiimwrap_psram_read_TOP____024unit(IData/*31:0*/ addr, IData/*31:0*/ &data);
void VysyxSoCFull___024unit____Vdpiimwrap_psram_write_TOP____024unit(IData/*31:0*/ addr, IData/*31:0*/ data, IData/*31:0*/ wcount);

VL_INLINE_OPT void VysyxSoCFull___024root___nba_comb__TOP__2(VysyxSoCFull___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VysyxSoCFull__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VysyxSoCFull___024root___nba_comb__TOP__2\n"); );
    // Body
    if (((~ (IData)(vlSelf->ysyxSoCFull__DOT___asic_psram_ce_n)) 
         & (3U == (IData)(vlSelf->ysyxSoCFull__DOT__psram__DOT__state)))) {
        VysyxSoCFull___024unit____Vdpiimwrap_psram_read_TOP____024unit(vlSelf->ysyxSoCFull__DOT__psram__DOT__addr_reg, vlSelf->__Vtask_psram_read__6__data);
        vlSelf->ysyxSoCFull__DOT__psram__DOT__data 
            = vlSelf->__Vtask_psram_read__6__data;
    } else if (((IData)(vlSelf->ysyxSoCFull__DOT___asic_psram_ce_n) 
                & (0U < (IData)(vlSelf->ysyxSoCFull__DOT__psram__DOT__wcount)))) {
        VysyxSoCFull___024unit____Vdpiimwrap_psram_write_TOP____024unit(vlSelf->ysyxSoCFull__DOT__psram__DOT__addr_reg, vlSelf->ysyxSoCFull__DOT__psram__DOT__wdata_reg, 
                                                                        (0xfU 
                                                                         & ((IData)(vlSelf->ysyxSoCFull__DOT__psram__DOT__wcount) 
                                                                            >> 1U)));
        vlSelf->ysyxSoCFull__DOT__psram__DOT__data = 0U;
    } else {
        vlSelf->ysyxSoCFull__DOT__psram__DOT__data = 0U;
    }
    vlSelf->ysyxSoCFull__DOT__asic__DOT__lpsram__DOT__mpsram__DOT__u0__DOT__mr_din 
        = ((((((3U & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lpsram__DOT__mpsram__DOT__qspi_dio__out__strong__out0) 
                      & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lpsram__DOT__mpsram__DOT__douten))) 
               | (0xcU & ((IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lpsram__DOT__mpsram__DOT__qspi_dio__out__strong__out1) 
                          & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lpsram__DOT__mpsram__DOT__douten)))) 
              & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lpsram__DOT__qspi_dio__en0)) 
             & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lpsram__DOT__qspi_dio__en0)) 
            & (IData)(vlSelf->ysyxSoCFull__DOT__asic__DOT__lpsram__DOT__qspi_dio__en0)) 
           | ((((0x15U > (IData)(vlSelf->ysyxSoCFull__DOT__psram__DOT__count))
                 ? 0U : ((IData)(vlSelf->ysyxSoCFull__DOT__psram__DOT____VdfgTmp_h568a1b22__0)
                          ? (vlSelf->ysyxSoCFull__DOT__psram__DOT__data_reg 
                             >> 4U) : ((IData)(vlSelf->ysyxSoCFull__DOT__psram__DOT____VdfgTmp_ha937f5ac__0)
                                        ? vlSelf->ysyxSoCFull__DOT__psram__DOT__data_reg
                                        : ((IData)(vlSelf->ysyxSoCFull__DOT__psram__DOT____VdfgTmp_ha908376e__0)
                                            ? (vlSelf->ysyxSoCFull__DOT__psram__DOT__data_reg 
                                               >> 0xcU)
                                            : ((IData)(vlSelf->ysyxSoCFull__DOT__psram__DOT____VdfgTmp_h568dc419__0)
                                                ? (vlSelf->ysyxSoCFull__DOT__psram__DOT__data_reg 
                                                   >> 8U)
                                                : ((IData)(vlSelf->ysyxSoCFull__DOT__psram__DOT____VdfgTmp_ha9fe39b4__0)
                                                    ? 
                                                   (vlSelf->ysyxSoCFull__DOT__psram__DOT__data_reg 
                                                    >> 0x14U)
                                                    : 
                                                   ((IData)(vlSelf->ysyxSoCFull__DOT__psram__DOT____VdfgTmp_ha9c6c44e__0)
                                                     ? 
                                                    (vlSelf->ysyxSoCFull__DOT__psram__DOT__data_reg 
                                                     >> 0x10U)
                                                     : 
                                                    ((IData)(vlSelf->ysyxSoCFull__DOT__psram__DOT____VdfgTmp_ha9d93457__0)
                                                      ? 
                                                     (vlSelf->ysyxSoCFull__DOT__psram__DOT__data_reg 
                                                      >> 0x1cU)
                                                      : 
                                                     ((IData)(vlSelf->ysyxSoCFull__DOT__psram__DOT____VdfgTmp_h557d1af5__0)
                                                       ? 
                                                      (vlSelf->ysyxSoCFull__DOT__psram__DOT__data_reg 
                                                       >> 0x18U)
                                                       : 0U))))))))) 
               & (IData)(vlSelf->ysyxSoCFull__DOT__dio__en2)) 
              & (IData)(vlSelf->ysyxSoCFull__DOT__dio__en2)));
}
