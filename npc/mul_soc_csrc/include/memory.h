#ifndef __PMEM_H__
#define __PMEM_H__

#include <stdint.h>
#include "common.h"
extern "C" void pmem_write(paddr_t addr,word_t data,int len) ;
extern "C" word_t pmem_read(paddr_t addr,int len) ;

#endif
