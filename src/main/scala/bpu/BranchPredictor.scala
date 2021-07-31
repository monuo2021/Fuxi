// 分支预测器
package bpu

import chisel3._

import io.BranchInfoIO
import consts.Parameters._

class BranchPredictor extends Module {
  val io = IO(new Bundle {
    // branch information (from decoder)
    val branchInfo  = Input(new BranchInfoIO)
    // predictor interface
    val lookupPc    = Input(UInt(ADDR_WIDTH.W))
    val predTaken   = Output(Bool())
    val predTarget  = Output(UInt(ADDR_WIDTH.W))
    val predIndex   = Output(UInt(GHR_WIDTH.W))       // GHR_WIDTH = 5
  })

  // instantiate necessary modules
  val ghr = Module(new GHR)
  val pht = Module(new PHT)
  val btb = Module(new BTB)

  // wire GHR
  ghr.io.branch := io.branchInfo.branch
  ghr.io.taken  := io.branchInfo.taken

  // wire PHT
  val index = io.lookupPc(GHR_WIDTH + ADDR_ALIGN_WIDTH - 1, // 在 Fetch 文件，bpu.io.lookupPc := pc
                          ADDR_ALIGN_WIDTH) ^ ghr.io.ghr    // 将分支指令的地址和GHR进行异或，再用异或操作的结果来查PHT
  pht.io.lastBranch := io.branchInfo.branch
  pht.io.lastTaken  := io.branchInfo.taken
  pht.io.lastIndex  := io.branchInfo.index
  pht.io.index      := index

  // wire BTB
  btb.io.branch   := io.branchInfo.branch
  btb.io.jump     := io.branchInfo.jump
  btb.io.pc       := io.branchInfo.pc
  btb.io.target   := io.branchInfo.target
  btb.io.lookupPc := io.lookupPc

  // wire output signals
  io.predTaken  := btb.io.lookupBranch &&
                   (pht.io.taken || btb.io.lookupJump)
  io.predTarget := btb.io.lookupTarget
  io.predIndex  := index
}
