#include <isa.h>
#include <memory/paddr.h>
#include <getopt.h>
#include <memory/paddr.h>

void init_rand();
void init_log(const char *log_file);
void init_mem();
void init_difftest(char *ref_so_file, long img_size, int port);
void init_device();
void init_sdb();
extern "C" void init_disasm();
void init_parse_elf(const char *elf_file);
void init_sim();

extern "C" void test();

extern "C" void init_module();


static void welcome() {
  Log("Trace: %s", MUXDEF(CONFIG_TRACE, ANSI_FMT("ON", ANSI_FG_GREEN), ANSI_FMT("OFF", ANSI_FG_RED)));
  IFDEF(CONFIG_TRACE, Log("If trace is enabled, a log file will be generated "
        "to record the trace. This may lead to a large log file. "
        "If it is not necessary, you can disable it in menuconfig"));
  Log("Build time: %s, %s", __TIME__, __DATE__);
    printf("Welcome to %s-NPC!\n", ANSI_FMT("riscv32e", ANSI_FG_YELLOW ANSI_BG_RED));
  printf("For help, type \"help\"\n");
 // assert(0);
}

#ifndef CONFIG_TARGET_AM
#include <getopt.h>

void sdb_set_batch_mode();

static char *log_file = NULL;
static char *diff_so_file = NULL;
static char *img_file = NULL;
static int difftest_port = 1234;
static char *elf_file = NULL;

static long load_img(){
    if(img_file == NULL){
        Log("No image file is given. Use the default build-in image.");
        return 4096;  //buile-in image size
    }

    FILE *fp = fopen(img_file, "rb");
    Assert(fp, "Can not open '%s'", img_file);

    fseek(fp, 0, SEEK_END);   //将fp指针指向文件末尾处
    long size = ftell(fp);    //获取文件大小
    Log("The image is %sm size = %ld", img_file, size);
    fseek(fp, 0, SEEK_SET); 
    int ret = fread(guest_to_host(RESET_VECTOR), size, 1, fp);
    assert(ret == 1);

    fclose(fp);
    return size;
}

static int parse_args(int argc, char *argv[]){
    const struct option table[] = {
    {"batch"    , no_argument      , NULL, 'b'},
    {"log"      , required_argument, NULL, 'l'},
    {"diff"     , required_argument, NULL, 'd'},
    {"port"     , required_argument, NULL, 'p'},
    {"help"     , no_argument      , NULL, 'h'},
    {"elf"      , required_argument, NULL, 'e'},
    };
    int o;
    while ( (o = getopt_long(argc, argv, "-bhl:d:p:e:", table, NULL)) != -1){
        switch (o) {
            case 'b': sdb_set_batch_mode(); break;
            case 'p': sscanf(optarg, "%d", &difftest_port); break;
            case 'l': log_file = optarg; break;
            case 'd': diff_so_file = optarg; break;
            case 'e': elf_file = optarg; break;
            case 1: img_file = optarg; return 0;
            default:
                printf("Usage: %s [OPTION...] IMAGE [args]\n\n", argv[0]);
                printf("\t-b,--batch              run with batch mode\n");
                printf("\t-l,--log=FILE           output log to FILE\n");
                printf("\t-d,--diff=REF_SO        run DiffTest with reference REF_SO\n");
                printf("\t-p,--port=PORT          run DiffTest with port PORT\n");
				        printf("\t-e,--elf=FILE           parse the elf file\n");
                printf("\n");
                exit(0);
        }
    }
    return 0;
}

void init_monitor(int argc, char *argv[]){
    /* Perform some global initialization. */

    /* Parse arguments. */
    parse_args(argc, argv);

    /* Set random seed. */
    init_rand();

    /* Open teh log file. */
    init_log(log_file);

    /* Initialize memory. */
    init_mem();

    /* Initialize device. */
    //IFDEF(CONFIG_DEVICE, init_device());

    /* Perform ISA. */
    init_isa();

    init_sim();

    init_module();
   
    /* Load the image to memory. This will overwrite the built-in image. */
    long img_size = load_img();

    /* Initialize difftest. */
    init_difftest(diff_so_file, img_size, difftest_port);

    /* Initialize the simple debugger. */
    init_sdb();
    
    IFDEF(CONFIG_ITRACE, init_disasm());

    /* Display welcom message. */
    welcome();
}

#endif
