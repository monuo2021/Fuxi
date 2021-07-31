// 分支信息
package io

import chisel3._

import consts.Parameters.{GHR_WIDTH, ADDR_WIDTH}

class BranchInfoIO extends Bundle {
  val branch  = Bool()              // last inst is a b/j，判断指令时条件跳转还是无条件跳转
  val jump    = Bool()              // is 'jal' or 'jalr'
  val taken   = Bool()              // is last branch taken，跳转或不跳转
  val index   = UInt(GHR_WIDTH.W)   // last index of PHT，PHT索引
  val pc      = UInt(ADDR_WIDTH.W)  // last instruction PC
  val target  = UInt(ADDR_WIDTH.W)  // last branch target，目标跳转
}
