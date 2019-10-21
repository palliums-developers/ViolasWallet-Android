package com.violas.wallet.biz

import android.content.Context
import com.quincysx.crypto.CoinTypes
import com.quincysx.crypto.bip32.ExtendedKey
import com.quincysx.crypto.bip44.BIP44
import com.quincysx.crypto.bip44.CoinPairDerive
import com.quincysx.crypto.bitcoin.BitCoinECKeyPair
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import org.palliums.libracore.mnemonic.Mnemonic
import org.palliums.libracore.wallet.Account
import org.palliums.libracore.wallet.KeyFactory
import org.palliums.libracore.wallet.Seed

class MnemonicException : RuntimeException()

class AccountManager() {
    fun createIdentity(context: Context, walletName: String, password: ByteArray): List<String> {
        val generate = Mnemonic.English().generate()
        importIdentity(context, generate, walletName, password)
        return generate
    }

    @Throws(MnemonicException::class)
    fun importIdentity(
        context: Context,
        wordList: List<String>,
        walletName: String,
        password: ByteArray
    ) {
        val seed = Mnemonic.English()
            .toByteArray(wordList) ?: throw MnemonicException()

        val deriveBitcoin = deriveBitcoin(seed)
        val deriveLibra = deriveLibra(wordList)

        val security = SimpleSecurity.instance(context)

        saveAsDB(
            AccountDO(
                privateKey = security.encrypt(password, deriveBitcoin.rawPrivateKey),
                publicKey = deriveBitcoin.publicKey,
                address = deriveBitcoin.address,
                coinNumber = CoinTypes.Bitcoin.coinType(),
                mnemonic = security.encrypt(password, wordList.toString().toByteArray()),
                walletNickname = "${CoinTypes.VToken.coinName()}-$walletName",
                walletType = 0
            )
        )

        saveAsDB(
            AccountDO(
                privateKey = security.encrypt(password, deriveLibra.keyPair.getPrivateKey()),
                publicKey = deriveLibra.getPublicKey(),
                address = deriveLibra.getAddress().toHex(),
                coinNumber = CoinTypes.Bitcoin.coinType(),
                mnemonic = security.encrypt(password, wordList.toString().toByteArray()),
                walletNickname = "${CoinTypes.Libra.coinName()}-$walletName",
                walletType = 0
            )
        )

        saveAsDB(
            AccountDO(
                privateKey = security.encrypt(password, deriveBitcoin.rawPrivateKey),
                publicKey = deriveBitcoin.publicKey,
                address = deriveBitcoin.address,
                coinNumber = CoinTypes.Bitcoin.coinType(),
                mnemonic = security.encrypt(password, wordList.toString().toByteArray()),
                walletNickname = "${CoinTypes.Bitcoin.coinName()}-$walletName",
                walletType = 0
            )
        )
    }

    private fun saveAsDB(
        account: AccountDO
    ) {
        DataRepository.getAccountStorage().insert(
            account
        )
    }

    private fun deriveLibra(wordList: List<String>): Account {
        val keyFactory = KeyFactory(Seed.fromMnemonic(wordList))
        return Account(keyFactory.generateKey(0))
    }

    private fun deriveBitcoin(seed: ByteArray): BitCoinECKeyPair {
        val extendedKey = ExtendedKey.create(seed)
        val bip44Path =
            BIP44.m().purpose44().coinType(CoinTypes.Bitcoin).account(0).external().address(0)
        val derive = CoinPairDerive(extendedKey).derive(bip44Path)
        return derive as BitCoinECKeyPair
    }
}