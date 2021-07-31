// GHR（Global History Register，全局历史寄存器）
package bpu

import chisel3._
import chisel3.util.Cat

import consts.Parameters.GHR_WIDTH

// global history register，全局历史寄存器
class GHR extends Module {
  val io = IO(new Bundle {
    // branch information
    val branch  = Input(Bool())
    val taken   = Input(Bool())
    // value of GHR
    val ghr     = Output(UInt(GHR_WIDTH.W))
  })

  val ghr = Reg(UInt(GHR_WIDTH.W))

  when (io.branch) {                              // 如果是分支跳转
    ghr := Cat(ghr(GHR_WIDTH - 2, 0), io.taken)
  }

  io.ghr := ghr
}
