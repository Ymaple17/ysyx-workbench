graph TD
    subgraph 取指单元 IFU
        IFU[IFU<br/>取指单元]
        IFU -->|pc| imem_pc
        IFU -->|inst| instr
    end
    
    subgraph 解码单元 IDU
        IDU[IDU<br/>解码单元]
        IDU -->|pc_sel<br/>jump_reg_target<br/>br_target<br/>jmp_target| IFU
    end
    
    subgraph 执行单元 EXU
        EXU[EXU<br/>执行单元]
        EXU -->|alu_result| LSU
        EXU -->|alu_result| WBU
        EXU -->|csr_we<br/>csr_wdata| csr_file
    end
    
    subgraph 加载存储单元 LSU
        LSU[LSU<br/>加载存储单元]
        LSU -->|dmem_rdata| WBU
        LSU -->|mem_addr| mem_addr
        LSU -->|wen| wen
    end
    
    subgraph 写回单元 WBU
        WBU[WBU<br/>写回单元]
        WBU -->|reg_write_data<br/>reg_we| RegisterFile
    end
    
    subgraph 寄存器文件
        RegisterFile[RegisterFile<br/>寄存器文件]
        RegisterFile -->|rdata1<br/>rdata2| IDU
    end
    
    subgraph CSR文件
        csr_file[csr_file<br/>CSR寄存器]
        csr_file -->|csr_rdata| EXU
        csr_file -->|csr_rdata| WBU
        csr_file -->|mtvec_val<br/>mepc_val| IFU
    end
    
    %% 主要数据流
    IFU -->|inst| IDU
    IFU -->|pc| IDU
    IFU -->|pc_plus4| WBU
    
    IDU -->|Op1<br/>Op2<br/>alu_op<br/>is_csr_instr<br/>csr_op| EXU
    IDU -->|rs1_addr<br/>rs2_addr| RegisterFile
    IDU -->|waddr| RegisterFile
    IDU -->|csr_addr| csr_file
    IDU -->|is_ecall<br/>is_ebreak<br/>is_mret| csr_file
    
    RegisterFile -->|rdata1<br/>rdata2| IDU
    RegisterFile -->|rdata2| LSU
    
    csr_file -->|csr_rdata| EXU
    csr_file -->|csr_rdata| WBU
    
    EXU -->|alu_result| LSU
    EXU -->|alu_result| WBU
    
    LSU -->|dmem_rdata| WBU
    
    %% 调试信号
    IFU -->|inst| instr[instr]
    IFU -->|pc| imem_pc[imem_pc]
    LSU -->|mem_addr| mem_addr[mem_addr]
    LSU -->|wen| wen[wen]
    
    %% 异常处理
    IDU -->|is_ebreak| ebreak[ebreak处理]
    ebreak --> sim_exit[sim_exit()<br/>结束仿真]
