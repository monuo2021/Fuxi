package core

import chisel3._

import io._
import consts.Parameters._
import bpu.BranchPredictor

class Fetch extends Module {
  val io = IO(new Bundle {
    // pipeline control signals，控制信号，由 PipelineController 提供
    val flush     = Input(Bool())     // 冲刷信号
    val stall     = Input(Bool())     // 停止信号
    val stallReq  = Output(Bool())    // 暂停请求
    val flushPc   = Input(UInt(ADDR_WIDTH.W))   // 冲刷后PC
    // ROM interface
    val rom       = new SramIO(ADDR_WIDTH, INST_WIDTH)
    // branch information (from decoder)，分支预测信息
    val branch    = Input(new BranchInfoIO)               // 定义在src/main/scala/io/BranchInfoIO.scala，定义了分支预测器的分支信息
    // to next stage
    val fetch     = Output(new FetchIO)
  })

  // program counter
  val pc = RegInit(RESET_PC)

  // branch predictor
  val bpu = Module(new BranchPredictor)                 // 分支预测器，在src/main/scala/bpu/BranchPredictor.scala
  bpu.io.branchInfo <> io.branch
  bpu.io.lookupPc   := pc

  // update PC，如果有冲刷信号，则 PC = io.flush；有停止信号，PC = PC；否则 PC = 分支预测结果。
  val nextPc = Mux(io.flush, io.flushPc,
               Mux(io.stall, pc,
               Mux(bpu.io.predTaken, bpu.io.predTarget,
                   pc + (INST_WIDTH / 8).U)))
  pc := nextPc

  // pipeline control signals，rom无效时，暂停请求。
  io.stallReq := !io.rom.valid

  // ROM control signals
  io.rom.en     := true.B
  io.rom.wen    := 0.U
  io.rom.addr   := pc
  io.rom.wdata  := 0.U

  // output signals
  io.fetch.valid      := io.rom.valid
  io.fetch.pc         := pc
  io.fetch.taken      := bpu.io.predTaken
  io.fetch.target     := bpu.io.predTarget
  io.fetch.predIndex  := bpu.io.predIndex
  io.fetch.pageFault  := io.rom.fault
}
