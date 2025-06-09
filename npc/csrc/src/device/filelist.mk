#***************************************************************************************
# Copyright (c) 2014-2024 Zihao Yu, Nanjing University
#
# NEMU is licensed under Mulan PSL v2.
# You can use this software according to the terms and conditions of the Mulan PSL v2.
# You may obtain a copy of Mulan PSL v2 at:
#          http://license.coscl.org.cn/MulanPSL2
#
# THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
# EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
# MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
#
# See the Mulan PSL v2 for more details.
#**************************************************************************************/

DIRS-y += csrc/src/device/io
SRCS-$(CONFIG_DEVICE) += csrc/src/device/device.c
SRCS-$(CONFIG_HAS_SERIAL) += csrc/src/device/serial.c
SRCS-$(CONFIG_HAS_TIMER) += csrc/src/device/time.c
SRCS-$(CONFIG_HAS_KEYBOARD) += csrc/src/device/keyboard.c
SRCS-$(CONFIG_HAS_VGA) += csrc/src/device/vga.c

ifdef CONFIG_DEVICE
ifndef CONFIG_TARGET_AM
LIBS += $(shell sdl2-config --libs)
endif
endif
