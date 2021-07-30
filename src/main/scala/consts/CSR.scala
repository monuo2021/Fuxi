// CSR相关定义，如CSR寄存器、CSR权限
package consts

import chisel3._

object CSR {
  // 定义RISC-V 的特权级模式，依次为：用户模式、监管者模式、机器模式
  val CSR_MODE_WIDTH  = 2
  val CSR_MODE_U      = "b00".U(CSR_MODE_WIDTH.W)
  val CSR_MODE_S      = "b01".U(CSR_MODE_WIDTH.W)
  val CSR_MODE_M      = "b11".U(CSR_MODE_WIDTH.W)

  // address
  val CSR_ADDR_WIDTH  = 12
  // user counters (read only)，用户模式下计数器
  val CSR_CYCLE       = 0xc00.U(CSR_ADDR_WIDTH.W)   // 统计自CPU复位以来共运行了多少个周期，周期计数器的低32位，mcycle寄存器的只读副本。
  val CSR_TIME        = 0xc01.U(CSR_ADDR_WIDTH.W)   // 统计自CPU复位以来共运行了多少时间，低32位，驱动time计数器是已知的固定频率的时钟， 	mtime寄存器的只读副本。
  val CSR_INSTRET     = 0xc02.U(CSR_ADDR_WIDTH.W)   // 统计自CPU复位以来共运行了多少条指令，minstret寄存器的只读副本。
  val CSR_CYCLEH      = 0xc80.U(CSR_ADDR_WIDTH.W)   // 以下三个位高位寄存器，寄存器高 32 位
  val CSR_TIMEH       = 0xc81.U(CSR_ADDR_WIDTH.W)
  val CSR_INSTRETH    = 0xc82.U(CSR_ADDR_WIDTH.W)
  // supervisor trap setup
  val CSR_SSTATUS     = 0x100.U(CSR_ADDR_WIDTH.W)
  val CSR_SIE         = 0x104.U(CSR_ADDR_WIDTH.W)
  val CSR_STVEC       = 0x105.U(CSR_ADDR_WIDTH.W)
  val CSR_SCOUNTEREN  = 0x106.U(CSR_ADDR_WIDTH.W)
  // supervisor trap handling
  val CSR_SSCRATCH    = 0x140.U(CSR_ADDR_WIDTH.W)
  val CSR_SEPC        = 0x141.U(CSR_ADDR_WIDTH.W)
  val CSR_SCAUSE      = 0x142.U(CSR_ADDR_WIDTH.W)
  val CSR_STVAL       = 0x143.U(CSR_ADDR_WIDTH.W)
  val CSR_SIP         = 0x144.U(CSR_ADDR_WIDTH.W)
  // supervisor protection and translation
  val CSR_SATP        = 0x180.U(CSR_ADDR_WIDTH.W)
  // machine information registers (read only)
  val CSR_MVENDERID   = 0xf11.U(CSR_ADDR_WIDTH.W)
  val CSR_MARCHID     = 0xf12.U(CSR_ADDR_WIDTH.W)
  val CSR_MIMPID      = 0xf13.U(CSR_ADDR_WIDTH.W)
  val CSR_MHARTID     = 0xf14.U(CSR_ADDR_WIDTH.W)
  // machine trap setup
  val CSR_MSTATUS     = 0x300.U(CSR_ADDR_WIDTH.W)   // 机器模式状态寄存器，Mstatus寄存器中的 MIE 域和 MPIE 域用于反映中断全局使能
  val CSR_MISA        = 0x301.U(CSR_ADDR_WIDTH.W)   // 机器模式指令集架构寄存器
  val CSR_MEDELEG     = 0x302.U(CSR_ADDR_WIDTH.W)
  val CSR_MIDELEG     = 0x303.U(CSR_ADDR_WIDTH.W)
  val CSR_MIE         = 0x304.U(CSR_ADDR_WIDTH.W)   // 机器模式中断使能寄存器，用于控制不同类型中断的局部使能
  val CSR_MTVEC       = 0x305.U(CSR_ADDR_WIDTH.W)   // 机器模式异常入口基地址寄存器，定义进入异常的程序 PC 地址
  val CSR_MCOUNTEREN  = 0x306.U(CSR_ADDR_WIDTH.W)   
  // machine trap handling
  val CSR_MSCRATCH    = 0x340.U(CSR_ADDR_WIDTH.W)
  val CSR_MEPC        = 0x341.U(CSR_ADDR_WIDTH.W)   // 机器模式异常 PC 寄存器，用于保存异常的返回地址
  val CSR_MCAUSE      = 0x342.U(CSR_ADDR_WIDTH.W)   // 机器模式状态寄存器，反映进入异常的原因
  val CSR_MTVAL       = 0x343.U(CSR_ADDR_WIDTH.W)   // 机器模式异常值寄存器，反映进入异常的信息
  val CSR_MIP         = 0x344.U(CSR_ADDR_WIDTH.W)   // 机器模式中断等待寄存器，反映不同类型中断的等待状态
  // machine memory protection
  val CSR_PMPCFG0     = 0x3a0.U(CSR_ADDR_WIDTH.W)
  val CSR_PMPCFG1     = 0x3a1.U(CSR_ADDR_WIDTH.W)
  val CSR_PMPCFG2     = 0x3a2.U(CSR_ADDR_WIDTH.W)
  val CSR_PMPCFG3     = 0x3a3.U(CSR_ADDR_WIDTH.W)
  val CSR_PMPADDR0    = 0x3b0.U(CSR_ADDR_WIDTH.W)
  val CSR_PMPADDR1    = 0x3b1.U(CSR_ADDR_WIDTH.W)
  val CSR_PMPADDR2    = 0x3b2.U(CSR_ADDR_WIDTH.W)
  val CSR_PMPADDR3    = 0x3b3.U(CSR_ADDR_WIDTH.W)
  val CSR_PMPADDR4    = 0x3b4.U(CSR_ADDR_WIDTH.W)
  val CSR_PMPADDR5    = 0x3b5.U(CSR_ADDR_WIDTH.W)
  val CSR_PMPADDR6    = 0x3b6.U(CSR_ADDR_WIDTH.W)
  val CSR_PMPADDR7    = 0x3b7.U(CSR_ADDR_WIDTH.W)
  val CSR_PMPADDR8    = 0x3b8.U(CSR_ADDR_WIDTH.W)
  val CSR_PMPADDR9    = 0x3b9.U(CSR_ADDR_WIDTH.W)
  val CSR_PMPADDR10   = 0x3ba.U(CSR_ADDR_WIDTH.W)
  val CSR_PMPADDR11   = 0x3bb.U(CSR_ADDR_WIDTH.W)
  val CSR_PMPADDR12   = 0x3bc.U(CSR_ADDR_WIDTH.W)
  val CSR_PMPADDR13   = 0x3bd.U(CSR_ADDR_WIDTH.W)
  val CSR_PMPADDR14   = 0x3be.U(CSR_ADDR_WIDTH.W)
  val CSR_PMPADDR15   = 0x3bf.U(CSR_ADDR_WIDTH.W)
  // machine counters
  val CSR_MCYCLE      = 0xb00.U(CSR_ADDR_WIDTH.W)
  val CSR_MINSTRET    = 0xb02.U(CSR_ADDR_WIDTH.W)
  val CSR_MCYCLEH     = 0xb80.U(CSR_ADDR_WIDTH.W)
  val CSR_MINSTRETH   = 0xb82.U(CSR_ADDR_WIDTH.W)
  // machine counter setup
  val CSR_MCOUNTINHIBIT = 0x320.U(CSR_ADDR_WIDTH.W)
}
