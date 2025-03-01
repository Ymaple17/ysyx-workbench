module instruction_memory (
    input wire [31:0] pc,               // 当前 PC 地址
    output reg [31:0] instruction       // 读取的指令
);
    // 存储器，包含 1024 个 32 位指令
    reg [31:0] memory [0:1023];

    // 初始化存储器内容
    initial begin
        // 手动为存储器加载一些指令（以十六进制表示）
        memory[0] = 32'h00000093;  // 例：NOP 或者某个简单指令
        memory[1] = 32'h00000293;  // 另一个简单指令
        memory[2] = 32'h00000313;  // 另一个简单指令
        memory[3] = 32'h00000000;  // 例：终止指令或其他

        // 你可以在这里继续加载更多指令
    end

    // 根据 PC 地址读取指令
    always @(*) begin
        // 通过 pc[11:2] 来确保正确提取内存索引
        instruction = memory[pc[11:2]];
    end

endmodule


