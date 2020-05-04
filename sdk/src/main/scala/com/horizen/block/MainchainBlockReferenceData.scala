package com.horizen.block

import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonProperty, JsonView}
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.horizen.serialization.{JsonMerklePathOptionSerializer, JsonMerkleRootsSerializer, Views}
import com.horizen.transaction.{MC2SCAggregatedTransaction, MC2SCAggregatedTransactionSerializer}
import com.horizen.utils.{ByteArrayWrapper, MerklePath}
import scorex.core.serialization.{BytesSerializable, ScorexSerializer}
import scorex.util.serialization.{Reader, Writer}

import scala.collection.mutable

@JsonView(Array(classOf[Views.Default]))
@JsonIgnoreProperties(Array("headerHash"))
case class MainchainBlockReferenceData(
                                   headerHash: Array[Byte],
                                   sidechainRelatedAggregatedTransaction: Option[MC2SCAggregatedTransaction],
                                   @JsonSerialize(using = classOf[JsonMerklePathOptionSerializer])
                                   mproof: Option[MerklePath],
                                   proofOfNoData: (Option[NeighbourProof], Option[NeighbourProof]),
                                   backwardTransferCertificate: Option[MainchainBackwardTransferCertificate]
                                 ) extends BytesSerializable {
  override type M = MainchainBlockReferenceData

  override def serializer: ScorexSerializer[MainchainBlockReferenceData] = MainchainBlockReferenceDataSerializer

  override def hashCode(): Int = java.util.Arrays.hashCode(headerHash)

  override def equals(obj: Any): Boolean = {
    obj match {
      case data: MainchainBlockReferenceData => bytes.sameElements(data.bytes)
      case _ => false
    }
  }
}


object MainchainBlockReferenceDataSerializer extends ScorexSerializer[MainchainBlockReferenceData] {
  val HASH_BYTES_LENGTH: Int = 32

  override def serialize(obj: MainchainBlockReferenceData, w: Writer): Unit = {
    w.putBytes(obj.headerHash)

    obj.sidechainRelatedAggregatedTransaction match {
      case Some(tx) =>
        w.putInt(tx.bytes().length)
        w.putBytes(tx.bytes())
      case _ =>
        w.putInt(0)
    }

    obj.mproof match {
      case Some(mp) =>
        w.putInt(mp.bytes().length)
        w.putBytes(mp.bytes())
      case None =>
        w.putInt(0)
    }

    obj.proofOfNoData._1 match {
      case Some(p) =>
        val pb = NeighbourProofSerializer.toBytes(p)
        w.putInt(pb.length)
        w.putBytes(pb)
      case None =>
        w.putInt(0)
    }

    obj.proofOfNoData._2 match {
      case Some(p) =>
        val pb = NeighbourProofSerializer.toBytes(p)
        w.putInt(pb.length)
        w.putBytes(pb)
      case None =>
        w.putInt(0)
    }

    obj.backwardTransferCertificate match {
      case Some(certificate) =>
        val cb = MainchainBackwardTransferCertificateSerializer.toBytes(certificate)
        w.putInt(cb.length)
        w.putBytes(cb)
      case _ => w.putInt(0)
    }

  }

  override def parse(r: Reader): MainchainBlockReferenceData = {
    val headerHash: Array[Byte] = r.getBytes(HASH_BYTES_LENGTH)

    val mc2scAggregatedTransactionSize: Int = r.getInt()

    val mc2scTx: Option[MC2SCAggregatedTransaction] = {
      if (mc2scAggregatedTransactionSize > 0)
        Some(MC2SCAggregatedTransactionSerializer.getSerializer.parseBytes(r.getBytes(mc2scAggregatedTransactionSize)))
      else
        None
    }

    val mproofSize: Int = r.getInt()

    val mproof: Option[MerklePath] = {
      if (mproofSize > 0)
        Some(MerklePath.parseBytes(r.getBytes(mproofSize)))
      else
        None
    }

    val leftNeighbourSize: Int = r.getInt()

    val leftNeighbour: Option[NeighbourProof] = {
      if (leftNeighbourSize > 0)
        Some(NeighbourProofSerializer.parseBytes(r.getBytes(leftNeighbourSize)))
      else
        None
    }

    val rightNeighbourSize: Int = r.getInt()

    val rightNeighbour: Option[NeighbourProof] = {
      if (rightNeighbourSize > 0)
        Some(NeighbourProofSerializer.parseBytes(r.getBytes(rightNeighbourSize)))
      else
        None
    }

    val certificateSize: Int = r.getInt()

    val certificate: Option[MainchainBackwardTransferCertificate] = {
      if (certificateSize > 0)
        Some(MainchainBackwardTransferCertificateSerializer.parseBytes(r.getBytes(certificateSize)))
      else
        None
    }


    MainchainBlockReferenceData(headerHash, mc2scTx, mproof, (leftNeighbour, rightNeighbour), certificate)
  }
}