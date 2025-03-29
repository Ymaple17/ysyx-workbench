/***************************************************************************************
* Copyright (c) 2014-2024 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/
#ifndef __MEM_PADDR_H__
#define __MEM_PADDR_H__

#include <stdint.h>
#include <common.h> // 假设 common.h 包含了 paddr_t 和 word_t 的定义

// 物理内存范围
#define PMEM_LEFT  ((paddr_t)CONFIG_MBASE) // 物理内存的起始地址（Memory Base）
#define PMEM_RIGHT ((paddr_t)CONFIG_MBASE + CONFIG_MSIZE - 1) // 物理内存的结束地址

/* convert the guest physical address in the guest program to host virtual address in NEMU */
uint8_t* guest_to_host(paddr_t paddr);
/* convert the host virtual address in NEMU to guest physical address in the guest program */
paddr_t host_to_guest(uint8_t *haddr);

bool in_pmem(paddr_t addr);
word_t paddr_read(paddr_t addr, int len);
void paddr_write(paddr_t addr, int len, word_t data);
word_t paddr_read_c(paddr_t addr, int len);  // 确保有此声明
void paddr_write_c(paddr_t addr, int len, word_t data);

#endif // __MEM_PADDR_H__
