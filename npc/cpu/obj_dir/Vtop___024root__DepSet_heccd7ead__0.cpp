// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See Vtop.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "Vtop___024root.h"

void Vtop___024root___eval_act(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___eval_act\n"); );
}

VL_INLINE_OPT void Vtop___024root___nba_sequent__TOP__0(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___nba_sequent__TOP__0\n"); );
    // Body
    VL_WRITEF("write %b in reg%2#\n",32,vlSelf->top__DOT__out,
              5,(0x1fU & (vlSelf->top__DOT__instr >> 7U)));
}

void Vtop___024root____Vdpiimwrap_top__DOT__my_id__DOT__nemu_trap_TOP();

VL_INLINE_OPT void Vtop___024root___nba_sequent__TOP__1(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___nba_sequent__TOP__1\n"); );
    // Init
    IData/*31:0*/ __Vdly__top__DOT__pc_next;
    __Vdly__top__DOT__pc_next = 0;
    SData/*15:0*/ __Vdlyvdim0__top__DOT__my_ram__DOT__data_memory__v0;
    __Vdlyvdim0__top__DOT__my_ram__DOT__data_memory__v0 = 0;
    IData/*31:0*/ __Vdlyvval__top__DOT__my_ram__DOT__data_memory__v0;
    __Vdlyvval__top__DOT__my_ram__DOT__data_memory__v0 = 0;
    CData/*0:0*/ __Vdlyvset__top__DOT__my_ram__DOT__data_memory__v0;
    __Vdlyvset__top__DOT__my_ram__DOT__data_memory__v0 = 0;
    // Body
    vlSelf->__Vdlyvset__top__DOT__my_reg__DOT__general_register__v0 = 0U;
    __Vdlyvset__top__DOT__my_ram__DOT__data_memory__v0 = 0U;
    __Vdly__top__DOT__pc_next = vlSelf->top__DOT__pc_next;
    if (((IData)(vlSelf->top__DOT__RegWr) & (0U != 
                                             (0x1fU 
                                              & (vlSelf->top__DOT__instr 
                                                 >> 7U))))) {
        vlSelf->__Vdlyvval__top__DOT__my_reg__DOT__general_register__v0 
            = vlSelf->top__DOT__out;
        vlSelf->__Vdlyvset__top__DOT__my_reg__DOT__general_register__v0 = 1U;
        vlSelf->__Vdlyvdim0__top__DOT__my_reg__DOT__general_register__v0 
            = (0x1fU & (vlSelf->top__DOT__instr >> 7U));
    }
    if ((IData)((0x23U == (0x7fU & vlSelf->top__DOT__instr)))) {
        __Vdlyvval__top__DOT__my_ram__DOT__data_memory__v0 
            = ((4U & (IData)(vlSelf->top__DOT__MemOp))
                ? ((2U & (IData)(vlSelf->top__DOT__MemOp))
                    ? vlSelf->top__DOT__op2 : ((1U 
                                                & (IData)(vlSelf->top__DOT__MemOp))
                                                ? (0xffffU 
                                                   & vlSelf->top__DOT__op2)
                                                : (0xffU 
                                                   & vlSelf->top__DOT__op2)))
                : ((2U & (IData)(vlSelf->top__DOT__MemOp))
                    ? vlSelf->top__DOT__op2 : ((1U 
                                                & (IData)(vlSelf->top__DOT__MemOp))
                                                ? (
                                                   ((- (IData)(
                                                               (vlSelf->top__DOT__op2 
                                                                >> 0x1fU))) 
                                                    << 0x10U) 
                                                   | (0xffffU 
                                                      & vlSelf->top__DOT__op2))
                                                : (
                                                   ((- (IData)(
                                                               (vlSelf->top__DOT__op2 
                                                                >> 0x1fU))) 
                                                    << 8U) 
                                                   | (0xffU 
                                                      & vlSelf->top__DOT__op2)))));
        __Vdlyvset__top__DOT__my_ram__DOT__data_memory__v0 = 1U;
        __Vdlyvdim0__top__DOT__my_ram__DOT__data_memory__v0 
            = (0xffffU & vlSelf->top__DOT__ALUout);
    }
    if ((0U == vlSelf->top__DOT__my_pc__DOT__cnt)) {
        __Vdly__top__DOT__pc_next = (vlSelf->top__DOT__my_pc__DOT__PCa 
                                     + vlSelf->top__DOT__my_pc__DOT__PCb);
        if (vlSelf->top__DOT__PCAsrc) {
            if (vlSelf->top__DOT__PCAsrc) {
                vlSelf->top__DOT__my_pc__DOT__PCa = vlSelf->top__DOT__imm;
            }
        } else {
            vlSelf->top__DOT__my_pc__DOT__PCa = 4U;
        }
        if (vlSelf->top__DOT__PCBsrc) {
            if (vlSelf->top__DOT__PCBsrc) {
                vlSelf->top__DOT__my_pc__DOT__PCb = vlSelf->top__DOT__op1;
            }
        } else {
            vlSelf->top__DOT__my_pc__DOT__PCb = vlSelf->top__DOT__pc_next;
        }
    }
    vlSelf->top__DOT__my_pc__DOT__cnt = ((0x2710U <= vlSelf->top__DOT__my_pc__DOT__cnt)
                                          ? 0U : ((IData)(1U) 
                                                  + vlSelf->top__DOT__my_pc__DOT__cnt));
    vlSelf->top__DOT__data_out = ((4U & (IData)(vlSelf->top__DOT__MemOp))
                                   ? ((2U & (IData)(vlSelf->top__DOT__MemOp))
                                       ? vlSelf->top__DOT__my_ram__DOT__data_memory
                                      [(0xffffU & vlSelf->top__DOT__ALUout)]
                                       : ((1U & (IData)(vlSelf->top__DOT__MemOp))
                                           ? (0xffffU 
                                              & vlSelf->top__DOT__my_ram__DOT__data_memory
                                              [(0xffffU 
                                                & vlSelf->top__DOT__ALUout)])
                                           : (0xffU 
                                              & vlSelf->top__DOT__my_ram__DOT__data_memory
                                              [(0xffffU 
                                                & vlSelf->top__DOT__ALUout)])))
                                   : ((2U & (IData)(vlSelf->top__DOT__MemOp))
                                       ? ((1U & (IData)(vlSelf->top__DOT__MemOp))
                                           ? vlSelf->top__DOT__my_ram__DOT__data_memory
                                          [(0xffffU 
                                            & vlSelf->top__DOT__ALUout)]
                                           : vlSelf->top__DOT__my_ram__DOT__data_memory
                                          [(0xffffU 
                                            & vlSelf->top__DOT__ALUout)])
                                       : ((1U & (IData)(vlSelf->top__DOT__MemOp))
                                           ? (((- (IData)(
                                                          (vlSelf->top__DOT__my_ram__DOT__data_memory
                                                           [
                                                           (0xffffU 
                                                            & vlSelf->top__DOT__ALUout)] 
                                                           >> 0x1fU))) 
                                               << 0x10U) 
                                              | (0xffffU 
                                                 & vlSelf->top__DOT__my_ram__DOT__data_memory
                                                 [(0xffffU 
                                                   & vlSelf->top__DOT__ALUout)]))
                                           : (((- (IData)(
                                                          (1U 
                                                           & (vlSelf->top__DOT__my_ram__DOT__data_memory
                                                              [
                                                              (0xffffU 
                                                               & vlSelf->top__DOT__ALUout)] 
                                                              >> 7U)))) 
                                               << 8U) 
                                              | (0xffU 
                                                 & vlSelf->top__DOT__my_ram__DOT__data_memory
                                                 [(0xffffU 
                                                   & vlSelf->top__DOT__ALUout)])))));
    vlSelf->top__DOT__pc_next = __Vdly__top__DOT__pc_next;
    if (__Vdlyvset__top__DOT__my_ram__DOT__data_memory__v0) {
        vlSelf->top__DOT__my_ram__DOT__data_memory[__Vdlyvdim0__top__DOT__my_ram__DOT__data_memory__v0] 
            = __Vdlyvval__top__DOT__my_ram__DOT__data_memory__v0;
    }
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
}

VL_INLINE_OPT void Vtop___024root___nba_sequent__TOP__2(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___nba_sequent__TOP__2\n"); );
    // Body
    if ((0U == (IData)(vlSelf->top__DOT__rs2))) {
        vlSelf->top__DOT__op2 = 0U;
    } else if ((0U == (IData)(vlSelf->top__DOT__rs1))) {
        if ((0U != (IData)(vlSelf->top__DOT__rs2))) {
            vlSelf->top__DOT__op2 = vlSelf->top__DOT__my_reg__DOT__general_register
                [vlSelf->top__DOT__rs2];
        }
    }
    if ((0U == (IData)(vlSelf->top__DOT__rs1))) {
        vlSelf->top__DOT__op1 = 0U;
    }
    if ((0U != (IData)(vlSelf->top__DOT__rs2))) {
        if ((0U != (IData)(vlSelf->top__DOT__rs1))) {
            vlSelf->top__DOT__op1 = vlSelf->top__DOT__my_reg__DOT__general_register
                [vlSelf->top__DOT__rs1];
        }
    }
}

VL_INLINE_OPT void Vtop___024root___nba_sequent__TOP__3(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___nba_sequent__TOP__3\n"); );
    // Body
    vlSelf->top__DOT__rs1 = (0x1fU & (vlSelf->top__DOT__instr 
                                      >> 0xfU));
    vlSelf->top__DOT__rs2 = (0x1fU & (vlSelf->top__DOT__instr 
                                      >> 0x14U));
    if (vlSelf->__Vdlyvset__top__DOT__my_reg__DOT__general_register__v0) {
        vlSelf->top__DOT__my_reg__DOT__general_register[vlSelf->__Vdlyvdim0__top__DOT__my_reg__DOT__general_register__v0] 
            = vlSelf->__Vdlyvval__top__DOT__my_reg__DOT__general_register__v0;
    }
}

extern const VlUnpacked<CData/*0:0*/, 32> Vtop__ConstPool__TABLE_h18daf42f_0;
extern const VlUnpacked<CData/*0:0*/, 32> Vtop__ConstPool__TABLE_h30a380ae_0;

VL_INLINE_OPT void Vtop___024root___nba_comb__TOP__0(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___nba_comb__TOP__0\n"); );
    // Body
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

void Vtop___024root___eval_nba(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___eval_nba\n"); );
    // Body
    if (vlSelf->__VnbaTriggered.at(1U)) {
        Vtop___024root___nba_sequent__TOP__0(vlSelf);
    }
    if (vlSelf->__VnbaTriggered.at(0U)) {
        Vtop___024root___nba_sequent__TOP__1(vlSelf);
        vlSelf->__Vm_traceActivity[1U] = 1U;
    }
    if (vlSelf->__VnbaTriggered.at(2U)) {
        Vtop___024root___nba_sequent__TOP__2(vlSelf);
    }
    if (vlSelf->__VnbaTriggered.at(0U)) {
        Vtop___024root___nba_sequent__TOP__3(vlSelf);
        vlSelf->__Vm_traceActivity[2U] = 1U;
    }
    if ((vlSelf->__VnbaTriggered.at(0U) | vlSelf->__VnbaTriggered.at(2U))) {
        Vtop___024root___nba_comb__TOP__0(vlSelf);
        vlSelf->__Vm_traceActivity[3U] = 1U;
    }
}

void Vtop___024root___eval_triggers__act(Vtop___024root* vlSelf);
#ifdef VL_DEBUG
VL_ATTR_COLD void Vtop___024root___dump_triggers__act(Vtop___024root* vlSelf);
#endif  // VL_DEBUG
#ifdef VL_DEBUG
VL_ATTR_COLD void Vtop___024root___dump_triggers__nba(Vtop___024root* vlSelf);
#endif  // VL_DEBUG

void Vtop___024root___eval(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___eval\n"); );
    // Init
    VlTriggerVec<3> __VpreTriggered;
    IData/*31:0*/ __VnbaIterCount;
    CData/*0:0*/ __VnbaContinue;
    // Body
    __VnbaIterCount = 0U;
    __VnbaContinue = 1U;
    while (__VnbaContinue) {
        __VnbaContinue = 0U;
        vlSelf->__VnbaTriggered.clear();
        vlSelf->__VactIterCount = 0U;
        vlSelf->__VactContinue = 1U;
        while (vlSelf->__VactContinue) {
            vlSelf->__VactContinue = 0U;
            Vtop___024root___eval_triggers__act(vlSelf);
            if (vlSelf->__VactTriggered.any()) {
                vlSelf->__VactContinue = 1U;
                if (VL_UNLIKELY((0x64U < vlSelf->__VactIterCount))) {
#ifdef VL_DEBUG
                    Vtop___024root___dump_triggers__act(vlSelf);
#endif
                    VL_FATAL_MT("vsrc/top.v", 1, "", "Active region did not converge.");
                }
                vlSelf->__VactIterCount = ((IData)(1U) 
                                           + vlSelf->__VactIterCount);
                __VpreTriggered.andNot(vlSelf->__VactTriggered, vlSelf->__VnbaTriggered);
                vlSelf->__VnbaTriggered.set(vlSelf->__VactTriggered);
                Vtop___024root___eval_act(vlSelf);
            }
        }
        if (vlSelf->__VnbaTriggered.any()) {
            __VnbaContinue = 1U;
            if (VL_UNLIKELY((0x64U < __VnbaIterCount))) {
#ifdef VL_DEBUG
                Vtop___024root___dump_triggers__nba(vlSelf);
#endif
                VL_FATAL_MT("vsrc/top.v", 1, "", "NBA region did not converge.");
            }
            __VnbaIterCount = ((IData)(1U) + __VnbaIterCount);
            Vtop___024root___eval_nba(vlSelf);
        }
    }
}

#ifdef VL_DEBUG
void Vtop___024root___eval_debug_assertions(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___eval_debug_assertions\n"); );
    // Body
    if (VL_UNLIKELY((vlSelf->clk & 0xfeU))) {
        Verilated::overWidthError("clk");}
}
#endif  // VL_DEBUG
