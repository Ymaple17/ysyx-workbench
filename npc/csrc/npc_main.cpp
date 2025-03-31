#include <common.h>
#include <isa.h>

void init_monitor(int argc, char *argv[]);
extern "C" void engine_start();
int is_exit_status_bad();
void sim_exit();
//void init_module();

int main(int argc, char *argv[])
{
    init_monitor(argc, argv);

    engine_start();

    sim_exit();

    return is_exit_status_bad();
}
