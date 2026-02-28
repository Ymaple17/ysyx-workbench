#include <am.h>
#include <klib.h>
#include <klib-macros.h>
#include <stdarg.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

int sprintf_number(char *out, int num, int base, int width, char fill);
int sprintf_string(char *out, const char *str);
int vsprintf(char *out, const char *fmt, va_list ap);

int sprintf_number(char *out, int num, int base, int width, char fill) {
  const char digits[] = "0123456789abcdef";
  char buf[32];
  int i = 0;
  int is_negative = 0;
  unsigned int unum = (unsigned int)num;

  if (base == 10 && num < 0) {
    is_negative = 1;
    unum = (unsigned int)(-num);
  }

  if (unum == 0) {
    buf[i++] = '0';
  } else {
    while (unum != 0) {
      buf[i++] = digits[unum % base];
      unum /= base;
    }
  }

  int num_digits = i;
  int pad = (width > num_digits + is_negative) ? (width - (num_digits + is_negative)) : 0;

  int len = 0;
  if (is_negative) {
    out[len++] = '-';
  }

  for (int p = 0; p < pad; p++) {
    out[len++] = fill;
  }

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
      
      char fill = ' ';
      int width = 0;
      if (*ptr == '0') {
        fill = '0';
        ptr++;
      }
      while (*ptr >= '0' && *ptr <= '9') {
        width = width * 10 + (*ptr - '0');
        ptr++;
      }

      switch (*ptr) {
        case 'd': {
          int value = va_arg(ap, int);
          len += sprintf_number(out + len, value, 10, width, fill);
          break;
        }
        case 's': {
          char *str = va_arg(ap, char*);
          len += sprintf_string(out + len, str);
          break;
        }
        case 'x': {
          int value = va_arg(ap, int);
          len += sprintf_number(out + len, value, 16, width, fill);
          break;
        }
        case 'c': {
          int value = va_arg(ap, int);
          out[len++] = (char)value;
          break;
        }
        case '%': {
          out[len++] = '%';
          break;
        }
        case 'p': {
	  unsigned int value = va_arg(ap, unsigned int);
	  out[len++] = '0';
	  out[len++] = 'x';
	  len += sprintf_number(out + len, value, 16, width, fill);
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
  va_list ap;
  va_start(ap, fmt);
  int result = vsnprintf(out, n, fmt, ap);
  va_end(ap);
  return result;
}

int vsnprintf(char *out, size_t n, const char *fmt, va_list ap) {
  int len = 0;
  const char *ptr = fmt;
  if (n == 0) return 0;

  while (*ptr && len < (int)(n - 1)) {
    if (*ptr == '%' && *(ptr + 1) != '\0') {
      ptr++;
      
      char fill = ' ';
      int width = 0;
      if (*ptr == '0') {
        fill = '0';
        ptr++;
      }
      while (*ptr >= '0' && *ptr <= '9') {
        width = width * 10 + (*ptr - '0');
        ptr++;
      }

      switch (*ptr) {
        case 'd': {
          int value = va_arg(ap, int);
          char num_buf[32];
          int num_len = sprintf_number(num_buf, value, 10, width, fill);
          for (int i = 0; i < num_len && len < (int)(n - 1); i++) {
            out[len++] = num_buf[i];
          }
          break;
        }
        case 's': {
          char *str = va_arg(ap, char*);
          int str_len = 0;
          while (str[str_len] && len + str_len < (int)(n - 1)) {
            out[len + str_len] = str[str_len];
            str_len++;
          }
          len += str_len;
          break;
        }
        case 'x': {
          int value = va_arg(ap, int);
          char num_buf[32];
          int num_len = sprintf_number(num_buf, value, 16, width, fill);
          for (int i = 0; i < num_len && len < (int)(n - 1); i++) {
            out[len++] = num_buf[i];
          }
          break;
        }
        case 'c': {
          int value = va_arg(ap, int);
          if (len < (int)(n - 1)) {
            out[len++] = (char)value;
          }
          break;
        }
        case '%': {
          out[len++] = '%';
          break;
        }
        default: {
          if (len < (int)(n - 2)) {
            out[len++] = '%';
            out[len++] = *ptr;
          } else {
            len = (int)(n - 1);
          }
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

#endif
    
