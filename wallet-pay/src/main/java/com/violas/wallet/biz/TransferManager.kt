package com.violas.wallet.biz

import android.annotation.SuppressLint
import android.content.Context
import com.palliums.content.ContextProvider.getContext
import com.palliums.utils.getString
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.biz.btc.TransactionManager
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.bitcoinChainApi.request.BitcoinChainApi
import com.violas.wallet.utils.validationBTCAddress
import com.violas.wallet.utils.validationLibraAddress
import org.palliums.libracore.serialization.hexToBytes
import org.palliums.libracore.serialization.toHex
import org.palliums.libracore.wallet.KeyPair
import org.palliums.violascore.wallet.Account

class ToTheirException : RuntimeException(getString(R.string.hint_to_their_error))
class WrongPasswordException : RuntimeException(getString(R.string.hint_password_error))
class AddressFaultException : RuntimeException(getString(R.string.hint_address_error))
class TransferUnknownException : RuntimeException(getString(R.string.hint_transfer_failed))
class LackOfBalanceException :
    RuntimeException(getString(R.string.hint_insufficient_or_trading_fees_are_confirmed))

class TransferManager {
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
    fun transfer(
        context: Context,
        address: String,
        amountStr: String,
        password: ByteArray,
        account: AccountDO,
        progress: Int,
        token: Boolean = false,
        tokenId: Long = 0,
        success: (String) -> Unit,
        error: (Throwable) -> Unit
    ) {
        val amount = amountStr.trim().toDouble()

        val decryptPrivateKey = SimpleSecurity.instance(getContext())
            .decrypt(password, account.privateKey)
        if (decryptPrivateKey == null) {
            error.invoke(WrongPasswordException())
            return
        }

        when (account.coinNumber) {
            CoinTypes.Libra.coinType() -> {
                transferLibra(
                    context,
                    address,
                    amount,
                    decryptPrivateKey,
                    account,
                    success,
                    error
                )
            }
            CoinTypes.Violas.coinType() -> {
                if (token) {
                    transferViolasToken(
                        context,
                        address,
                        amount,
                        decryptPrivateKey,
                        account,
                        tokenId,
                        success,
                        error
                    )
                } else {
                    transferViolas(
                        context,
                        address,
                        amount,
                        decryptPrivateKey,
                        account,
                        success,
                        error
                    )
                }
            }
        }
    }

    @SuppressLint("CheckResult")
    fun transferBtc(
        transactionManager: TransactionManager,
        address: String,
        amount: Double,
        password: ByteArray,
        account: AccountDO,
        progress: Int,
        success: (String) -> Unit,
        error: (Throwable) -> Unit
    ) {
        val decryptPrivateKey = SimpleSecurity.instance(getContext())
            .decrypt(password, account.privateKey)
        if (decryptPrivateKey == null) {
            error.invoke(WrongPasswordException())
            return
        }

        transactionManager.checkBalance(amount, 1, progress)
            .flatMap {
                if (it) {
                    transactionManager.obtainTransaction(
                        decryptPrivateKey,
                        account.publicKey.hexToBytes(),
                        it,
                        address,
                        account.address
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

    private fun transferViolasToken(
        context: Context,
        address: String,
        amount: Double,
        decryptPrivateKey: ByteArray,
        account: AccountDO,
        tokenId: Long,
        success: (String) -> Unit,
        error: (Throwable) -> Unit
    ) {
        val token = DataRepository.getTokenStorage().findById(tokenId)
        token?.let {
            DataRepository.getViolasService().sendViolasToken(
                context,
                token.tokenAddress,
                Account(
                    KeyPair(decryptPrivateKey)
                ),
                address,
                (amount * 1000000L).toLong()
            ) {
                if (it) {
                    success.invoke("")
                } else {
                    error.invoke(TransferUnknownException())
                }
            }
        }
    }

    private fun transferLibra(
        context: Context,
        address: String,
        amount: Double,
        decryptPrivateKey: ByteArray,
        account: AccountDO,
        success: (String) -> Unit,
        error: (Throwable) -> Unit
    ) {
        DataRepository.getLibraService().sendCoin(
            context,
            org.palliums.libracore.wallet.Account(
                KeyPair(decryptPrivateKey)
            ),
            address,
            (amount * 1000000L).toLong()
        ) {
            if (it) {
                success.invoke("")
            } else {
                error.invoke(TransferUnknownException())
            }
        }
    }

    private fun transferViolas(
        context: Context,
        address: String,
        amount: Double,
        decryptPrivateKey: ByteArray,
        account: AccountDO,
        success: (String) -> Unit,
        error: (Throwable) -> Unit
    ) {
        DataRepository.getViolasService().sendCoin(
            context,
            Account(
                KeyPair(decryptPrivateKey)
            ),
            address,
            (amount * 1000000L).toLong()
        ) {
            if (it) {
                success.invoke("")
            } else {
                error.invoke(TransferUnknownException())
            }
        }
    }

    companion object {
        fun checkAddress(address: String, coinNumber: Int): Boolean {
            when (coinNumber) {
                CoinTypes.Libra.coinType(),
                CoinTypes.Violas.coinType() -> {
                    if (!validationLibraAddress(address)) {
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