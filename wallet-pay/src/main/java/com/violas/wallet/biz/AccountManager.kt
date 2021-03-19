package com.violas.wallet.biz

import android.content.Context
import androidx.annotation.WorkerThread
import com.palliums.content.ContextProvider.getContext
import com.quincysx.crypto.CoinType
import com.quincysx.crypto.bip32.ExtendedKey
import com.quincysx.crypto.bip39.SeedCalculator
import com.quincysx.crypto.bip39.wordlists.English
import com.quincysx.crypto.bip44.BIP44
import com.quincysx.crypto.bip44.CoinPairDerive
import com.quincysx.crypto.bitcoin.BitCoinECKeyPair
import com.violas.wallet.biz.command.CommandActuator
import com.violas.wallet.biz.command.RefreshAssetsCommand
import com.violas.wallet.common.*
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.dao.AccountDao
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.database.entity.AccountType
import com.violas.wallet.repository.database.entity.TokenDo
import com.violas.wallet.viewModel.bean.DiemCoinAssetVo
import org.palliums.violascore.common.CURRENCY_DEFAULT_CODE
import org.palliums.violascore.crypto.KeyFactory
import org.palliums.violascore.crypto.Seed
import org.palliums.violascore.mnemonic.Mnemonic
import org.palliums.violascore.mnemonic.WordCount
import org.palliums.violascore.serialization.toHex
import org.palliums.violascore.wallet.Account
import java.util.*

class MnemonicException : RuntimeException()
class AccountNotExistsException : RuntimeException()
class UnsupportedWalletException : RuntimeException()

object AccountManager {

    private const val CURRENT_ACCOUNT = "key_0"
    private const val KEY_FAST_INTO_WALLET = "key_1"
    private const val KEY_IDENTITY_MNEMONIC_BACKUP = "key_2"
    private const val KEY_PROMPT_OPEN_BIOMETRICS = "key_3"
    private const val KEY_SECURITY_PASSWORD = "key_4"
    private const val KEY_APP_TOKEN = "key_5"

    private val mConfigSharedPreferences by lazy {
        getContext().getSharedPreferences(SP_FILE_NAME_APP_CONFIG, Context.MODE_PRIVATE)
    }

    private val mAccountStorage by lazy {
        DataRepository.getAccountStorage()
    }

    private val mAccountTokenStorage by lazy {
        DataRepository.getTokenStorage()
    }

    // <editor-fold defaultState="collapsed" desc="配置相关">
    /**
     * 获取生物识别加解密所用的key
     */
    fun getBiometricKey(): String {
        return KEY_SECURITY_PASSWORD
    }

    /**
     * 更新安全密码
     */
    fun updateSecurityPassword(securityPassword: String) {
        mConfigSharedPreferences.edit().putString(KEY_SECURITY_PASSWORD, securityPassword).apply()
    }

    /**
     * 获取安全密码
     */
    fun getSecurityPassword(): String? {
        return mConfigSharedPreferences.getString(KEY_SECURITY_PASSWORD, null)
    }

    /**
     * 身份钱包助记词是否备份
     */
    fun isIdentityMnemonicBackup(): Boolean {
        return mConfigSharedPreferences.getBoolean(KEY_IDENTITY_MNEMONIC_BACKUP, false)
    }

    /**
     * 设置身份钱包助记词已备份
     */
    fun setIdentityMnemonicBackup() {
        mConfigSharedPreferences.edit().putBoolean(KEY_IDENTITY_MNEMONIC_BACKUP, true).apply()
    }

    /**
     * 是否第一次进入钱包
     */
    fun isFastIntoWallet(): Boolean {
        val fastInto = mConfigSharedPreferences.getBoolean(KEY_FAST_INTO_WALLET, true)
        if (fastInto) {
            mConfigSharedPreferences.edit().putBoolean(KEY_FAST_INTO_WALLET, false).apply()
        }
        return fastInto
    }

    /**
     * 设置已提示开启指纹
     */
    fun setOpenBiometricsPrompted() {
        mConfigSharedPreferences.edit().putBoolean(KEY_PROMPT_OPEN_BIOMETRICS, true).apply()
    }

    /**
     * 是否已提示开启指纹识别
     */
    fun isOpenBiometricsPrompted(): Boolean {
        return mConfigSharedPreferences.getBoolean(KEY_PROMPT_OPEN_BIOMETRICS, false)
    }

    /**
     * 获取App Token
     */
    fun getAppToken(): String? {
        return mConfigSharedPreferences.getString(KEY_APP_TOKEN, null)
    }

    /**
     * 设置App Token
     */
    fun setAppToken(appToken: String) {
        mConfigSharedPreferences.edit().putString(KEY_APP_TOKEN, appToken).apply()
    }

    /**
     * 清除用户配置
     */
    fun clearUserConfig() {
        mConfigSharedPreferences.edit()
            .putBoolean(KEY_IDENTITY_MNEMONIC_BACKUP, false)
            .putBoolean(KEY_PROMPT_OPEN_BIOMETRICS, false)
            .putString(KEY_SECURITY_PASSWORD, "")
            .apply()
    }
    // </editor-fold>

    // <editor-fold defaultState="collapsed" desc="账户操作相关">
    fun getAccountStorage(): AccountDao {
        return mAccountStorage
    }

    /**
     * 获取一个默认账户
     */
    @Throws(AccountNotExistsException::class)
    @WorkerThread
    fun getDefaultAccount(): AccountDO {
        val accounts = mAccountStorage.loadAll()
        if (accounts.isEmpty()) {
            throw AccountNotExistsException()
        }
        return accounts[0]
    }

    /**
     * 根据account id获取账户
     */
    @Throws(AccountNotExistsException::class)
    @WorkerThread
    fun getAccountById(accountId: Long = 1): AccountDO {
        return mAccountStorage.findById(accountId) ?: throw AccountNotExistsException()
    }

    /**
     * 根据coin number获取账户
     */
    @WorkerThread
    fun getAccountByCoinNumber(coinType: Int): AccountDO? {
        return mAccountStorage.findByCoinType(coinType)
    }

    /**
     * 更新账户
     */
    @WorkerThread
    fun updateAccount(account: AccountDO) {
        mAccountStorage.update(account)
    }

    /**
     * 删除账户
     */
    @WorkerThread
    fun deleteAccount(account: AccountDO) {
        mAccountStorage.delete(account)
    }

    /**
     * 删除所有账户
     */
    @WorkerThread
    fun deleteAllAccount() {
        mAccountStorage.deleteAll()
    }

    /**
     * 获取当前账户账户
     */
    @Deprecated("功能删除")
    @Throws(AccountNotExistsException::class)
    fun currentAccount(): AccountDO {
        val currentWallet = mConfigSharedPreferences.getLong(CURRENT_ACCOUNT, 1)
        return mAccountStorage.findById(currentWallet) ?: throw AccountNotExistsException()
    }

    /**
     * 切换当前钱包账户
     */
    @Deprecated("功能删除")
    fun switchCurrentAccount(currentAccountID: Long = getDefaultAccountId()) {
        mConfigSharedPreferences.edit().putLong(CURRENT_ACCOUNT, currentAccountID).apply()
    }

    private fun getDefaultAccountId(): Long {
        return mAccountStorage.loadByWalletType()?.id ?: 1L
    }
    // </editor-fold>

    // <editor-fold defaultState="collapsed" desc="钱包操作相关">
    /**
     * 生成助记词
     */
    fun generateMnemonicWords(words: WordCount = WordCount.TWELVE): ArrayList<String> {
        return Mnemonic.English().generate(words)
    }

    /**
     * 导入身份钱包
     */
    @Throws(MnemonicException::class)
    fun importIdentityWallet(
        mnemonicWords: List<String>,
        password: ByteArray
    ) {
        checkMnemonicCount(mnemonicWords)

        val violasAccount = deriveViolas(mnemonicWords)
        val diemAccount = deriveDiem(mnemonicWords)
        val seed = SeedCalculator()
            .withWordsFromWordList(English.INSTANCE)
            .calculateSeed(mnemonicWords, "") ?: throw MnemonicException()
        val bitcoinKeyPair = deriveBitcoin(seed)
        val security = SimpleSecurity.instance(getContext())

        val accountIds = mAccountStorage.insert(
            AccountDO(
                privateKey = security.encrypt(
                    password,
                    violasAccount.keyPair.getPrivateKey().toByteArray()
                ),
                publicKey = violasAccount.getPublicKey(),
                authKeyPrefix = violasAccount.getAuthenticationKey().prefix().toHex(),
                address = violasAccount.getAddress().toHex(),
                coinNumber = getViolasCoinType().coinNumber(),
                mnemonic = security.encrypt(password, mnemonicWords.toString().toByteArray()),
                accountType = AccountType.NoDollars
            ),
            AccountDO(
                privateKey = security.encrypt(
                    password,
                    diemAccount.keyPair.getPrivateKey().toByteArray()
                ),
                publicKey = diemAccount.getPublicKey(),
                authKeyPrefix = diemAccount.getAuthenticationKey().prefix().toHex(),
                address = diemAccount.getAddress().toHex(),
                coinNumber = getDiemCoinType().coinNumber(),
                mnemonic = security.encrypt(password, mnemonicWords.toString().toByteArray()),
                accountType = AccountType.NoDollars
            ),
            AccountDO(
                privateKey = security.encrypt(password, bitcoinKeyPair.rawPrivateKey),
                publicKey = bitcoinKeyPair.publicKey,
                address = bitcoinKeyPair.address,
                coinNumber = getBitcoinCoinType().coinNumber(),
                mnemonic = security.encrypt(password, mnemonicWords.toString().toByteArray()),
                logo = "file:///android_asset/logo/ic_bitcoin_logo.png"
            )
        )

        if (accountIds.isNotEmpty()) {
            switchCurrentAccount(accountIds[0])
            CommandActuator.post(RefreshAssetsCommand(true))
        }

        if (accountIds.size > 1) {
            mAccountTokenStorage.insert(
                TokenDo(
                    accountId = accountIds[0],
                    assetsName = CURRENCY_DEFAULT_CODE,
                    module = CURRENCY_DEFAULT_CODE,
                    name = CURRENCY_DEFAULT_CODE,
                    address = CURRENCY_DEFAULT_ADDRESS,
                    enable = true,
                    logo = "file:///android_asset/logo/ic_violas_logo.png"
                ),
                TokenDo(
                    accountId = accountIds[1],
                    assetsName = org.palliums.libracore.common.CURRENCY_DEFAULT_CODE,
                    module = org.palliums.libracore.common.CURRENCY_DEFAULT_CODE,
                    name = org.palliums.libracore.common.CURRENCY_DEFAULT_CODE,
                    address = CURRENCY_DEFAULT_ADDRESS,
                    enable = true,
                    logo = "file:///android_asset/logo/ic_libra_logo.png"
                )
            )
        }
    }

    /**
     * 导入非身份钱包
     */
    @Throws(
        MnemonicException::class,
        ArrayIndexOutOfBoundsException::class,
        UnsupportedWalletException::class
    )
    @WorkerThread
    fun importNonIdentityWallet(
        coinType: CoinType,
        mnemonicWords: List<String>,
        password: ByteArray,
        walletName: String
    ): Long {
        checkMnemonicCount(mnemonicWords)

        val security = SimpleSecurity.instance(getContext())

        return when (coinType) {
            getViolasCoinType() -> {
                val violasAccount = deriveViolas(mnemonicWords)

                mAccountStorage.insert(
                    AccountDO(
                        privateKey = security.encrypt(
                            password,
                            violasAccount.keyPair.getPrivateKey().toByteArray()
                        ),
                        publicKey = violasAccount.getPublicKey(),
                        authKeyPrefix = violasAccount.getAuthenticationKey().prefix().toHex(),
                        address = violasAccount.getAddress().toHex(),
                        coinNumber = getViolasCoinType().coinNumber(),
                        mnemonic = security.encrypt(
                            password,
                            mnemonicWords.toString().toByteArray()
                        )
                    )
                )
            }

            getDiemCoinType() -> {
                val diemAccount = deriveDiem(mnemonicWords)

                mAccountStorage.insert(
                    AccountDO(
                        privateKey = security.encrypt(
                            password,
                            diemAccount.keyPair.getPrivateKey().toByteArray()
                        ),
                        publicKey = diemAccount.getPublicKey(),
                        authKeyPrefix = diemAccount.getAuthenticationKey().prefix().toHex(),
                        address = diemAccount.getAddress().toHex(),
                        coinNumber = getDiemCoinType().coinNumber(),
                        mnemonic = security.encrypt(
                            password,
                            mnemonicWords.toString().toByteArray()
                        )
                    )
                )
            }

            getBitcoinCoinType() -> {
                val seed = SeedCalculator()
                    .withWordsFromWordList(English.INSTANCE)
                    .calculateSeed(mnemonicWords, "") ?: throw MnemonicException()
                val bitcoinKeyPair = deriveBitcoin(seed)

                mAccountStorage.insert(
                    AccountDO(
                        privateKey = security.encrypt(password, bitcoinKeyPair.rawPrivateKey),
                        publicKey = bitcoinKeyPair.publicKey,
                        address = bitcoinKeyPair.address,
                        coinNumber = getBitcoinCoinType().coinNumber(),
                        mnemonic = security.encrypt(
                            password,
                            mnemonicWords.toString().toByteArray()
                        )
                    )
                )
            }

            else -> {
                throw UnsupportedWalletException()
            }
        }
    }

    private fun checkMnemonicCount(mnemonicWords: List<String>) {
        if (mnemonicWords.size != 12 &&
            mnemonicWords.size != 15 &&
            mnemonicWords.size != 18 &&
            mnemonicWords.size != 21 &&
            mnemonicWords.size != 24
        ) {
            throw MnemonicException()
        }
    }

    private fun deriveViolas(mnemonicWords: List<String>): Account {
        val keyFactory = KeyFactory(
            Seed.fromMnemonic(mnemonicWords)
        )
        return Account(keyFactory.generateKey(0))
    }

    private fun deriveDiem(mnemonicWords: List<String>): org.palliums.libracore.wallet.Account {
        val keyFactory = org.palliums.libracore.crypto.KeyFactory(
            org.palliums.libracore.crypto.Seed.fromMnemonic(mnemonicWords)
        )
        return org.palliums.libracore.wallet.Account(keyFactory.generateKey(0))
    }

    private fun deriveBitcoin(seed: ByteArray): BitCoinECKeyPair {
        val extendedKey = ExtendedKey.create(seed)
        val bip44Path = BIP44.m().purpose44().coinType(getBitcoinCoinType())
            .account(0).external().address(0)
        val derive = CoinPairDerive(extendedKey).derive(bip44Path)
        return derive as BitCoinECKeyPair
    }

    /**
     * 激活钱包（Violas链和Diem链的钱包需要激活才能使用）
     */
    suspend fun activateWallet(assets: DiemCoinAssetVo): Boolean {
        return when (assets.getCoinNumber()) {
            getViolasCoinType().coinNumber() -> {
                if (assets.authKey == null || isWalletActivated(assets.authKey!!)) {
                    false
                } else {
                    DataRepository.getViolasService()
                        .activateWallet(assets.address, assets.authKeyPrefix)
                    true
                }
            }

            getDiemCoinType().coinNumber() -> {
                if (assets.authKey == null || isWalletActivated(assets.authKey!!)) {
                    false
                } else {
                    DataRepository.getDiemBizService()
                        .activateWallet(assets.address, assets.authKeyPrefix)
                    true
                }
            }

            else -> {
                false
            }
        }
    }

    private fun isWalletActivated(authKey: String): Boolean {
        return authKey.length > 32 && authKey.substring(0, 32) != "00000000000000000000000000000000"
    }
    // </editor-fold>
}