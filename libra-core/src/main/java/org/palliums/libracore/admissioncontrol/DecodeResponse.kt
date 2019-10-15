package com.smallraw.libardemo.grpcResponse

import org.palliums.libracore.admissioncontrol.AccountStateBean
import org.palliums.libracore.admissioncontrol.DecodeHelper.readAccountStates
import org.palliums.libracore.admissioncontrol.DecodeHelper.readSignedTransactionWithProof
import org.palliums.libracore.admissioncontrol.SignedTransactionWithProofBean
import org.palliums.libracore.admissioncontrol.UpdateToLatestLedgerResultBean
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