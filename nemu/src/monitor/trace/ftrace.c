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
extern int elf_count;
void parse_elf_file(int fd);
SymEntry*  find_symbol_func(paddr_t target);

int call_depth = 0;
typedef struct tail_rec_node {
	paddr_t pc;
	int depth;
	struct tail_rec_node *next;
} TailRecNode;
TailRecNode *tail_rec_head = NULL; // linklist with head, dynamic allocated

static void init_tail_rec_list() {
	tail_rec_head = (TailRecNode *)malloc(sizeof(TailRecNode));
	tail_rec_head->pc = 0;
	tail_rec_head->next = NULL;
}

/* ELF64 as default */
void parse_elf(const char **elf_files, int elf_file_count) {
  if (elf_file_count == 0) return;

  Log("total elf: %d\n", elf_file_count);
  while (elf_file_count--) {
    Log("specified ELF file: %s", elf_files[elf_file_count]);
    int fd = open(elf_files[elf_file_count], O_RDONLY|O_SYNC);
    Assert(fd >= 0, "Error %d: unable to open %s\n", fd, elf_files[elf_file_count]);
    parse_elf_file(fd);
    close(fd);    
  }
  
	init_tail_rec_list();
}

static void insert_tail_rec(paddr_t pc, int depth) {
	TailRecNode *node = (TailRecNode *)malloc(sizeof(TailRecNode));
	node->pc = pc;
	node->depth = depth;
	node->next = tail_rec_head->next;
	tail_rec_head->next = node;
}

static void remove_tail_rec() {
	TailRecNode *node = tail_rec_head->next;
	tail_rec_head->next = node->next;
	free(node);
}

static bool func_filter(char *name) {
	return strcmp(name, "putch");
}

void trace_func_call(paddr_t pc, paddr_t target, bool is_tail) {
  if (elf_count == 0) return;

	// 判断是否落在当前函数，如果是话就不是函数调用
	SymEntry* cur_i = find_symbol_func(pc);
	SymEntry* i = find_symbol_func(target);
	if (cur_i == i) return;

	++call_depth;
  char *name = i==NULL?"???":i->name;
	if (func_filter(name)) {
		ftrace_write(FMT_PADDR ": %*scall [%s@" FMT_PADDR "]\n",pc,(call_depth-1)*2, "",name,target);
	}

	if (is_tail) {
		insert_tail_rec(pc, call_depth-1);
	}
}

void trace_func_ret(paddr_t pc) {
	if (elf_count == 0) return;

	SymEntry* i = find_symbol_func(pc);
  char *name = i==NULL?"???":i->name;
	if (func_filter(name)) {
		ftrace_write(FMT_PADDR ": %*sret [%s]\n",pc,(call_depth-1)*2, "",name);
	}
	
	--call_depth;

	TailRecNode *node = tail_rec_head->next;
	if (node != NULL) {
		if (node->depth == call_depth) {
			paddr_t ret_target = node->pc;
			remove_tail_rec();
			trace_func_ret(ret_target);
		}
	}
}

