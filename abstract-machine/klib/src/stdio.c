#include <am.h>
#include <klib.h>
#include <klib-macros.h>
#include <stdarg.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

int printf(const char *fmt, ...){
  va_list ap;
  va_start(ap, fmt);
  int i = 0;
  while(*fmt != '\0'){
    if(*fmt == '%'){
      fmt++;
      switch(*fmt){
        case 's': {
          char *string = va_arg(ap, char *);
          while(*string != '\0'){
            putch(*string);
            i++;
            string++;
          }
          break;
        }
        case 'd': {
          uint64_t d = va_arg(ap, uint64_t);
          int j = 0;
          char dd[30];
          while(d / 10){
            dd[j] = (char)(d % 10 + '0');
            j++;
            d = d / 10;
          }
          dd[j] = d + '0';
          for(; j >= 0; j--){
            putch(dd[j]);
            i++;
          }
          break;
        }
        case 'x': {
          uint64_t d = va_arg(ap, uint64_t);
          int j = 0;
          char dd[30];
          while(d / 16){
            if(d % 16 < 10){
              dd[j] = (char)(d % 16 + '0');
            }
            else{
              dd[j] = (char)(d % 16 - 10 + 'a');
            }
            j++;
            d = d / 16;
          }
          if(d < 10){
            dd[j] = (char)(d % 16 + '0');
          }
          else{
            dd[j] = (char)(d % 16 - 10 + 'a');
          }
          for(; j >= 0; j--){
            putch(dd[j]);
            i++;
          }
          break;
        }
        default: va_end(ap); return -1;
      }
    }
    else{
      putch(*fmt);
      i++;
    }
    fmt++;
  }
  va_end(ap);
  return i;
}

int sprintf(char *out, const char *fmt, ...) {
  va_list ap;
  va_start(ap, fmt);
  int i = 0;
  while(*fmt != '\0'){
    if(*fmt == '%'){
      fmt++;
      switch(*fmt){
        case 's': {
          char *string = va_arg(ap, char *);
          while(*string != '\0'){
            out[i] = *string;
            i++;
            string++;
          }
          break;
        }
        case 'd': {
          int d = va_arg(ap, int);
          int j = 0;
          char dd[30];
          while(d / 10){
            dd[j] = (char)(d % 10 + '0');
            j++;
            d = d / 10;
          }
          dd[j] = d + '0';
          for(; j >= 0; j--){
            out[i] = dd[j];
            i++;
          }
          break;
        }
        default: va_end(ap); return -1;
      }
    }
    else{
      out[i] = *fmt;
      i++;
    }
    fmt++;
  }
  va_end(ap);
  out[i] = '\0';
  return i;
}

int snprintf(char *out, size_t n, const char *fmt, ...) {
  va_list ap;
  va_start(ap, fmt);
  int i = 0;
  while(*fmt != '\0'){
    if(i == n - 1){
      break;
    }
    if(*fmt == '%'){
      fmt++;
      switch(*fmt){
        case 's': {
          char *string = va_arg(ap, char *);
          while(*string != '\0'){
            out[i] = *string;
            i++;
            string++;
          }
          break;
        }
        case 'd': {
          int d = va_arg(ap, int);
          int j = 0;
          char dd[30];
          while(d / 10){
            dd[j] = (char)(d % 10 + '0');
            j++;
            d = d / 10;
          }
          dd[j] = d + '0';
          for(; j >= 0; j--){
            out[i] = dd[j];
            i++;
          }
          break;
        }
        default: va_end(ap); return -1;
      }
    }
    else{
      out[i] = *fmt;
      i++;
    }
    fmt++;
  }
  va_end(ap);
  out[i] = '\0';
  if(i == n - 1){
    return n;
  }
  else{
    return i;
  }
}

int vsprintf(char *out, const char *fmt, va_list ap) {
  panic("Not implemented");
}

int vsnprintf(char *out, size_t n, const char *fmt, va_list ap) {
  panic("Not implemented");
}


#endif    
