package org.palliums.violascore.multiContract

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.palliums.violascore.serialization.hexToBytes
import org.palliums.violascore.serialization.toHex
import com.palliums.violas.smartcontract.multitoken.MultiTokenContract
import org.palliums.violascore.transaction.RawTransaction
import org.palliums.violascore.transaction.SignedTransaction
import org.palliums.violascore.transaction.TransactionSignAuthenticator
import org.palliums.violascore.transaction.optionTransaction
import org.palliums.violascore.wallet.Account
import org.palliums.violascore.wallet.KeyPair

@RunWith(AndroidJUnit4::class)
class MoveHandlerTest {
    private fun getSendAccount(): Account {
        //7f4644ae2b51b65bd3c9d414aa853407
        return Account(KeyPair.fromSecretKey("a65111b2a22a1874f0f532f6b1a0991fbf1fedb9df8ce55a49b3e35be73ff228".hexToBytes()))
    }

    private fun getViolasMultiTokenContract(): MultiTokenContract {
        return MultiTokenContract("e1be1ab8360a35a0259f1c93e3eac736")
    }

    @Test
    fun test_release_token_move() {
        val account = getSendAccount()

        val optionPublishTransactionPayload =
            getViolasMultiTokenContract().optionReleaseTokenPayload()

        val rawTransaction = RawTransaction.optionTransaction(
            account.getAddress().toHex(),
            optionPublishTransactionPayload,
            15
        )
        Log.e("===", account.keyPair.getPrivateKey().toHex())

        val signedTransaction = SignedTransaction(
            rawTransaction,
            TransactionSignAuthenticator(
                account.keyPair.getPublicKey(),
                account.keyPair.signMessage(rawTransaction.toHashByteArray())
            )
        )

        Log.e("===", signedTransaction.toByteArray().toHex())
    }

    @Test
    fun test_publish_move() {
        val account = getSendAccount()

        val optionPublishTransactionPayload =
            getViolasMultiTokenContract().optionPublishTransactionPayload()

        val rawTransaction = RawTransaction.optionTransaction(
            account.getAddress().toHex(),
            optionPublishTransactionPayload,
            16
        )
        Log.e("===", account.keyPair.getPrivateKey().toHex())

        val signedTransaction = SignedTransaction(
            rawTransaction,
            TransactionSignAuthenticator(
                account.keyPair.getPublicKey(),
                account.keyPair.signMessage(rawTransaction.toHashByteArray())
            )
        )

        Log.e("===", signedTransaction.toByteArray().toHex())
    }

    @Test
    fun test_transfer_move() {
        val account = getSendAccount()

        val optionTokenTransactionPayload =
            getViolasMultiTokenContract().optionTokenTransactionPayload(
                0,
                "1409fc67d04cddf259240703809b6d12",
                100,
                byteArrayOf()
            )

        val rawTransaction = RawTransaction.optionTransaction(
            account.getAddress().toHex(),
            optionTokenTransactionPayload,
            18
        )
        Log.e("===", account.keyPair.getPrivateKey().toHex())

        val signedTransaction = SignedTransaction(
            rawTransaction,
            TransactionSignAuthenticator(
                account.keyPair.getPublicKey(),
                account.keyPair.signMessage(rawTransaction.toHashByteArray())
            )
        )

        Log.e("===", signedTransaction.toByteArray().toHex())
    }

    @Test
    fun test_mint_move() {
        val account = getSendAccount()

        val optionTokenTransactionPayload =
            getViolasMultiTokenContract().optionMintTransactionPayload(
                0,
                "1409fc67d04cddf259240703809b6d12",
                10 * 1000000,
                byteArrayOf()
            )

        val rawTransaction = RawTransaction.optionTransaction(
            account.getAddress().toHex(),
            optionTokenTransactionPayload,
            12
        )
        Log.e("===", account.keyPair.getPrivateKey().toHex())

        val signedTransaction = SignedTransaction(
            rawTransaction,
            TransactionSignAuthenticator(
                account.keyPair.getPublicKey(),
                account.keyPair.signMessage(rawTransaction.toHashByteArray())
            )
        )

        Log.e("===", signedTransaction.toByteArray().toHex())
    }
}