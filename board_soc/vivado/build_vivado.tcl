set script_dir [file dirname [file normalize [info script]]]
set board_dir [file normalize [file join $script_dir ..]]
set rtl_dir [file join $board_dir rtl]
set vendor_dir [file join $board_dir vendor verilog-axi rtl]
set cpu_freq_mhz 84.0
if {[info exists ::env(BOARD_SOC_CPU_FREQ_MHZ)] &&
    $::env(BOARD_SOC_CPU_FREQ_MHZ) ne ""} {
  set cpu_freq_mhz $::env(BOARD_SOC_CPU_FREQ_MHZ)
} elseif {[info exists ::env(BOARD_SOC_CLOCK_PERIOD_NS)] &&
         $::env(BOARD_SOC_CLOCK_PERIOD_NS) ne ""} {
  # Keep the old variable as a compatibility alias for existing scripts.
  set cpu_freq_mhz [expr {1000.0 / $::env(BOARD_SOC_CLOCK_PERIOD_NS)}]
}
set input_clock_period_ns 20.000
set cpu_freq_khz [expr {round($cpu_freq_mhz * 1000.0)}]
set mmcm_vco_freq_mhz 1050.0
if {$cpu_freq_mhz <= 0.0} { error "BOARD_SOC_CPU_FREQ_MHZ must be positive" }
set mmcm_output_divide [expr {$mmcm_vco_freq_mhz / $cpu_freq_mhz}]
set mmcm_output_divide_quantized [expr {round($mmcm_output_divide * 8.0) / 8.0}]
if {abs($mmcm_output_divide - $mmcm_output_divide_quantized) > 1.0e-6 ||
    $mmcm_output_divide_quantized < 1.0 ||
    $mmcm_output_divide_quantized > 128.0} {
  error [format "CPU frequency %.6f MHz gives illegal MMCM CLKOUT0 divider %.6f; use a frequency that makes 1050/freq a 0.125-step value" $cpu_freq_mhz $mmcm_output_divide]
}
set out_name "out_84mhz_mmcm_coremark"
if {[info exists ::env(BOARD_SOC_OUT_DIR)] && $::env(BOARD_SOC_OUT_DIR) ne ""} {
  set out_name $::env(BOARD_SOC_OUT_DIR)
}
set out_dir [file join $script_dir $out_name]
file mkdir $out_dir
cd $board_dir

set rtl_files [glob -nocomplain [file join $rtl_dir *.sv] [file join $rtl_dir cpu *.sv]]
lappend rtl_files [file join $vendor_dir axi_ram.v]
if {[llength $rtl_files] < 10} { error "board_soc RTL set is incomplete" }
if {![file exists [file join $board_dir board_image.hex]]} {
  error "board_image.hex is missing; build firmware first"
}

read_verilog -sv $rtl_files
read_xdc [file join $board_dir constraints navigator_zynq7020_pins.xdc]
set clock_xdc [file join $out_dir generated_clock.xdc]
set clock_file [open $clock_xdc w]
puts $clock_file [format {create_clock -period %s -name clk_50m -waveform {0.000 %s} [get_ports clk_50m]} $input_clock_period_ns [expr {$input_clock_period_ns / 2.0}]]
close $clock_file
read_xdc $clock_xdc

puts "BOARD_SOC_INPUT_CLOCK_PERIOD_NS=$input_clock_period_ns"
puts "BOARD_SOC_CPU_FREQ_MHZ=$cpu_freq_mhz"
puts "BOARD_SOC_CPU_FREQ_KHZ=$cpu_freq_khz"
puts "BOARD_SOC_MMCM_VCO_FREQ_MHZ=$mmcm_vco_freq_mhz"
puts "BOARD_SOC_MMCM_OUTPUT_DIVIDE=$mmcm_output_divide"
puts "BOARD_SOC_OUTPUT_DIR=$out_dir"

synth_design -top board_soc_top -part xc7z020clg400-1 -flatten_hierarchy rebuilt \
  -generic [list CPU_FREQ_KHZ=$cpu_freq_khz]
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
