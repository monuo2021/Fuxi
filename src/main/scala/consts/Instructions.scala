// 该文件定义指令信息
package consts

import chisel3._
import chisel3.util.BitPat

object Instructions {
  // arithmetic，算术指令
  val ADD       = BitPat("b0000000??????????000?????0110011")
  val ADDI      = BitPat("b?????????????????000?????0010011")
  val SUB       = BitPat("b0100000??????????000?????0110011")
  val LUI       = BitPat("b?????????????????????????0110111")   // 高位立即数加载。将符号拓展后的 20 位立即数左移 12 位，并将第 12 位置零，写入寄存器中。
  val AUIPC     = BitPat("b?????????????????????????0010111")   // PC 加立即数

  // logical，逻辑指令
  val XOR       = BitPat("b0000000??????????100?????0110011")
  val XORI      = BitPat("b?????????????????100?????0010011")
  val OR        = BitPat("b0000000??????????110?????0110011")
  val ORI       = BitPat("b?????????????????110?????0010011")
  val AND       = BitPat("b0000000??????????111?????0110011")
  val ANDI      = BitPat("b?????????????????111?????0010011")

  // compare，比较置位指令
  val SLT       = BitPat("b0000000??????????010?????0110011")   // 小于则置位
  val SLTI      = BitPat("b?????????????????010?????0010011")   // 小于立即数则置位
  val SLTU      = BitPat("b0000000??????????011?????0110011")   // 无符号数小于则置位
  val SLTIU     = BitPat("b?????????????????011?????0010011")   // 无符号数小于立即数则置位

  // shift，移位指令
  val SLL       = BitPat("b0000000??????????001?????0110011")   // 逻辑左移
  val SLLI      = BitPat("b0000000??????????001?????0010011")   // 立即数逻辑左移
  val SRL       = BitPat("b0000000??????????101?????0110011")   // 逻辑右移
  val SRLI      = BitPat("b0000000??????????101?????0010011")   // 立即数逻辑右移
  val SRA       = BitPat("b0100000??????????101?????0110011")   // 算术右移
  val SRAI      = BitPat("b0100000??????????101?????0010011")   // 立即数算数右移

  // branch & jump，分支跳转/跳转指令
  val BEQ       = BitPat("b?????????????????000?????1100011")   // 相等则跳转
  val BNE       = BitPat("b?????????????????001?????1100011")   // 不相等则跳转
  val BLT       = BitPat("b?????????????????100?????1100011")   // 小于则跳转
  val BGE       = BitPat("b?????????????????101?????1100011")   // 大于则跳转
  val BLTU      = BitPat("b?????????????????110?????1100011")   // 无符号小于则跳转
  val BGEU      = BitPat("b?????????????????111?????1100011")   // 无符号大于则跳转
  val JAL       = BitPat("b?????????????????????????1101111")   // 跳转并链接
  val JALR      = BitPat("b?????????????????000?????1100111")   // 跳转并寄存器链接

  // load & store，加载存数指令
  val LB        = BitPat("b?????????????????000?????0000011")   // 字节加载
  val LH        = BitPat("b?????????????????001?????0000011")   // 半字加载
  val LW        = BitPat("b?????????????????010?????0000011")   // 一字加载
  val LBU       = BitPat("b?????????????????100?????0000011")   // 无符号字节加载
  val LHU       = BitPat("b?????????????????101?????0000011")   // 无符号半字加载
  val SB        = BitPat("b?????????????????000?????0100011")   // 存字节
  val SH        = BitPat("b?????????????????001?????0100011")   // 存半字
  val SW        = BitPat("b?????????????????010?????0100011")   // 存字

  // sync，同步指令
  val FENCE     = BitPat("b0000????????00000000000000001111")   // 同步内存和I/O
  val FENCEI    = BitPat("b00000000000000000001000000001111")   // 同步指令流

  // CSR access，CSR（控制与状态寄存器）相关指令
  val CSRRW     = BitPat("b?????????????????001?????1110011")   // 读后写控制状态寄存器
  val CSRRS     = BitPat("b?????????????????010?????1110011")   // 读后置位控制状态寄存器
  val CSRRC     = BitPat("b?????????????????011?????1110011")   // 读后清除控制状态寄存器
  val CSRRWI    = BitPat("b?????????????????101?????1110011")   // 立即数读后写控制状态寄存器
  val CSRRSI    = BitPat("b?????????????????110?????1110011")   // 立即数读后置位控制状态寄存器
  val CSRRCI    = BitPat("b?????????????????111?????1110011")   // 立即数读后清除控制状态寄存器

  // multiplication & division，乘除指令
  val MUL       = BitPat("b0000001??????????000?????0110011")   //
  val MULH      = BitPat("b0000001??????????001?????0110011")   //
  val MULHSU    = BitPat("b0000001??????????010?????0110011")   //
  val MULHU     = BitPat("b0000001??????????011?????0110011")   //
  val DIV       = BitPat("b0000001??????????100?????0110011")   //
  val DIVU      = BitPat("b0000001??????????101?????0110011")   //
  val REM       = BitPat("b0000001??????????110?????0110011")   // 求余数
  val REMU      = BitPat("b0000001??????????111?????0110011")   // 求无符号数的余数

  // atomic，原子指令
  val LRW       = BitPat("b00010??00000?????010?????0101111")   //
  val SCW       = BitPat("b00011????????????010?????0101111")   //
  val AMOSWAPW  = BitPat("b00001????????????010?????0101111")   // 原子双字交换
  val AMOADDW   = BitPat("b00000????????????010?????0101111")   // 原子加字
  val AMOXORW   = BitPat("b00100????????????010?????0101111")   //
  val AMOANDW   = BitPat("b01100????????????010?????0101111")   //
  val AMOORW    = BitPat("b01000????????????010?????0101111")   //
  val AMOMINW   = BitPat("b10000????????????010?????0101111")   //
  val AMOMAXW   = BitPat("b10100????????????010?????0101111")   //
  val AMOMINUW  = BitPat("b11000????????????010?????0101111")   //
  val AMOMAXUW  = BitPat("b11100????????????010?????0101111")   //

  // privilege，特权级指令
  val ECALL     = BitPat("b00000000000000000000000001110011")   // 环境调用
  val EBREAK    = BitPat("b00000000000100000000000001110011")   // 环境断点
  val SRET      = BitPat("b00010000001000000000000001110011")   // 管理者/监督者模式异常调用
  val MRET      = BitPat("b00110000001000000000000001110011")   // 机器模式异常调用
  val WFI       = BitPat("b00010000010100000000000001110011")   // 等待中断
  val SFENCEVMA = BitPat("b0001001??????????000000001110011")   // 虚拟内存屏障

  // pseudo instruction，伪指令
  val NOP       = "b00000000000000000000000000010011".U(32.W)   // 无指令
}
