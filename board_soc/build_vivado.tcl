set script_dir [file dirname [file normalize [info script]]]
set board_dir [file normalize [file join $script_dir ..]]
set rtl_dir [file join $board_dir rtl]
set vendor_dir [file join $board_dir vendor verilog-axi rtl]
set out_dir [file join $script_dir out_50mhz]
file mkdir $out_dir
cd $board_dir

set rtl_files [glob -nocomplain [file join $rtl_dir *.sv]]
lappend rtl_files [file join $vendor_dir axi_ram.v]
if {[llength $rtl_files] < 3} { error "board_soc RTL set is incomplete" }
if {![file exists [file join $board_dir board_image.hex]]} {
  error "board_image.hex is missing; build firmware first"
}

read_verilog -sv $rtl_files
read_xdc [file join $board_dir constraints navigator_zynq7020.xdc]

synth_design -top board_soc_top -part xc7z020clg400-1 -flatten_hierarchy rebuilt
write_checkpoint -force [file join $out_dir post_synth.dcp]
report_utilization -hierarchical -file [file join $out_dir utilization_synth.rpt]
report_timing_summary -delay_type max -report_unconstrained -file [file join $out_dir timing_synth.rpt]

opt_design
place_design -directive ExtraNetDelay_high
phys_opt_design -directive AggressiveExplore
route_design -directive AggressiveExplore
write_checkpoint -force [file join $out_dir post_route.dcp]
report_utilization -hierarchical -file [file join $out_dir utilization_route.rpt]
report_timing_summary -delay_type max -report_unconstrained -file [file join $out_dir timing_route.rpt]
report_timing -delay_type max -max_paths 20 -sort_by group -file [file join $out_dir timing_paths.rpt]
write_bitstream -force [file join $out_dir board_soc.bit]
puts "BOARD_SOC_BITSTREAM=[file join $out_dir board_soc.bit]"
puts "BOARD_SOC_VIVADO_DONE"
