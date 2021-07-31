package core

import chisel3._
import chisel3.util._

import io._
import consts.Control._
import consts.ExceptType._
import consts.CsrOp._
import consts.Instructions.NOP
import consts.Parameters.{ADDR_WIDTH, ADDR_ALIGN_WIDTH, INST_WIDTH}
import consts.MduOp.MDU_NOP
import consts.LsuOp.LSU_NOP

class Decoder extends Module {
  val io = IO(new Bundle {
    // from fetch stage
    val fetch   = Input(new FetchIO)
    // from ROM
    val inst    = Input(UInt(INST_WIDTH.W))
    // pipeline control
    val stallId = Input(Bool())
    val flushIf = Output(Bool())
    val flushPc = Output(UInt(ADDR_WIDTH.W))
    // regfile read channels
    val read1   = new RegReadIO
    val read2   = new RegReadIO
    // branch information
    val branch  = Output(new BranchInfoIO)
    // to next stage
    val decoder = Output(new DecoderIO)
  })

  // fetch instruction data from ROM
  // TODO: a little bit tricky and ugly, fix it?
  val stallDelay  = RegNext(io.stallId)                 // RegNext()把输入延迟一个周期
  val lastInst    = Reg(UInt(INST_WIDTH.W))
  val inst        = Mux(!io.fetch.valid, NOP,           // 如果指令无效，则 inst = NOP；如果暂停，则 inst = lastInst；否则等于输入指令
                    Mux(stallDelay, lastInst, io.inst))
  when (!stallDelay) { lastInst := io.inst }

  // regfile addresses
  val rd  = inst(11, 7)         // 目标寄存器 ，7~11 为
  val rs1 = inst(19, 15)        // 两个源寄存器，15~19、20~24
  val rs2 = inst(24, 20)

  // immediate
  val immI  = inst(31, 20)      // I 型指令（短立即数和访存指令），立即数 20~31位
  val immS  = Cat(inst(31, 25), inst(11, 7))      // S 型指令（访存store的指令）立即数，立即数为7~11位，25~31位。
  val immB  = Cat(inst(31), inst(7), inst(30, 25), inst(11, 8), 0.U(1.W)) // B型指令（条件跳转指令）立即数
  val immU  = Cat(inst(31, 12), 0.U(12.W))                                // U型指令（长立即数）
  val immJ  = Cat(inst(31), inst(19, 12), inst(20), inst(30, 21), 0.U(1.W)) // J型指令（无条件跳转）

  // operand generator
  def generateOpr(oprSel: UInt) =
      MuxLookup(oprSel, 0.S, Seq(
        OPR_REG1  -> io.read1.data.asSInt,
        OPR_REG2  -> io.read2.data.asSInt,
        OPR_IMMI  -> immI.asSInt,
        OPR_IMMS  -> immS.asSInt,
        OPR_IMMU  -> immU.asSInt,
        OPR_IMMR  -> rs2.zext,
        OPR_PC    -> io.fetch.pc.asSInt,
        OPR_4     -> 4.S,
      ))

  // control signals，译码关键模块。通过指令类型来得到信号，就是单周期CPU的if-else语句部分
  val ((regEn1: Bool) :: (regEn2: Bool) :: (regWen: Bool) ::
       aluSrc1 :: aluSrc2 :: aluOp :: branchFlag :: lsuOp :: csrOp ::
       mduOp :: excType :: Nil) = ListLookup(inst, DEFAULT, TABLE)      // DEFAULT、TABLE 在src/main/scala/consts/Control.scala
  
  // branch taken/target
  val isBranch    = branchFlag =/= BR_N         // 不相等，是BR_N，isBranch = 0，表示不是条件跳转指令
  val isJump      = branchFlag === BR_AL        // 相等
  val targetJAL   = (io.fetch.pc.asSInt + immJ.asSInt).asUInt   // jal指令，newpc = pc + immJ
  val sumR1immI   = io.read1.data.asSInt + immI.asSInt          // 寄存器数据与立即数相加
  val targetJALR  = Cat(sumR1immI(ADDR_WIDTH - 1, 1), 0.U)
  val targetJ     = Mux(regEn1, targetJALR, targetJAL)
  val targetB     = (io.fetch.pc.asSInt + immB.asSInt).asUInt   // 条件指令，newpc = pc + immB
  // 条件判断
  val branchTaken = MuxLookup(branchFlag, false.B, Seq(
    BR_AL   -> true.B,
    BR_EQ   -> (io.read1.data === io.read2.data),
    BR_NE   -> (io.read1.data =/= io.read2.data),
    BR_LT   -> (io.read1.data.asSInt < io.read2.data.asSInt),
    BR_GE   -> (io.read1.data.asSInt >= io.read2.data.asSInt),
    BR_LTU  -> (io.read1.data < io.read2.data),
    BR_GEU  -> (io.read1.data >= io.read2.data),
  ))
  val branchTarget  = Mux(branchFlag === BR_N, 0.U,         // 非条件跳转，branchTarget = 0
                      Mux(isJump, targetJ, targetB))
  val branchMiss  = io.fetch.taken =/= branchTaken ||       // 信号，表示预测与实际不符
                    (branchTaken && io.fetch.target =/= branchTarget)
  val flushPc     = Mux(branchTaken, branchTarget, io.fetch.pc + 4.U)   
  val addrFault   = branchTaken &&
                    branchTarget(ADDR_ALIGN_WIDTH - 1, 0) =/= 0.U

  // CSR related signals，CSR相关信号
  val csrActOp  = MuxLookup(csrOp, CSR_NOP, Seq(
    CSR_RW  -> Mux(rd === 0.U, CSR_W, CSR_RW),
    CSR_RS  -> Mux(rs1 === 0.U, CSR_R, CSR_RS),
    CSR_RC  -> Mux(rs1 === 0.U, CSR_R, CSR_RC),
  ))
  val csrData   = Mux(csrActOp === CSR_NOP, 0.U,
                  Mux(regEn1, io.read1.data, rs1))

  // exception signal，异常信号
  val exceptType  = Mux(io.fetch.pageFault, EXC_IPAGE,
                    Mux(addrFault, EXC_IADDR, excType))
  val exceptValue = Mux(addrFault, branchTarget, 0.U)

  // operation related signals
  // cancel all unnecessary operations after fetching illegal instructions，取非法指令后取消所有不必要的操作
  val illegalFetch  = exceptType === EXC_ILLEG || exceptType === EXC_IPAGE
  val mduOperation  = Mux(illegalFetch, MDU_NOP, mduOp)
  val lsuOperation  = Mux(illegalFetch, LSU_NOP, lsuOp)
  val csrOperation  = Mux(illegalFetch, CSR_NOP, csrActOp)

  // pipeline control
  io.flushIf  := !io.stallId && !addrFault && branchMiss
  io.flushPc  := flushPc

  // regfile read signals，寄存器读信号
  io.read1.en   := regEn1
  io.read1.addr := rs1
  io.read2.en   := regEn2
  io.read2.addr := rs2

  // branch information
  io.branch.branch  := isBranch
  io.branch.jump    := isJump
  io.branch.taken   := branchTaken
  io.branch.index   := io.fetch.predIndex
  io.branch.pc      := io.fetch.pc
  io.branch.target  := branchTarget

  // signals to next stage
  io.decoder.aluOp      := aluOp
  io.decoder.opr1       := generateOpr(aluSrc1).asUInt
  io.decoder.opr2       := generateOpr(aluSrc2).asUInt
  io.decoder.mduOp      := mduOperation
  io.decoder.lsuOp      := lsuOperation
  io.decoder.lsuData    := io.read2.data
  io.decoder.regWen     := regWen
  io.decoder.regWaddr   := rd
  io.decoder.csrOp      := csrOperation
  io.decoder.csrAddr    := immI
  io.decoder.csrData    := csrData
  io.decoder.excType    := exceptType
  io.decoder.excValue   := exceptValue
  io.decoder.valid      := io.fetch.valid
  io.decoder.inst       := inst
  io.decoder.currentPc  := io.fetch.pc
}
