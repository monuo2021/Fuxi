// GHR（Global History Register，全局历史寄存器）
// 使用一个k比特的GHR来记录所有最近k条分支指令的历史跳转方向，
// 并使用PHT（Pattern History Table，模式历史表）来记录各分支指令的分支历史。
// 预测时，首先将分支指令的地址和GHR进行异或，再用异或操作的结果来查PHT，该步骤在 BrachPredictor 文件
// 然后根据PHT当前行的分支历史和分支目标地址，对该分支指令的分支跳转方向和分支目标地址进行预测
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
    val ghr     = Output(UInt(GHR_WIDTH.W))       // GHR_WIDTH = 5
  })

  val ghr = Reg(UInt(GHR_WIDTH.W))

  when (io.branch) {                              // 如果是分支跳转指令
    ghr := Cat(ghr(GHR_WIDTH - 2, 0), io.taken)
  }

  io.ghr := ghr
}
