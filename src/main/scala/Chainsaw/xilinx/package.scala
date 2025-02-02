package Chainsaw

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.fsm._

import java.io.File
import scala.language.postfixOps

package object xilinx {

  sealed trait XilinxDeviceFamily

  object UltraScale extends XilinxDeviceFamily

  object Series7 extends XilinxDeviceFamily

  sealed trait EdaFlowType

  object SYNTH extends EdaFlowType

  object IMPL extends EdaFlowType

  object BITGEN extends EdaFlowType // generate bitstream

  val xilinxCDConfig = ClockDomainConfig( // recommended by Xilinx UG901
    clockEdge = RISING,
    resetKind = ASYNC,
    resetActiveLevel = HIGH,
    softResetActiveLevel = HIGH,
    clockEnableActiveLevel = HIGH
  )

  // TODO: add Zybo, Nexys4 and their xdc files
  val vu9p = XilinxDevice(UltraScale, "xcvu9p-flga2104-2-i", 800 MHz, None)
  val zcu104 = XilinxDevice(UltraScale, "xczu7ev-ffvc1156-2-e", 200 MHz, None)
  val u250 = XilinxDevice(UltraScale, "XCU250-FIGD2104-2L-E".toLowerCase, 800 MHz, None)
  val u200 = XilinxDevice(UltraScale, "XCU200-FSGD2104-2-E".toLowerCase, 800 MHz, None)
  val kcu1500 = XilinxDevice(UltraScale, "xcku115-flvb2104-2-e", 800 MHz, None)
}
