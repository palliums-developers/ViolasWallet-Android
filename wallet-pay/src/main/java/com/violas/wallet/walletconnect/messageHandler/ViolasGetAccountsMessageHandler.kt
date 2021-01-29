package com.violas.wallet.walletconnect.messageHandler

import com.google.gson.JsonArray
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.walletconnect.IWalletConnectMessage
import com.violas.wallet.walletconnect.TransactionSwapVo
import com.violas.walletconnect.models.WCMethod
import com.violas.walletconnect.models.violasprivate.WCViolasAccount

class ViolasGetAccountsMessageHandler(private val iMessageHandler: IWalletConnectMessage) : IMessageHandler<JsonArray> {
    private val mAccountStorage by lazy { DataRepository.getAccountStorage() }

    override fun canHandle(method: WCMethod): Boolean {
        return method == WCMethod.GET_ACCOUNTS
    }

    override fun decodeMessage(requestID: Long, param: JsonArray): TransactionSwapVo? {
        val accounts = mAccountStorage.loadAll().map {
            WCViolasAccount(
                coinType = when (it.coinNumber) {
                    CoinTypes.Violas.coinType() -> "violas"
                    CoinTypes.Libra.coinType() -> "libra"
                    else -> "bitcoin"
                },
                address = it.address,
                chainId = when (it.coinNumber) {
                    CoinTypes.Violas.coinType() -> Vm.ViolasChainId
                    CoinTypes.Libra.coinType() -> Vm.LibraChainId
                    else -> 0
                }
            )
        }
        iMessageHandler.sendSuccessMessage(requestID, accounts)
        return null
    }
}