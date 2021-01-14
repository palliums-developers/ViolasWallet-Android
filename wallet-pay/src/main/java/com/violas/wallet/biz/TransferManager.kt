package com.violas.wallet.biz

import android.annotation.SuppressLint
import android.content.Context
import com.palliums.utils.getString
import com.palliums.violas.error.ViolasException
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.biz.btc.TransactionManager
import com.violas.wallet.common.Vm
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
class ToTheirException : RuntimeException(getString(R.string.hint_to_their_error))
class WrongPasswordException : RuntimeException(getString(R.string.hint_password_error))
class AddressFaultException : RuntimeException(getString(R.string.hint_address_error))
class TransferUnknownException : RuntimeException(getString(R.string.hint_transfer_failed))
class LackOfBalanceException :
    RuntimeException(getString(R.string.hint_insufficient_or_trading_fees_are_confirmed))

class AccountNoActivationException :
    RuntimeException(getString(R.string.hint_transfer_account_no_activation))

class ReceiveAccountNoActivationException :
    RuntimeException(getString(R.string.hint_transfer_receive_account_no_activation))

class AccountNoControlException :
    RuntimeException(getString(R.string.hint_transfer_account_no_control))

class NodeNotResponseException :
    RuntimeException(getString(R.string.hint_transfer_account_no_control))


class TransferManager {
    private fun convertViolasTransferException(e: Exception): Exception {
        e.printStackTrace()
        return when (e) {
            is ViolasException.LackOfBalanceException -> {
                LackOfBalanceException()
            }
            is ViolasException.AccountNoActivation -> {
                AccountNoActivationException()
            }
            is ViolasException.AccountNoControl -> {
                AccountNoControlException()
            }
            is ViolasException.ReceiveAccountNoActivation -> {
                ReceiveAccountNoActivationException()
            }
            is ViolasException.NodeResponseException -> {
                NodeNotResponseException()
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
                AccountNoActivationException()
            }
            is LibraException.AccountNoControl -> {
                AccountNoControlException()
            }
            is LibraException.ReceiveAccountNoActivation -> {
                ReceiveAccountNoActivationException()
            }
            is LibraException.NodeResponseException -> {
                NodeNotResponseException()
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
            throw RuntimeException(getString(R.string.hint_please_input_address))
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
            throw RuntimeException(getString(R.string.hint_please_input_amount))
        }
        if (amount <= 0) {
            throw RuntimeException(getString(R.string.hint_please_input_amount))
        }
    }

    @Throws(AddressFaultException::class, WrongPasswordException::class)
    suspend fun transfer(
        context: Context,
        receiverAddress: String,
        receiverSubAddress: String?,
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
            CoinTypes.Libra.coinType() -> {
                transferLibra(
                    context,
                    receiverAddress,
                    receiverSubAddress,
                    amount,
                    privateKey,
                    accountDO,
                    tokenId,
                    success,
                    error
                )
            }
            CoinTypes.Violas.coinType() -> {
                transferViolas(
                    context,
                    receiverAddress,
                    amount,
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
        address: String,
        amount: Double,
        privateKey: ByteArray,
        accountDO: AccountDO,
        progress: Int,
        success: (String) -> Unit,
        error: (Throwable) -> Unit
    ) {
        transactionManager.checkBalance(amount, 1, progress)
            .flatMap {
                if (it) {
                    transactionManager.obtainTransaction(
                        privateKey,
                        accountDO.publicKey.hexToBytes(),
                        it,
                        address,
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

    private suspend fun transferLibra(
        context: Context,
        address: String,
        subAddress: String?,
        amount: Double,
        decryptPrivateKey: ByteArray,
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

            val transactionMetadata = if (subAddress.isNullOrBlank()) {
                null
            } else {
                TransactionMetadata.createGeneralMetadataToSubAddress(SubAddress(subAddress))
            }
            val metadata = transactionMetadata?.metadata ?: byteArrayOf()
            val metadataSignature = transactionMetadata?.signatureMessage ?: byteArrayOf()

            DataRepository.getLibraRpcService().sendTransaction(
                payload = TransactionPayload.optionTransactionPayload(
                    context,
                    address,
                    (amount * 1000000L).toLong(),
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
                account = Account(Ed25519KeyPair(decryptPrivateKey)),
                gasCurrencyCode = token.module,
                chainId = Vm.LibraChainId
            )
            success.invoke("")
        } catch (e: Exception) {
            error.invoke(convertLibraTransferException(e))
        }
    }

    private suspend fun transferViolas(
        context: Context,
        address: String,
        amount: Double,
        decryptPrivateKey: ByteArray,
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
            DataRepository.getViolasChainRpcService().sendViolasToken(
                context,
                org.palliums.violascore.wallet.Account(
                    org.palliums.violascore.crypto.Ed25519KeyPair(decryptPrivateKey)
                ),
                address,
                (amount * 1000000L).toLong(),
                org.palliums.violascore.transaction.storage.TypeTag.newStructTag(
                    org.palliums.violascore.transaction.storage.StructTag(
                        org.palliums.violascore.transaction.AccountAddress(token.address.hexStringToByteArray()),
                        token.module,
                        token.name,
                        arrayListOf()
                    )
                ),
                token.module, chainId = Vm.ViolasChainId
            )
            success.invoke("")
        } catch (e: Exception) {
            error.invoke(convertViolasTransferException(e))
        }
    }

    companion object {
        fun checkAddress(address: String, coinNumber: Int): Boolean {
            when (coinNumber) {
                CoinTypes.Libra.coinType() -> {
                    if (!validationLibraAddress(address)) {
                        return false
                    }
                }
                CoinTypes.Violas.coinType() -> {
                    if (!validationViolasAddress(address)) {
                        return false
                    }
                }
                CoinTypes.Bitcoin.coinType(),
                CoinTypes.BitcoinTest.coinType() -> {
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