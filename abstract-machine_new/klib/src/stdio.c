#include <am.h>
#include <klib.h>
#include <klib-macros.h>
#include <stdarg.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

int printf(const char *fmt, ...) {
  panic("Not implemented");
}

int vsprintf(char *out, const char *fmt, va_list ap) {
  panic("Not implemented");
}

static int int_to_str(int num, char *buf) {
    int len = 0;
    int is_neg = 0;
    unsigned int n;

    if (num == 0) {
        buf[len++] = '0';
        buf[len] = '\0';
        return len;
    }

    if (num < 0) {
        is_neg = 1;
        n = (unsigned int)(-num);
    } else {
        n = (unsigned int)num;
    }

    while (n > 0) {
        buf[len++] = '0' + (n % 10);
        n /= 10;
    }

    if (is_neg) {
        buf[len++] = '-';
    }

    for (int i = 0; i < len / 2; i++) {
        char tmp = buf[i];
        buf[i] = buf[len - 1 - i];
        buf[len - 1 - i] = tmp;
    }

    buf[len] = '\0';
    return len;
}

int sprintf(char *out, const char *fmt, ...) {
    va_list args;
    va_start(args, fmt);

    int j = 0; 
    char int_buf[20]; 
    char *p;

    for (size_t i = 0; fmt[i] != '\0'; i++) {
        if (fmt[i] == '%') {
            i++; 
            switch (fmt[i]) {
                case 'd': {
                    int len = int_to_str(va_arg(args, int), int_buf);
                    for (int k = 0; k < len; k++) {
                        out[j++] = int_buf[k];
                    }
                    break;
                }
                case 's': {
                    p = va_arg(args, char*);
                    if (p != NULL) {
                        for (int k = 0; p[k] != '\0'; k++) {
                            out[j++] = p[k];
                        }
                    }
                    break;
                }
                default: {
                    out[j++] = '%';   
                    out[j++] = fmt[i];
                    break;
                }
            }
        } else {
            out[j++] = fmt[i];
        }
    }

    out[j] = '\0';
    va_end(args);
    return j;
}

int snprintf(char *out, size_t n, const char *fmt, ...) {
  panic("Not implemented");
}

int vsnprintf(char *out, size_t n, const char *fmt, va_list ap) {
  panic("Not implemented");
}

#endif
