#include <klib.h>
#include <klib-macros.h>
#include <stdint.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

size_t strlen(const char *s) {
  size_t num = 0;
  for(size_t i = 0; s[i] != '\0'; i++) {
    num++;
  }
  return num;
}

char *strcpy(char *dst, const char *src) {
  size_t i;
  for(i = 0; src[i] != '\0'; i++) {
    dst[i] = src[i];
  }
  dst[i] = '\0';
  return dst;
}

char *strncpy(char *dst, const char *src, size_t n) {
  for(size_t i = 0; i < n; i++) {
    if(src[i] == '\0') {
      for(size_t j = i; j < n; j++) {
        dst[j] = '\0';
      }
      break;
    }
    dst[i] = src[i];
  }
  return dst;
}

char *strcat(char *dst, const char *src) {
  size_t i = 0;
  while(dst[i] != '\0') {
    i++;
  }
  size_t j;
  for(j = 0; src[j] != '\0'; j++) {
    dst[i+j] = src[j];
  }
  dst[i+j] = '\0';
  return dst;
}

int strcmp(const char *s1, const char *s2) {
  for(size_t i = 0; s1[i] != '\0' || s2[i] != '\0'; i++) {
    if(s1[i] != s2[i]) {
      return (unsigned char)s1[i] - (unsigned char)s2[i];
    }
  }
  return 0;
}

int strncmp(const char *s1, const char *s2, size_t n) {
  for(size_t i = 0; i < n ; i++) {
    if(s1[i] != s2[i]) {
      return (unsigned char)s1[i] - (unsigned char)s2[i];
    }
  }
  return 0;
}

void *memset(void *s, int c, size_t n) {
  unsigned char *p = (unsigned char *)s;
  unsigned char value = (unsigned char)c;
  for (size_t i = 0; i < n; i++) {
    p[i] = value;
  }
  return s;
}

void *memmove(void *dst, const void *src, size_t n) {

    for(size_t i = n; i > 0; i--) {
        ((char *)dst)[i-1] = ((char *)src)[i-1];
    }
    return dst;
}

void *memcpy(void *out, const void *in, size_t n) {
  for(size_t i = 0; i < n; i++) {
    ((char *)out)[i] = ((char *)in)[i];
  }
  return out;
}

int memcmp(const void *s1, const void *s2, size_t n) {
  for(size_t i = 0; i < n; i++) {
    if(((char *)s1)[i] != ((char *)s2)[i]) {
      return (unsigned char)((char *)s1)[i] - (unsigned char)((char *)s2)[i];
    }
  }
  return 0;
}

#endif
