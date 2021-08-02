package core

import chisel3._
import chisel3.util._

import io._
import lsu._
import consts.Parameters._
import consts.LsuOp._
import lsu.LsuDecode._
import consts.ExceptType._
import consts.ExceptCause._
import consts.CSR._

class Mem extends Module {
  val io = IO(new Bundle {
    // from ALU
    val alu       = Input(new AluIO)
    // pipeline control
    val flush     = Input(Bool())
    val stallReq  = Output(Bool())
    val flushReq  = Output(Bool())
    val flushPc   = Output(UInt(ADDR_WIDTH.W))
    // RAM interface
    val ram       = new SramIO(ADDR_WIDTH, DATA_WIDTH)
    // cache/TLB control
    val flushIc   = Output(Bool())
    val flushDc   = Output(Bool())
    val flushIt   = Output(Bool())
    val flushDt   = Output(Bool())
    // CSR status
    val csrHasInt = Input(Bool())
    val csrBusy   = Input(Bool())
    val csrMode   = Input(UInt(CSR_MODE_WIDTH.W))
    // exclusive monitor check channel
    val excMon    = new ExcMonCheckIO             // src/main/scala/io/ExcMonIO.scala
    // exception information
    val except    = Output(new ExceptInfoIO)      // src/main/scala/io/ExceptInfoIO.scala
    // to next stage
    val mem       = Output(new MemIO)
  })

  // decode load store unit operation
  val ((en: Bool) :: (wen: Bool) :: (load: Bool) :: width ::
       (signed: Bool) :: (setExcMon: Bool) :: (checkExcMon: Bool) ::
       amoOp :: (flushIc: Bool) :: (flushDc: Bool) :: (flushIt: Bool) ::
       (flushDt: Bool) :: Nil) = ListLookup(io.alu.lsuOp, DEFAULT, TABLE)     // DEFAULT、TABLE定义在src/main/scala/lsu/LsuDecode.scala

  // address of memory accessing，多体交叉存储器？sel 为体号，addr为地址
  val addr  = Cat(io.alu.reg.data(ADDR_WIDTH - 1, ADDR_ALIGN_WIDTH),          // io.alu.reg.data是load、store指令运算所得的内存地址
                  0.U(ADDR_ALIGN_WIDTH.W))
  val sel   = io.alu.reg.data(ADDR_ALIGN_WIDTH - 1, 0)

  // AMO execute unit，原子指令执行模块，通过对LS指令译码，获得 AMO 指令（在该文件 44 行进行）。
  val amo = Module(new AmoExecute)      // src/main/scala/lsu/AmoExecute.scala
  amo.io.op       := amoOp
  amo.io.flush    := io.flush
  amo.io.regOpr   := io.alu.lsuData     // 该数据来自 HazardResolver 模块
  amo.io.ramValid := io.ram.valid
  amo.io.ramRdata := io.ram.rdata

  // write enable
  val selWord = "b1111".U((DATA_WIDTH / 8).W)
  val writeEn = MuxLookup(width, 0.U, Seq(
    LS_DATA_BYTE -> ("b1".U((DATA_WIDTH / 8).W) << sel),
    LS_DATA_HALF -> ("b11".U((DATA_WIDTH / 8).W) << sel),
    LS_DATA_WORD -> selWord,
  ))
  val scWen   = Mux(io.excMon.valid, selWord, 0.U)
  val amoWen  = Mux(amo.io.ramWen, selWord, 0.U)
  val ramWen  = Mux(wen, writeEn, Mux(checkExcMon, scWen, amoWen))

  // write data，写数据
  def mapWriteData(i: Int, w: Int) =
      (i * w / 8).U -> (io.alu.lsuData << (i * w))
  val byteSeq = 0 until ADDR_WIDTH /  8 map { i => mapWriteData(i,  8) }
  val halfSeq = 0 until ADDR_WIDTH / 16 map { i => mapWriteData(i, 16) }
  val wordSeq = 0 until ADDR_WIDTH / 32 map { i => mapWriteData(i, 32) }
  val lsuData = MuxLookup(width, 0.U, Seq(
    LS_DATA_BYTE -> MuxLookup(sel, 0.U, byteSeq),
    LS_DATA_HALF -> MuxLookup(sel, 0.U, halfSeq),
    LS_DATA_WORD -> MuxLookup(sel, 0.U, wordSeq),
  ))
  val wdata   = Mux(wen || checkExcMon, lsuData, amo.io.ramWdata)

  // stall request，暂停请求
  val memStall  = Mux(amoOp =/= AMO_OP_NOP, !amo.io.ready,    // amoOp = AMO_OP_NOP，memStall = en && !io.ram.valid
                      en && !io.ram.valid)                    // amoOp = 原子指令，此时 AMO 不是最终状态（要么空闲、要么store）
  val fencStall = flushDc && !io.ram.valid
  val stallReq  = memStall || fencStall || io.csrBusy

  // write back data，写回数据
  val data = Mux(checkExcMon, Mux(io.excMon.valid, 0.U, 1.U),
             Mux(amoOp =/= AMO_OP_NOP, amo.io.regWdata, io.alu.reg.data))     // 该局为关键，判断指令是否是原子指令

  // exclusive monitor clear flag
  val clearEm = wen || checkExcMon || amoOp =/= AMO_OP_NOP

  // exception related signals
  // signals about memory accessing
  val memAddr   = MuxLookup(width, false.B, Seq(
    LS_DATA_BYTE  -> false.B,
    LS_DATA_HALF  -> (sel(0) =/= 0.U),
    LS_DATA_WORD  -> (sel(1, 0) =/= 0.U),
  ))
  val memExcept = memAddr || io.ram.fault
  // signals about instruction exceptions，指令异常信号
  val illgSret  = io.alu.excType === EXC_SRET && io.csrMode === CSR_MODE_U
  val illgMret  = io.alu.excType === EXC_MRET && io.csrMode =/= CSR_MODE_M
  val illgSpriv = io.alu.excType === EXC_SPRIV && io.csrMode === CSR_MODE_U
  val instAddr  = io.alu.excType === EXC_IADDR
  val instPage  = io.alu.excType === EXC_IPAGE
  val instIllg  = io.alu.excType === EXC_ILLEG ||
                  illgSret || illgMret || illgSpriv
  // whether exception occurred，是否发生异常
  val excMem    = io.alu.excType === EXC_LOAD ||
                  io.alu.excType === EXC_STAMO
  val excOther  = io.alu.excType === EXC_ECALL ||
                  io.alu.excType === EXC_EBRK ||
                  io.alu.excType === EXC_SRET ||
                  io.alu.excType === EXC_MRET
  val hasTrap   = instAddr || instIllg || instPage ||
                  (excMem && memExcept) || excOther || io.csrHasInt
  // trap return instructions & interruptions
  val isSret    = io.alu.excType === EXC_SRET && !instIllg
  val isMret    = io.alu.excType === EXC_MRET && !instIllg
  // exception cause
  // NOTE: priority is important
  val cause     = MuxLookup(io.alu.excType, 0.U, Seq(
    EXC_ECALL -> Mux(io.csrMode === CSR_MODE_U, EXC_U_ECALL,
                 Mux(io.csrMode === CSR_MODE_S, EXC_S_ECALL, EXC_M_ECALL)),
    EXC_EBRK  -> EXC_BRK_POINT,
    EXC_LOAD  -> Mux(memAddr, EXC_LOAD_ADDR, EXC_LOAD_PAGE),
    EXC_STAMO -> Mux(memAddr, EXC_STAMO_ADDR, EXC_STAMO_PAGE),
  ))
  val excCause  = Mux(instPage, EXC_INST_PAGE,
                  Mux(instIllg, EXC_ILL_INST,
                  Mux(instAddr, EXC_INST_ADDR, cause)))
  // exception pc & value
  val excPc     = io.alu.currentPc
  val excValue  = Mux(instIllg, io.alu.inst,
                  Mux(instPage, io.alu.currentPc,
                  Mux(memExcept, io.alu.reg.data, io.alu.excValue)))

  // pipeline control
  io.stallReq := stallReq
  io.flushReq := !stallReq && (flushIc || flushIt)
  io.flushPc  := io.alu.currentPc + 4.U

  // RAM control signals
  io.ram.en     := Mux(hasTrap, false.B, en)
  io.ram.wen    := ramWen
  io.ram.addr   := addr
  io.ram.wdata  := wdata

  // cache/TLB control signals
  io.flushIc  := Mux(hasTrap, false.B, flushIc)
  io.flushDc  := Mux(hasTrap, false.B, flushDc)
  io.flushIt  := Mux(hasTrap, false.B, flushIt)
  io.flushDt  := Mux(hasTrap, false.B, flushDt)

  // exclusive monitor check signals
  io.excMon.addr  := addr

  // exception information
  io.except.hasTrap   := !io.csrBusy && io.alu.valid && hasTrap
  io.except.isSret    := isSret
  io.except.isMret    := isMret
  io.except.excCause  := excCause
  io.except.excPc     := excPc
  io.except.excValue  := excValue

  // output signals
  io.mem.reg.en       := io.alu.reg.en
  io.mem.reg.addr     := io.alu.reg.addr
  io.mem.reg.data     := data
  io.mem.reg.load     := load
  io.mem.memSigned    := signed
  io.mem.memSel       := sel
  io.mem.memWidth     := width
  io.mem.csr          <> io.alu.csr
  io.mem.excMon.addr  := addr
  io.mem.excMon.set   := setExcMon
  io.mem.excMon.clear := clearEm
  io.mem.currentPc    := io.alu.currentPc
}
