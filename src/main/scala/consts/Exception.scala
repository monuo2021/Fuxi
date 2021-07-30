package consts

import chisel3._
import chisel3.util.log2Ceil

// exception type，异常类型
object ExceptType {
  val EXC_TYPE_WIDTH = log2Ceil(11)

  val EXC_NONE  = 0.U(EXC_TYPE_WIDTH.W)
  val EXC_ECALL = 1.U(EXC_TYPE_WIDTH.W)   // 机器模式环境调用
  val EXC_EBRK  = 2.U(EXC_TYPE_WIDTH.W)   // 断点
  val EXC_SRET  = 3.U(EXC_TYPE_WIDTH.W)
  val EXC_MRET  = 4.U(EXC_TYPE_WIDTH.W)   
  val EXC_ILLEG = 5.U(EXC_TYPE_WIDTH.W)   // illegal instruction
  val EXC_IPAGE = 6.U(EXC_TYPE_WIDTH.W)   // instruction page fault
  val EXC_IADDR = 7.U(EXC_TYPE_WIDTH.W)   // instruction address misaligned，指令地址非对齐
  val EXC_LOAD  = 8.U(EXC_TYPE_WIDTH.W)   // 读存储器访问错误
  val EXC_STAMO = 9.U(EXC_TYPE_WIDTH.W)   // 写存储器和 AMO 地址/访问非对齐
  val EXC_SPRIV = 10.U(EXC_TYPE_WIDTH.W)  // S-mode instruction
}

// exception cause
object ExceptCause {
  val EXC_CAUSE_WIDTH = 31

  // interruptions
  val EXC_S_SOFT_INT  = 1.U(EXC_CAUSE_WIDTH.W)    // super模式软件中断
  val EXC_M_SOFT_INT  = 3.U(EXC_CAUSE_WIDTH.W)    // 机器模式
  val EXC_S_TIMER_INT = 5.U(EXC_CAUSE_WIDTH.W)    // 时钟中断
  val EXC_M_TIMER_INT = 7.U(EXC_CAUSE_WIDTH.W)
  val EXC_S_EXT_INT   = 9.U(EXC_CAUSE_WIDTH.W)    // 外部中断
  val EXC_M_EXT_INT   = 11.U(EXC_CAUSE_WIDTH.W)

  // exceptions
  val EXC_INST_ADDR   = 0.U(EXC_CAUSE_WIDTH.W)
  val EXC_ILL_INST    = 2.U(EXC_CAUSE_WIDTH.W)
  val EXC_BRK_POINT   = 3.U(EXC_CAUSE_WIDTH.W)
  val EXC_LOAD_ADDR   = 4.U(EXC_CAUSE_WIDTH.W)
  val EXC_STAMO_ADDR  = 6.U(EXC_CAUSE_WIDTH.W)
  val EXC_U_ECALL     = 8.U(EXC_CAUSE_WIDTH.W)
  val EXC_S_ECALL     = 9.U(EXC_CAUSE_WIDTH.W)
  val EXC_M_ECALL     = 11.U(EXC_CAUSE_WIDTH.W)
  val EXC_INST_PAGE   = 12.U(EXC_CAUSE_WIDTH.W)
  val EXC_LOAD_PAGE   = 13.U(EXC_CAUSE_WIDTH.W)
  val EXC_STAMO_PAGE  = 15.U(EXC_CAUSE_WIDTH.W)
}
