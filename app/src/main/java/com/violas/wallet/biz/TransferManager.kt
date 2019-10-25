package com.violas.wallet.biz

import android.annotation.SuppressLint
import android.content.Context
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.btc.TransactionManager
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.getContext
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.btcBrowser.request.BlockChainRequest
import com.violas.wallet.utils.validationBTCAddress
import com.violas.wallet.utils.validationLibraAddress
import org.palliums.libracore.serialization.hexToBytes
import org.palliums.libracore.serialization.toHex
import org.palliums.libracore.wallet.KeyPair

class WrongPasswordException : RuntimeException()
class AddressFaultException : RuntimeException()

class TransferManager {
    @Throws(AddressFaultException::class, WrongPasswordException::class)
    fun transfer(
        context: Context,
        address: String,
        amount: Double,
        password: String,
        account: AccountDO,
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
            .decrypt(password.toByteArray(), account.privateKey)
        if (decryptPrivateKey == null) {
            error.invoke(WrongPasswordException())
            return
        }


        when (account.coinNumber) {
            CoinTypes.Libra.coinType(),
            CoinTypes.VToken.coinType() -> {
                if (token) {
                    transferLibra(
                        context,
                        address,
                        amount,
                        decryptPrivateKey,
                        account,
                        success,
                        error
                    )
                } else {
                    transferLibraToken(
                        address,
                        amount,
                        decryptPrivateKey,
                        account,
                        tokenId,
                        success,
                        error
                    )
                }
            }
            CoinTypes.Bitcoin.coinType(),
            CoinTypes.BitcoinTest.coinType() -> {
                transferBtc(address, amount, decryptPrivateKey, account, success, error)
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun transferBtc(
        address: String,
        amount: Double,
        decryptPrivateKey: ByteArray,
        account: AccountDO,
        success: (String) -> Unit,
        error: (Throwable) -> Unit
    ) {
        val transactionManager = TransactionManager(arrayListOf(account.address))
        transactionManager.checkBalance(amount, 1, 30)
            .flatMap {
                transactionManager.obtainTransaction(
                    decryptPrivateKey,
                    account.publicKey.hexToBytes(),
                    it,
                    address,
                    account.address
                )
            }
            .flatMap {
                BlockChainRequest.get().pushTx(it.signBytes.toHex())
            }
            .subscribe({
                success.invoke(it)
            }, {
                error.invoke(it)
            }, {

            })
    }

    private fun transferLibraToken(
        address: String,
        amount: Double,
        decryptPrivateKey: ByteArray,
        account: AccountDO,
        tokenId: Long,
        success: (String) -> Unit,
        error: (Throwable) -> Unit
    ) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
                error.invoke(Exception())
            }
        }
    }

    private fun checkAddress(address: String, coinNumber: Int): Boolean {
        when (coinNumber) {
            CoinTypes.Libra.coinType(),
            CoinTypes.VToken.coinType() -> {
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