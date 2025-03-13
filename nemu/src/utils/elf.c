#include <common.h>
#include <fcntl.h>
#include <unistd.h>
#include <elf.h>

typedef struct {
	char name[32]; // func name, 32 should be enough
	paddr_t addr;
	unsigned char info;
	Elf64_Xword size;
} SymEntry;

SymEntry *symbol_tbl[2]; // dynamic allocated
int symbol_tbl_size[2];
int elf_count = 0;

static void read_elf_header(int fd, Elf64_Ehdr *eh) {
	assert(lseek(fd, 0, SEEK_SET) == 0);
  assert(read(fd, (void *)eh, sizeof(Elf64_Ehdr)) == sizeof(Elf64_Ehdr));

	  // check if is elf using fixed format of Magic: 7f 45 4c 46 ...
  if(strncmp((char*)eh->e_ident, "\177ELF", 4)) {
    Log("malformed ELF file");
    return;
	}
}

__attribute__((unused)) static void display_elf_hedaer(Elf64_Ehdr eh) {
	/* Storage capacity class */
	ftrace_write("Storage class\t= ");
	switch(eh.e_ident[EI_CLASS])
	{
		case ELFCLASS32:
			ftrace_write("32-bit objects\n");
			break;

		case ELFCLASS64:
			ftrace_write("64-bit objects\n");
			break;

		default:
			ftrace_write("INVALID CLASS\n");
			break;
	}

	/* Data Format */
	ftrace_write("Data format\t= ");
	switch(eh.e_ident[EI_DATA])
	{
		case ELFDATA2LSB:
			ftrace_write("2's complement, little endian\n");
			break;

		case ELFDATA2MSB:
			ftrace_write("2's complement, big endian\n");
			break;

		default:
			ftrace_write("INVALID Format\n");
			break;
	}

	/* OS ABI */
	ftrace_write("OS ABI\t\t= ");
	switch(eh.e_ident[EI_OSABI])
	{
		case ELFOSABI_SYSV:
			ftrace_write("UNIX System V ABI\n");
			break;

		case ELFOSABI_HPUX:
			ftrace_write("HP-UX\n");
			break;

		case ELFOSABI_NETBSD:
			ftrace_write("NetBSD\n");
			break;

		case ELFOSABI_LINUX:
			ftrace_write("Linux\n");
			break;

		case ELFOSABI_SOLARIS:
			ftrace_write("Sun Solaris\n");
			break;

		case ELFOSABI_AIX:
			ftrace_write("IBM AIX\n");
			break;

		case ELFOSABI_IRIX:
			ftrace_write("SGI Irix\n");
			break;

		case ELFOSABI_FREEBSD:
			ftrace_write("FreeBSD\n");
			break;

		case ELFOSABI_TRU64:
			ftrace_write("Compaq TRU64 UNIX\n");
			break;

		case ELFOSABI_MODESTO:
			ftrace_write("Novell Modesto\n");
			break;

		case ELFOSABI_OPENBSD:
			ftrace_write("OpenBSD\n");
			break;

		case ELFOSABI_ARM_AEABI:
			ftrace_write("ARM EABI\n");
			break;

		case ELFOSABI_ARM:
			ftrace_write("ARM\n");
			break;

		case ELFOSABI_STANDALONE:
			ftrace_write("Standalone (embedded) app\n");
			break;

		default:
			ftrace_write("Unknown (0x%x)\n", eh.e_ident[EI_OSABI]);
			break;
	}

	/* ELF filetype */
	ftrace_write("Filetype \t= ");
	switch(eh.e_type)
	{
		case ET_NONE:
			ftrace_write("N/A (0x0)\n");
			break;

		case ET_REL:
			ftrace_write("Relocatable\n");
			break;

		case ET_EXEC:
			ftrace_write("Executable\n");
			break;

		case ET_DYN:
			ftrace_write("Shared Object\n");
			break;
		default:
			ftrace_write("Unknown (0x%x)\n", eh.e_type);
			break;
	}

	/* ELF Machine-id */
	ftrace_write("Machine\t\t= ");
	switch(eh.e_machine)
	{
		case EM_NONE:
			ftrace_write("None (0x0)\n");
			break;

		case EM_386:
			ftrace_write("INTEL x86 (0x%x)\n", EM_386);
			break;

		case EM_X86_64:
			ftrace_write("AMD x86_64 (0x%x)\n", EM_X86_64);
			break;

		case EM_AARCH64:
			ftrace_write("AARCH64 (0x%x)\n", EM_AARCH64);
			break;

		default:
			ftrace_write(" 0x%x\n", eh.e_machine);
			break;
	}

	/* Entry point */
	ftrace_write("Entry point\t= 0x%08lx\n", eh.e_entry);

	/* ELF header size in bytes */
	ftrace_write("ELF header size\t= 0x%08x\n", eh.e_ehsize);

	/* Program Header */
	ftrace_write("Program Header\t= ");
	ftrace_write("0x%08lx\n", eh.e_phoff);		/* start */
	ftrace_write("\t\t  %d entries\n", eh.e_phnum);	/* num entry */
	ftrace_write("\t\t  %d bytes\n", eh.e_phentsize);	/* size/entry */

	/* Section header starts at */
	ftrace_write("Section Header\t= ");
	ftrace_write("0x%08lx\n", eh.e_shoff);		/* start */
	ftrace_write("\t\t  %d entries\n", eh.e_shnum);	/* num entry */
	ftrace_write("\t\t  %d bytes\n", eh.e_shentsize);	/* size/entry */
	ftrace_write("\t\t  0x%08x (string table offset)\n", eh.e_shstrndx);

	/* File flags (Machine specific)*/
	ftrace_write("File flags \t= 0x%08x\n", eh.e_flags);

	/* ELF file flags are machine specific.
	 * INTEL implements NO flags.
	 * ARM implements a few.
	 * Add support below to parse ELF file flags on ARM
	 */
	int32_t ef = eh.e_flags;
	ftrace_write("\t\t  ");

	if(ef & EF_ARM_RELEXEC)
		ftrace_write(",RELEXEC ");

	if(ef & EF_ARM_HASENTRY)
		ftrace_write(",HASENTRY ");

	if(ef & EF_ARM_INTERWORK)
		ftrace_write(",INTERWORK ");

	if(ef & EF_ARM_APCS_26)
		ftrace_write(",APCS_26 ");

	if(ef & EF_ARM_APCS_FLOAT)
		ftrace_write(",APCS_FLOAT ");

	if(ef & EF_ARM_PIC)
		ftrace_write(",PIC ");

	if(ef & EF_ARM_ALIGN8)
		ftrace_write(",ALIGN8 ");

	if(ef & EF_ARM_NEW_ABI)
		ftrace_write(",NEW_ABI ");

	if(ef & EF_ARM_OLD_ABI)
		ftrace_write(",OLD_ABI ");

	if(ef & EF_ARM_SOFT_FLOAT)
		ftrace_write(",SOFT_FLOAT ");

	if(ef & EF_ARM_VFP_FLOAT)
		ftrace_write(",VFP_FLOAT ");

	if(ef & EF_ARM_MAVERICK_FLOAT)
		ftrace_write(",MAVERICK_FLOAT ");

	ftrace_write("\n");

	/* MSB of flags conatins ARM EABI version */
	ftrace_write("ARM EABI\t= Version %d\n", (ef & EF_ARM_EABIMASK)>>24);

	ftrace_write("\n");	/* End of ELF header */
}

static void read_section(int fd, Elf64_Shdr sh, void *dst) {
	assert(dst != NULL);
	assert(lseek(fd, (off_t)sh.sh_offset, SEEK_SET) == (off_t)sh.sh_offset);
	assert(read(fd, dst, sh.sh_size) == sh.sh_size);
}

static void read_section_headers(int fd, Elf64_Ehdr eh, Elf64_Shdr *sh_tbl) {
	assert(lseek(fd, eh.e_shoff, SEEK_SET) == eh.e_shoff);
	for(int i = 0; i < eh.e_shnum; i++) {
		assert(read(fd, (void *)&sh_tbl[i], eh.e_shentsize) == eh.e_shentsize);
	}
}

__attribute__((unused)) static void display_section_headers(int fd, Elf64_Ehdr eh, Elf64_Shdr sh_tbl[]) {
  // warn: C99
	char sh_str[sh_tbl[eh.e_shstrndx].sh_size];
  read_section(fd, sh_tbl[eh.e_shstrndx], sh_str);
  
	/* Read section-header string-table */

	ftrace_write("========================================");
	ftrace_write("========================================\n");
	ftrace_write(" idx offset     load-addr  size       algn"
			" flags      type       section\n");
	ftrace_write("========================================");
	ftrace_write("========================================\n");

	for(int i = 0; i < eh.e_shnum; i++) {
		ftrace_write(" %03d ", i);
		ftrace_write("0x%08lx ", sh_tbl[i].sh_offset);
		ftrace_write("0x%08lx ", sh_tbl[i].sh_addr);
		ftrace_write("0x%08lx ", sh_tbl[i].sh_size);
		ftrace_write("%-4ld ", sh_tbl[i].sh_addralign);
		ftrace_write("0x%08lx ", sh_tbl[i].sh_flags);
		ftrace_write("0x%08x ", sh_tbl[i].sh_type);
		ftrace_write("%s\t", (sh_str + sh_tbl[i].sh_name));
		ftrace_write("\n");
	}
	ftrace_write("========================================");
	ftrace_write("========================================\n");
	ftrace_write("\n");	/* end of section header table */
}

static void read_symbol_table(int fd, Elf64_Ehdr eh, Elf64_Shdr sh_tbl[], int sym_idx) {
  Elf64_Sym sym_tbl[sh_tbl[sym_idx].sh_size];
  read_section(fd, sh_tbl[sym_idx], sym_tbl);
  
  int str_idx = sh_tbl[sym_idx].sh_link;
  char str_tbl[sh_tbl[str_idx].sh_size];
  read_section(fd, sh_tbl[str_idx], str_tbl);
  
  int sym_count = (sh_tbl[sym_idx].sh_size / sizeof(Elf64_Sym));
	// log
  ftrace_write("Symbol count: %d\n", sym_count);
  ftrace_write("====================================================\n");
  ftrace_write(" num    value            type size       name\n");
  ftrace_write("====================================================\n");
  for (int i = 0; i < sym_count; i++) {
    ftrace_write(" %-3d    %016lx %-4d %-10ld %s\n",
      i,
      sym_tbl[i].st_value, 
      ELF64_ST_TYPE(sym_tbl[i].st_info),
			sym_tbl[i].st_size,
      str_tbl + sym_tbl[i].st_name
    );
  }
  ftrace_write("====================================================\n\n");

	// read
	symbol_tbl_size[elf_count] = sym_count;
	symbol_tbl[elf_count] = malloc(sizeof(SymEntry) * sym_count);
  for (int i = 0; i < sym_count; i++) {
    symbol_tbl[elf_count][i].addr = sym_tbl[i].st_value;
		symbol_tbl[elf_count][i].info = sym_tbl[i].st_info;
		symbol_tbl[elf_count][i].size = sym_tbl[i].st_size;
		memset(symbol_tbl[elf_count][i].name, 0, 32);
		strncpy(symbol_tbl[elf_count][i].name, str_tbl + sym_tbl[i].st_name, 31);
  }
}

static void read_symbols(int fd, Elf64_Ehdr eh, Elf64_Shdr sh_tbl[]) {
  for (int i = 0; i < eh.e_shnum; i++) {
		switch (sh_tbl[i].sh_type) {
		case SHT_SYMTAB: case SHT_DYNSYM:
			read_symbol_table(fd, eh, sh_tbl, i); break;
		}
  }
}

/* structured result are stored in symbol_tbl */
void parse_elf_file(int fd) {
  Elf64_Ehdr eh;
	read_elf_header(fd, &eh);
  // display_elf_hedaer(eh);

  Elf64_Shdr sh_tbl[eh.e_shentsize * eh.e_shnum];
	read_section_headers(fd, eh, sh_tbl);
  // display_section_headers(fd, eh, sh_tbl);

  read_symbols(fd, eh, sh_tbl);
  elf_count++;
}

/* return the index in symbol_tbl or -1 if not found */
SymEntry* find_symbol_func(paddr_t target) {
  int idx;
  for (idx = 0; idx < elf_count; idx++) {
    int i;
    for (i = 0; i < symbol_tbl_size[idx]; i++) {
      if (ELF64_ST_TYPE(symbol_tbl[idx][i].info) == STT_FUNC) {
        if (symbol_tbl[idx][i].addr <= target && target < symbol_tbl[idx][i].addr + symbol_tbl[idx][i].size) {
          return symbol_tbl[idx] + i;
        }
      }
    }    
  }

	return NULL;
}
