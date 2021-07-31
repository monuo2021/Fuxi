// 控制器控制单元，控制各阶段暂停、刷新等
package utils

import chisel3._

import io._
import consts.Parameters._

class PipelineController extends Module {
  val io = IO(new Bundle {
    // stall request from pipeline stages
    val fetch     = Input(Bool())
    val alu       = Input(Bool())
    val mem       = Input(Bool())
    // flush request from pipeline stages
    val decFlush  = Input(Bool())
    val decTarget = Input(UInt(ADDR_WIDTH.W))
    val memFlush  = Input(Bool())
    val memTarget = Input(UInt(ADDR_WIDTH.W))
    // hazard flags
    val load      = Input(Bool())
    val csr       = Input(Bool())
    // exception information
    val except    = Input(new ExceptInfoIO)       // 定义在src/main/scala/io/ExceptInfoIO.scala
    // CSR status
    val csrSepc   = Input(UInt(ADDR_WIDTH.W))
    val csrMepc   = Input(UInt(ADDR_WIDTH.W))
    val csrTvec   = Input(UInt(ADDR_WIDTH.W))
    // stall signals to each mig-stages
    val stallIf   = Output(Bool())
    val stallId   = Output(Bool())
    val stallEx   = Output(Bool())
    val stallMm   = Output(Bool())
    val stallWb   = Output(Bool())
    // flush signals
    val flush     = Output(Bool())
    val flushIf   = Output(Bool())
    val flushPc   = Output(UInt(ADDR_WIDTH.W))
  })

  // stall signals (If -> Wb)，设置各阶段暂停信号，从高位到低位依次为取指阶段、译码阶段、执行阶段、访存阶段、执行阶段
  val stall = Mux(io.mem,           "b11110".U(5.W),            // 多路选择器“Mux(sel, in1, in2)”。
              Mux(io.csr || io.alu, "b11100".U(5.W),            // sel是Bool类型，in1和in2的类型相同，都是Data的任意子类型。
              Mux(io.load,          "b11000".U(5.W),            // 当sel为true.B时，返回in1，否则返回in2。
              Mux(io.fetch,         "b10000".U(5.W), 0.U))))      

  // final exception PC
  val excPc   = Mux(io.except.isSret, io.csrSepc,
                Mux(io.except.isMret, io.csrMepc, io.csrTvec))

  // flush signals
  val excFlush  = io.except.hasTrap       // 是否有异常
  val memFlush  = io.memFlush             // mem中与cache、TLB相关的“异常”
  // avoid CSR RAW hazard before trap handling
  val flushAll  = excFlush || memFlush
  val flushIf   = flushAll || io.decFlush
  val flushPc   = Mux(excFlush, excPc,
                  Mux(memFlush, io.memTarget, io.decTarget))

  // stall signals，根据stall的每一位设置每阶段的暂停信号
  io.stallIf  := stall(4)
  io.stallId  := stall(3)
  io.stallEx  := stall(2)
  io.stallMm  := stall(1)
  io.stallWb  := stall(0)

  // flush signals，设置阶段的冲刷信号
  io.flush    := flushAll
  io.flushIf  := flushIf
  io.flushPc  := flushPc
}
