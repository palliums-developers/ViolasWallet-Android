package com.violas.wallet.biz

import android.content.Context
import androidx.annotation.WorkerThread
import com.quincysx.crypto.CoinTypes
import com.quincysx.crypto.bip32.ExtendedKey
import com.quincysx.crypto.bip44.BIP44
import com.quincysx.crypto.bip44.CoinPairDerive
import com.quincysx.crypto.bitcoin.BitCoinECKeyPair
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.getContext
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import org.palliums.libracore.mnemonic.Mnemonic
import org.palliums.libracore.wallet.Account
import org.palliums.libracore.wallet.KeyFactory
import org.palliums.libracore.wallet.Seed

class MnemonicException : RuntimeException()
class AccountNotExistsException : RuntimeException()

class AccountManager {
    companion object {
        private const val CURRENT_ACCOUNT = "ab1"
    }

    private val mConfigSharedPreferences by lazy {
        getContext().getSharedPreferences("config", Context.MODE_PRIVATE)
    }

    private val mAccountStorage by lazy {
        DataRepository.getAccountStorage()
    }

    @WorkerThread
    fun refreshAccountAmount(currentAccount: AccountDO, callback: (AccountDO) -> Unit) {
        when (CoinTypes.parseCoinType(currentAccount.coinNumber)) {
            CoinTypes.Libra -> {
                DataRepository.getLibraService().getBalanceInMicroLibras(currentAccount.address) {
                    currentAccount.amount = it
                    mAccountStorage.update(currentAccount)
                    callback.invoke(currentAccount)
                }
            }
            CoinTypes.Bitcoin -> {
                DataRepository.getBitcoinService().getBalance(currentAccount.address)
                    .subscribe({
                        currentAccount.amount = it.toLong()
                        mAccountStorage.update(currentAccount)
                        callback.invoke(currentAccount)
                    }, {
                        callback.invoke(currentAccount)
                    }, {})
            }
            CoinTypes.VToken -> {
                callback.invoke(currentAccount)
            }
            else -> callback.invoke(currentAccount)
        }
    }

    /**
     * 获取当前账户账户
     */
    @Throws(AccountNotExistsException::class)
    fun currentAccount(): AccountDO {
        val currentWallet = mConfigSharedPreferences.getLong(CURRENT_ACCOUNT, 1)
        return mAccountStorage.findById(currentWallet) ?: throw AccountNotExistsException()
    }

    /**
     * 获取当前账户账户
     */
    @Throws(AccountNotExistsException::class)
    fun getAccountById(accountId: Long): AccountDO {
        return mAccountStorage.findById(accountId) ?: throw AccountNotExistsException()
    }

    /**
     * 是否存在账户
     */
    fun existsWalletAccount(): Boolean {
        if (mAccountStorage.loadByWalletType(0) == null) {
            return false
        }
        return true
    }

    /**
     * 切换当前钱包账户
     */
    fun switchCurrentAccount(currentAccountID: Long) {
        mConfigSharedPreferences.edit().putLong(CURRENT_ACCOUNT, currentAccountID).apply()
    }

    /**
     * 获取身份钱包的助记词
     */
    fun getIdentityWalletMnemonic(context: Context, password: ByteArray): List<String> {
        val account = mAccountStorage.loadByWalletType(0)!!
        val security = SimpleSecurity.instance(context)
        val mnemonic = String(security.decrypt(password, account.mnemonic)!!)
        return mnemonic.split(" ")
    }

    /**
     * 导入Violas钱包（非身份钱包）
     */
    fun importViolasWallet(
        context: Context,
        wordList: List<String>,
        walletName: String,
        password: ByteArray
    ) {
        val deriveLibra = deriveLibra(wordList)
        val security = SimpleSecurity.instance(context)

        val insertIds = mAccountStorage.insert(
            AccountDO(
                privateKey = security.encrypt(password, deriveLibra.keyPair.getPrivateKey()),
                publicKey = deriveLibra.getPublicKey(),
                address = deriveLibra.getAddress().toHex(),
                coinNumber = CoinTypes.VToken.coinType(),
                mnemonic = security.encrypt(password, wordList.toString().toByteArray()),
                walletNickname = "${CoinTypes.VToken.coinName()}-$walletName",
                walletType = 1
            )
        )
    }

    /**
     * 导入Libra钱包（非身份钱包）
     */
    fun importLibraWallet(
        context: Context,
        wordList: List<String>,
        walletName: String,
        password: ByteArray
    ) {
        val deriveLibra = deriveLibra(wordList)
        val security = SimpleSecurity.instance(context)

        val insertIds = mAccountStorage.insert(
            AccountDO(
                privateKey = security.encrypt(password, deriveLibra.keyPair.getPrivateKey()),
                publicKey = deriveLibra.getPublicKey(),
                address = deriveLibra.getAddress().toHex(),
                coinNumber = CoinTypes.Libra.coinType(),
                mnemonic = security.encrypt(password, wordList.toString().toByteArray()),
                walletNickname = "${CoinTypes.Libra.coinName()}-$walletName",
                walletType = 1
            )
        )
    }

    /**
     * 导入BTC钱包（非身份钱包）
     */
    @Throws(MnemonicException::class)
    fun importBtcWallet(
        context: Context,
        wordList: List<String>,
        walletName: String,
        password: ByteArray
    ) {
        val seed = Mnemonic.English()
            .toByteArray(wordList) ?: throw MnemonicException()

        val deriveBitcoin = deriveBitcoin(seed)
        val security = SimpleSecurity.instance(context)

        val insertIds = mAccountStorage.insert(
            AccountDO(
                privateKey = security.encrypt(password, deriveBitcoin.rawPrivateKey),
                publicKey = deriveBitcoin.publicKey,
                address = deriveBitcoin.address,
                coinNumber = CoinTypes.Bitcoin.coinType(),
                mnemonic = security.encrypt(password, wordList.toString().toByteArray()),
                walletNickname = "${CoinTypes.Bitcoin.coinName()}-$walletName",
                walletType = 1
            )
        )
    }

    /**
     * 创建身份
     */
    fun createIdentity(context: Context, walletName: String, password: ByteArray): List<String> {
        val generate = Mnemonic.English().generate()
        importIdentity(context, generate, walletName, password)
        return generate
    }

    /**
     * 导入身份
     */
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

        val insertIds = mAccountStorage.insert(
            AccountDO(
                privateKey = security.encrypt(password, deriveBitcoin.rawPrivateKey),
                publicKey = deriveBitcoin.publicKey,
                address = deriveBitcoin.address,
                coinNumber = CoinTypes.Bitcoin.coinType(),
                mnemonic = security.encrypt(password, wordList.toString().toByteArray()),
                walletNickname = "${CoinTypes.VToken.coinName()}-$walletName",
                walletType = 0
            ),
            AccountDO(
                privateKey = security.encrypt(password, deriveLibra.keyPair.getPrivateKey()),
                publicKey = deriveLibra.getPublicKey(),
                address = deriveLibra.getAddress().toHex(),
                coinNumber = CoinTypes.Bitcoin.coinType(),
                mnemonic = security.encrypt(password, wordList.toString().toByteArray()),
                walletNickname = "${CoinTypes.Libra.coinName()}-$walletName",
                walletType = 0
            ),
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
        if (insertIds.isNotEmpty()) {
            switchCurrentAccount(insertIds[0])
        }
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