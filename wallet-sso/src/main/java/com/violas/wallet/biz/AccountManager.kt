package com.violas.wallet.biz

import android.content.Context
import com.palliums.content.ContextProvider.getContext
import com.palliums.utils.IOScope
import com.palliums.utils.isMainThread
import com.quincysx.crypto.CoinTypes
import com.quincysx.crypto.bip32.ExtendedKey
import com.quincysx.crypto.bip44.BIP44
import com.quincysx.crypto.bip44.CoinPairDerive
import com.quincysx.crypto.bitcoin.BitCoinECKeyPair
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.utils.convertAmountToDisplayUnit
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.palliums.libracore.mnemonic.English
import org.palliums.libracore.mnemonic.Mnemonic
import org.palliums.libracore.mnemonic.WordCount
import org.palliums.libracore.wallet.Account
import org.palliums.libracore.wallet.KeyFactory
import org.palliums.libracore.wallet.Seed
import java.util.concurrent.Executors

class MnemonicException : RuntimeException()
class AccountNotExistsException : RuntimeException()

enum class WalletType(val type: Int) {
    Governor(1), SSO(0);

    companion object {
        fun parse(type: Int): WalletType {
            return when (type) {
                Governor.type -> {
                    Governor
                }
                SSO.type -> {
                    SSO
                }
                else -> {
                    Governor
                }
            }
        }
    }
}

class AccountManager : CoroutineScope by IOScope() {
    companion object {
        private const val CURRENT_ACCOUNT = "ab1"
        private const val GOVERNOR_MNEMONIC_BACKUP = "GOVERNOR_MNEMONIC_BACKUP"
        private const val SSO_MNEMONIC_BACKUP = "SSO_MNEMONIC_BACKUP"
        private const val FAST_INTO_WALLET = "ab2"
        private const val FAST_INTO_SSO_WALLET = "ab3"
        private const val FAST_INTO_GOVERNOR_WALLET = "ab4"
    }

    private val handler = CoroutineExceptionHandler { _, exception ->
        println("Caught $exception")
    }

    private val mExecutor by lazy { Executors.newFixedThreadPool(2) }

    private val mConfigSharedPreferences by lazy {
        getContext().getSharedPreferences("config", Context.MODE_PRIVATE)
    }

    private val mAccountStorage by lazy {
        DataRepository.getAccountStorage()
    }

    fun refreshAccountAmount(currentAccount: AccountDO, callback: (AccountDO) -> Unit) {
        getBalance(currentAccount) { amount, success ->
            if (success) {
                currentAccount.amount = amount
                updateAccount(currentAccount)
            }
            callback.invoke(currentAccount)
        }
    }

    fun updateAccount(account: AccountDO) {
        mExecutor.submit {
            mAccountStorage.update(account)
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
        if (mAccountStorage.loadByCoinType(CoinTypes.Violas.coinType()) == null) {
            return false
        }
        return true
    }

    private fun getDefWallet(): Long {
        return mAccountStorage.loadByCoinType(CoinTypes.Violas.coinType())?.id ?: 1L
    }

    /**
     * 切换当前钱包账户
     */
    fun switchCurrentAccount(currentAccountID: Long = getDefWallet()) {
        mConfigSharedPreferences.edit().putLong(CURRENT_ACCOUNT, currentAccountID).apply()
    }

    /**
     * 钱包助记词是否备份
     */
    fun isMnemonicBackup(walletType: WalletType): Boolean {
        return mConfigSharedPreferences.getBoolean(
            if (walletType == WalletType.Governor)
                GOVERNOR_MNEMONIC_BACKUP
            else
                SSO_MNEMONIC_BACKUP,
            false
        )
    }

    /**
     * 设置钱包助记词已备份
     */
    fun setMnemonicBackup(walletType: WalletType) {
        mConfigSharedPreferences.edit().putBoolean(
            if (walletType == WalletType.Governor)
                GOVERNOR_MNEMONIC_BACKUP
            else
                SSO_MNEMONIC_BACKUP,
            true
        ).apply()
    }

    fun isFastIntoCurrentTypeWallet(walletType: WalletType): Boolean {
        return if (walletType == WalletType.Governor)
            isFastIntoGovernorWallet()
        else
            isFastIntoSSOWallet()
    }

    fun isFastIntoWallet(): Boolean {
        val fastInto = mConfigSharedPreferences.getBoolean(FAST_INTO_WALLET, true)
        if (fastInto) {
            mConfigSharedPreferences.edit().putBoolean(FAST_INTO_WALLET, false).apply()
        }
        return fastInto
    }

    private fun isFastIntoSSOWallet(): Boolean {
        val fastInto = mConfigSharedPreferences.getBoolean(FAST_INTO_SSO_WALLET, true)
        if (fastInto) {
            mConfigSharedPreferences.edit().putBoolean(FAST_INTO_SSO_WALLET, false).apply()
        }
        return fastInto
    }

    private fun isFastIntoGovernorWallet(): Boolean {
        val fastInto = mConfigSharedPreferences.getBoolean(FAST_INTO_GOVERNOR_WALLET, true)
        if (fastInto) {
            mConfigSharedPreferences.edit().putBoolean(FAST_INTO_GOVERNOR_WALLET, false).apply()
        }
        return fastInto
    }

    /**
     * 获取当前账号的助记词
     */
    fun getAccountMnemonic(
        context: Context,
        password: ByteArray,
        account: AccountDO
    ): ArrayList<String>? {
        val security = SimpleSecurity.instance(context)
        val bytes = security.decrypt(password, account.mnemonic) ?: return null
        val mnemonic = String(bytes)
        return mnemonic.substring(1, mnemonic.length - 1)
            .split(",")
            .map { it.trim() }
            .toMutableList() as ArrayList
    }

    /**
     * 生成助记词
     */
    fun generateWalletMnemonic(words: WordCount = WordCount.TWELVE): ArrayList<String> {
        return Mnemonic.English().generate(words)
    }

    /**
     * 导入钱包
     */
    @Throws(MnemonicException::class)
    fun importWallet(
        coinTypes: CoinTypes,
        context: Context,
        wordList: List<String>,
        walletName: String,
        password: ByteArray
    ): Long {
        checkMnemonicCount(wordList)
        return when (coinTypes) {
            CoinTypes.Libra -> {
                AccountManager().importLibraWallet(
                    context,
                    wordList,
                    walletName,
                    password
                )
            }
            CoinTypes.Violas -> {
                AccountManager().importViolasWallet(
                    context,
                    wordList,
                    walletName,
                    password
                )
            }
            CoinTypes.Bitcoin,
            CoinTypes.BitcoinTest -> {
                AccountManager().importBtcWallet(
                    context,
                    wordList,
                    walletName,
                    password
                )
            }
        }
    }

    /**
     * 导入Violas钱包（非身份钱包）
     */
    private fun importViolasWallet(
        context: Context,
        wordList: List<String>,
        walletName: String,
        password: ByteArray
    ): Long {
        val deriveLibra = deriveLibra(wordList)
        val security = SimpleSecurity.instance(context)

        return mAccountStorage.insert(
            AccountDO(
                privateKey = security.encrypt(password, deriveLibra.keyPair.getPrivateKey()),
                publicKey = deriveLibra.getPublicKey(),
                address = deriveLibra.getAddress().toHex(),
                coinNumber = CoinTypes.Violas.coinType(),
                mnemonic = security.encrypt(password, wordList.toString().toByteArray()),
                walletNickname = "$walletName",
                walletType = 1
            )
        )
    }

    /**
     * 导入Libra钱包（非身份钱包）
     */
    private fun importLibraWallet(
        context: Context,
        wordList: List<String>,
        walletName: String,
        password: ByteArray
    ): Long {
        val deriveLibra = deriveLibra(wordList)
        val security = SimpleSecurity.instance(context)

        return mAccountStorage.insert(
            AccountDO(
                privateKey = security.encrypt(password, deriveLibra.keyPair.getPrivateKey()),
                publicKey = deriveLibra.getPublicKey(),
                address = deriveLibra.getAddress().toHex(),
                coinNumber = CoinTypes.Libra.coinType(),
                mnemonic = security.encrypt(password, wordList.toString().toByteArray()),
                walletNickname = "$walletName",
                walletType = 1
            )
        )
    }

    /**
     * 导入BTC钱包（非身份钱包）
     */
    @Throws(MnemonicException::class)
    private fun importBtcWallet(
        context: Context,
        wordList: List<String>,
        walletName: String,
        password: ByteArray
    ): Long {
        val seed = Mnemonic.English()
            .toByteArray(wordList) ?: throw MnemonicException()

        val deriveBitcoin = deriveBitcoin(seed)
        val security = SimpleSecurity.instance(context)

        return mAccountStorage.insert(
            AccountDO(
                privateKey = security.encrypt(password, deriveBitcoin.rawPrivateKey),
                publicKey = deriveBitcoin.publicKey,
                address = deriveBitcoin.address,
                coinNumber = if (Vm.TestNet) {
                    CoinTypes.BitcoinTest.coinType()
                } else {
                    CoinTypes.Bitcoin.coinType()
                },
                mnemonic = security.encrypt(password, wordList.toString().toByteArray()),
                walletNickname = "$walletName",
                walletType = 1
            )
        )
    }

    /**
     * 创建身份
     */
    fun createIdentity(
        context: Context,
        walletName: String,
        walletType: WalletType,
        password: ByteArray
    ): List<String> {
        val generate = Mnemonic.English().generate()
        importIdentity(context, generate, walletName, walletType, password)
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
        walletType: WalletType,
        password: ByteArray
    ) {
        checkMnemonicCount(wordList)

        if (!Mnemonic(English.INSTANCE).validation(wordList)) {
            throw MnemonicException()
        }

        val deriveLibra = deriveLibra(wordList)

        val security = SimpleSecurity.instance(context)

        val insertIds = mAccountStorage.insert(
            AccountDO(
                privateKey = security.encrypt(password, deriveLibra.keyPair.getPrivateKey()),
                publicKey = deriveLibra.getPublicKey(),
                address = deriveLibra.getAddress().toHex(),
                coinNumber = CoinTypes.Violas.coinType(),
                mnemonic = security.encrypt(password, wordList.toString().toByteArray()),
                walletNickname = walletName,
                walletType = walletType.type
            )
        )
        switchCurrentAccount(insertIds)
    }

    private fun checkMnemonicCount(wordList: List<String>) {
        if (!(wordList.size == 12 ||
                    wordList.size == 15 ||
                    wordList.size == 18 ||
                    wordList.size == 21 ||
                    wordList.size == 24)
        ) {
            throw MnemonicException()
        }
    }

    private fun deriveLibra(wordList: List<String>): Account {
        val keyFactory = KeyFactory(Seed.fromMnemonic(wordList))
        return Account(keyFactory.generateKey(0))
    }

    private fun deriveBitcoin(seed: ByteArray): BitCoinECKeyPair {
        val extendedKey = ExtendedKey.create(seed)
        val bip44Path = if (Vm.TestNet) {
            BIP44.m().purpose44().coinType(CoinTypes.BitcoinTest).account(0).external().address(0)
        } else {
            BIP44.m().purpose44().coinType(CoinTypes.Bitcoin).account(0).external().address(0)
        }
        val derive = CoinPairDerive(extendedKey).derive(bip44Path)
        return derive as BitCoinECKeyPair
    }

    fun getBalanceWithUnit(account: AccountDO, callback: (String, String) -> Unit) {
        getBalance(account) { amount, success ->
            val parseCoinType = CoinTypes.parseCoinType(account.coinNumber)
            val convertAmountToDisplayUnit =
                convertAmountToDisplayUnit(amount, parseCoinType)
            callback.invoke(convertAmountToDisplayUnit.first, convertAmountToDisplayUnit.second)
        }
    }

    fun getBalance(account: AccountDO, callback: (Long, Boolean) -> Unit) {
        if (isMainThread()) {
            launch(handler) {
                getBalance(account, callback)
            }
            return
        }

        when (account.coinNumber) {
            CoinTypes.Violas.coinType() -> {
                DataRepository.getViolasService()
                    .getBalanceInMicroLibras(account.address) { balance, success ->
                        callback.invoke(balance, success)
                    }
            }
            CoinTypes.Libra.coinType() -> {
                DataRepository.getLibraService().getBalanceInMicroLibraWithCallback(
                    account.address
                ) { amount, exception ->
                    callback.invoke(amount, exception == null)
                }
            }
            CoinTypes.Bitcoin.coinType(),
            CoinTypes.BitcoinTest.coinType() -> {
                DataRepository.getBitcoinService().getBalance(account.address)
                    .subscribe({
                        try {
                            callback.invoke(it.toLong(), true)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            callback.invoke(0, false)
                        }
                    }, {
                        callback.invoke(0, false)
                    }, {

                    })
            }
        }
    }

    fun removeWallet(accountId: AccountDO) {
        mAccountStorage.delete(accountId)
    }

    fun getIdentityByCoinType(coinType: Int): AccountDO? {
        return mAccountStorage.loadByCoinType(coinType)
    }

    fun getIdentityByWalletType(walletType: Int): AccountDO? {
        return mAccountStorage.findByCoinTypeAndWalletType(CoinTypes.Violas.coinType(), walletType)
    }
}