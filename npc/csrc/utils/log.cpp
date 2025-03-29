#include <common.h>
#include <utils.h>

FILE *log_fp = NULL;

#ifdef CONFIG_LOG
void init_log(const char *log_file) {
  log_fp = stdout;
  if (log_file != NULL) {
    FILE *fp = fopen(log_file, "w");
    Assert(fp, "Can not open '%s'", log_file);
    log_fp = fp;
  }
  Log("Log is written to %s", log_file ? log_file : "stdout");
}
#else
void init_log(const char *) {}
#endif