// 该文件定义了每个模块的中间模块，如ifid模块、idex模块......
package utils

import chisel3._

import io._

class MidStage[T <: StageIO](sio: T) extends Module {       // StageIO定义在src/main/scala/io/StageIO.scala
  val io = IO(new Bundle {
    // pipeline control
    val flush     = Input(Bool())
    val stallPrev = Input(Bool())
    val stallNext = Input(Bool())
    // IO of previous/next stage
    val prev      = Input(sio)
    val next      = Output(sio)
  })

  // latch stage IO in every cycle
  val ff = RegInit(sio, sio.default())
  when (io.flush || (io.stallPrev && !io.stallNext)) {      // 需要冲刷或者前一阶段暂停后一阶段不暂停
    ff := sio.default()
  } .elsewhen(!io.stallPrev) {
    ff := io.prev
  }

  io.next := ff
}
