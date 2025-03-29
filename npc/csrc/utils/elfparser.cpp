#include <elfparser.h>

static SymEntry *symbol_tbl[2]; // dynamic allocated
static int symbol_tbl_size[2];
static int elf_count = 0;

static void read_elf_header(int fd, Elf64_Ehdr *eh) {
	assert(lseek(fd, 0, SEEK_SET) == 0);
  assert(read(fd, (void *)eh, sizeof(Elf64_Ehdr)) == sizeof(Elf64_Ehdr));

	  // check if is elf using fixed format of Magic: 7f 45 4c 46 ...
  if(strncmp((char*)eh->e_ident, "\177ELF", 4)) {
    Panic("malformed ELF file");
    return;
	}
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

static void read_symbol_table(int fd, Elf64_Ehdr eh, Elf64_Shdr sh_tbl[], int sym_idx) {
  Elf64_Sym sym_tbl[sh_tbl[sym_idx].sh_size];
  read_section(fd, sh_tbl[sym_idx], sym_tbl);
  
  int str_idx = sh_tbl[sym_idx].sh_link;
  char str_tbl[sh_tbl[str_idx].sh_size];
  read_section(fd, sh_tbl[str_idx], str_tbl);
  
  int sym_count = (sh_tbl[sym_idx].sh_size / sizeof(Elf64_Sym));

	// read
	symbol_tbl_size[elf_count] = sym_count;
	symbol_tbl[elf_count] = (SymEntry*)malloc(sizeof(SymEntry) * sym_count);
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
void parse_elf(int fd) {
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