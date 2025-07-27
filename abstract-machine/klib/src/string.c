#include <klib.h>
#include <klib-macros.h>
#include <stdint.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

size_t strlen(const char *s) {
  size_t i = 0;
  while(s[i] != '\0'){
    i++;
  }
  return i;
}

char *strcpy(char *dst, const char *src) {
  int i = 0;
  while(src[i] != '\0'){
    dst[i] = src[i];
    i++;
  }
  dst[i] = '\0';
  return dst;
}

char *strncpy(char *dst, const char *src, size_t n) {
  char *m = dst;
  while(n){
    if((*m = *src) != '\0'){
      m++;
      src++;
      n--;
    }
    else{
      n--;
      break;
    } 
  }
  while(n){
    *m = '\0';
    m++;
    n--;
  }
  return dst;
}

char *strcat(char *dst, const char *src) {
  char *m = dst;
  while(*m != '\0'){
    m++;
  }
  while(*src != '\0'){
    *m = *src;
    m++;
    src++;
  }
  *m = '\0';
  return dst;
}

int strcmp(const char *s1, const char *s2) {
  int a;
  while(*s1 == *s2 && *s1 != '\0'){
    s1++;
    s2++;
  }
  a = *s1 - *s2;
  return a;
}

int strncmp(const char *s1, const char *s2, size_t n) {
  int a;
  while(*s1 == *s2 && *s1 != '\0' && n != 0){
    s1++;
    s2++;
    n--;
  }
  a = n == 0 ? 0 : (*s1 - *s2);
  return a;
}

void *memset(void *s, int c, size_t n) {
  char *m = s;
  while(n){
    *m = (char)c;
    n--;
    m++;
  }
  return s;
}

void *memmove(void *dst, const void *src, size_t n) {
  char *m = (char *)dst;
  if(dst <= src){
    while(n){
      *m = *(char *)src;
      m++;
      src++;
      n--;
    }
  }
  else{
    while(n){
      *(m + n - 1) = *(char *)(src + n - 1);
      n--;
    }
  }
  return dst;
}


void *memcpy(void *out, const void *in, size_t n) {
  char *m = (char *)out;
  while(n){
    *m = *(char *)in;
    m++;
    in++;
    n--;
  }
  return out;
}

int memcmp(const void *s1, const void *s2, size_t n) {
  int a;
  while(*(char *)s1 == *(char *)s2 && n != 0){
    s1++;
    s2++;
    n--;
  }
  a = n == 0 ? 0 : (int)*(char *)s1 - (int)*(char *)s2;
  return a;
}


#endif
