set script_dir [file dirname [file normalize [info script]]]
set out_name "out_84mhz_mmcm_coremark"
if {[info exists ::env(BOARD_SOC_OUT_DIR)] && $::env(BOARD_SOC_OUT_DIR) ne ""} {
    set out_name $::env(BOARD_SOC_OUT_DIR)
}
set bit_file [file normalize [file join $script_dir $out_name board_soc.bit]]
if {[info exists ::env(BOARD_SOC_BIT_FILE)] && $::env(BOARD_SOC_BIT_FILE) ne ""} {
    set bit_file [file normalize $::env(BOARD_SOC_BIT_FILE)]
}

if {![file exists $bit_file]} {
    error "board_soc bitstream is missing: $bit_file"
}

open_hw_manager
connect_hw_server -url localhost:3121
set targets [get_hw_targets *]
if {[llength $targets] == 0} {
    error "no JTAG hardware target found"
}
current_hw_target [lindex $targets 0]
open_hw_target

set devices [get_hw_devices xc7z020*]
if {[llength $devices] == 0} {
    error "xc7z020 device not found"
}
set device [lindex $devices 0]
current_hw_device $device
refresh_hw_device $device
set_property PROGRAM.FILE $bit_file $device
program_hw_devices $device
refresh_hw_device $device
puts "BOARD_SOC_PROGRAMMED_DEVICE=$device"
puts "BOARD_SOC_PROGRAMMED_FILE=$bit_file"

close_hw_target
disconnect_hw_server localhost:3121
close_hw_manager
puts "BOARD_SOC_PROGRAM_DONE"
