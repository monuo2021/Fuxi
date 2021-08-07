// BTB（Branch Target Buffer, 分支目标缓冲器）
package bpu

import chisel3._
import chisel3.util.Cat

import consts.Parameters._

// single line in BTB
class BtbLine extends Bundle {
  val jump    = Bool()
  val pc      = UInt(BTB_PC_WIDTH.W)
  val target  = UInt(BTB_TARGET_WIDTH.W)
}

// branch target buffer
class BTB extends Module {
  val io = IO(new Bundle {
    // branch information (from decoder)
    val branch        = Input(Bool())
    val jump          = Input(Bool())
    val pc            = Input(UInt(ADDR_WIDTH.W))
    val target        = Input(UInt(ADDR_WIDTH.W))
    // BTB lookup interface
    val lookupPc      = Input(UInt(ADDR_WIDTH.W))
    val lookupBranch  = Output(Bool())
    val lookupJump    = Output(Bool())
    val lookupTarget  = Output(UInt(ADDR_WIDTH.W))
  })

  // definitions of BTB lines and valid bits
  val valids  = RegInit(VecInit(Seq.fill(BTB_SIZE) { false.B }))    // BTB_SIZE = 1 << 6
  val lines   = Mem(BTB_SIZE, new BtbLine)

  // branch info for BTB lines，BTB里的历史记录
  val index   = io.pc(BTB_INDEX_WIDTH + ADDR_ALIGN_WIDTH - 1,       // pc的 7~2 位
                      ADDR_ALIGN_WIDTH)
  val linePc  = io.pc(ADDR_WIDTH - 1, BTB_INDEX_WIDTH + ADDR_ALIGN_WIDTH) // pc 31~8 位

  // write to BTB lines，更新BTB记录
  when (io.branch) {                // 如果是跳转指令
    valids(index)       := true.B
    lines(index).jump   := io.jump
    lines(index).pc     := linePc
    lines(index).target := io.target(ADDR_WIDTH - 1, ADDR_ALIGN_WIDTH)
  }

  // signals about BTB lookup
  val lookupIndex = io.lookupPc(BTB_INDEX_WIDTH + ADDR_ALIGN_WIDTH - 1,         // BTB_INDEX_WIDTH = 6
                                ADDR_ALIGN_WIDTH)
  val lookupPcSel = io.lookupPc(ADDR_WIDTH - 1,
                                BTB_INDEX_WIDTH + ADDR_ALIGN_WIDTH)
  val btbHit      = valids(lookupIndex) &&
                    lines(lookupIndex).pc === lookupPcSel

  // BTB lookup，关键步骤
  io.lookupBranch := btbHit           // BTB命中
  io.lookupJump   := Mux(btbHit, lines(lookupIndex).jump, false.B)
  io.lookupTarget := Cat(Mux(btbHit, lines(lookupIndex).target, 0.U),
                         0.U(ADDR_ALIGN_WIDTH.W))
}
