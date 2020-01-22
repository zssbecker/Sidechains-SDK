package com.horizen.consensus

import com.horizen.params.NetworkParams
import scorex.core.block.Block

trait TimeToEpochSlotConverter {
  this: {val params: NetworkParams} =>

  val epochInSeconds: Long = Math.multiplyExact(params.consensusSlotsInEpoch, params.consensusSecondsInSlot) // will throw exception in case of overflow
  val virtualGenesisBlockTimeStamp: Long = params.sidechainGenesisBlockTimestamp - epochInSeconds + params.consensusSecondsInSlot
  require(virtualGenesisBlockTimeStamp > 0)

  def timeStampToEpochNumber(timestamp: Block.Timestamp): ConsensusEpochNumber = intToConsensusEpochNumber(getEpochIndex(timestamp) + 1)

  def timeStampToSlotNumber(timestamp: Block.Timestamp): ConsensusSlotNumber = {
      val secondsFromEpochStart = timestamp - (getEpochIndex(timestamp) * epochInSeconds) - virtualGenesisBlockTimeStamp
      val slotIndex = secondsFromEpochStart / params.consensusSecondsInSlot //integer division here

      intToConsensusSlotNumber(slotIndex.toInt + 1)
  }

  private def getEpochIndex(timestamp: Block.Timestamp): Int = {
    require(timestamp >= params.sidechainGenesisBlockTimestamp)

    val epochIndex = (timestamp - virtualGenesisBlockTimeStamp) / epochInSeconds //integer division here
    epochIndex.toInt
  }
}
