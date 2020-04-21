package com.violas.wallet.biz

import android.content.Context
import com.palliums.content.ContextProvider.getContext
import com.palliums.utils.getString
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.utils.validationBTCAddress
import com.violas.wallet.utils.validationLibraAddress
import com.violas.wallet.utils.validationViolasAddress
import org.palliums.violascore.wallet.Account
import org.palliums.violascore.wallet.KeyPair

class ToTheirException : RuntimeException(getString(R.string.hint_to_their_error))
class WrongPasswordException : RuntimeException(getString(R.string.hint_password_error))
class AddressFaultException : RuntimeException(getString(R.string.hint_address_error))
class TransferUnknownException : RuntimeException(getString(R.string.hint_transfer_failed))
class LackOfBalanceException :
    RuntimeException(getString(R.string.hint_insufficient_or_trading_fees_are_confirmed))

class TransferManager {
    private val mTokenManager by lazy {
        TokenManager()
    }

    @Throws(AddressFaultException::class, RuntimeException::class)
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
        address: String,
        amount: Double,
        password: ByteArray,
        account: AccountDO,
        progress: Int,
        token: Boolean = false,
        tokenId: Long = 0,
        success: (String) -> Unit,
        error: (Throwable) -> Unit
    ) {
        if (!checkAddress(address, account.coinNumber)) {
            error.invoke(AddressFaultException())
            return
        }

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

    private suspend fun transferViolasToken(
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
            try {
                mTokenManager.sendToken(
                    Account(KeyPair(decryptPrivateKey)),
                    token.tokenIdx,
                    address,
                    (amount * 1000000L).toLong()
                )
                success.invoke("")
            } catch (e: Exception) {
                error.invoke(e)
            }
        }
    }

    private suspend fun transferLibra(
        context: Context,
        address: String,
        amount: Double,
        decryptPrivateKey: ByteArray,
        account: AccountDO,
        success: (String) -> Unit,
        error: (Throwable) -> Unit
    ) {
        try {
            DataRepository.getLibraService().sendCoin(
                context,
                org.palliums.libracore.wallet.Account(
                    org.palliums.libracore.wallet.KeyPair(decryptPrivateKey)
                ),
                address,
                (amount * 1000000L).toLong()
            )
            success.invoke("")
        } catch (e: Exception) {
            error.invoke(e)
        }
    }

    private suspend fun transferViolas(
        context: Context,
        address: String,
        amount: Double,
        decryptPrivateKey: ByteArray,
        account: AccountDO,
        success: (String) -> Unit,
        error: (Throwable) -> Unit
    ) {
        try {
            mTokenManager.mViolasService.sendCoin(
                context,
                Account(
                    KeyPair(decryptPrivateKey)
                ),
                address,
                (amount * 1000000L).toLong()
            )
            success.invoke("")
        } catch (e: Exception) {
            error.invoke(e)
        }
    }

    private fun checkAddress(address: String, coinNumber: Int): Boolean {
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