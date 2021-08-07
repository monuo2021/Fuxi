package bus

import chisel3._
import chisel3.util._

import io._
import consts.Parameters._
import consts.Paging._

class PTE extends Bundle {
  val ppn = UInt(PPN_WIDTH.W)       // PPN_WIDTH = 22，包含物理页号，物理地址一部分
  val rsw = UInt(2.W)               // 留给操作系统使用，被硬件忽略
  val d   = Bool()                  // 表示自从上次清除 D 位以来页面是否被弄脏
  val a   = Bool()                  // 表示自从上次 A 位被清除以来，该页面是否被访问过
  val g   = Bool()                  // 表示这个映射是否对所有虚拟地址空间是否有效
  val u   = Bool()                  // 表示该页是否是用户页面
  val x   = Bool()                  // 该页是否可以执行
  val w   = Bool()                  // 该页是否可以写入
  val r   = Bool()                  // 该页是否可以读取
  val v   = Bool()                  // 该页表项的其余部分是否有效

  def asTlbEntry() = {
    val entry = Wire(new TlbEntry)
    entry.ppn := ppn
    entry.d   := d
    entry.a   := a
    entry.u   := u
    entry.x   := x
    entry.w   := w
    entry.r   := r
    entry
  }
}

class MMU(val size: Int, val isInst: Boolean) extends Module {
  val io = IO(new Bundle {
    // TLB control
    val en      = Input(Bool())
    val flush   = Input(Bool())
    val basePpn = Input(UInt(PPN_WIDTH.W))
    val sum     = Input(Bool())
    val smode   = Input(Bool())
    // address lookup channel
    val lookup  = Input(Bool())
    val write   = Input(Bool())
    val vaddr   = Input(UInt(ADDR_WIDTH.W))
    val valid   = Output(Bool())
    val fault   = Output(Bool())
    val paddr   = Output(UInt(ADDR_WIDTH.W))
    // data transfer channel
    val data    = new SramIO(ADDR_WIDTH, DATA_WIDTH)
  })

  // states of finite state machine
  val sIdle :: sAddr :: sRead :: sUpdate :: sFlush :: Nil = Enum(5)
  val state = RegInit(sIdle)

  // some other registers
  val entry = Reg(new TlbEntry)
  val addr  = Reg(UInt(ADDR_WIDTH.W))
  val level = Reg(UInt(LEVEL_WIDTH.W))
  val pte   = io.data.rdata(PTE_WIDTH - 1, 0).asTypeOf(new PTE)

  // TLB
  val tlb = Module(new TLB(size))
  tlb.io.flush  := io.flush
  tlb.io.wen    := state === sUpdate
  tlb.io.vaddr  := io.vaddr
  tlb.io.went   := entry

  // valid flag
  val tlbFlush  = state === sFlush
  val tlbHit    = state === sIdle && tlb.io.valid
  val valid     = !io.en || tlbFlush || tlbHit

  // fault flag
  val daFault = !tlb.io.rent.a || (io.write && !tlb.io.rent.d)
  val rFault  = if (!isInst) !io.write && !tlb.io.rent.r else false.B
  val wFault  = io.write && !tlb.io.rent.w
  val xFault  = if (isInst) !tlb.io.rent.x else false.B
  val uFault  = io.smode && !io.sum && tlb.io.rent.u
  val vmFault = daFault || rFault || wFault || xFault || uFault
  val fault   = io.en && vmFault

  // physical address
  val voffset = io.vaddr(PAGE_OFFSET_WIDTH - 1, 0)
  val tlbAddr = Cat(tlb.io.rent.ppn, voffset)(ADDR_WIDTH - 1, 0)
  val paddr   = Mux(io.en, tlbAddr, io.vaddr)

  def raisePageFault() = {
    state := sUpdate
    entry := 0.U.asTypeOf(new TlbEntry)
  }

  // finite state machine of TLB fill
  switch (state) {
    is (sIdle) {
      when (io.flush) {
        // perform TLB flush
        state := sFlush
      } .elsewhen (io.en) {
        // entry not found in TLB
        when (io.lookup && !tlb.io.valid) {
          state := sAddr
          addr  := getPteAddr(io.basePpn, io.vaddr, (LEVELS - 1).U)
          level := (LEVELS - 1).U
        }
      }
    }
    is (sAddr) {
      // wait until data valid
      when (io.data.valid) {
        state := sRead
      }
    }
    is (sRead) {
      // walk through page table
      when (!pte.v || (!pte.r && pte.w)) {
        // invalid PTE
        raisePageFault()
      } .elsewhen (pte.r || pte.x) {
        when (level > 0.U && getPpnToZero(pte.ppn, level - 1.U).orR) {
          // misaligned superpage
          raisePageFault()
        } .otherwise {
          // target PTE found, write entry to TLB
          state := sUpdate
          entry := pte.asTlbEntry
          // current page is a superpage
          when (level > 0.U) {
            entry.ppn := getSuperPpn(pte.ppn, io.vaddr, level - 1.U)
          }
        }
      } .elsewhen (level === 0.U) {
        // leaf PTE not found
        raisePageFault()
      } .otherwise {
        // fetch next PTE
        state := sAddr
        addr  := getPteAddr(pte.ppn, io.vaddr, level - 1.U)
        level := level - 1.U
      }
    }
    is (sUpdate) {
      // back to idle state
      state := sIdle
    }
    is (sFlush) {
      // back to idle state if there is no flush request
      when (!io.flush) { state := sIdle }
    }
  }

  // data transfer signals
  io.data.en    := state === sAddr
  io.data.wen   := 0.U
  io.data.addr  := addr
  io.data.wdata := 0.U

  // output signals
  io.valid  := valid
  io.fault  := valid && fault
  io.paddr  := paddr
}
