#include <am.h>
#include <klib.h>
#include <klib-macros.h>
#include <stdarg.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

int sprintf_number(char *out, int num, int base) {
  const char digits[] = "0123456789abcdef";
  char buf[32];
  int i = 0;
  
  if (num == 0) {
    buf[i++] = '0';
  } else {
    while (num != 0) {
      buf[i++] = digits[num % base];
      num /= base;
    }
  }
  int len = 0;
  while (--i >= 0) {
    out[len++] = buf[i];
  }
  return len;
}

int sprintf_string(char *out, const char *str) {
  int len = 0;
  while (*str) {
    out[len++] = *str++;
  }
  return len;
}

int printf(const char *fmt, ...) {
  panic("Not implemented");
}

int vsprintf(char *out, const char *fmt, va_list ap) {
  int len = 0;
  const char *ptr = fmt;
  while (*ptr) {
     if (*ptr == '%' && *(ptr + 1) != '\0') {
      ptr++;
      switch (*ptr) {
        case 'd': {
          int value = va_arg(ap, int);
          len += sprintf_number(out + len, value, 10);
          break;
        }
        case 's': {
          char *str = va_arg(ap, char*);
          len += sprintf_string(out + len, str);
          break;
        }
        case 'x': {
          int value = va_arg(ap, int);
          len += sprintf_number(out + len, value, 16);
          break;
        }
        case '%': {
          out[len++] = '%';
          break;
        }
        default: {
          out[len++] = '%';
          out[len++] = *ptr;
          break;
        }
      }
    } else {
      out[len++] = *ptr;
    }
    ptr++;
  }
  out[len] = '\0';
  return len;
}

int sprintf(char *out, const char *fmt, ...) {
  va_list ap;
  va_start(ap, fmt);
  int result = vsprintf(out, fmt, ap);
  va_end(ap);
  return result;
}

int snprintf(char *out, size_t n, const char *fmt, ...) {
  panic("Not implemented");
}

int vsnprintf(char *out, size_t n, const char *fmt, va_list ap) {
  panic("Not implemented");
}

#endif
