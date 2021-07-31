// PHT（Pattern History Table，模式历史表），记录各分支指令的分支历史，用于预测跳转方向
package bpu

import chisel3._

import consts.Parameters.{GHR_WIDTH, PHT_SIZE}

// pattern history table
class PHT extends Module {
  val io = IO(new Bundle {
    // branch information (from decoder)
    val lastBranch  = Input(Bool())
    val lastTaken   = Input(Bool())
    val lastIndex   = Input(UInt(GHR_WIDTH.W))
    // index for looking up counter table
    val index       = Input(UInt(GHR_WIDTH.W))
    // prediction result
    val taken       = Output(Bool())
  })

  // 2-bit saturation counters，2bit饱和计数器
  val init      = Seq.fill(PHT_SIZE) { "b10".U(2.W) }     // PHT_SIZE = 1 << GHR_WIDTH。生成序列，第一个接收Int类型的对象，表示序列的元素个数；第二个是传名参数，接收序列的元素。'b10'表示默认为弱不跳转
  val counters  = RegInit(VecInit(init))

  // update counter
  when (io.lastBranch) {    // 如果是分支跳转指令
    when (counters(io.lastIndex) === "b11".U) {     // 如果是强跳转
      when (!io.lastTaken) {                        // 如果预测不跳转，则转变为弱跳转
        counters(io.lastIndex) := counters(io.lastIndex) - 1.U
      }
    } .elsewhen (counters(io.lastIndex) === "b00".U) {    // 如果是强不跳转
      when (io.lastTaken) {                               // 如果预测跳转，则转变为弱不跳转
        counters(io.lastIndex) := counters(io.lastIndex) + 1.U
      }
    } .otherwise {
      // counter === b01（弱不跳转） || counter === b10（弱跳转）
      when (!io.lastTaken) {
        counters(io.lastIndex) := counters(io.lastIndex) - 1.U
      } .otherwise {
        counters(io.lastIndex) := counters(io.lastIndex) + 1.U
      }
    }
  }

  // generate output
  io.taken := counters(io.index)(1)     // 判断是否跳转
}
