// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Model implementation (design independent parts)

#include "Vdouble1.h"
#include "Vdouble1__Syms.h"
#include "verilated_fst_c.h"

//============================================================
// Constructors

Vdouble1::Vdouble1(VerilatedContext* _vcontextp__, const char* _vcname__)
    : VerilatedModel{*_vcontextp__}
    , vlSymsp{new Vdouble1__Syms(contextp(), _vcname__, this)}
    , a{vlSymsp->TOP.a}
    , b{vlSymsp->TOP.b}
    , f{vlSymsp->TOP.f}
    , rootp{&(vlSymsp->TOP)}
{
    // Register model with the context
    contextp()->addModel(this);
}

Vdouble1::Vdouble1(const char* _vcname__)
    : Vdouble1(Verilated::threadContextp(), _vcname__)
{
}

//============================================================
// Destructor

Vdouble1::~Vdouble1() {
    delete vlSymsp;
}

//============================================================
// Evaluation function

#ifdef VL_DEBUG
void Vdouble1___024root___eval_debug_assertions(Vdouble1___024root* vlSelf);
#endif  // VL_DEBUG
void Vdouble1___024root___eval_static(Vdouble1___024root* vlSelf);
void Vdouble1___024root___eval_initial(Vdouble1___024root* vlSelf);
void Vdouble1___024root___eval_settle(Vdouble1___024root* vlSelf);
void Vdouble1___024root___eval(Vdouble1___024root* vlSelf);

void Vdouble1::eval_step() {
    VL_DEBUG_IF(VL_DBG_MSGF("+++++TOP Evaluate Vdouble1::eval_step\n"); );
#ifdef VL_DEBUG
    // Debug assertions
    Vdouble1___024root___eval_debug_assertions(&(vlSymsp->TOP));
#endif  // VL_DEBUG
    vlSymsp->__Vm_activity = true;
    vlSymsp->__Vm_deleter.deleteAll();
    if (VL_UNLIKELY(!vlSymsp->__Vm_didInit)) {
        vlSymsp->__Vm_didInit = true;
        VL_DEBUG_IF(VL_DBG_MSGF("+ Initial\n"););
        Vdouble1___024root___eval_static(&(vlSymsp->TOP));
        Vdouble1___024root___eval_initial(&(vlSymsp->TOP));
        Vdouble1___024root___eval_settle(&(vlSymsp->TOP));
    }
    // MTask 0 start
    VL_DEBUG_IF(VL_DBG_MSGF("MTask0 starting\n"););
    Verilated::mtaskId(0);
    VL_DEBUG_IF(VL_DBG_MSGF("+ Eval\n"););
    Vdouble1___024root___eval(&(vlSymsp->TOP));
    // Evaluate cleanup
    Verilated::endOfThreadMTask(vlSymsp->__Vm_evalMsgQp);
    Verilated::endOfEval(vlSymsp->__Vm_evalMsgQp);
}

//============================================================
// Events and timing
bool Vdouble1::eventsPending() { return false; }

uint64_t Vdouble1::nextTimeSlot() {
    VL_FATAL_MT(__FILE__, __LINE__, "", "%Error: No delays in the design");
    return 0;
}

//============================================================
// Utilities

const char* Vdouble1::name() const {
    return vlSymsp->name();
}

//============================================================
// Invoke final blocks

void Vdouble1___024root___eval_final(Vdouble1___024root* vlSelf);

VL_ATTR_COLD void Vdouble1::final() {
    Vdouble1___024root___eval_final(&(vlSymsp->TOP));
}

//============================================================
// Implementations of abstract methods from VerilatedModel

const char* Vdouble1::hierName() const { return vlSymsp->name(); }
const char* Vdouble1::modelName() const { return "Vdouble1"; }
unsigned Vdouble1::threads() const { return 1; }
std::unique_ptr<VerilatedTraceConfig> Vdouble1::traceConfig() const {
    return std::unique_ptr<VerilatedTraceConfig>{new VerilatedTraceConfig{false, false, false}};
};

//============================================================
// Trace configuration

void Vdouble1___024root__trace_init_top(Vdouble1___024root* vlSelf, VerilatedFst* tracep);

VL_ATTR_COLD static void trace_init(void* voidSelf, VerilatedFst* tracep, uint32_t code) {
    // Callback from tracep->open()
    Vdouble1___024root* const __restrict vlSelf VL_ATTR_UNUSED = static_cast<Vdouble1___024root*>(voidSelf);
    Vdouble1__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    if (!vlSymsp->_vm_contextp__->calcUnusedSigs()) {
        VL_FATAL_MT(__FILE__, __LINE__, __FILE__,
            "Turning on wave traces requires Verilated::traceEverOn(true) call before time 0.");
    }
    vlSymsp->__Vm_baseCode = code;
    tracep->scopeEscape(' ');
    tracep->pushNamePrefix(std::string{vlSymsp->name()} + ' ');
    Vdouble1___024root__trace_init_top(vlSelf, tracep);
    tracep->popNamePrefix();
    tracep->scopeEscape('.');
}

VL_ATTR_COLD void Vdouble1___024root__trace_register(Vdouble1___024root* vlSelf, VerilatedFst* tracep);

VL_ATTR_COLD void Vdouble1::trace(VerilatedFstC* tfp, int levels, int options) {
    if (tfp->isOpen()) {
        vl_fatal(__FILE__, __LINE__, __FILE__,"'Vdouble1::trace()' shall not be called after 'VerilatedFstC::open()'.");
    }
    if (false && levels && options) {}  // Prevent unused
    tfp->spTrace()->addModel(this);
    tfp->spTrace()->addInitCb(&trace_init, &(vlSymsp->TOP));
    Vdouble1___024root__trace_register(&(vlSymsp->TOP), tfp->spTrace());
}
