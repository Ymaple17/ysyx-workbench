#ifndef __AXI_MONITOR_H__
#define __AXI_MONITOR_H__

#include <stdint.h>
#include <stdbool.h>

#ifdef CONFIG_AXI_MONITOR

#define AXI_STUCK_THRESHOLD 500
#define AXI_HANDSHAKE_TIMEOUT 200

void axi_monitor_init();
void axi_monitor_check();

#endif

#endif
