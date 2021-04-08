package com.violas.wallet.walletconnect.messageHandle

import android.content.Context
import com.google.gson.JsonArray
import com.microsoft.appcenter.crashes.Crashes
import com.palliums.exceptions.RequestException
import com.palliums.violas.error.ViolasException
import com.violas.wallet.ui.walletconnect.WalletConnectActivity
import com.violas.wallet.walletconnect.IWalletConnectMessage
import com.violas.walletconnect.jsonrpc.JsonRpcError
import com.violas.walletconnect.models.WCMethod

class MessageHandlerChain(
    private val context: Context,
    private val iMessageHandler: IWalletConnectMessage
) {
    private val mHandlerChain = ArrayList<IMessageHandler<JsonArray>>()

    init {
        mHandlerChain.add(ViolasGetAccountsMessageHandler(iMessageHandler))
        mHandlerChain.add(ViolasSendTransactionMessageHandler())
        mHandlerChain.add(DiemSendTransactionMessageHandler())
        mHandlerChain.add(ViolasSendRawTransactionMessageHandler())
        mHandlerChain.add(ViolasSignTransactionMessageHandler())
        mHandlerChain.add(ViolasSignRawTransactionMessageHandler())
        mHandlerChain.add(BitcoinSendTransactionMessageHandler())
    }

    fun tryDecodeMessage(id: Long, method: WCMethod, param: JsonArray): Boolean {
        for (item in mHandlerChain) {
            if (item.canHandle(method)) {
                handler(id, param, item)
                return true
            }
        }
        return false
    }

    private fun handler(
        requestID: Long,
        param: JsonArray,
        item: IMessageHandler<JsonArray>
    ) {
        try {
            val transactionSwapVo = item.decodeMessage(requestID, param)
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
            iMessageHandler.sendErrorMessage(
                requestID,
                JsonRpcError.accountNotActivationError("Please check whether the account is activated.")
            )
            Crashes.trackError(e)
        } catch (e: RequestException) {
            e.printStackTrace()
            iMessageHandler.sendErrorMessage(
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
        iMessageHandler.sendErrorMessage(
            id,
            JsonRpcError.invalidParams("Invalid Parameter:$msg")
        )
    }
}