package com.violas.wallet.biz.exchangeMapping.transactionProcessor

import com.palliums.utils.getString
import com.violas.wallet.R
import com.violas.wallet.biz.LackOfBalanceException
import com.violas.wallet.biz.TokenManager
import com.violas.wallet.biz.TransferUnknownException
import com.violas.wallet.biz.btc.TransactionManager
import com.violas.wallet.biz.btc.outputScript.ViolasOutputScript
import com.violas.wallet.biz.exchangeMapping.BTCMappingAccount
import com.violas.wallet.biz.exchangeMapping.MappingAccount
import com.violas.wallet.biz.exchangeMapping.ViolasMappingAccount
import com.violas.wallet.repository.http.bitcoinChainApi.request.BitcoinChainApi
import kotlinx.coroutines.suspendCancellableCoroutine
import org.palliums.libracore.serialization.toHex
import org.palliums.violascore.wallet.KeyPair
import org.palliums.violascore.wallet.Account
import java.math.BigDecimal
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TransactionProcessorBTCTovBTC :
    TransactionProcessor {
    private val mTokenManager by lazy {
        TokenManager()
    }

    override fun dispense(sendAccount: MappingAccount, receiveAccount: MappingAccount): Boolean {
        return sendAccount is BTCMappingAccount
                && sendAccount.isSendAccount()
                && receiveAccount is ViolasMappingAccount
                && receiveAccount.isSendAccount()
    }

    @Throws(Exception::class)
    override suspend fun handle(
        sendAccount: MappingAccount,
        receiveAccount: MappingAccount,
        sendAmount: BigDecimal,
        receiveAddress: String
    ): String {
        val sendAccount = sendAccount as BTCMappingAccount
        val receiveAccount = receiveAccount as ViolasMappingAccount

        val checkTokenRegister = mTokenManager.isPublish(receiveAccount.getAddress().toHex())

        if (!checkTokenRegister) {
            try {
                publishToken(
                    Account(KeyPair(receiveAccount.getPrivateKey()!!))
                )
            } catch (e: Exception) {
                throw RuntimeException(
                    getString(R.string.hint_exchange_error)
                )
            }
        }

        val mTransactionManager: TransactionManager =
            TransactionManager(arrayListOf(sendAccount.getAddress()))
        val checkBalance = mTransactionManager.checkBalance(sendAmount.toDouble(), 3)
        val violasOutputScript =
            ViolasOutputScript()

        if (!checkBalance) {
            throw LackOfBalanceException()
        }

        return suspendCancellableCoroutine { coroutin ->
            val subscribe = mTransactionManager.obtainTransaction(
                sendAccount.getPrivateKey(),
                sendAccount.getPublicKey(),
                checkBalance,
                receiveAddress,
                sendAccount.getAddress(),
                violasOutputScript.requestExchange(
                    receiveAccount.getAddress(),
                    receiveAccount.getAddress()
                )
            ).flatMap {
                try {
                    BitcoinChainApi.get()
                        .pushTx(it.signBytes.toHex())
                } catch (e: Exception) {
                    e.printStackTrace()
                    throw TransferUnknownException()
                }
            }.subscribe({
                coroutin.resume(it)
            }, {
                coroutin.resumeWithException(it)
            })
            coroutin.invokeOnCancellation {
                subscribe.dispose()
            }
        }
    }

    private suspend fun publishToken(mAccount: Account) {
        return mTokenManager.publishToken(mAccount)
    }
}