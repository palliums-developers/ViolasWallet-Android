package com.violas.wallet.walletconnect.walletConnectMessageHandler

import com.violas.wallet.repository.DataRepository
import com.violas.walletconnect.models.violas.WCViolasSendRawTransaction

class ViolasSendRawTransactionMessageHandler(private val iWalletConnectMessage: IWalletConnectMessage) :
    MessageHandler(iWalletConnectMessage) {
    private val mViolasService by lazy { DataRepository.getViolasService() }

    override suspend fun handler(
        requestID: Long,
        tx: Any
    ): TransactionSwapVo? {
        tx as WCViolasSendRawTransaction

        return TransactionSwapVo(
            requestID,
            tx.tx,
            true,
            -1L,
            TransactionDataType.None.value,
            ""
        )
    }
}