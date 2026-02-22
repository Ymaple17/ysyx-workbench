import sys
import re

def process_file(input_path, no_sram_path, sram_path):
    with open(input_path, 'r') as f:
        lines = f.readlines()

    with open(no_sram_path, 'w') as f_no:
        with open(sram_path, 'w') as f_sram:
            state = "NORMAL" # NORMAL, IN_SRAM_HEADER, IN_SRAM_BODY
            
            for line in lines:
                if state == "NORMAL":
                    if 'module SRAM' in line:
                        state = "IN_SRAM_HEADER"
                        # Start of SRAM module
                        f_no.write('(* blackbox *)\n')
                        f_no.write(line)
                        f_sram.write(line)
                        if ');' in line:
                            state = "IN_SRAM_BODY"
                    else:
                        f_no.write(line)
                
                elif state == "IN_SRAM_HEADER":
                    f_no.write(line)
                    f_sram.write(line)
                    if ');' in line:
                        state = "IN_SRAM_BODY"

                elif state == "IN_SRAM_BODY":
                    f_sram.write(line)
                    if 'endmodule' in line:
                        f_no.write(line)
                        state = "NORMAL"

if __name__ == "__main__":
    if len(sys.argv) != 4:
        print("Usage: python3 filter_sram.py input.sv output_no_sram.sv output_sram.sv")
        sys.exit(1)
        
    process_file(sys.argv[1], sys.argv[2], sys.argv[3])
