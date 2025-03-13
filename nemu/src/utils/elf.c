#include <common.h>
#include <fcntl.h>
#include <unistd.h>
#include <elf.h>
#include <stdio.h>  // 添加标准输入输出头文件
#include <string.h> // 添加字符串操作头文件

typedef struct {
    char name[32]; // 函数名，32 字节足够
    paddr_t addr;  // 地址，使用 paddr_t 类型
    unsigned char info; // 符号信息
    Elf32_Xword size;   // 符号大小，使用 Elf32_Xword
} SymEntry;

SymEntry *symbol_tbl[2]; // 动态分配的符号表
int symbol_tbl_size[2];
int elf_count = 0;

static void read_elf_header(int fd, Elf32_Ehdr *eh) {
    if (lseek(fd, 0, SEEK_SET) != 0) {
        perror("Failed to seek to start of ELF file");
        return;
    }
    if (read(fd, (void *)eh, sizeof(Elf32_Ehdr)) != sizeof(Elf32_Ehdr)) {
        perror("Failed to read ELF header");
        return;
    }

    // 检查是否为 ELF 文件，使用魔数 7f 45 4c 46
    if (strncmp((char *)eh->e_ident, "\177ELF", 4) != 0) {
        Log("malformed ELF file");
        return;
    }
}

__attribute__((unused)) static void display_elf_header(Elf32_Ehdr eh) {
    /* Storage capacity class */
    ftrace_write("Storage class\t= ");
    switch (eh.e_ident[EI_CLASS]) {
        case ELFCLASS32: ftrace_write("32-bit objects\n"); break;
        case ELFCLASS64: ftrace_write("64-bit objects\n"); break;
        default: ftrace_write("INVALID CLASS\n"); break;
    }

    /* Data Format */
    ftrace_write("Data format\t= ");
    switch (eh.e_ident[EI_DATA]) {
        case ELFDATA2LSB: ftrace_write("2's complement, little endian\n"); break;
        case ELFDATA2MSB: ftrace_write("2's complement, big endian\n"); break;
        default: ftrace_write("INVALID Format\n"); break;
    }

    /* OS ABI */
    ftrace_write("OS ABI\t\t= ");
    switch (eh.e_ident[EI_OSABI]) {
        case ELFOSABI_LINUX: ftrace_write("Linux\n"); break;
        case ELFOSABI_SYSV: ftrace_write("UNIX System V ABI\n"); break;
        default: ftrace_write("Unknown (0x%x)\n", eh.e_ident[EI_OSABI]); break;
    }

    /* ELF filetype */
    ftrace_write("Filetype \t= ");
    switch (eh.e_type) {
        case ET_EXEC: ftrace_write("Executable\n"); break;
        case ET_DYN: ftrace_write("Shared Object\n"); break;
        case ET_REL: ftrace_write("Relocatable\n"); break;
        default: ftrace_write("Unknown (0x%x)\n", eh.e_type); break;
    }

    /* ELF Machine-id */
    ftrace_write("Machine\t\t= ");
    switch (eh.e_machine) {
        case EM_RISCV: ftrace_write("RISC-V (0x%x)\n", EM_RISCV); break;
        default: ftrace_write("0x%x\n", eh.e_machine); break;
    }

    /* Entry point */
    ftrace_write("Entry point\t= 0x%08x\n", eh.e_entry);

    /* ELF header size */
    ftrace_write("ELF header size\t= 0x%08x\n", eh.e_ehsize);

    /* Program Header */
    ftrace_write("Program Header\t= 0x%08x (start), %d entries, %d bytes/entry\n",
                 eh.e_phoff, eh.e_phnum, eh.e_phentsize);

    /* Section Header */
    ftrace_write("Section Header\t= 0x%08x (start), %d entries, %d bytes/entry, 0x%08x (string table)\n",
                 eh.e_shoff, eh.e_shnum, eh.e_shentsize, eh.e_shstrndx);

    /* File flags */
    ftrace_write("File flags\t= 0x%08x\n", eh.e_flags);
    ftrace_write("\n");
}

static void read_section(int fd, Elf32_Shdr sh, void *dst) {
    if (dst == NULL) {
        Log("Destination buffer is NULL");
        return;
    }
    if (lseek(fd, (off_t)sh.sh_offset, SEEK_SET) != (off_t)sh.sh_offset) {
        perror("Failed to seek to section offset");
        return;
    }
    if (read(fd, dst, sh.sh_size) != sh.sh_size) {
        perror("Failed to read section data");
        return;
    }
}

static void read_section_headers(int fd, Elf32_Ehdr eh, Elf32_Shdr *sh_tbl) {
    off_t offset = lseek(fd, eh.e_shoff, SEEK_SET);
    if (offset != eh.e_shoff) {
        perror("Failed to seek to section headers");
        printf("fd: %d, Expected offset: %ld, Actual: %ld, File size: %ld\n",
               fd, (long)eh.e_shoff, (long)offset, (long)lseek(fd, 0, SEEK_END));
        return;
    }
    for (int i = 0; i < eh.e_shnum; i++) {
        if (read(fd, (void *)&sh_tbl[i], eh.e_shentsize) != eh.e_shentsize) {
            perror("Failed to read section header");
            return;
        }
    }
}

__attribute__((unused)) static void display_section_headers(int fd, Elf32_Ehdr eh, Elf32_Shdr sh_tbl[]) {
    char sh_str[sh_tbl[eh.e_shstrndx].sh_size];
    read_section(fd, sh_tbl[eh.e_shstrndx], sh_str);

    ftrace_write("========================================\n");
    ftrace_write(" idx offset     load-addr  size       algn flags      type       section\n");
    ftrace_write("========================================\n");

    for (int i = 0; i < eh.e_shnum; i++) {
        ftrace_write(" %03d 0x%08x 0x%08x 0x%08x %-4d 0x%08x 0x%08x %s\n",
                     i, sh_tbl[i].sh_offset, sh_tbl[i].sh_addr, sh_tbl[i].sh_size,
                     sh_tbl[i].sh_addralign, sh_tbl[i].sh_flags, sh_tbl[i].sh_type,
                     sh_str + sh_tbl[i].sh_name);
    }
    ftrace_write("========================================\n\n");
}

static void read_symbol_table(int fd, Elf32_Ehdr eh, Elf32_Shdr sh_tbl[], int sym_idx) {
    Elf32_Sym *sym_tbl = malloc(sh_tbl[sym_idx].sh_size);
    read_section(fd, sh_tbl[sym_idx], sym_tbl);

    int str_idx = sh_tbl[sym_idx].sh_link;
    char *str_tbl = malloc(sh_tbl[str_idx].sh_size);
    read_section(fd, sh_tbl[str_idx], str_tbl);

    int sym_count = sh_tbl[sym_idx].sh_size / sizeof(Elf32_Sym);
    ftrace_write("Symbol count: %d\n", sym_count);
    ftrace_write("====================================================\n");
    ftrace_write(" num    value      type size       name\n");
    ftrace_write("====================================================\n");
    for (int i = 0; i < sym_count; i++) {
        ftrace_write(" %-3d    %08x %-4d %-10u %s\n",
             i, sym_tbl[i].st_value, ELF32_ST_TYPE(sym_tbl[i].st_info),
             sym_tbl[i].st_size, str_tbl + sym_tbl[i].st_name);
    }
    ftrace_write("====================================================\n\n");

    symbol_tbl_size[elf_count] = sym_count;
    symbol_tbl[elf_count] = malloc(sizeof(SymEntry) * sym_count);
    for (int i = 0; i < sym_count; i++) {
        symbol_tbl[elf_count][i].addr = sym_tbl[i].st_value;
        symbol_tbl[elf_count][i].info = sym_tbl[i].st_info;
        symbol_tbl[elf_count][i].size = sym_tbl[i].st_size;
        strncpy(symbol_tbl[elf_count][i].name, str_tbl + sym_tbl[i].st_name, 31);
        symbol_tbl[elf_count][i].name[31] = '\0'; // 确保字符串终止
    }

    free(sym_tbl);
    free(str_tbl);
}

static void read_symbols(int fd, Elf32_Ehdr eh, Elf32_Shdr sh_tbl[]) {
    for (int i = 0; i < eh.e_shnum; i++) {
        switch (sh_tbl[i].sh_type) {
            case SHT_SYMTAB:
            case SHT_DYNSYM:
                read_symbol_table(fd, eh, sh_tbl, i);
                break;
        }
    }
}

void parse_elf_file(int fd) {
    Elf32_Ehdr eh;
    read_elf_header(fd, &eh);
    // display_elf_header(eh);

    Elf32_Shdr *sh_tbl = malloc(eh.e_shentsize * eh.e_shnum);
    read_section_headers(fd, eh, sh_tbl);
    // display_section_headers(fd, eh, sh_tbl);

    read_symbols(fd, eh, sh_tbl);
    elf_count++;

    free(sh_tbl);
}

SymEntry* find_symbol_func(paddr_t target) {
    for (int idx = 0; idx < elf_count; idx++) {
        for (int i = 0; i < symbol_tbl_size[idx]; i++) {
            if (ELF32_ST_TYPE(symbol_tbl[idx][i].info) == STT_FUNC) {
                if (symbol_tbl[idx][i].addr <= target &&
                    target < symbol_tbl[idx][i].addr + symbol_tbl[idx][i].size) {
                    return &symbol_tbl[idx][i];
                }
            }
        }
    }
    return NULL;
}
