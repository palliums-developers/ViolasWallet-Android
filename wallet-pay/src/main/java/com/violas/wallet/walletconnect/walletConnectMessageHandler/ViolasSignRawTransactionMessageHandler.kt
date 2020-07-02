package com.violas.wallet.walletconnect.walletConnectMessageHandler

import com.quincysx.crypto.CoinTypes
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.walletconnect.transferDataHandler.TransferDecodeEngine
import com.violas.walletconnect.extensions.hexStringToByteArray
import com.violas.walletconnect.jsonrpc.JsonRpcError
import com.violas.walletconnect.models.violas.WCViolasSignRawTransaction
import org.palliums.violascore.serialization.LCSInputStream
import org.palliums.violascore.serialization.toHex
import org.palliums.violascore.transaction.RawTransaction

class ViolasSignRawTransactionMessageHandler(private val iWalletConnectMessage: IWalletConnectMessage) :
    MessageHandler(iWalletConnectMessage) {
    private val mAccountStorage by lazy { DataRepository.getAccountStorage() }

    override suspend fun handler(
        requestID: Long,
        tx: Any
    ): TransactionSwapVo? {
        tx as WCViolasSignRawTransaction
        val account = mAccountStorage.findByCoinTypeAndCoinAddress(
            CoinTypes.Violas.coinType(),
            tx.address
        )

        if (account == null) {
            sendInvalidParameterErrorMessage(requestID, "Account does not exist.")
            return null
        }

        val rawTransaction =
            RawTransaction.decode(LCSInputStream(tx.message.hexStringToByteArray()))

        val decode = try {
            TransferDecodeEngine(rawTransaction).decode()
        } catch (e: ProcessedRuntimeException) {
            iWalletConnectMessage.sendErrorMessage(
                requestID,
                JsonRpcError.invalidParams("Invalid Parameter:${e.message}")
            )
            throw ProcessedRuntimeException()
        }

        return TransactionSwapVo(
            requestID,
            rawTransaction.toByteArray().toHex(),
            false,
            account.id,
            decode.first.value,
            decode.second
        )
    }
}