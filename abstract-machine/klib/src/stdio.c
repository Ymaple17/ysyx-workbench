#include <am.h>
#include <klib.h>
#include <klib-macros.h>
#include <stdarg.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

int sprintf_number(char *out, int num, int base) {
  const char digits[] = "0123456789abcdef";
  char buf[255];
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
  va_list ap;
  va_start(ap, fmt);
  char buf[255];
  int len = vsprintf(buf, fmt, ap);
  va_end(ap);
  for (int i = 0; i < len; i++) {
    putch(buf[i]);
  }
  return len;
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

// 实现 snprintf 函数
int snprintf(char *out, size_t n, const char *fmt, ...) {
  va_list ap;
  va_start(ap, fmt);
  int result = vsnprintf(out, n, fmt, ap);
  va_end(ap);
  return result;
}

// 实现 vsnprintf 函数
int vsnprintf(char *out, size_t n, const char *fmt, va_list ap) {
  int len = 0;
  const char *ptr = fmt;
  while (*ptr && len < n - 1) {
     if (*ptr == '%' && *(ptr + 1) != '\0') {
      ptr++;
      switch (*ptr) {
        case 'd': {
          int value = va_arg(ap, int);
          int num_len = sprintf_number(out + len, value, 10);
          if (len + num_len < n - 1) {
            len += num_len;
          } else {
            len = n - 1;
          }
          break;
        }
        case 's': {
          char *str = va_arg(ap, char*);
          int str_len = 0;
          while (str[str_len] && len + str_len < n - 1) {
            out[len + str_len] = str[str_len];
            str_len++;
          }
          len += str_len;
          break;
        }
        case 'x': {
          int value = va_arg(ap, int);
          int num_len = sprintf_number(out + len, value, 16);
          if (len + num_len < n - 1) {
            len += num_len;
          } else {
            len = n - 1;
          }
          break;
        }
        case '%': {
          out[len++] = '%';
          break;
        }
        default: {
          if (len < n - 2) {
            out[len++] = '%';
            out[len++] = *ptr;
          } else {
            len = n - 1;
          }
          break;
        }
      }
    } else {
      out[len++] = *ptr;
    }
    ptr++;
  }
  if (n > 0) {
    out[len] = '\0';
  }
  return len;
}

#endif    
