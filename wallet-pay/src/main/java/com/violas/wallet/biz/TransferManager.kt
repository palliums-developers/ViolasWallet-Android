package com.violas.wallet.biz

import android.annotation.SuppressLint
import android.content.Context
import com.palliums.utils.getString
import com.palliums.violas.error.ViolasException
import com.quincysx.crypto.CoinType
import com.violas.wallet.R
import com.violas.wallet.biz.bean.DiemAppToken
import com.violas.wallet.biz.btc.TransactionManager
import com.violas.wallet.biz.transaction.DiemTxnManager
import com.violas.wallet.biz.transaction.ViolasTxnManager
import com.violas.wallet.common.*
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.bitcoinChainApi.request.BitcoinChainApi
import com.violas.wallet.utils.validationBTCAddress
import com.violas.wallet.utils.validationLibraAddress
import com.violas.wallet.utils.validationViolasAddress
import com.violas.walletconnect.extensions.hexStringToByteArray
import org.palliums.libracore.crypto.Ed25519KeyPair
import org.palliums.libracore.http.LibraException
import org.palliums.libracore.transaction.AccountAddress
import org.palliums.libracore.transaction.TransactionMetadata
import org.palliums.libracore.transaction.TransactionPayload
import org.palliums.libracore.transaction.optionTransactionPayload
import org.palliums.libracore.transaction.storage.StructTag
import org.palliums.libracore.transaction.storage.TypeTag
import org.palliums.libracore.wallet.Account
import org.palliums.libracore.wallet.SubAddress
import org.palliums.violascore.serialization.hexToBytes
import org.palliums.violascore.serialization.toHex

class ForcedStopException : RuntimeException("")
class ToTheirException : RuntimeException(getString(R.string.transfer_tips_address_is_myself))
class WrongPasswordException : RuntimeException(getString(R.string.auth_pwd_hint_pwd_error))
class AddressFaultException : RuntimeException(getString(R.string.transfer_tips_address_error))
class TransferUnknownException :
    RuntimeException(getString(R.string.transfer_tips_transfer_failure))

class LackOfBalanceException :
    RuntimeException(getString(R.string.transfer_tips_insufficient_balance_or_assets_unconfirmed))

class SenderAccountNotActivationException :
    RuntimeException(getString(R.string.transfer_tips_account_no_activation))

class SenderAccountNoControlException :
    RuntimeException(getString(R.string.transfer_tips_account_no_control))

class ReceiverAccountNotActivationException :
    RuntimeException(getString(R.string.transfer_tips_receive_account_no_activation))

class ReceiverAccountCurrencyNotAddException(
    val coinType: CoinType,
    val address: String,
    val appToken: DiemAppToken
) : RuntimeException()

class NodeNotResponseException :
    RuntimeException(getString(R.string.transfer_tips_node_not_response))


class TransferManager {
    private fun convertViolasTransferException(e: Exception): Exception {
        e.printStackTrace()
        return when (e) {
            is ViolasException.LackOfBalanceException -> {
                LackOfBalanceException()
            }
            is ViolasException.AccountNoActivation -> {
                SenderAccountNotActivationException()
            }
            is ViolasException.AccountNoControl -> {
                SenderAccountNoControlException()
            }
            is ViolasException.ReceiveAccountNoActivation -> {
                ReceiverAccountNotActivationException()
            }
            is ViolasException.NodeResponseException -> {
                NodeNotResponseException()
            }
            is LackOfBalanceException,
            is NodeNotResponseException,
            is SenderAccountNotActivationException,
            is SenderAccountNoControlException,
            is ReceiverAccountNotActivationException,
            is ReceiverAccountCurrencyNotAddException -> {
                e
            }
            else -> TransferUnknownException()
        }
    }

    private fun convertLibraTransferException(e: Exception): Exception {
        e.printStackTrace()
        return when (e) {
            is LibraException.LackOfBalanceException -> {
                LackOfBalanceException()
            }
            is LibraException.AccountNoActivation -> {
                SenderAccountNotActivationException()
            }
            is LibraException.AccountNoControl -> {
                SenderAccountNoControlException()
            }
            is LibraException.ReceiveAccountNoActivation -> {
                ReceiverAccountNotActivationException()
            }
            is LibraException.NodeResponseException -> {
                NodeNotResponseException()
            }
            is LackOfBalanceException,
            is NodeNotResponseException,
            is SenderAccountNotActivationException,
            is SenderAccountNoControlException,
            is ReceiverAccountNotActivationException,
            is ReceiverAccountCurrencyNotAddException -> {
                e
            }
            else -> TransferUnknownException()
        }
    }

    @Throws(AddressFaultException::class, RuntimeException::class, ToTheirException::class)
    fun checkTransferParam(
        amountStr: String,
        address: String,
        account: AccountDO
    ) {
        if (address.isEmpty()) {
            throw RuntimeException(getString(R.string.transfer_tips_address_empty))
        }

        if (!checkAddress(address, account.coinNumber)) {
            throw AddressFaultException()
        }

        if (account.address == address) {
            throw ToTheirException()
        }

        val amount = try {
            amountStr.trim().toDouble()
        } catch (e: Exception) {
            throw RuntimeException(getString(R.string.transfer_tips_transfer_amount_empty))
        }
        if (amount <= 0) {
            throw RuntimeException(getString(R.string.transfer_tips_transfer_amount_empty))
        }
    }

    @Throws(AddressFaultException::class, WrongPasswordException::class)
    suspend fun transfer(
        context: Context,
        payeeAddress: String,
        payeeSubAddress: String?,
        amountStr: String,
        privateKey: ByteArray,
        accountDO: AccountDO,
        progress: Int,
        token: Boolean = false,
        tokenId: Long = 0,
        success: (String) -> Unit,
        error: (Throwable) -> Unit
    ) {
        val amount = amountStr.trim().toDouble()

        when (accountDO.coinNumber) {
            getDiemCoinType().coinNumber() -> {
                transferDiem(
                    context,
                    payeeAddress,
                    payeeSubAddress,
                    (amount * 1000000L).toLong(),
                    privateKey,
                    accountDO,
                    tokenId,
                    success,
                    error
                )
            }

            getViolasCoinType().coinNumber() -> {
                transferViolas(
                    context,
                    payeeAddress,
                    (amount * 1000000L).toLong(),
                    privateKey,
                    accountDO,
                    tokenId,
                    success,
                    error
                )
            }
        }
    }

    @SuppressLint("CheckResult")
    fun transferBtc(
        transactionManager: TransactionManager,
        payeeAddress: String,
        transferAmount: Double,
        privateKey: ByteArray,
        accountDO: AccountDO,
        progress: Int,
        success: (String) -> Unit,
        error: (Throwable) -> Unit
    ) {
        transactionManager.checkBalance(transferAmount, 1, progress)
            .flatMap {
                if (it) {
                    transactionManager.obtainTransaction(
                        privateKey,
                        accountDO.publicKey.hexToBytes(),
                        it,
                        payeeAddress,
                        accountDO.address
                    )
                } else {
                    throw LackOfBalanceException()
                }
            }
            .flatMap {
                try {
                    BitcoinChainApi.get().pushTx(it.signBytes.toHex())
                } catch (e: Exception) {
                    e.printStackTrace()
                    throw TransferUnknownException()
                }
            }
            .subscribe({
                success.invoke(it)
            }, {
                error.invoke(it)
            }, {

            })
    }

    private suspend fun transferDiem(
        context: Context,
        payeeAddress: String,
        payeeSubAddress: String?,
        transferAmount: Long,
        privateKey: ByteArray,
        account: AccountDO,
        tokenId: Long,
        success: (String) -> Unit,
        error: (Throwable) -> Unit
    ) {
        try {
            val token = DataRepository.getTokenStorage().findById(tokenId)
            if (token == null) {
                error.invoke(LibraException.CurrencyNotExistException())
                return
            }

            val diemRpcService = DataRepository.getDiemRpcService()
            val diemTxnManager = DiemTxnManager()

            // 检查收款人账户
            diemTxnManager.getReceiverAccountState(
                payeeAddress,
                DiemAppToken.convert(token)
            ) {
                diemRpcService.getAccountState(it)
            }

            // 检查付款人账户
            val payerAccount = Account(Ed25519KeyPair(privateKey))
            val payerAccountState =
                diemTxnManager.getSenderAccountState(payerAccount) {
                    diemRpcService.getAccountState(it)
                }

            // 计算gas info
            val gasInfo = diemTxnManager.calculateGasInfo(
                payerAccountState,
                listOf(Pair(token.module, transferAmount))
            )

            val transactionMetadata = if (payeeSubAddress.isNullOrBlank()) {
                null
            } else {
                TransactionMetadata.createGeneralMetadataToSubAddress(SubAddress(payeeSubAddress))
            }
            val metadata = transactionMetadata?.metadata ?: byteArrayOf()
            val metadataSignature = transactionMetadata?.signatureMessage ?: byteArrayOf()

            diemRpcService.sendTransaction(
                TransactionPayload.optionTransactionPayload(
                    context,
                    payeeAddress,
                    transferAmount,
                    metadata,
                    metadataSignature,
                    TypeTag.newStructTag(
                        StructTag(
                            AccountAddress(token.address.hexStringToByteArray()),
                            token.module,
                            token.name,
                            arrayListOf()
                        )
                    )
                ),
                payerAccount,
                sequenceNumber = payerAccountState.sequenceNumber,
                gasCurrencyCode = gasInfo.gasCurrencyCode,
                maxGasAmount = gasInfo.maxGasAmount,
                gasUnitPrice = gasInfo.gasUnitPrice,
                chainId = getDiemChainId()
            )
            success.invoke("")
        } catch (e: Exception) {
            error.invoke(convertLibraTransferException(e))
        }
    }

    private suspend fun transferViolas(
        context: Context,
        payeeAddress: String,
        transferAmount: Long,
        privateKey: ByteArray,
        account: AccountDO,
        tokenId: Long,
        success: (String) -> Unit,
        error: (Throwable) -> Unit
    ) {
        try {
            val token = DataRepository.getTokenStorage().findById(tokenId)
            if (token == null) {
                error.invoke(LibraException.CurrencyNotExistException())
                return
            }

            val violasRpcService = DataRepository.getViolasRpcService()
            val violasTxnManager = ViolasTxnManager()

            // 检查收款人账户
            violasTxnManager.getReceiverAccountState(
                payeeAddress,
                DiemAppToken.convert(token)
            ) {
                violasRpcService.getAccountState(it)
            }

            // 检查付款人账户
            val payerAccount = org.palliums.violascore.wallet.Account(
                org.palliums.violascore.crypto.Ed25519KeyPair(privateKey)
            )
            val payerAccountState =
                violasTxnManager.getSenderAccountState(payerAccount) {
                    violasRpcService.getAccountState(it)
                }

            // 计算gas info
            val gasInfo = violasTxnManager.calculateGasInfo(
                payerAccountState,
                listOf(Pair(token.module, transferAmount))
            )

            violasRpcService.sendCurrency(
                context,
                payerAccount,
                payeeAddress = payeeAddress,
                transferAmount = transferAmount,
                currencyAddress = token.address,
                currencyModule = token.module,
                currencyName = token.name,
                sequenceNumber = payerAccountState.sequenceNumber,
                gasCurrencyCode = gasInfo.gasCurrencyCode,
                maxGasAmount = gasInfo.maxGasAmount,
                gasUnitPrice = gasInfo.gasUnitPrice,
                chainId = getViolasChainId()
            )
            success.invoke("")
        } catch (e: Exception) {
            error.invoke(convertViolasTransferException(e))
        }
    }

    companion object {
        fun checkAddress(address: String, coinNumber: Int): Boolean {
            when (coinNumber) {

                getDiemCoinType().coinNumber() -> {
                    if (!validationLibraAddress(address)) {
                        return false
                    }
                }

                getViolasCoinType().coinNumber() -> {
                    if (!validationViolasAddress(address)) {
                        return false
                    }
                }

                getBitcoinCoinType().coinNumber() -> {
                    if (!validationBTCAddress(address)) {
                        return false
                    }
                }

                else -> {
                    return false
                }
            }
            return true
        }
    }
}