`include "include/defs.vh"

module alignment_network (
  input  [`DATA_WIDTH-1:0] data_in,  
  input  [`DATA_WIDTH-1:0] dmem_addr,     
  input  [2:0]  control,       
  output [`DATA_WIDTH-1:0] data_out       
);

  // shift logic
  wire [1:0] shift_amount = dmem_addr[1:0];
  wire [`DATA_WIDTH-1:0] shifted_data = data_in >> (shift_amount * 8);

  // signed/unsigned extent signal
  wire [`DATA_WIDTH-1:0] zero_ext_byte;   

  // signed/unsigned extent logic
  assign zero_ext_byte = {24'b0, shifted_data[7:0]};

  MuxKey #(
    .NR_KEY(2),         
    .KEY_LEN(3),        
    .DATA_LEN(`DATA_WIDTH)       
  ) mem_mux (
    .out(data_out),
    .key(control), 
    .lut({              
      3'b100, zero_ext_byte,    // LBU                       
      3'b010, data_in          // LW                 
    })
  );

endmodule
