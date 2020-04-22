package com.violas.wallet.biz

import android.content.Context
import com.palliums.content.ContextProvider.getContext
import com.palliums.net.RequestException
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
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.suspendCancellableCoroutine
import org.palliums.libracore.mnemonic.Mnemonic
import org.palliums.libracore.mnemonic.WordCount
import org.palliums.libracore.wallet.Account
import org.palliums.libracore.crypto.KeyFactory
import org.palliums.libracore.crypto.Seed
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MnemonicException : RuntimeException()
class AccountNotExistsException : RuntimeException()

class AccountManager {
    companion object {
        private const val CURRENT_ACCOUNT = "ab1"
        private const val IDENTITY_MNEMONIC_BACKUP = "IDENTITY_MNEMONIC_BACKUP"
        private const val FAST_INTO_WALLET = "ab2"
    }

    private val mExecutor by lazy { Executors.newFixedThreadPool(2) }

    private val mConfigSharedPreferences by lazy {
        getContext().getSharedPreferences("config", Context.MODE_PRIVATE)
    }

    private val mAccountStorage by lazy {
        DataRepository.getAccountStorage()
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

    private fun getDefWallet(): Long {
        return mAccountStorage.loadByWalletType(0)?.id ?: 1L
    }

    fun removeWallet(accountId: AccountDO) {
        mAccountStorage.delete(accountId)
    }

    fun getIdentityByCoinType(coinType: Int): AccountDO? {
        return mAccountStorage.findByCoinTypeByIdentity(coinType)
    }

    /**
     * 切换当前钱包账户
     */
    fun switchCurrentAccount(currentAccountID: Long = getDefWallet()) {
        mConfigSharedPreferences.edit().putLong(CURRENT_ACCOUNT, currentAccountID).apply()
    }

    /**
     * 身份钱包助记词是否备份
     */
    fun isIdentityMnemonicBackup(): Boolean {
        return mConfigSharedPreferences.getBoolean(IDENTITY_MNEMONIC_BACKUP, false)
    }

    /**
     * 设置身份钱包助记词已备份
     */
    fun setIdentityMnemonicBackup() {
        mConfigSharedPreferences.edit().putBoolean(IDENTITY_MNEMONIC_BACKUP, true).apply()
    }

    fun isFastIntoWallet(): Boolean {
        val fastInto = mConfigSharedPreferences.getBoolean(FAST_INTO_WALLET, true)
        if (fastInto) {
            mConfigSharedPreferences.edit().putBoolean(FAST_INTO_WALLET, false).apply()
        }
        return fastInto
    }

    /**
     * 获取身份钱包的助记词
     */
    fun getIdentityWalletMnemonic(context: Context, password: ByteArray): ArrayList<String>? {
        val account = mAccountStorage.loadByWalletType(0)!!
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
    @Throws(MnemonicException::class, ArrayIndexOutOfBoundsException::class)
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
                privateKey = security.encrypt(password, deriveLibra.keyPair.getPrivateKey().toByteArray()),
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
                privateKey = security.encrypt(password, deriveLibra.keyPair.getPrivateKey().toByteArray()),
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
    fun createIdentity(context: Context, walletName: String, password: ByteArray): List<String> {
        val generate = Mnemonic.English().generate()
        importIdentity(context, generate, walletName, password)
        return generate
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
        checkMnemonicCount(wordList)

        val seed = Mnemonic.English()
            .toByteArray(wordList) ?: throw MnemonicException()

        val deriveBitcoin = deriveBitcoin(seed)
        val deriveLibra = deriveLibra(wordList)

        val security = SimpleSecurity.instance(context)

        val insertIds = mAccountStorage.insert(
            AccountDO(
                privateKey = security.encrypt(password, deriveLibra.keyPair.getPrivateKey().toByteArray()),
                publicKey = deriveLibra.getPublicKey(),
                address = deriveLibra.getAddress().toHex(),
                coinNumber = CoinTypes.Violas.coinType(),
                mnemonic = security.encrypt(password, wordList.toString().toByteArray()),
                walletNickname = "${CoinTypes.Violas.fullName()}-$walletName",
                walletType = 0
            ),
            AccountDO(
                privateKey = security.encrypt(password, deriveLibra.keyPair.getPrivateKey().toByteArray()),
                publicKey = deriveLibra.getPublicKey(),
                address = deriveLibra.getAddress().toHex(),
                coinNumber = CoinTypes.Libra.coinType(),
                mnemonic = security.encrypt(password, wordList.toString().toByteArray()),
                walletNickname = "${CoinTypes.Libra.fullName()}-$walletName",
                walletType = 0
            ),
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
                walletNickname = "${CoinTypes.Bitcoin.fullName()}-$walletName",
                walletType = 0
            )
        )
        if (insertIds.isNotEmpty()) {
            switchCurrentAccount(insertIds[0])
        }
    }

    private fun deriveLibra(wordList: List<String>): Account {
        val keyFactory = KeyFactory(
            Seed.fromMnemonic(wordList)
        )
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

    fun updateAccount(account: AccountDO) {
        mExecutor.submit {
            mAccountStorage.update(account)
        }
    }

    suspend fun refreshAccount(currentAccount: AccountDO) {
        val balance = getBalance(currentAccount)
        if (balance != currentAccount.amount) {
            currentAccount.amount = balance
            updateAccount(currentAccount)
        }
    }

    suspend fun getBalanceWithUnit(account: AccountDO): Pair<String, String> {
        val balance = getBalance(account)
        val coinType = CoinTypes.parseCoinType(account.coinNumber)
        return convertAmountToDisplayUnit(balance, coinType)
    }

    suspend fun getBalance(account: AccountDO): Long {
        return when (account.coinNumber) {
            CoinTypes.Violas.coinType() -> {
                DataRepository.getViolasService().getBalanceInMicroLibras(account.address)
            }

            CoinTypes.Libra.coinType() -> {
                DataRepository.getLibraService().getBalanceInMicroLibra(account.address)
            }
            else -> {
                suspendCancellableCoroutine { coroutine ->
                    val subscribe = DataRepository.getBitcoinService()
                        .getBalance(account.address)
                        .subscribeOn(Schedulers.io())
                        .subscribe({
                            coroutine.resume(it.toLong())
                        }, {
                            coroutine.resumeWithException(RequestException(it))
                        })
                    coroutine.invokeOnCancellation {
                        subscribe.dispose()
                    }
                }
            }
        }
    }
}