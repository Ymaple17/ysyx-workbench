// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See Vtop.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "Vtop___024root.h"

VL_ATTR_COLD void Vtop___024root___eval_static(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___eval_static\n"); );
}

VL_ATTR_COLD void Vtop___024root___eval_initial__TOP(Vtop___024root* vlSelf);

VL_ATTR_COLD void Vtop___024root___eval_initial(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___eval_initial\n"); );
    // Body
    Vtop___024root___eval_initial__TOP(vlSelf);
    vlSelf->__Vm_traceActivity[3U] = 1U;
    vlSelf->__Vm_traceActivity[2U] = 1U;
    vlSelf->__Vm_traceActivity[1U] = 1U;
    vlSelf->__Vm_traceActivity[0U] = 1U;
    vlSelf->__Vtrigrprev__TOP__clk = vlSelf->clk;
    vlSelf->__Vtrigrprev__TOP__top__DOT__out = vlSelf->top__DOT__out;
    vlSelf->__Vtrigrprev__TOP__top__DOT__rs1 = vlSelf->top__DOT__rs1;
    vlSelf->__Vtrigrprev__TOP__top__DOT__rs2 = vlSelf->top__DOT__rs2;
}

VL_ATTR_COLD void Vtop___024root___eval_initial__TOP(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___eval_initial__TOP\n"); );
    // Body
    vlSelf->top__DOT__PCAsrc = 0U;
    vlSelf->top__DOT__PCBsrc = 0U;
    vlSelf->top__DOT__pc_next = 0U;
    vlSelf->top__DOT__my_pc__DOT__cnt = 1U;
    vlSelf->top__DOT__my_instrmem__DOT__instr_memory[0U] = 0x108113U;
    vlSelf->top__DOT__my_instrmem__DOT__instr_memory[1U] = 0x111193U;
    vlSelf->top__DOT__my_instrmem__DOT__instr_memory[2U] = 0x100073U;
    vlSelf->top__DOT__my_reg__DOT__general_register[1U] = 1U;
}

VL_ATTR_COLD void Vtop___024root___eval_final(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___eval_final\n"); );
}

VL_ATTR_COLD void Vtop___024root___eval_triggers__stl(Vtop___024root* vlSelf);
#ifdef VL_DEBUG
VL_ATTR_COLD void Vtop___024root___dump_triggers__stl(Vtop___024root* vlSelf);
#endif  // VL_DEBUG
VL_ATTR_COLD void Vtop___024root___eval_stl(Vtop___024root* vlSelf);

VL_ATTR_COLD void Vtop___024root___eval_settle(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___eval_settle\n"); );
    // Init
    CData/*0:0*/ __VstlContinue;
    // Body
    vlSelf->__VstlIterCount = 0U;
    __VstlContinue = 1U;
    while (__VstlContinue) {
        __VstlContinue = 0U;
        Vtop___024root___eval_triggers__stl(vlSelf);
        if (vlSelf->__VstlTriggered.any()) {
            __VstlContinue = 1U;
            if (VL_UNLIKELY((0x64U < vlSelf->__VstlIterCount))) {
#ifdef VL_DEBUG
                Vtop___024root___dump_triggers__stl(vlSelf);
#endif
                VL_FATAL_MT("vsrc/top.v", 1, "", "Settle region did not converge.");
            }
            vlSelf->__VstlIterCount = ((IData)(1U) 
                                       + vlSelf->__VstlIterCount);
            Vtop___024root___eval_stl(vlSelf);
        }
    }
}

#ifdef VL_DEBUG
VL_ATTR_COLD void Vtop___024root___dump_triggers__stl(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___dump_triggers__stl\n"); );
    // Body
    if ((1U & (~ (IData)(vlSelf->__VstlTriggered.any())))) {
        VL_DBG_MSGF("         No triggers active\n");
    }
    if (vlSelf->__VstlTriggered.at(0U)) {
        VL_DBG_MSGF("         'stl' region trigger index 0 is active: Internal 'stl' trigger - first iteration\n");
    }
}
#endif  // VL_DEBUG

void Vtop___024root____Vdpiimwrap_top__DOT__my_id__DOT__nemu_trap_TOP();
extern const VlUnpacked<CData/*0:0*/, 32> Vtop__ConstPool__TABLE_h18daf42f_0;
extern const VlUnpacked<CData/*0:0*/, 32> Vtop__ConstPool__TABLE_h30a380ae_0;

VL_ATTR_COLD void Vtop___024root___stl_sequent__TOP__0(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___stl_sequent__TOP__0\n"); );
    // Body
    vlSelf->top__DOT__instr = vlSelf->top__DOT__my_instrmem__DOT__instr_memory
        [(0xffffU & (vlSelf->top__DOT__pc_next >> 2U))];
    if ((0x100073U == vlSelf->top__DOT__instr)) {
        Vtop___024root____Vdpiimwrap_top__DOT__my_id__DOT__nemu_trap_TOP();
    }
    if ((1U & (~ (vlSelf->top__DOT__instr >> 6U)))) {
        if ((0x20U & vlSelf->top__DOT__instr)) {
            if ((1U & (~ (vlSelf->top__DOT__instr >> 4U)))) {
                if ((1U & (~ (vlSelf->top__DOT__instr 
                              >> 3U)))) {
                    if ((1U & (~ (vlSelf->top__DOT__instr 
                                  >> 2U)))) {
                        if ((2U & vlSelf->top__DOT__instr)) {
                            if ((1U & vlSelf->top__DOT__instr)) {
                                vlSelf->top__DOT__MemOp 
                                    = ((0U == (7U & 
                                               (vlSelf->top__DOT__instr 
                                                >> 0xcU)))
                                        ? 0U : ((1U 
                                                 == 
                                                 (7U 
                                                  & (vlSelf->top__DOT__instr 
                                                     >> 0xcU)))
                                                 ? 1U
                                                 : 2U));
                            }
                        }
                    }
                }
            }
        } else if ((0x10U & vlSelf->top__DOT__instr)) {
            if ((1U & (~ (vlSelf->top__DOT__instr >> 3U)))) {
                if ((1U & (~ (vlSelf->top__DOT__instr 
                              >> 2U)))) {
                    if ((2U & vlSelf->top__DOT__instr)) {
                        if ((1U & vlSelf->top__DOT__instr)) {
                            if ((0x4000U & vlSelf->top__DOT__instr)) {
                                if ((0x2000U & vlSelf->top__DOT__instr)) {
                                    if ((1U & (~ (vlSelf->top__DOT__instr 
                                                  >> 0xcU)))) {
                                        vlSelf->top__DOT__MemOp = 0U;
                                    }
                                } else if ((1U & (~ 
                                                  (vlSelf->top__DOT__instr 
                                                   >> 0xcU)))) {
                                    vlSelf->top__DOT__MemOp = 0U;
                                }
                            } else if ((0x2000U & vlSelf->top__DOT__instr)) {
                                vlSelf->top__DOT__MemOp = 0U;
                            } else if ((1U & (~ (vlSelf->top__DOT__instr 
                                                 >> 0xcU)))) {
                                vlSelf->top__DOT__MemOp = 0U;
                            }
                        }
                    }
                }
            }
        } else if ((1U & (~ (vlSelf->top__DOT__instr 
                             >> 3U)))) {
            if ((1U & (~ (vlSelf->top__DOT__instr >> 2U)))) {
                if ((2U & vlSelf->top__DOT__instr)) {
                    if ((1U & vlSelf->top__DOT__instr)) {
                        vlSelf->top__DOT__MemOp = (
                                                   (0x4000U 
                                                    & vlSelf->top__DOT__instr)
                                                    ? 
                                                   ((0x2000U 
                                                     & vlSelf->top__DOT__instr)
                                                     ? 5U
                                                     : 
                                                    ((0x1000U 
                                                      & vlSelf->top__DOT__instr)
                                                      ? 5U
                                                      : 4U))
                                                    : 
                                                   ((0x2000U 
                                                     & vlSelf->top__DOT__instr)
                                                     ? 
                                                    ((0x1000U 
                                                      & vlSelf->top__DOT__instr)
                                                      ? 5U
                                                      : 2U)
                                                     : 
                                                    ((0x1000U 
                                                      & vlSelf->top__DOT__instr)
                                                      ? 1U
                                                      : 0U)));
                    }
                }
            }
        }
        if ((1U & (~ (vlSelf->top__DOT__instr >> 5U)))) {
            if ((0x10U & vlSelf->top__DOT__instr)) {
                if ((1U & (~ (vlSelf->top__DOT__instr 
                              >> 3U)))) {
                    if ((1U & (~ (vlSelf->top__DOT__instr 
                                  >> 2U)))) {
                        if ((2U & vlSelf->top__DOT__instr)) {
                            if ((1U & vlSelf->top__DOT__instr)) {
                                if ((0x4000U & vlSelf->top__DOT__instr)) {
                                    if ((0x2000U & vlSelf->top__DOT__instr)) {
                                        if ((1U & (~ 
                                                   (vlSelf->top__DOT__instr 
                                                    >> 0xcU)))) {
                                            vlSelf->top__DOT__MemtoReg = 0U;
                                        }
                                    } else if ((1U 
                                                & (~ 
                                                   (vlSelf->top__DOT__instr 
                                                    >> 0xcU)))) {
                                        vlSelf->top__DOT__MemtoReg = 0U;
                                    }
                                } else if ((0x2000U 
                                            & vlSelf->top__DOT__instr)) {
                                    vlSelf->top__DOT__MemtoReg = 0U;
                                } else if ((1U & (~ 
                                                  (vlSelf->top__DOT__instr 
                                                   >> 0xcU)))) {
                                    vlSelf->top__DOT__MemtoReg = 0U;
                                }
                            }
                        }
                    }
                }
            } else if ((1U & (~ (vlSelf->top__DOT__instr 
                                 >> 3U)))) {
                if ((1U & (~ (vlSelf->top__DOT__instr 
                              >> 2U)))) {
                    if ((2U & vlSelf->top__DOT__instr)) {
                        if ((1U & vlSelf->top__DOT__instr)) {
                            vlSelf->top__DOT__MemtoReg = 1U;
                        }
                    }
                }
            }
        }
    }
    vlSelf->top__DOT__rs1 = (0x1fU & (vlSelf->top__DOT__instr 
                                      >> 0xfU));
    vlSelf->top__DOT__rs2 = (0x1fU & (vlSelf->top__DOT__instr 
                                      >> 0x14U));
    vlSelf->top__DOT__my_id__DOT__immI = (((- (IData)(
                                                      (vlSelf->top__DOT__instr 
                                                       >> 0x1fU))) 
                                           << 0xcU) 
                                          | (vlSelf->top__DOT__instr 
                                             >> 0x14U));
    vlSelf->top__DOT__my_id__DOT__immB = (((- (IData)(
                                                      (vlSelf->top__DOT__instr 
                                                       >> 0x1fU))) 
                                           << 0xcU) 
                                          | ((0x800U 
                                              & (vlSelf->top__DOT__instr 
                                                 << 4U)) 
                                             | ((0x7e0U 
                                                 & (vlSelf->top__DOT__instr 
                                                    >> 0x14U)) 
                                                | (0x1eU 
                                                   & (vlSelf->top__DOT__instr 
                                                      >> 7U)))));
    vlSelf->top__DOT__my_id__DOT__immS = (((- (IData)(
                                                      (vlSelf->top__DOT__instr 
                                                       >> 0x1fU))) 
                                           << 0xcU) 
                                          | ((0xfe0U 
                                              & (vlSelf->top__DOT__instr 
                                                 >> 0x14U)) 
                                             | (0x1fU 
                                                & (vlSelf->top__DOT__instr 
                                                   >> 7U))));
    if ((0x40U & vlSelf->top__DOT__instr)) {
        vlSelf->top__DOT__RegWr = (1U & (IData)(((0x20U 
                                                  == 
                                                  (0x30U 
                                                   & vlSelf->top__DOT__instr)) 
                                                 & ((8U 
                                                     & vlSelf->top__DOT__instr)
                                                     ? (IData)(
                                                               (7U 
                                                                == 
                                                                (7U 
                                                                 & vlSelf->top__DOT__instr)))
                                                     : (IData)(
                                                               (7U 
                                                                == 
                                                                (7U 
                                                                 & vlSelf->top__DOT__instr)))))));
        if ((0x20U & vlSelf->top__DOT__instr)) {
            if ((0x10U & vlSelf->top__DOT__instr)) {
                vlSelf->top__DOT__branch = 0U;
                vlSelf->top__DOT__ALUctr = 0U;
                vlSelf->top__DOT__imm = 0U;
            } else if ((8U & vlSelf->top__DOT__instr)) {
                if ((4U & vlSelf->top__DOT__instr)) {
                    if ((2U & vlSelf->top__DOT__instr)) {
                        if ((1U & vlSelf->top__DOT__instr)) {
                            vlSelf->top__DOT__branch = 1U;
                            vlSelf->top__DOT__imm = 
                                (((- (IData)((vlSelf->top__DOT__instr 
                                              >> 0x1fU))) 
                                  << 0x14U) | ((0xff000U 
                                                & vlSelf->top__DOT__instr) 
                                               | ((0x800U 
                                                   & (vlSelf->top__DOT__instr 
                                                      >> 9U)) 
                                                  | (0x7feU 
                                                     & (vlSelf->top__DOT__instr 
                                                        >> 0x14U)))));
                        } else {
                            vlSelf->top__DOT__branch = 0U;
                            vlSelf->top__DOT__imm = 0U;
                        }
                    } else {
                        vlSelf->top__DOT__branch = 0U;
                        vlSelf->top__DOT__imm = 0U;
                    }
                } else {
                    vlSelf->top__DOT__branch = 0U;
                    vlSelf->top__DOT__imm = 0U;
                }
                vlSelf->top__DOT__ALUctr = 0U;
            } else if ((4U & vlSelf->top__DOT__instr)) {
                if ((2U & vlSelf->top__DOT__instr)) {
                    if ((1U & vlSelf->top__DOT__instr)) {
                        vlSelf->top__DOT__branch = 2U;
                        vlSelf->top__DOT__imm = vlSelf->top__DOT__my_id__DOT__immI;
                    } else {
                        vlSelf->top__DOT__branch = 0U;
                        vlSelf->top__DOT__imm = 0U;
                    }
                } else {
                    vlSelf->top__DOT__branch = 0U;
                    vlSelf->top__DOT__imm = 0U;
                }
                vlSelf->top__DOT__ALUctr = 0U;
            } else if ((2U & vlSelf->top__DOT__instr)) {
                if ((1U & vlSelf->top__DOT__instr)) {
                    if ((0x4000U & vlSelf->top__DOT__instr)) {
                        if ((0x2000U & vlSelf->top__DOT__instr)) {
                            vlSelf->top__DOT__branch 
                                = ((0x1000U & vlSelf->top__DOT__instr)
                                    ? 7U : 6U);
                            vlSelf->top__DOT__ALUctr = 0xaU;
                        } else {
                            vlSelf->top__DOT__branch 
                                = ((0x1000U & vlSelf->top__DOT__instr)
                                    ? 7U : 6U);
                            vlSelf->top__DOT__ALUctr = 2U;
                        }
                    } else if ((0x2000U & vlSelf->top__DOT__instr)) {
                        vlSelf->top__DOT__branch = 7U;
                        vlSelf->top__DOT__ALUctr = 0xaU;
                    } else {
                        vlSelf->top__DOT__branch = 
                            ((0x1000U & vlSelf->top__DOT__instr)
                              ? 5U : 4U);
                        vlSelf->top__DOT__ALUctr = 2U;
                    }
                    vlSelf->top__DOT__imm = vlSelf->top__DOT__my_id__DOT__immB;
                } else {
                    vlSelf->top__DOT__branch = 0U;
                    vlSelf->top__DOT__ALUctr = 0U;
                    vlSelf->top__DOT__imm = 0U;
                }
            } else {
                vlSelf->top__DOT__branch = 0U;
                vlSelf->top__DOT__ALUctr = 0U;
                vlSelf->top__DOT__imm = 0U;
            }
        } else {
            vlSelf->top__DOT__branch = 0U;
            vlSelf->top__DOT__ALUctr = 0U;
            vlSelf->top__DOT__imm = 0U;
        }
        vlSelf->top__DOT__ALUBsrc = (1U & (IData)((
                                                   (0x20U 
                                                    == 
                                                    (0x30U 
                                                     & vlSelf->top__DOT__instr)) 
                                                   & ((8U 
                                                       & vlSelf->top__DOT__instr)
                                                       ? (IData)(
                                                                 (7U 
                                                                  == 
                                                                  (7U 
                                                                   & vlSelf->top__DOT__instr)))
                                                       : (IData)(
                                                                 (7U 
                                                                  == 
                                                                  (7U 
                                                                   & vlSelf->top__DOT__instr)))))));
    } else {
        if ((0x20U & vlSelf->top__DOT__instr)) {
            vlSelf->top__DOT__RegWr = (1U & (IData)(
                                                    ((0x13U 
                                                      == 
                                                      (0x1fU 
                                                       & vlSelf->top__DOT__instr)) 
                                                     & ((0x4000U 
                                                         & vlSelf->top__DOT__instr)
                                                         ? (IData)(
                                                                   ((0x1000U 
                                                                     != 
                                                                     (0x3000U 
                                                                      & vlSelf->top__DOT__instr)) 
                                                                    | ((0U 
                                                                        == 
                                                                        (vlSelf->top__DOT__instr 
                                                                         >> 0x19U)) 
                                                                       | (0x20U 
                                                                          == 
                                                                          (vlSelf->top__DOT__instr 
                                                                           >> 0x19U)))))
                                                         : (IData)(
                                                                   ((0U 
                                                                     != 
                                                                     (0x3000U 
                                                                      & vlSelf->top__DOT__instr)) 
                                                                    | ((0U 
                                                                        == 
                                                                        (vlSelf->top__DOT__instr 
                                                                         >> 0x19U)) 
                                                                       | (0x20U 
                                                                          == 
                                                                          (vlSelf->top__DOT__instr 
                                                                           >> 0x19U)))))))));
            if ((0x10U & vlSelf->top__DOT__instr)) {
                if ((8U & vlSelf->top__DOT__instr)) {
                    vlSelf->top__DOT__ALUctr = 0U;
                    vlSelf->top__DOT__imm = 0U;
                } else if ((4U & vlSelf->top__DOT__instr)) {
                    vlSelf->top__DOT__ALUctr = 0U;
                    vlSelf->top__DOT__imm = 0U;
                } else if ((2U & vlSelf->top__DOT__instr)) {
                    vlSelf->top__DOT__ALUctr = ((1U 
                                                 & vlSelf->top__DOT__instr)
                                                 ? 
                                                ((0x4000U 
                                                  & vlSelf->top__DOT__instr)
                                                  ? 
                                                 ((0x2000U 
                                                   & vlSelf->top__DOT__instr)
                                                   ? 
                                                  ((0x1000U 
                                                    & vlSelf->top__DOT__instr)
                                                    ? 7U
                                                    : 6U)
                                                   : 
                                                  ((0x1000U 
                                                    & vlSelf->top__DOT__instr)
                                                    ? 
                                                   ((0U 
                                                     == 
                                                     (vlSelf->top__DOT__instr 
                                                      >> 0x19U))
                                                     ? 5U
                                                     : 
                                                    ((0x20U 
                                                      == 
                                                      (vlSelf->top__DOT__instr 
                                                       >> 0x19U))
                                                      ? 0xdU
                                                      : 0U))
                                                    : 4U))
                                                  : 
                                                 ((0x2000U 
                                                   & vlSelf->top__DOT__instr)
                                                   ? 
                                                  ((0x1000U 
                                                    & vlSelf->top__DOT__instr)
                                                    ? 0xaU
                                                    : 2U)
                                                   : 
                                                  ((0x1000U 
                                                    & vlSelf->top__DOT__instr)
                                                    ? 1U
                                                    : 
                                                   ((0U 
                                                     == 
                                                     (vlSelf->top__DOT__instr 
                                                      >> 0x19U))
                                                     ? 0U
                                                     : 
                                                    ((0x20U 
                                                      == 
                                                      (vlSelf->top__DOT__instr 
                                                       >> 0x19U))
                                                      ? 8U
                                                      : 0U)))))
                                                 : 0U);
                    if ((1U & (~ vlSelf->top__DOT__instr))) {
                        vlSelf->top__DOT__imm = 0U;
                    }
                } else {
                    vlSelf->top__DOT__ALUctr = 0U;
                    vlSelf->top__DOT__imm = 0U;
                }
            } else {
                vlSelf->top__DOT__ALUctr = 0U;
                vlSelf->top__DOT__imm = ((8U & vlSelf->top__DOT__instr)
                                          ? 0U : ((4U 
                                                   & vlSelf->top__DOT__instr)
                                                   ? 0U
                                                   : 
                                                  ((2U 
                                                    & vlSelf->top__DOT__instr)
                                                    ? 
                                                   ((1U 
                                                     & vlSelf->top__DOT__instr)
                                                     ? vlSelf->top__DOT__my_id__DOT__immS
                                                     : 0U)
                                                    : 0U)));
            }
            vlSelf->top__DOT__ALUBsrc = (1U & (IData)(
                                                      (3U 
                                                       == 
                                                       (0x1fU 
                                                        & vlSelf->top__DOT__instr))));
        } else if ((0x10U & vlSelf->top__DOT__instr)) {
            vlSelf->top__DOT__RegWr = (1U & (IData)(
                                                    ((3U 
                                                      == 
                                                      (0xfU 
                                                       & vlSelf->top__DOT__instr)) 
                                                     & ((0x5000U 
                                                         != 
                                                         (0x7000U 
                                                          & vlSelf->top__DOT__instr)) 
                                                        | ((0U 
                                                            == 
                                                            (vlSelf->top__DOT__instr 
                                                             >> 0x19U)) 
                                                           | (0x20U 
                                                              == 
                                                              (vlSelf->top__DOT__instr 
                                                               >> 0x19U)))))));
            if ((8U & vlSelf->top__DOT__instr)) {
                vlSelf->top__DOT__ALUctr = 0U;
                vlSelf->top__DOT__imm = 0U;
            } else if ((4U & vlSelf->top__DOT__instr)) {
                vlSelf->top__DOT__ALUctr = 0U;
                vlSelf->top__DOT__imm = 0U;
            } else if ((2U & vlSelf->top__DOT__instr)) {
                if ((1U & vlSelf->top__DOT__instr)) {
                    if ((0x4000U & vlSelf->top__DOT__instr)) {
                        if ((0x2000U & vlSelf->top__DOT__instr)) {
                            vlSelf->top__DOT__ALUctr 
                                = ((0x1000U & vlSelf->top__DOT__instr)
                                    ? 7U : 6U);
                            vlSelf->top__DOT__imm = vlSelf->top__DOT__my_id__DOT__immI;
                        } else if ((0x1000U & vlSelf->top__DOT__instr)) {
                            if ((0U == (vlSelf->top__DOT__instr 
                                        >> 0x19U))) {
                                vlSelf->top__DOT__ALUctr = 5U;
                                vlSelf->top__DOT__imm 
                                    = vlSelf->top__DOT__my_id__DOT__immI;
                            } else if ((0x20U == (vlSelf->top__DOT__instr 
                                                  >> 0x19U))) {
                                vlSelf->top__DOT__ALUctr = 0xdU;
                                vlSelf->top__DOT__imm 
                                    = vlSelf->top__DOT__my_id__DOT__immI;
                            } else {
                                vlSelf->top__DOT__ALUctr = 0U;
                                vlSelf->top__DOT__imm = 0U;
                            }
                        } else {
                            vlSelf->top__DOT__ALUctr = 4U;
                            vlSelf->top__DOT__imm = vlSelf->top__DOT__my_id__DOT__immI;
                        }
                    } else {
                        vlSelf->top__DOT__ALUctr = 
                            ((0x2000U & vlSelf->top__DOT__instr)
                              ? ((0x1000U & vlSelf->top__DOT__instr)
                                  ? 0xaU : 2U) : ((0x1000U 
                                                   & vlSelf->top__DOT__instr)
                                                   ? 1U
                                                   : 0U));
                        vlSelf->top__DOT__imm = vlSelf->top__DOT__my_id__DOT__immI;
                    }
                } else {
                    vlSelf->top__DOT__ALUctr = 0U;
                    vlSelf->top__DOT__imm = 0U;
                }
            } else {
                vlSelf->top__DOT__ALUctr = 0U;
                vlSelf->top__DOT__imm = 0U;
            }
            vlSelf->top__DOT__ALUBsrc = (1U & (IData)(
                                                      (3U 
                                                       == 
                                                       (0xfU 
                                                        & vlSelf->top__DOT__instr))));
        } else {
            vlSelf->top__DOT__RegWr = (1U & (IData)(
                                                    (3U 
                                                     == 
                                                     (0xfU 
                                                      & vlSelf->top__DOT__instr))));
            vlSelf->top__DOT__ALUctr = 0U;
            vlSelf->top__DOT__ALUBsrc = (1U & (IData)(
                                                      (3U 
                                                       == 
                                                       (0xfU 
                                                        & vlSelf->top__DOT__instr))));
            vlSelf->top__DOT__imm = ((8U & vlSelf->top__DOT__instr)
                                      ? 0U : ((4U & vlSelf->top__DOT__instr)
                                               ? 0U
                                               : ((2U 
                                                   & vlSelf->top__DOT__instr)
                                                   ? 
                                                  ((1U 
                                                    & vlSelf->top__DOT__instr)
                                                    ? vlSelf->top__DOT__my_id__DOT__immI
                                                    : 0U)
                                                   : 0U)));
        }
        vlSelf->top__DOT__branch = 0U;
    }
    if (vlSelf->top__DOT__ALUBsrc) {
        if (vlSelf->top__DOT__ALUBsrc) {
            vlSelf->top__DOT__my_alu__DOT__B = vlSelf->top__DOT__imm;
        }
    } else {
        vlSelf->top__DOT__my_alu__DOT__B = vlSelf->top__DOT__op2;
    }
    vlSelf->top__DOT__ALUout = 0U;
    vlSelf->top__DOT__zero = 0U;
    vlSelf->top__DOT__less = 0U;
    vlSelf->top__DOT__of = 0U;
    vlSelf->top__DOT__zf = 0U;
    vlSelf->top__DOT__cf = 0U;
    vlSelf->top__DOT__my_alu__DOT__xb = 0U;
    if ((8U & (IData)(vlSelf->top__DOT__ALUctr))) {
        if ((4U & (IData)(vlSelf->top__DOT__ALUctr))) {
            if ((2U & (IData)(vlSelf->top__DOT__ALUctr))) {
                vlSelf->top__DOT__ALUout = ((1U & (IData)(vlSelf->top__DOT__ALUctr))
                                             ? (vlSelf->top__DOT__op1 
                                                & vlSelf->top__DOT__my_alu__DOT__B)
                                             : (vlSelf->top__DOT__op1 
                                                | vlSelf->top__DOT__my_alu__DOT__B));
            } else if ((1U & (IData)(vlSelf->top__DOT__ALUctr))) {
                if ((8U & (IData)(vlSelf->top__DOT__ALUctr))) {
                    vlSelf->top__DOT__ALUout = ((0x10U 
                                                 & vlSelf->top__DOT__my_alu__DOT__B)
                                                 ? 
                                                (((- (IData)(
                                                             (vlSelf->top__DOT__op1 
                                                              >> 0x1fU))) 
                                                  << 0x10U) 
                                                 | (vlSelf->top__DOT__op1 
                                                    >> 0x10U))
                                                 : vlSelf->top__DOT__op1);
                    vlSelf->top__DOT__ALUout = ((8U 
                                                 & vlSelf->top__DOT__my_alu__DOT__B)
                                                 ? 
                                                (((- (IData)(
                                                             (vlSelf->top__DOT__ALUout 
                                                              >> 0x1fU))) 
                                                  << 0x18U) 
                                                 | (vlSelf->top__DOT__ALUout 
                                                    >> 8U))
                                                 : vlSelf->top__DOT__ALUout);
                    vlSelf->top__DOT__ALUout = ((4U 
                                                 & vlSelf->top__DOT__my_alu__DOT__B)
                                                 ? 
                                                (((- (IData)(
                                                             (vlSelf->top__DOT__ALUout 
                                                              >> 0x1fU))) 
                                                  << 0x1cU) 
                                                 | (vlSelf->top__DOT__ALUout 
                                                    >> 4U))
                                                 : vlSelf->top__DOT__ALUout);
                    vlSelf->top__DOT__ALUout = ((2U 
                                                 & vlSelf->top__DOT__my_alu__DOT__B)
                                                 ? 
                                                (((- (IData)(
                                                             (vlSelf->top__DOT__ALUout 
                                                              >> 0x1fU))) 
                                                  << 0x1eU) 
                                                 | (vlSelf->top__DOT__ALUout 
                                                    >> 2U))
                                                 : vlSelf->top__DOT__ALUout);
                    vlSelf->top__DOT__ALUout = ((1U 
                                                 & vlSelf->top__DOT__my_alu__DOT__B)
                                                 ? 
                                                ((0x80000000U 
                                                  & vlSelf->top__DOT__ALUout) 
                                                 | (vlSelf->top__DOT__ALUout 
                                                    >> 1U))
                                                 : vlSelf->top__DOT__ALUout);
                } else {
                    vlSelf->top__DOT__ALUout = ((0x10U 
                                                 & vlSelf->top__DOT__my_alu__DOT__B)
                                                 ? 
                                                (vlSelf->top__DOT__op1 
                                                 >> 0x10U)
                                                 : vlSelf->top__DOT__op1);
                    vlSelf->top__DOT__ALUout = ((8U 
                                                 & vlSelf->top__DOT__my_alu__DOT__B)
                                                 ? 
                                                (vlSelf->top__DOT__ALUout 
                                                 >> 8U)
                                                 : vlSelf->top__DOT__ALUout);
                    vlSelf->top__DOT__ALUout = ((4U 
                                                 & vlSelf->top__DOT__my_alu__DOT__B)
                                                 ? 
                                                (vlSelf->top__DOT__ALUout 
                                                 >> 4U)
                                                 : vlSelf->top__DOT__ALUout);
                    vlSelf->top__DOT__ALUout = ((2U 
                                                 & vlSelf->top__DOT__my_alu__DOT__B)
                                                 ? 
                                                (vlSelf->top__DOT__ALUout 
                                                 >> 2U)
                                                 : vlSelf->top__DOT__ALUout);
                    vlSelf->top__DOT__ALUout = ((1U 
                                                 & vlSelf->top__DOT__my_alu__DOT__B)
                                                 ? 
                                                (vlSelf->top__DOT__ALUout 
                                                 >> 1U)
                                                 : vlSelf->top__DOT__ALUout);
                }
            } else {
                vlSelf->top__DOT__ALUout = (vlSelf->top__DOT__op1 
                                            ^ vlSelf->top__DOT__my_alu__DOT__B);
            }
        } else if ((2U & (IData)(vlSelf->top__DOT__ALUctr))) {
            if ((1U & (IData)(vlSelf->top__DOT__ALUctr))) {
                vlSelf->top__DOT__ALUout = vlSelf->top__DOT__my_alu__DOT__B;
            } else {
                vlSelf->top__DOT__my_alu__DOT__xb = 
                    (1U ^ vlSelf->top__DOT__my_alu__DOT__B);
                vlSelf->top__DOT__cf = (1U & (IData)(
                                                     (1ULL 
                                                      & ((1ULL 
                                                          + 
                                                          ((QData)((IData)(vlSelf->top__DOT__my_alu__DOT__xb)) 
                                                           + (QData)((IData)(vlSelf->top__DOT__op1)))) 
                                                         >> 0x20U))));
                vlSelf->top__DOT__ALUout = ((IData)(1U) 
                                            + (vlSelf->top__DOT__my_alu__DOT__xb 
                                               + vlSelf->top__DOT__op1));
                vlSelf->top__DOT__zf = (1U & (~ (IData)(
                                                        (0U 
                                                         != vlSelf->top__DOT__ALUout))));
                if (vlSelf->top__DOT__zf) {
                    vlSelf->top__DOT__zero = 1U;
                    vlSelf->top__DOT__ALUout = 0U;
                } else if ((vlSelf->top__DOT__ALUout 
                            >> 0x1fU)) {
                    vlSelf->top__DOT__less = 1U;
                    vlSelf->top__DOT__ALUout = 1U;
                } else {
                    vlSelf->top__DOT__ALUout = 0U;
                    vlSelf->top__DOT__less = 0U;
                }
            }
        } else if ((1U & (IData)(vlSelf->top__DOT__ALUctr))) {
            vlSelf->top__DOT__ALUout = ((0x10U & vlSelf->top__DOT__my_alu__DOT__B)
                                         ? (vlSelf->top__DOT__op1 
                                            << 0x10U)
                                         : vlSelf->top__DOT__op1);
            vlSelf->top__DOT__ALUout = ((8U & vlSelf->top__DOT__my_alu__DOT__B)
                                         ? (vlSelf->top__DOT__ALUout 
                                            << 8U) : vlSelf->top__DOT__op1);
            vlSelf->top__DOT__ALUout = ((4U & vlSelf->top__DOT__my_alu__DOT__B)
                                         ? (vlSelf->top__DOT__ALUout 
                                            << 4U) : vlSelf->top__DOT__ALUout);
            vlSelf->top__DOT__ALUout = ((2U & vlSelf->top__DOT__my_alu__DOT__B)
                                         ? (vlSelf->top__DOT__ALUout 
                                            << 2U) : vlSelf->top__DOT__ALUout);
            vlSelf->top__DOT__ALUout = ((1U & vlSelf->top__DOT__my_alu__DOT__B)
                                         ? (vlSelf->top__DOT__ALUout 
                                            << 1U) : vlSelf->top__DOT__ALUout);
        } else {
            vlSelf->top__DOT__my_alu__DOT__xb = (vlSelf->top__DOT__my_alu__DOT__B 
                                                 ^ 
                                                 (- (IData)(
                                                            (1U 
                                                             & ((IData)(vlSelf->top__DOT__ALUctr) 
                                                                >> 3U)))));
            vlSelf->top__DOT__cf = (1U & (IData)((1ULL 
                                                  & ((((QData)((IData)(vlSelf->top__DOT__my_alu__DOT__xb)) 
                                                       + (QData)((IData)(vlSelf->top__DOT__op1))) 
                                                      + (QData)((IData)(
                                                                        (1U 
                                                                         & ((IData)(vlSelf->top__DOT__ALUctr) 
                                                                            >> 3U))))) 
                                                     >> 0x20U))));
            vlSelf->top__DOT__ALUout = ((vlSelf->top__DOT__my_alu__DOT__xb 
                                         + vlSelf->top__DOT__op1) 
                                        + (IData)((QData)((IData)(
                                                                  (1U 
                                                                   & ((IData)(vlSelf->top__DOT__ALUctr) 
                                                                      >> 3U))))));
            vlSelf->top__DOT__of = (((vlSelf->top__DOT__op1 
                                      >> 0x1fU) == 
                                     (vlSelf->top__DOT__my_alu__DOT__xb 
                                      >> 0x1fU)) & 
                                    ((vlSelf->top__DOT__ALUout 
                                      >> 0x1fU) != 
                                     (vlSelf->top__DOT__op1 
                                      >> 0x1fU)));
            vlSelf->top__DOT__zf = (1U & (~ (IData)(
                                                    (0U 
                                                     != vlSelf->top__DOT__ALUout))));
        }
    } else if ((4U & (IData)(vlSelf->top__DOT__ALUctr))) {
        if ((2U & (IData)(vlSelf->top__DOT__ALUctr))) {
            vlSelf->top__DOT__ALUout = ((1U & (IData)(vlSelf->top__DOT__ALUctr))
                                         ? (vlSelf->top__DOT__op1 
                                            & vlSelf->top__DOT__my_alu__DOT__B)
                                         : (vlSelf->top__DOT__op1 
                                            | vlSelf->top__DOT__my_alu__DOT__B));
        } else if ((1U & (IData)(vlSelf->top__DOT__ALUctr))) {
            if ((8U & (IData)(vlSelf->top__DOT__ALUctr))) {
                vlSelf->top__DOT__ALUout = ((0x10U 
                                             & vlSelf->top__DOT__my_alu__DOT__B)
                                             ? (((- (IData)(
                                                            (vlSelf->top__DOT__op1 
                                                             >> 0x1fU))) 
                                                 << 0x10U) 
                                                | (vlSelf->top__DOT__op1 
                                                   >> 0x10U))
                                             : vlSelf->top__DOT__op1);
                vlSelf->top__DOT__ALUout = ((8U & vlSelf->top__DOT__my_alu__DOT__B)
                                             ? (((- (IData)(
                                                            (vlSelf->top__DOT__ALUout 
                                                             >> 0x1fU))) 
                                                 << 0x18U) 
                                                | (vlSelf->top__DOT__ALUout 
                                                   >> 8U))
                                             : vlSelf->top__DOT__ALUout);
                vlSelf->top__DOT__ALUout = ((4U & vlSelf->top__DOT__my_alu__DOT__B)
                                             ? (((- (IData)(
                                                            (vlSelf->top__DOT__ALUout 
                                                             >> 0x1fU))) 
                                                 << 0x1cU) 
                                                | (vlSelf->top__DOT__ALUout 
                                                   >> 4U))
                                             : vlSelf->top__DOT__ALUout);
                vlSelf->top__DOT__ALUout = ((2U & vlSelf->top__DOT__my_alu__DOT__B)
                                             ? (((- (IData)(
                                                            (vlSelf->top__DOT__ALUout 
                                                             >> 0x1fU))) 
                                                 << 0x1eU) 
                                                | (vlSelf->top__DOT__ALUout 
                                                   >> 2U))
                                             : vlSelf->top__DOT__ALUout);
                vlSelf->top__DOT__ALUout = ((1U & vlSelf->top__DOT__my_alu__DOT__B)
                                             ? ((0x80000000U 
                                                 & vlSelf->top__DOT__ALUout) 
                                                | (vlSelf->top__DOT__ALUout 
                                                   >> 1U))
                                             : vlSelf->top__DOT__ALUout);
            } else {
                vlSelf->top__DOT__ALUout = ((0x10U 
                                             & vlSelf->top__DOT__my_alu__DOT__B)
                                             ? (vlSelf->top__DOT__op1 
                                                >> 0x10U)
                                             : vlSelf->top__DOT__op1);
                vlSelf->top__DOT__ALUout = ((8U & vlSelf->top__DOT__my_alu__DOT__B)
                                             ? (vlSelf->top__DOT__ALUout 
                                                >> 8U)
                                             : vlSelf->top__DOT__ALUout);
                vlSelf->top__DOT__ALUout = ((4U & vlSelf->top__DOT__my_alu__DOT__B)
                                             ? (vlSelf->top__DOT__ALUout 
                                                >> 4U)
                                             : vlSelf->top__DOT__ALUout);
                vlSelf->top__DOT__ALUout = ((2U & vlSelf->top__DOT__my_alu__DOT__B)
                                             ? (vlSelf->top__DOT__ALUout 
                                                >> 2U)
                                             : vlSelf->top__DOT__ALUout);
                vlSelf->top__DOT__ALUout = ((1U & vlSelf->top__DOT__my_alu__DOT__B)
                                             ? (vlSelf->top__DOT__ALUout 
                                                >> 1U)
                                             : vlSelf->top__DOT__ALUout);
            }
        } else {
            vlSelf->top__DOT__ALUout = (vlSelf->top__DOT__op1 
                                        ^ vlSelf->top__DOT__my_alu__DOT__B);
        }
    } else if ((2U & (IData)(vlSelf->top__DOT__ALUctr))) {
        if ((1U & (IData)(vlSelf->top__DOT__ALUctr))) {
            vlSelf->top__DOT__ALUout = vlSelf->top__DOT__my_alu__DOT__B;
        } else {
            vlSelf->top__DOT__my_alu__DOT__xb = (1U 
                                                 ^ vlSelf->top__DOT__my_alu__DOT__B);
            vlSelf->top__DOT__cf = (1U & (IData)((1ULL 
                                                  & ((1ULL 
                                                      + 
                                                      ((QData)((IData)(vlSelf->top__DOT__my_alu__DOT__xb)) 
                                                       + (QData)((IData)(vlSelf->top__DOT__op1)))) 
                                                     >> 0x20U))));
            vlSelf->top__DOT__ALUout = ((IData)(1U) 
                                        + (vlSelf->top__DOT__my_alu__DOT__xb 
                                           + vlSelf->top__DOT__op1));
            vlSelf->top__DOT__zf = (1U & (~ (IData)(
                                                    (0U 
                                                     != vlSelf->top__DOT__ALUout))));
            if (vlSelf->top__DOT__zf) {
                vlSelf->top__DOT__zero = 1U;
                vlSelf->top__DOT__ALUout = 0U;
            } else if ((vlSelf->top__DOT__ALUout >> 0x1fU)) {
                vlSelf->top__DOT__less = 1U;
                vlSelf->top__DOT__ALUout = 1U;
            } else {
                vlSelf->top__DOT__ALUout = 0U;
                vlSelf->top__DOT__less = 0U;
            }
        }
    } else if ((1U & (IData)(vlSelf->top__DOT__ALUctr))) {
        vlSelf->top__DOT__ALUout = ((0x10U & vlSelf->top__DOT__my_alu__DOT__B)
                                     ? (vlSelf->top__DOT__op1 
                                        << 0x10U) : vlSelf->top__DOT__op1);
        vlSelf->top__DOT__ALUout = ((8U & vlSelf->top__DOT__my_alu__DOT__B)
                                     ? (vlSelf->top__DOT__ALUout 
                                        << 8U) : vlSelf->top__DOT__op1);
        vlSelf->top__DOT__ALUout = ((4U & vlSelf->top__DOT__my_alu__DOT__B)
                                     ? (vlSelf->top__DOT__ALUout 
                                        << 4U) : vlSelf->top__DOT__ALUout);
        vlSelf->top__DOT__ALUout = ((2U & vlSelf->top__DOT__my_alu__DOT__B)
                                     ? (vlSelf->top__DOT__ALUout 
                                        << 2U) : vlSelf->top__DOT__ALUout);
        vlSelf->top__DOT__ALUout = ((1U & vlSelf->top__DOT__my_alu__DOT__B)
                                     ? (vlSelf->top__DOT__ALUout 
                                        << 1U) : vlSelf->top__DOT__ALUout);
    } else {
        vlSelf->top__DOT__my_alu__DOT__xb = (vlSelf->top__DOT__my_alu__DOT__B 
                                             ^ (- (IData)(
                                                          (1U 
                                                           & ((IData)(vlSelf->top__DOT__ALUctr) 
                                                              >> 3U)))));
        vlSelf->top__DOT__cf = (1U & (IData)((1ULL 
                                              & ((((QData)((IData)(vlSelf->top__DOT__my_alu__DOT__xb)) 
                                                   + (QData)((IData)(vlSelf->top__DOT__op1))) 
                                                  + (QData)((IData)(
                                                                    (1U 
                                                                     & ((IData)(vlSelf->top__DOT__ALUctr) 
                                                                        >> 3U))))) 
                                                 >> 0x20U))));
        vlSelf->top__DOT__ALUout = ((vlSelf->top__DOT__my_alu__DOT__xb 
                                     + vlSelf->top__DOT__op1) 
                                    + (IData)((QData)((IData)(
                                                              (1U 
                                                               & ((IData)(vlSelf->top__DOT__ALUctr) 
                                                                  >> 3U))))));
        vlSelf->top__DOT__of = (((vlSelf->top__DOT__op1 
                                  >> 0x1fU) == (vlSelf->top__DOT__my_alu__DOT__xb 
                                                >> 0x1fU)) 
                                & ((vlSelf->top__DOT__ALUout 
                                    >> 0x1fU) != (vlSelf->top__DOT__op1 
                                                  >> 0x1fU)));
        vlSelf->top__DOT__zf = (1U & (~ (IData)((0U 
                                                 != vlSelf->top__DOT__ALUout))));
    }
    if (vlSelf->top__DOT__MemtoReg) {
        if (vlSelf->top__DOT__MemtoReg) {
            vlSelf->top__DOT__out = vlSelf->top__DOT__data_out;
        }
    } else {
        vlSelf->top__DOT__out = vlSelf->top__DOT__ALUout;
    }
    vlSelf->__Vtableidx1 = (((IData)(vlSelf->top__DOT__zf) 
                             << 4U) | (((IData)(vlSelf->top__DOT__less) 
                                        << 3U) | (IData)(vlSelf->top__DOT__branch)));
    vlSelf->top__DOT__PCAsrc = Vtop__ConstPool__TABLE_h18daf42f_0
        [vlSelf->__Vtableidx1];
    vlSelf->top__DOT__PCBsrc = Vtop__ConstPool__TABLE_h30a380ae_0
        [vlSelf->__Vtableidx1];
}

VL_ATTR_COLD void Vtop___024root___eval_stl(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___eval_stl\n"); );
    // Body
    if (vlSelf->__VstlTriggered.at(0U)) {
        Vtop___024root___stl_sequent__TOP__0(vlSelf);
        vlSelf->__Vm_traceActivity[3U] = 1U;
        vlSelf->__Vm_traceActivity[2U] = 1U;
        vlSelf->__Vm_traceActivity[1U] = 1U;
        vlSelf->__Vm_traceActivity[0U] = 1U;
    }
}

#ifdef VL_DEBUG
VL_ATTR_COLD void Vtop___024root___dump_triggers__act(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___dump_triggers__act\n"); );
    // Body
    if ((1U & (~ (IData)(vlSelf->__VactTriggered.any())))) {
        VL_DBG_MSGF("         No triggers active\n");
    }
    if (vlSelf->__VactTriggered.at(0U)) {
        VL_DBG_MSGF("         'act' region trigger index 0 is active: @(posedge clk)\n");
    }
    if (vlSelf->__VactTriggered.at(1U)) {
        VL_DBG_MSGF("         'act' region trigger index 1 is active: @([changed] top.out)\n");
    }
    if (vlSelf->__VactTriggered.at(2U)) {
        VL_DBG_MSGF("         'act' region trigger index 2 is active: @([changed] top.rs1 or [changed] top.rs2)\n");
    }
}
#endif  // VL_DEBUG

#ifdef VL_DEBUG
VL_ATTR_COLD void Vtop___024root___dump_triggers__nba(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___dump_triggers__nba\n"); );
    // Body
    if ((1U & (~ (IData)(vlSelf->__VnbaTriggered.any())))) {
        VL_DBG_MSGF("         No triggers active\n");
    }
    if (vlSelf->__VnbaTriggered.at(0U)) {
        VL_DBG_MSGF("         'nba' region trigger index 0 is active: @(posedge clk)\n");
    }
    if (vlSelf->__VnbaTriggered.at(1U)) {
        VL_DBG_MSGF("         'nba' region trigger index 1 is active: @([changed] top.out)\n");
    }
    if (vlSelf->__VnbaTriggered.at(2U)) {
        VL_DBG_MSGF("         'nba' region trigger index 2 is active: @([changed] top.rs1 or [changed] top.rs2)\n");
    }
}
#endif  // VL_DEBUG

VL_ATTR_COLD void Vtop___024root___ctor_var_reset(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___ctor_var_reset\n"); );
    // Body
    vlSelf->clk = VL_RAND_RESET_I(1);
    vlSelf->top__DOT__instr = VL_RAND_RESET_I(32);
    vlSelf->top__DOT__rs1 = VL_RAND_RESET_I(5);
    vlSelf->top__DOT__rs2 = VL_RAND_RESET_I(5);
    vlSelf->top__DOT__ALUctr = VL_RAND_RESET_I(4);
    vlSelf->top__DOT__ALUBsrc = VL_RAND_RESET_I(1);
    vlSelf->top__DOT__less = VL_RAND_RESET_I(1);
    vlSelf->top__DOT__zero = VL_RAND_RESET_I(1);
    vlSelf->top__DOT__of = VL_RAND_RESET_I(1);
    vlSelf->top__DOT__cf = VL_RAND_RESET_I(1);
    vlSelf->top__DOT__zf = VL_RAND_RESET_I(1);
    vlSelf->top__DOT__RegWr = VL_RAND_RESET_I(1);
    vlSelf->top__DOT__PCAsrc = VL_RAND_RESET_I(1);
    vlSelf->top__DOT__PCBsrc = VL_RAND_RESET_I(1);
    vlSelf->top__DOT__MemOp = VL_RAND_RESET_I(3);
    vlSelf->top__DOT__MemtoReg = VL_RAND_RESET_I(1);
    vlSelf->top__DOT__pc_next = VL_RAND_RESET_I(32);
    vlSelf->top__DOT__branch = VL_RAND_RESET_I(3);
    vlSelf->top__DOT__ALUout = VL_RAND_RESET_I(32);
    vlSelf->top__DOT__data_out = VL_RAND_RESET_I(32);
    vlSelf->top__DOT__opcode = VL_RAND_RESET_I(7);
    vlSelf->top__DOT__op1 = VL_RAND_RESET_I(32);
    vlSelf->top__DOT__op2 = VL_RAND_RESET_I(32);
    vlSelf->top__DOT__out = VL_RAND_RESET_I(32);
    vlSelf->top__DOT__imm = VL_RAND_RESET_I(32);
    vlSelf->top__DOT__my_id__DOT__immI = VL_RAND_RESET_I(32);
    vlSelf->top__DOT__my_id__DOT__immS = VL_RAND_RESET_I(32);
    vlSelf->top__DOT__my_id__DOT__immB = VL_RAND_RESET_I(32);
    vlSelf->top__DOT__my_pc__DOT__cnt = VL_RAND_RESET_I(32);
    vlSelf->top__DOT__my_pc__DOT__PCa = VL_RAND_RESET_I(32);
    vlSelf->top__DOT__my_pc__DOT__PCb = VL_RAND_RESET_I(32);
    for (int __Vi0 = 0; __Vi0 < 65536; ++__Vi0) {
        vlSelf->top__DOT__my_instrmem__DOT__instr_memory[__Vi0] = VL_RAND_RESET_I(32);
    }
    for (int __Vi0 = 0; __Vi0 < 32; ++__Vi0) {
        vlSelf->top__DOT__my_reg__DOT__general_register[__Vi0] = VL_RAND_RESET_I(32);
    }
    vlSelf->top__DOT__my_alu__DOT__B = VL_RAND_RESET_I(32);
    vlSelf->top__DOT__my_alu__DOT__xb = VL_RAND_RESET_I(32);
    for (int __Vi0 = 0; __Vi0 < 65536; ++__Vi0) {
        vlSelf->top__DOT__my_ram__DOT__data_memory[__Vi0] = VL_RAND_RESET_I(32);
    }
    vlSelf->__Vtableidx1 = 0;
    vlSelf->__Vdlyvdim0__top__DOT__my_reg__DOT__general_register__v0 = 0;
    vlSelf->__Vdlyvval__top__DOT__my_reg__DOT__general_register__v0 = VL_RAND_RESET_I(32);
    vlSelf->__Vdlyvset__top__DOT__my_reg__DOT__general_register__v0 = 0;
    vlSelf->__Vtrigrprev__TOP__clk = VL_RAND_RESET_I(1);
    vlSelf->__Vtrigrprev__TOP__top__DOT__out = VL_RAND_RESET_I(32);
    vlSelf->__Vtrigrprev__TOP__top__DOT__rs1 = VL_RAND_RESET_I(5);
    vlSelf->__Vtrigrprev__TOP__top__DOT__rs2 = VL_RAND_RESET_I(5);
    vlSelf->__VactDidInit = 0;
    for (int __Vi0 = 0; __Vi0 < 4; ++__Vi0) {
        vlSelf->__Vm_traceActivity[__Vi0] = 0;
    }
}
