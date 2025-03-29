#pragma once

#define concat_temp(x, y) x ## y
#define concat(x, y) concat_temp(x, y)

#define CHOOSE2nd(a, b, ...) b
#define MUX_WITH_COMMA(contain_comma, a, b) CHOOSE2nd(contain_comma a, b)
#define MUX_MACRO_PROPERTY(p, macro, a, b) MUX_WITH_COMMA(concat(p, macro), a, b)

#define __P_DEF_1  X,
#define __P_DEF_1  X,
#define __P_ONE_1  X,
#define __P_ZERO_0 X,

#define MUXDEF(macro, X, Y)  MUX_MACRO_PROPERTY(__P_DEF_, macro, X, Y)
#define MUXNDEF(macro, X, Y)  MUX_MACRO_PROPERTY(__P_DEF_, macro, Y, X)
#define IFDEF(macro, ...) MUXDEF(macro, __VA_ARGS__,)
#define IFNDEF(macro, ...) MUXNDEF(macro, __VA_ARGS__,)

// length of array
#define ARRLEN(arr) (int)(sizeof(arr) / sizeof(arr[0]))

// ----------- supress warning -----------
#ifdef __GNUC__
#  define UNUSED(x) UNUSED_ ## x __attribute__((__unused__))
#else
#  define UNUSED(x) UNUSED_ ## x
#endif

// ----------- branch test optimization -----------
#if !defined(likely)
#define likely(cond)   __builtin_expect(cond, 1)
#define unlikely(cond) __builtin_expect(cond, 0)
#endif

// ----------- bit operations -----------
#define SEXT(x, len) ({ struct { long long n : len; } __x = { .n = x }; (unsigned long long)__x.n; })
