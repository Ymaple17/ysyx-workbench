module alu(
  input [31:0] A,  // rs1
  input [31:0] rs2,
  input [31:0] imm,
  input [3:0] ALUctr,
  input ALUBsrc,
  output reg less,
  output reg zero,
  output reg [31:0] ALUout,
  output reg of,zf,cf
);

reg [31:0] B;
always @(*) begin
    case(ALUBsrc)
        1'b0: B = rs2;
        1'b1: B = imm;
    endcase
end

reg [31:0] xb;  // b after xor with cin

always@(*)begin
    ALUout = 32'd0; zero = 1'b0; less = 1'b0; of = 0; zf = 0; cf = 0; xb = 32'd0;

    casez(ALUctr)
    4'bz000 : // add or sub
    begin
        xb = B ^ { {32{ALUctr[3]}} };  // extend and xor
        {cf,ALUout} = xb + A + {31'b0,ALUctr[3]};
	of = (A[31] == xb[31]) && (ALUout[31] != A[31]);
	zf = ~(|ALUout);
    end

    4'bz001:  // left shift
    begin
        ALUout = B[4] ? {A[15:0],16'b0} : A;
        ALUout = B[3] ? {ALUout[23:0], 8'b0} : A;
        ALUout = B[2] ? {ALUout[27:0], 4'b0} : ALUout;
        ALUout = B[1] ? {ALUout[29:0], 2'b0} : ALUout;
        ALUout = B[0] ? {ALUout[30:0], 1'b0} : ALUout; 
    end
    
    4'b0010: // signed compare with sub
    begin
        xb = B ^ 32'b1;
        {cf,ALUout} = xb + A + 32'b1;
	    zf = ~(|ALUout);
        if(zf == 1) begin 
            zero = 1;
            ALUout = 32'b0;
        end
        else begin
            if(ALUout[31] == 1) begin
                less = 1;
                ALUout = 32'b1;
            end
            else begin
                ALUout = 32'b0;
                less = 0;
            end
        end
    end

    4'b1010: // unsigned compare with sub
    begin
        xb = B ^ 32'b1;
        {cf,ALUout} = xb + A + 32'b1;
	    zf = ~(|ALUout);
        if(zf == 1) begin
            zero = 1;
            ALUout = 32'b0;
        end
        else begin
            if(ALUout[31] == 1) begin
                less = 1;
                ALUout = 32'b1;
            end
            else begin
                ALUout = 32'b0;
                less = 0;
            end
        end
    end

    4'bz011: // straight
    begin
        ALUout = B;
    end

    4'bz100: // XOR
    begin
        ALUout = A ^ B;
    end

    4'bz101: // right shift
    begin
        if(ALUctr[3] == 0) begin // logical
            ALUout = B[4] ? {16'b0, A[31:16]} : A;
            ALUout = B[3] ? { 8'b0, ALUout[31:8]} : ALUout;
            ALUout = B[2] ? { 4'b0, ALUout[31:4]} : ALUout;
            ALUout = B[1] ? { 2'b0, ALUout[31:2]} : ALUout;
            ALUout = B[0] ? { 1'b0, ALUout[31:1]} : ALUout;
        end
        else begin // athigram
            ALUout = B[4] ? {{16{A[31]}}, A[31:16]} : A;
            ALUout = B[3] ? {{ 8{ALUout[31]}}, ALUout[31:8]} : ALUout;
            ALUout = B[2] ? {{ 4{ALUout[31]}}, ALUout[31:4]} : ALUout;
            ALUout = B[1] ? {{ 2{ALUout[31]}}, ALUout[31:2]} : ALUout;
            ALUout = B[0] ? {{ 1{ALUout[31]}}, ALUout[31:1]} : ALUout;
        end
    end

    4'bz110:  // OR
    ALUout = A | B;

    4'bz111 : // AND
    begin
	ALUout = A & B;
    end

    default : ALUout = 32'd0;
  endcase
end

endmodule

