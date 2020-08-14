package com.violas.wallet.walletconnect.walletConnectMessageHandler

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.quincysx.crypto.CoinTypes
import com.quincysx.crypto.utils.Base64
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.walletconnect.libraTransferDataHandler.LibraTransferDecodeEngine
import com.violas.walletconnect.jsonrpc.JsonRpcError
import com.violas.walletconnect.models.violasprivate.WCBitcoinSendTransaction
import com.violas.walletconnect.models.violasprivate.WCLibraSendTransaction
import org.palliums.libracore.serialization.hexToBytes
import org.palliums.libracore.serialization.toHex
import org.palliums.libracore.transaction.AccountAddress
import org.palliums.libracore.transaction.TransactionArgument
import org.palliums.libracore.transaction.TransactionPayload
import org.palliums.libracore.transaction.lbrStructTagType
import org.palliums.libracore.transaction.storage.StructTag
import org.palliums.libracore.transaction.storage.TypeTag

class BitcoinSendTransactionMessageHandler(private val iWalletConnectMessage: IWalletConnectMessage) :
    MessageHandler(iWalletConnectMessage) {
    private val mAccountStorage by lazy { DataRepository.getAccountStorage() }
    private val mLibraService by lazy { DataRepository.getLibraService() }

    override suspend fun handler(
        requestID: Long,
        tx: Any
    ): TransactionSwapVo? {
        tx as WCBitcoinSendTransaction
        val account = mAccountStorage.findByCoinTypeAndCoinAddress(
            if (Vm.TestNet) {
                CoinTypes.BitcoinTest.coinType()
            } else {
                CoinTypes.Bitcoin.coinType()
            },
            tx.from
        )

        if (account == null) {
            sendInvalidParameterErrorMessage(requestID, "Account does not exist.")
            return null
        }

        val amount = tx.amount
        val from = tx.from
        val changeAddress = tx.changeAddress
        val payeeAddress = tx.payeeAddress
        val script = tx.script

        Log.e("WalletConnect", Gson().toJson(tx))

        val data = script

        val transferBitcoinDataType = TransferBitcoinDataType(
            from, payeeAddress, amount, changeAddress, data ?: ""
        )

        return TransactionSwapVo(
            requestID,
            "",
            false,
            account.id,
            if (Vm.TestNet) {
                CoinTypes.Bitcoin
            } else {
                CoinTypes.BitcoinTest
            },
            TransactionDataType.BITCOIN_TRANSFER.value,
            Gson().toJson(transferBitcoinDataType)
        )
    }
}