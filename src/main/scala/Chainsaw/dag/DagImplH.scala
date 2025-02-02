package Chainsaw.dag

import spinal.core._
import spire.math.{NumberTag, log}

import scala.collection.JavaConverters._
import scala.collection.mutable
import Chainsaw._
import Chainsaw.memory._

object DagImplH {

  def apply(dag: Dag): ChainsawModule = {

    implicit val ref: Dag = dag

    val signalMap = mutable.Map[DagPort, Bits]() // vertex with its output ports

    def getImplemented: Seq[DagVertex] = signalMap.keys.map(_.vertex).toSeq.distinct // vertices already implemented

    def getRemained: Seq[DagVertex] = dag.vertexSet().asScala.toSeq.diff(getImplemented) // vertices not implemented yet

    def getNextStage: Seq[DagVertex] = getRemained.filter(v => v.sources.forall(getImplemented.contains(_))) // vertices ready to be implemented

    def implVertex(target: DagVertex): Unit = {
      if (verbose >= 1) logger.info(s"implementing ${target.vertexName}")
      if (target.isIo) signalMap(target.out(0)) = signalMap(target.sourcePorts.head)
      else {
        val incomingEdges = target.incomingEdges.sortBy(_.inOrder)
        val drivingSignals = target.sourcePorts.map(signalMap)
        val pipelinedSignals = drivingSignals.zip(incomingEdges).map { case (signal, e) =>
          val level = e.weight.toInt
          if (level >= 16 && signal.getBitsWidth >= 32) {
            logger.info(s"big delay conversion -> $level X ${signal.getBitsWidth} bit RAM")
            DelayByRam(signal.getBitsWidth, level).asFunc(Seq(signal)).head
          }
          else signal.d(level)
        }

        // TODO: better resize strategy for implH
        val resizedSignals = pipelinedSignals.zip(target.gen.inputTypes).map { case (bits, info) =>
          if (verbose >= 1 && bits.getBitsWidth != info.bitWidth) logger.info(s"resize: ${bits.getBitsWidth} -> ${info.bitWidth}")
          info.resize(bits).get
        }
        target.gen match {
          case combinational: Combinational =>
            val dataOut = combinational.comb(resizedSignals)
            target.outPorts.zip(dataOut).foreach { case (port, bits) => signalMap(port) = bits }
          case _ =>
            val core = target.gen.getImplH
            core.setDefinitionName(target.gen.name)
            core.dataIn.zip(resizedSignals).foreach { case (port, bits) => port := bits }
            target.outPorts.zip(core.dataOut).foreach { case (port, bits) => signalMap(port) = bits }
        }
      }
    }

    new ChainsawModule(dag) {
      dag.inputs.zip(dataIn).foreach { case (input, bits) => signalMap += input.out(0) -> bits }
      while (getNextStage.nonEmpty) {
        if (verbose >= 1) logger.info(s"implH next stage: ${getNextStage.mkString("\n")}")
        getNextStage.foreach(implVertex)
      }
      dag.outputs.zip(dataOut).foreach { case (output, port) => port := signalMap(output.out(0)) }
    }
  }

}
