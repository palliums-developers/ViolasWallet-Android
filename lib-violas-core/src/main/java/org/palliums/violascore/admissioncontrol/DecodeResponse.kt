package org.palliums.violascore.admissioncontrol

import org.palliums.violascore.admissioncontrol.DecodeHelper.readAccountStates
import org.palliums.violascore.admissioncontrol.DecodeHelper.readSignedTransactionWithProof
import types.GetWithProof


object DecodeResponse {
    fun decode(response: GetWithProof.UpdateToLatestLedgerResponse): UpdateToLatestLedgerResultBean {
        val accountStates = ArrayList<AccountStateBean>()
        val accountTransactionsBySequenceNumber = ArrayList<SignedTransactionWithProofBean>()

        response.responseItemsList.forEach { responseItem ->
            accountStates.addAll(readAccountStates(responseItem.getGetAccountStateResponse()))

            accountTransactionsBySequenceNumber.add(
                readSignedTransactionWithProof(responseItem.getGetAccountTransactionBySequenceNumberResponse())
            )
        }

        return UpdateToLatestLedgerResultBean(
            accountStates,
            accountTransactionsBySequenceNumber
        )
    }
}