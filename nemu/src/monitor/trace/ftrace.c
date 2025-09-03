#include <cpu/cpu.h>
#include <cpu/ifetch.h>
#include <cpu/decode.h>
#include <stdio.h>
#include <string.h>
#include <stdbool.h>
#include <assert.h>
#include <common.h>
#include <elf.h>


#ifdef CONFIG_FTRACE

#define CALL_STACK_SIZE 1024
static const char *call_stack[CALL_STACK_SIZE];
static int call_stack_top = 0;
#define MAX_FUNC 1000  
typedef struct {
  uint32_t start;  
  uint32_t size;   
  char *name;      
} FuncSymbol;
static FuncSymbol func_table[MAX_FUNC];
static int nr_func = 0;  

const char* find_func(uint32_t addr);
void parse_elf(const char *elf_file);

// 根据地址查找函数名
const char* find_func(uint32_t addr) {
  for(int i = 0; i < nr_func; i++) {
    uint32_t start = func_table[i].start;
    uint32_t size = func_table[i].size;
    
    if (size > 0 && addr >= start && addr < start + size) {
      return func_table[i].name;
    }
    else if (size == 0 && addr == start) {
      return func_table[i].name;
    }
  }
  return "?";
}

// 解析ELF文件
void parse_elf(const char *elf_file) {
  FILE *fp = fopen(elf_file, "rb");
  if (!fp) {
    fprintf(stderr, "ftrace: failed to open ELF file: %s (check path)\n", elf_file);
    return;
  }

  // 读取ELF头部
  Elf32_Ehdr ehdr;
  if (fread(&ehdr, sizeof(Elf32_Ehdr), 1, fp) != 1) {
    fprintf(stderr, "ftrace: read ELF header failed\n");
    fclose(fp);
    return;
  }
// 检查ELF文件类型
  if (memcmp(ehdr.e_ident, ELFMAG, SELFMAG) != 0) {//e_ident魔数
    fprintf(stderr, "ftrace: %s is not a valid ELF file\n", elf_file);
    fclose(fp);
    return;
  }

  // 读取节头表
  Elf32_Shdr *sh_table = (Elf32_Shdr *)malloc(ehdr.e_shnum * sizeof(Elf32_Shdr));//e_shnum总数量
  fseek(fp, ehdr.e_shoff, SEEK_SET);//定位
  if (fread(sh_table, sizeof(Elf32_Shdr), ehdr.e_shnum, fp) != ehdr.e_shnum) {
    fprintf(stderr, "ftrace: read section header failed\n");
    free(sh_table);
    fclose(fp);
    return;
  }

  // 查找符号表
  int symtab_indices[2] = {-1, -1};  // 0:.symtab, 1:.dynsym
  for(int i = 0; i < ehdr.e_shnum; i++) {
    if(sh_table[i].sh_type == SHT_SYMTAB) symtab_indices[0] = i;
    if(sh_table[i].sh_type == SHT_DYNSYM) symtab_indices[1] = i;
  }

  // 遍历符号表
  for (int st = 0; st < 2; st++) {
    int symtab_idx = symtab_indices[st];
    if (symtab_idx == -1) continue;

    Elf32_Shdr symtab_hdr = sh_table[symtab_idx];//属性
    int strtab_idx = symtab_hdr.sh_link;//index
    if (strtab_idx < 0 || strtab_idx >= ehdr.e_shnum || sh_table[strtab_idx].sh_type != SHT_STRTAB) {
      fprintf(stderr, "ftrace: invalid strtab for symtab (index %d)\n", symtab_idx);
      continue;
    }
    Elf32_Shdr strtab_hdr = sh_table[strtab_idx];

    // 读取符号表和字符串表
    Elf32_Sym *symtab = (Elf32_Sym *)malloc(symtab_hdr.sh_size);
    fseek(fp, symtab_hdr.sh_offset, SEEK_SET);
    if (fread(symtab, symtab_hdr.sh_size, 1, fp) != 1) {
      fprintf(stderr, "ftrace: read symtab (index %d) failed\n", symtab_idx);
      free(symtab);
      continue;
    }

    char *strtab = (char *)malloc(strtab_hdr.sh_size);
    fseek(fp, strtab_hdr.sh_offset, SEEK_SET);
    if (fread(strtab, strtab_hdr.sh_size, 1, fp) != 1) {
      fprintf(stderr, "ftrace: read strtab (index %d) failed\n", strtab_idx);
      free(symtab);
      free(strtab);
      continue;
    }

    // 提取函数符号
    int symbol_count = symtab_hdr.sh_size / sizeof(Elf32_Sym);
    for(int i = 0; i < symbol_count; i++) {
  if (ELF32_ST_TYPE(symtab[i].st_info) == STT_FUNC) {
    char *func_name = &strtab[symtab[i].st_name];
    uint32_t func_addr = symtab[i].st_value;
    uint32_t func_size = symtab[i].st_size;
    
    printf("[ftrace]: loaded func: %-20s [0x%08x, 0x%08x)%s\n", 
           func_name, func_addr, func_addr + func_size,
           func_size ? "" : " (size unknown)");
    //存入func_table
    if(nr_func < MAX_FUNC) {
      func_table[nr_func++] = (FuncSymbol){
        .start = func_addr,  
        .size = func_size,
        .name = strdup(func_name)
      };
    }
  }
}

    free(symtab);
    free(strtab);
  }

  printf("ftrace: parsed %d functions from ELF\n", nr_func);
  free(sh_table);
  fclose(fp);
}

// 调用栈操作
static void push_func(const char *func_name) {
  if(call_stack_top < CALL_STACK_SIZE) {
    call_stack[call_stack_top++] = func_name;
  } else {
    printf("ftrace: call stack overflow!\n");
  }
}

static const char *pop_func() {
  if(call_stack_top > 0) {
    return call_stack[--call_stack_top];
  }
  return "???";
}

// 缩进打印
static void print_indent() {
  for (int i = 0; i < call_stack_top; i++) {
    printf("  ");
  }
}

// 调用输出
void ftrace_call(uint32_t call_site, uint32_t target) {
  const char *callee = find_func(target);
  print_indent();
  printf("0x%08x: call [%s@0x%08x]\n", call_site, callee, target);
  push_func(callee);
}

void ftrace_ret(uint32_t ret_site) {
  const char *popped = pop_func();
  print_indent();
  printf("0x%08x: ret  [%s]\n", ret_site, popped);
}

#endif  // CONFIG_FTRACE