package com.violas.wallet.walletconnect.walletConnectMessageHandler

import android.content.Context
import com.microsoft.appcenter.crashes.Crashes
import com.palliums.exceptions.RequestException
import com.palliums.violas.error.ViolasException
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.ui.walletconnect.WalletConnectActivity
import com.violas.walletconnect.jsonrpc.JsonRpcError
import com.violas.walletconnect.models.violas.WCViolasSendRawTransaction
import com.violas.walletconnect.models.violas.WCViolasSendTransaction
import com.violas.walletconnect.models.violas.WCViolasSignRawTransaction
import com.violas.walletconnect.models.violasprivate.WCViolasAccount
import kotlinx.coroutines.runBlocking

/**
 * 内部处理过的异常
 */
class ProcessedRuntimeException(msg: String = "") : RuntimeException(msg)

class WalletConnectMessageHandler(
    private val context: Context,
    private val iWalletConnectMessage: IWalletConnectMessage
) {
    private val mAccountStorage by lazy { DataRepository.getAccountStorage() }

    private val mTransactionHandlerMessages by lazy {
        hashMapOf(
            Pair(
                WCViolasSignRawTransaction::class.java,
                ViolasSignRawTransactionMessageHandler(iWalletConnectMessage)
            ),
            Pair(
                WCViolasSendTransaction::class.java,
                ViolasSendTransactionMessageHandler(iWalletConnectMessage)
            ),
            Pair(
                WCViolasSendRawTransaction::class.java,
                ViolasSendRawTransactionMessageHandler(iWalletConnectMessage)
            )
        )
    }

    fun convertAndCheckTransaction(requestID: Long, tx: Any) = runBlocking {
        try {
            val transactionSwapVo =
                mTransactionHandlerMessages[tx::class.java]?.handler(requestID, tx)
            transactionSwapVo?.let {
                WalletConnectActivity.startActivity(context, transactionSwapVo)
            }
        } catch (e: ProcessedRuntimeException) {
            e.printStackTrace()
            Crashes.trackError(e)
        } catch (e: NullPointerException) {
            e.printStackTrace()
            sendInvalidParameterErrorMessage(
                requestID,
                "Parameters of the abnormal"
            )
            Crashes.trackError(e)
        } catch (e: ViolasException.AccountNoActivation) {
            e.printStackTrace()
            iWalletConnectMessage.sendErrorMessage(
                requestID,
                JsonRpcError.accountNotActivationError("Please check whether the account is activated.")
            )
            Crashes.trackError(e)
        } catch (e: RequestException) {
            e.printStackTrace()
            iWalletConnectMessage.sendErrorMessage(
                requestID,
                JsonRpcError.serverError("Phone Service Network Error.")
            )
            Crashes.trackError(e)
        } catch (e: Exception) {
            e.printStackTrace()
            sendInvalidParameterErrorMessage(
                requestID,
                "Unknown exception"
            )
            Crashes.trackError(e)
        }
    }

    private fun sendInvalidParameterErrorMessage(id: Long, msg: String) {
        iWalletConnectMessage.sendErrorMessage(
            id,
            JsonRpcError.invalidParams("Invalid Parameter:$msg")
        )
    }

    fun handlerGetAccounts(id: Long) = runBlocking {
        val accounts = mAccountStorage.loadAll().map {
            WCViolasAccount(
                coinType = when (it.coinNumber) {
                    CoinTypes.Violas.coinType() -> "violas"
                    CoinTypes.Libra.coinType() -> "libra"
                    else -> "bitcoin"
                },
                name = "",
                address = it.address,
                walletType = 0
            )
        }
        iWalletConnectMessage.sendSuccessMessage(id, accounts)
    }
}