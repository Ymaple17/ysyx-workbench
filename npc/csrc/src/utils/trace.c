#include <common.h>
#include <elf.h>
#include <device/map.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>

#define MAX_SIZE 16    // 环形缓冲区最大空间
#define MAX_IRINGBUF 16
extern "C" void disassemble(char *str, int size, uint64_t pc, uint8_t *code, int nbyte);
// 指令缓冲区结构
typedef struct {
    word_t pc;      // 程序计数器
    uint32_t inst;  // 指令
} Instbuf;
int func_num = 0;           // 符号表中函数数量
Instbuf iringbuf[MAX_IRINGBUF]; // 环形缓冲区
int cur_inst = 0;           // 当前指令位置
bool full = false;          // 缓冲区是否满

// 跟踪指令
extern "C" void trace_inst(word_t pc, uint32_t inst) {
  iringbuf[cur_inst].pc = pc;
  iringbuf[cur_inst].inst = inst;
  cur_inst = (cur_inst + 1) % MAX_IRINGBUF;
  full = full || cur_inst == 0;
}

// 显示最近执行的指令
void display_inst() {
  if (!full && !cur_inst) return;

  int end = cur_inst;
  int i = full?cur_inst:0;

  void disassemble(char *str, int size, uint64_t pc, uint8_t *code, int nbyte);
  char buf[128]; // 128 should be enough!
  char *p;
  //Statement("Most recently executed instructions");
  //printf("[DEBUG] inst raw = 0x%08x at pc = 0x%08x\n", iringbuf[i].inst, iringbuf[i].pc);
  do {
    p = buf;
    p += sprintf(buf, "%s" FMT_WORD ": %08x ", (i+1)%MAX_IRINGBUF==end?" --> ":"     ", iringbuf[i].pc, iringbuf[i].inst);
    //printf("[disassemble] call with inst = 0x%08x at pc = 0x%08x\n", iringbuf[i].inst, iringbuf[i].pc);
    disassemble(p, buf+sizeof(buf)-p, iringbuf[i].pc, (uint8_t *)&iringbuf[i].inst, 4);

    if ((i+1)%MAX_IRINGBUF==end) printf(ANSI_FG_RED);
    puts(buf);
  } while ((i = (i+1)%MAX_IRINGBUF) != end);
  puts(ANSI_NONE);
}

// 显示内存读操作
void display_mread(paddr_t addr, int len) {
    printf("memory read at " FMT_PADDR " len = %d\n", addr, len);
}

// 显示内存写操作
void display_mwrite(paddr_t addr, int len, word_t data) {
    printf("memory write at " FMT_PADDR " len = %d, data = " FMT_PADDR "\n", addr, len, data);
}

// 符号表结构
typedef struct {
    char name[32];      // 函数名
    paddr_t addr;       // 函数起始地址
    Elf32_Xword size;   // 函数大小
} Symbol;

Symbol *symbol = NULL;    // 符号表数组

// 解析 ELF 文件以提取符号表
void parse_elf(const char *elf_file) {
    if (elf_file == NULL) return;

    FILE *fp = fopen(elf_file, "rb");
    if (fp == NULL) {
        perror("Failed to open the ELF file");
        exit(1);
    }

    Elf32_Ehdr ehdr;
    if (fread(&ehdr, sizeof(Elf32_Ehdr), 1, fp) <= 0) {
        perror("Failed to read the ELF header");
        fclose(fp);
        exit(1);
    }

    // 检查 ELF 魔数
    if (ehdr.e_ident[0] != 0x7f || ehdr.e_ident[1] != 'E' || ehdr.e_ident[2] != 'L' || ehdr.e_ident[3] != 'F') {
        printf("This file is not a ELF file\n");
        fclose(fp);
        exit(1);
    }

    // 定位到节头表
    fseek(fp, ehdr.e_shoff, SEEK_SET);

    Elf32_Shdr shdr;
    char *string_table = NULL;

    // 查找字符串表
    for (int i = 0; i < ehdr.e_shnum; i++) {
        if (fread(&shdr, sizeof(Elf32_Shdr), 1, fp) <= 0) {
            perror("Failed to read section header");
            fclose(fp);
            exit(1);
        }

        if (shdr.sh_type == SHT_STRTAB) {
            string_table = (char*)malloc(shdr.sh_size);    // 显式转换为 char*
            if (string_table == NULL) {
                perror("Failed to allocate memory for string table");
                fclose(fp);
                exit(1);
            }
            fseek(fp, shdr.sh_offset, SEEK_SET);
            if (fread(string_table, shdr.sh_size, 1, fp) <= 0) {
                perror("Failed to read string table");
                free(string_table);
                fclose(fp);
                exit(1);
            }
        }
    }

    // 查找符号表
    fseek(fp, ehdr.e_shoff, SEEK_SET);

    for (int i = 0; i < ehdr.e_shnum; i++) {
        if (fread(&shdr, sizeof(Elf32_Shdr), 1, fp) <= 0) {
            perror("Failed to read section header");
            free(string_table);
            fclose(fp);
            exit(1);
        }

        if (shdr.sh_type == SHT_SYMTAB) {
            fseek(fp, shdr.sh_offset, SEEK_SET);

            Elf32_Sym sym;
            size_t sym_count = shdr.sh_size / shdr.sh_entsize;
            symbol = (Symbol*)malloc(sizeof(Symbol) * sym_count);    // 显式转换为 Symbol*
            if (symbol == NULL) {
                perror("Failed to allocate memory for symbols");
                free(string_table);
                fclose(fp);
                exit(1);
            }

            for (size_t j = 0; j < sym_count; j++) {
                if (fread(&sym, sizeof(Elf32_Sym), 1, fp) <= 0) {
                    perror("Failed to read symbol table");
                    free(string_table);
                    free(symbol);
                    fclose(fp);
                    exit(1);
                }

                if (ELF32_ST_TYPE(sym.st_info) == STT_FUNC) {
                    const char *name = string_table + sym.st_name;
                    strncpy(symbol[func_num].name, name, sizeof(symbol[func_num].name) - 1);
                    symbol[func_num].name[sizeof(symbol[func_num].name) - 1] = '\0'; // 确保字符串以 null 结尾
                    symbol[func_num].addr = sym.st_value;
                    symbol[func_num].size = sym.st_size;
                    func_num++;
                }
            }
        }
    }

    fclose(fp);
    free(string_table);    // 释放字符串表
}

// 跟踪函数调用
int rec_depth = 1;

void display_call_func(word_t pc, word_t func_addr) {
    int i = 0;
    for (; i < func_num; i++) {
        if (func_addr >= symbol[i].addr && func_addr < (symbol[i].addr + symbol[i].size))
            break;
    }

    printf("0x%08x:", pc);
    rec_depth++;

    printf("call [%s@0x%08x]\n", symbol[i].name, func_addr);
}

// 跟踪函数返回
void display_ret_func(word_t pc) {
    int i = 0;
    for (; i < func_num; i++) {
        if (pc >= symbol[i].addr && pc < (symbol[i].addr + symbol[i].size))
            break;
    }

    printf("0x%08x", pc);
    rec_depth--;

    for (int k = 0; k < rec_depth; k++)
        printf(" ");
    printf("ret [%s]\n", symbol[i].name);
}

// 跟踪设备读操作
void display_dread(paddr_t addr, int len, IOMap *map) {
    printf("device %10s read at addr = " FMT_PADDR " ,%d\n", map->name, addr, len);
}

// 跟踪设备写操作
void display_dwrite(paddr_t addr, int len, word_t data, IOMap *map) {
    printf("device %10s write at addr = " FMT_PADDR " ,%d, data = " FMT_WORD "\n", map->name, addr, len, data);
}
