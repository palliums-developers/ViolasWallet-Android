package org.palliums.libracore.transaction

import org.junit.Test
import org.palliums.libracore.crypto.KeyFactory
import org.palliums.libracore.crypto.KeyPair
import org.palliums.libracore.crypto.Seed
import org.palliums.libracore.serialization.hexToBytes
import org.palliums.libracore.transaction.storage.StructTag
import org.palliums.libracore.transaction.storage.TypeTag
import org.palliums.libracore.wallet.Account

class TransactionUnitTest {
    fun generateKey(childDepth: Long = 0): KeyPair {
        val mnemonics = arrayListOf(
            "museum",
            "liquid",
            "spider",
            "explain",
            "vicious",
            "pave",
            "silent",
            "allow",
            "depth",
            "you",
            "adjust",
            "begin"
        )

        val keyFactory = KeyFactory(
            Seed.fromMnemonic(mnemonics)
        )
        return keyFactory.generateKey(childDepth)
    }

    fun getSignedTransaction(
        senderAddress: String,
        payload: TransactionPayload,
        sequenceNumber: Long,
        keyPair: KeyPair
    ): SignedTransaction {
        val rawTransaction = RawTransaction.optionTransaction(
            senderAddress,
            payload,
            sequenceNumber,
            gasCurrencyCode = lbrStructTagType(),
            maxGasAmount = 1_000_000,
            gasUnitPrice = 0,
            delayed = 600,
            chainId = 4
        )

        val transactionAuthenticator = TransactionSignAuthenticator(
            keyPair.getPublicKey(),
            keyPair.signMessage(rawTransaction.toHashByteArray())
        )

        return SignedTransaction(rawTransaction, transactionAuthenticator)
    }

    @Test
    fun test_sendTransaction() {

        val account = Account(generateKey(1))

        println("Address: ${account.getAddress().toHex()}")
        println("AuthenticationKey: ${account.getAuthenticationKey().toHex()}")

        val sequenceNumber = 0L
        val payeeAmount = 100L
        val payeeAddress = "53e59e4b4fa3c35770846f6c87ca2d35"
        val senderAddress = account.getAddress()

        val payload = TransactionPayload(
            TransactionPayload.Script(
                "a11ceb0b010000000701000202020403061004160205181d0735610896011000000001010000020001000003020301010004010300010501060c0108000506080005030a020a020005060c05030a020a020109000c4c696272614163636f756e741257697468647261774361706162696c6974791b657874726163745f77697468647261775f6361706162696c697479087061795f66726f6d1b726573746f72655f77697468647261775f6361706162696c69747900000000000000000000000000000001010104010c0b0011000c050e050a010a020b030b0438000b05110202".hexToBytes(),
                arrayListOf(
                    lbrStructTag()
                ),
                arrayListOf(
                    TransactionArgument.newAddress(payeeAddress),
                    TransactionArgument.newU64(payeeAmount),
                    TransactionArgument.newByteArray(byteArrayOf()),
                    TransactionArgument.newByteArray(byteArrayOf())
                )
            )
        )

        val signedTransaction =
            getSignedTransaction(
                senderAddress.toHex(),
                payload,
                sequenceNumber,
                account.keyPair
            )

        println(signedTransaction.toHex())
    }

    @Test
    fun test_sendRotateAuthenticationKeyTransaction() {

        val account1 = Account(generateKey(1))
        val account2 = Account(generateKey(0))

        println("Address: ${account1.getAddress().toHex()}")
        println("AuthenticationKey: ${account1.getAuthenticationKey().toHex()}")

        val sequenceNumber = 1L
        val payeeAmount = 1000000L
        val payeeAddress = "53e59e4b4fa3c35770846f6c87ca2d35"
        val senderAddress = account1.getAddress()

        val payload = TransactionPayload(
            TransactionPayload.Script(
                "a11ceb0b01000000060100040204040308190521200741af0108f0011000000001000301000102000100000400020000050304000006020500000706050001060c01050108000106080001060500020608000a0202060c0a0203080001030c4c696272614163636f756e74065369676e65720a616464726573735f6f66154b6579526f746174696f6e4361706162696c6974791f657874726163745f6b65795f726f746174696f6e5f6361706162696c6974791f6b65795f726f746174696f6e5f6361706162696c6974795f616464726573731f726573746f72655f6b65795f726f746174696f6e5f6361706162696c69747919726f746174655f61757468656e7469636174696f6e5f6b657900000000000000000000000000000001000708140a0011010c020e021102140b001100210c030b03030e060000000000000000270e020b0111040b02110302".hexToBytes(),
                arrayListOf(),
                arrayListOf(
                    TransactionArgument.newByteArray(account2.getAuthenticationKey().toBytes())
                )
            )
        )

        val signedTransaction =
            getSignedTransaction(
                senderAddress.toHex(),
                payload,
                sequenceNumber,
                account1.keyPair
            )

        println(signedTransaction.toHex())
    }
}