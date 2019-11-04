package org.palliums.libracore.admissioncontrol

import org.palliums.libracore.serialization.LCSInputStream
import org.palliums.libracore.transaction.SignedTransaction
import types.Proof
import types.TransactionInfoOuterClass
import java.io.IOException

data class AccountStateBean(
    val address: String,
    val balanceInMicroLibras: Long,
    val receivedEvents: EventHandle,
    val sentEvents: EventHandle,
    val sequenceNumber: Long,
    val delegatedKeyRotationCapability: Boolean,
    val delegatedWithdrawalCapability: Boolean
)

data class EventHandle(
    val key: ByteArray,
    val count: Long
)

class EventPathBean(val suffix: String, val tag: Byte, val accountResourcePath: ByteArray) {
    enum class EventType {
        SEND_LIBRA, RECEIVE_LIBRA
    }

    var TAG_CODE: Byte = 0
    var TAG_RESOURCE: Byte = 1

    var SUFFIX_SENT = "/sent_events_count/"
    var SUFFIX_RECEIVED = "/received_events_count/"

    fun getEventType(): EventType {
        return if (suffix == SUFFIX_SENT) EventType.SEND_LIBRA else EventType.RECEIVE_LIBRA
    }
}

data class PaymentEventBean(
    val address: ByteArray,
    val amount: Long,
    val key: ByteArray,
    val sequenceNumber: Long
) {
    companion object {
        fun deserialize(event: types.Events.Event): PaymentEventBean {
            val eventData = event.eventData.toByteArray()
            try {
                LCSInputStream(eventData).use { eventDataStream ->
                    return PaymentEventBean(
                        eventDataStream.readBytes(),
                        eventDataStream.readLong(),
                        event.key.toByteArray(),
                        event.sequenceNumber
                    )
                }
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
    }
}

data class AccumulatorProofBean(
    val bitmap: Long,
    val nonDefaultSiblings: ByteArray
) {
    companion object {
        fun decode(toByteArray: ByteArray): AccumulatorProofBean {
            LCSInputStream(toByteArray).use {
                return AccumulatorProofBean(
                    it.readLong(),
                    it.readBytes()
                )
            }
        }
    }
}

data class SignedTransactionProofBean(
    val ledgerInfoToTransactionInfoProof: Proof.AccumulatorProof,
    val transactionInfo: TransactionInfoOuterClass.TransactionInfo
)

data class SignedTransactionWithProofBean(
    val version: Long,
    val signedTransaction: SignedTransaction?,
    val proof: SignedTransactionProofBean?,
    val events: List<PaymentEventBean>
)

data class UpdateToLatestLedgerResultBean(
    val accountStates: List<AccountStateBean>,
    val accountTransactionsBySequenceNumber: List<SignedTransactionWithProofBean>
)