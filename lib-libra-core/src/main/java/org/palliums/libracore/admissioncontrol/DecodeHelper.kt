package org.palliums.libracore.admissioncontrol

import android.util.Log
import org.palliums.libracore.serialization.LCSInputStream
import org.palliums.libracore.serialization.toHex
import org.palliums.libracore.transaction.SignedTransaction
import org.palliums.libracore.utils.HexUtils
import types.GetWithProof.GetAccountTransactionBySequenceNumberResponse
import types.GetWithProof.GetAccountStateResponse

object DecodeHelper {
    /**
     * see https://github.com/libra/libra/blob/testnet/types/src/account_config.rs#L164
     */
    fun readAccountStates(getAccountStateResponse: GetAccountStateResponse): List<AccountStateBean> {
        val accountStates = ArrayList<AccountStateBean>()

        val blobBytes = getAccountStateResponse.accountStateWithProof.blob.blob.toByteArray()

        val inputStream = LCSInputStream(blobBytes)
        val dataSize = inputStream.readIntAsLEB128()

        val states = LinkedHashSet<ByteArray>()

        for (i in 0 until dataSize) {
            val key = inputStream.readBytes()
            val value = inputStream.readBytes()
            states.add(value)
        }

        val iterator = states.iterator()

        while (iterator.hasNext()){
            val state = iterator.next()
            val lcsInputStream = LCSInputStream(state)
            val authenticationKey = lcsInputStream.readBytes()

            val delegatedKeyRotationCapability = lcsInputStream.readBool()
            val delegatedWithdrawalCapability = lcsInputStream.readBool()
            val receivedEventsCount = lcsInputStream.readLong()
            val receivedEvents = EventHandle(lcsInputStream.readBytes(), receivedEventsCount)
            val sentEventsCount = lcsInputStream.readLong()
            val sentEvents = EventHandle(lcsInputStream.readBytes(), sentEventsCount)
            val sequenceNumber = lcsInputStream.readLong()
            val eventGenerator = lcsInputStream.readLong()

            val balanceState = iterator.next()
            val lcsInputStream1 = LCSInputStream(balanceState)
            val balance = lcsInputStream1.readLong()
            accountStates.add(
                AccountStateBean(
                    HexUtils.toHex(authenticationKey),
                    balance,
                    receivedEvents,
                    sentEvents,
                    sequenceNumber,
                    delegatedKeyRotationCapability,
                    delegatedWithdrawalCapability
                )
            )
        }

        return accountStates
    }

    fun readSignedTransactionWithProof(
        getAccountTransactionBySequenceNumberResponse: GetAccountTransactionBySequenceNumberResponse
    ): SignedTransactionWithProofBean {

//        val signedTransactionWithProof =
//            getAccountTransactionBySequenceNumberResponse.signedTransactionWithProof
//
//        val signedTransactionProofBean = SignedTransactionProofBean(
//            signedTransactionWithProof.proof.ledgerInfoToTransactionInfoProof,
//            signedTransactionWithProof.proof.transactionInfo
//        )
//
//        val signedTransaction =
//            SignedTransaction.decode(signedTransactionWithProof.signedTransaction.signedTxn.toByteArray())
//
//        val events = signedTransactionWithProof.events.eventsList
//            .map {
//                PaymentEventBean.deserialize(it)
//            }
//            .toList()

//        return SignedTransactionWithProofBean(
//            signedTransactionWithProof.version,
//            signedTransaction,
//            signedTransactionProofBean,
//            events
//        )
        //Todo
        return SignedTransactionWithProofBean(
            1L,
            null,
            null,
            arrayListOf()
        )
    }
}