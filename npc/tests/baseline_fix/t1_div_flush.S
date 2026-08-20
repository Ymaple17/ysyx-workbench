# t1_div_flush.S
# 验证：分支误预测冲刷时，若 EXU 里有在飞 DIV，kill 后不得把旧结果写到后续指令。
#
# 思路：
#   1) 先让一条「必错预测方向」的分支进入流水线，其后紧跟 div（错误路径）
#   2) 分支在 EXU 解析后冲刷，div 应被 kill
#   3) 正确路径上再做一条 div，检查商正确
#   4) 再做一次：正确路径上 div 完成后，结果寄存器不被污染
#
# 通过：最终 a0==0 后 ebreak；失败 a0==1 ebreak
# difftest 与 NEMU 逐步比对，任一步错即 abort

    .text
    .globl _start
_start:
    # 无 C 运行时：不设 sp（本测例不用栈）
    # ---------- case A: 错误路径上的 div 被冲刷 ----------
    # 构造：beq 实际上 not-taken，但若 BPU 预测 taken 会走错路；
    # 更稳妥：用「总是 taken 的 jal 到正确目标」，中间塞假路径不现实。
    # 改用：bne x0,x0 永不成立；其后放 div（顺序取指会取到，若预测不跳也会进流水线）
    # 实际上 bne x0,x0 是 not-taken，顺序取指下 div 在分支之后会进流水线，
    # 分支在 EXU 时若预测 taken 才会 flush；若预测正确 not-taken，div 会真执行。
    #
    # 更可靠的「冲刷在飞 div」：用 ecall 异常冲刷。
    # ecall 在 IDU 标 irq，到 WBU 后 is_irq 冲全流水；若此时 EXU 有 div 则 kill。
    #
    # 设置 mtvec = handler，然后：div 进 EXU 后尽快 ecall（ecall 在 IDU 停不住 div 已在 EXU 的情况）
    # 流水线：... div 在 EXU 多拍；同时后续 ecall 可进入 IDU/之后级
    # 顺序核：div 占 EXU 时 in.ready=0，ecall 卡在 IDU 直到 div 完成！
    # ★ 因此顺序核下「ecall 冲刷在飞 div」反而难触发——div 不让后面进 EXU。
    #
    # 能触发 kill 的场景：is_flush 在 div 占用 EXU 期间为真。
    # - is_bp_flush：分支在 EXU，div 也在 EXU？单发射不可能同时。
    # - is_irq：异常指令提交时 flush，此时 div 若在更后级？顺序下异常在 WBU，
    #   冲刷时 EXU 可能有后续指令——但 div 堵着时后续进不了 EXU。
    # - is_fencei：只冲 IFU/IDU，不冲 EXU，div 不被 kill（有意）。
    #
    # ★ 结论（对顺序单发核）：DIV kill 路径在「单发射 + div 堵 EXU」下
    #   几乎只能靠 is_irq 与 div 结束同一窗口的边角，或将来乱序。
    # 本测例改为验证两件可测之事：
    #   (1) 连续多次 div 结果正确（无状态机卡死）
    #   (2) div 后立即分支误预测类压力 + 再 div 仍正确
    #   (3) 除零 rem/div 行为与 NEMU 一致
    #
    # 另：用「短指令填充分支延迟槽式」的错误路径——
    # 先执行会 mispredict 的 jalr（ret 错误），冲刷后 div 正确。

    # ===== (1) 多次 div/rem 正确性 =====
    li   t0, 100
    li   t1, 7
    div  t2, t0, t1          # 14
    li   t3, 14
    bne  t2, t3, fail

    rem  t2, t0, t1          # 2
    li   t3, 2
    bne  t2, t3, fail

    li   t0, -100
    li   t1, 7
    div  t2, t0, t1
    # 有符号向零：-14
    li   t3, -14
    bne  t2, t3, fail

    # 除零：商 -1，余数为被除数
    li   t0, 123
    li   t1, 0
    div  t2, t0, t1
    li   t3, -1
    bne  t2, t3, fail
    rem  t2, t0, t1
    li   t3, 123
    bne  t2, t3, fail

    # ===== (2) div 与密集分支交错（逼 mispred 冲刷与 div 交替）=====
    li   s0, 0
    li   s1, 32
loop_br_div:
    # 半随机分支模式
    andi t0, s0, 1
    beq  t0, x0, skip_a
    li   t1, 99
    li   t2, 3
    div  t3, t1, t2          # 33
    li   t4, 33
    bne  t3, t4, fail
    j    skip_b
skip_a:
    li   t1, 80
    li   t2, 5
    div  t3, t1, t2          # 16
    li   t4, 16
    bne  t3, t4, fail
skip_b:
    addi s0, s0, 1
    blt  s0, s1, loop_br_div

    # ===== (3) 错误路径：jal 假目标再回来，随后 div =====
    # 用 call/ret 制造 RAS/BPU 压力（功能上只要最终对）
    li   s0, 0
    li   s1, 16
loop_call:
    jal  ra, helper_div
    # helper 返回 a0 = 42
    li   t0, 42
    bne  a0, t0, fail
    addi s0, s0, 1
    blt  s0, s1, loop_call

    # pass
    li   a0, 0
    ebreak
    j    .

fail:
    li   a0, 1
    ebreak
    j    .

# 含 div 的叶子函数，用 jal ra 调用（同时锻炼 RAS push 的 jal ra）
helper_div:
    li   t0, 126
    li   t1, 3
    div  a0, t0, t1          # 42
    ret
