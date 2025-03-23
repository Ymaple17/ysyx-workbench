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

/* structured result are stored in symbol_tbl */
void parse_elf(int fd);

/* return the index in symbol_tbl or -1 if not found */
SymEntry* find_symbol_func(paddr_t target);
