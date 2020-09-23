package com.violas.wallet.biz

import android.content.Context
import androidx.annotation.WorkerThread
import com.palliums.content.ContextProvider.getContext
import com.palliums.exceptions.RequestException
import com.palliums.utils.exceptionAsync
import com.palliums.utils.toMap
import com.quincysx.crypto.CoinTypes
import com.quincysx.crypto.bip32.ExtendedKey
import com.quincysx.crypto.bip44.BIP44
import com.quincysx.crypto.bip44.CoinPairDerive
import com.quincysx.crypto.bip44.CoinType
import com.quincysx.crypto.bitcoin.BitCoinECKeyPair
import com.quincysx.crypto.ethereum.EthECKeyPair
import com.violas.wallet.biz.command.CommandActuator
import com.violas.wallet.biz.command.RefreshAssetsAllListCommand
import com.violas.wallet.biz.command.SaveAssetsFiatBalanceCommand
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.database.entity.AccountType
import com.violas.wallet.repository.database.entity.TokenDo
import com.violas.wallet.utils.convertAmountToDisplayUnit
import com.violas.wallet.viewModel.bean.*
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.palliums.libracore.crypto.KeyFactory
import org.palliums.libracore.crypto.Seed
import org.palliums.libracore.mnemonic.Mnemonic
import org.palliums.libracore.mnemonic.WordCount
import org.palliums.libracore.transaction.AccountAddress
import org.palliums.libracore.wallet.Account
import org.palliums.violascore.serialization.toHex
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MnemonicException : RuntimeException()
class AccountNotExistsException : RuntimeException()

class AccountManager {
    companion object {
        private const val CURRENT_ACCOUNT = "key_0"
        private const val KEY_FAST_INTO_WALLET = "key_1"
        private const val KEY_IDENTITY_MNEMONIC_BACKUP = "key_2"
        private const val KEY_PROMPT_OPEN_BIOMETRICS = "key_3"
        private const val KEY_SECURITY_PASSWORD = "key_4"

        /**
         * 获取生物识别加解密所用的key
         */
        fun getBiometricKey(): String {
            return KEY_SECURITY_PASSWORD
        }
    }

    private val mExecutor by lazy { Executors.newFixedThreadPool(4) }

    private val mConfigSharedPreferences by lazy {
        getContext().getSharedPreferences("config", Context.MODE_PRIVATE)
    }

    private val mAccountStorage by lazy {
        DataRepository.getAccountStorage()
    }

    private val mAccountTokenStorage by lazy {
        DataRepository.getTokenStorage()
    }

    fun updateSecurityPassword(securityPassword: String) {
        mConfigSharedPreferences.edit().putString(KEY_SECURITY_PASSWORD, securityPassword).apply()
    }

    fun getSecurityPassword(): String? {
        return mConfigSharedPreferences.getString(KEY_SECURITY_PASSWORD, null)
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
     * 获取当前账户账户
     */
    @Throws(AccountNotExistsException::class)
    fun getAccountById(accountId: Long = 1): AccountDO {
        return mAccountStorage.findById(accountId) ?: throw AccountNotExistsException()
    }

    /**
     * 获取当前账户账户
     */
    @Throws(AccountNotExistsException::class)
    suspend fun getDefaultAccount(): AccountDO = withContext(Dispatchers.IO) {
        val accounts = mAccountStorage.loadAll()
        if (accounts.isEmpty()) {
            throw AccountNotExistsException()
        }
        return@withContext accounts[0]
    }

    /**
     * 是否存在账户
     */
    @Deprecated("功能删除")
    fun existsWalletAccount(): Boolean {
        if (mAccountStorage.loadByWalletType() == null) {
            return false
        }
        return true
    }

    private fun getDefWallet(): Long {
        return mAccountStorage.loadByWalletType()?.id ?: 1L
    }

    fun removeWallet(accountId: AccountDO) {
        mAccountStorage.delete(accountId)
    }

    @WorkerThread
    fun deleteAllAccount() {
        mAccountStorage.deleteAll()
    }

    fun clearLocalConfig() {
        mConfigSharedPreferences.edit()
            .putBoolean(KEY_IDENTITY_MNEMONIC_BACKUP, false)
            .putBoolean(KEY_PROMPT_OPEN_BIOMETRICS, false)
            .putString(KEY_SECURITY_PASSWORD, "")
            .apply()
    }

    fun getIdentityByCoinType(coinType: Int): AccountDO? {
        return mAccountStorage.findByCoinType(coinType)
    }

    fun getIdentityAccount(): AccountDO {
        return mAccountStorage.loadByWalletType()!!
    }

    /**
     * 切换当前钱包账户
     */
    @Deprecated("功能删除")
    fun switchCurrentAccount(currentAccountID: Long = getDefWallet()) {
        mConfigSharedPreferences.edit().putLong(CURRENT_ACCOUNT, currentAccountID).apply()
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
     * 获取身份钱包的助记词
     */
    fun getIdentityWalletMnemonic(context: Context, password: ByteArray): ArrayList<String>? {
        val account = mAccountStorage.loadByWalletType()!!
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
            CoinTypes.Ethereum -> {
                AccountManager().importEthWallet(
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
        val deriveLibra = deriveViolas(wordList)
        val security = SimpleSecurity.instance(context)

        return mAccountStorage.insert(
            AccountDO(
                privateKey = security.encrypt(
                    password,
                    deriveLibra.keyPair.getPrivateKey().toByteArray()
                ),
                publicKey = deriveLibra.getPublicKey(),
                authKeyPrefix = deriveLibra.getAuthenticationKey().prefix().toHex(),
                address = deriveLibra.getAddress().toHex(),
                coinNumber = CoinTypes.Violas.coinType(),
                mnemonic = security.encrypt(password, wordList.toString().toByteArray())
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
                privateKey = security.encrypt(
                    password,
                    deriveLibra.keyPair.getPrivateKey().toByteArray()
                ),
                publicKey = deriveLibra.getPublicKey(),
                authKeyPrefix = deriveLibra.getAuthenticationKey().prefix().toHex(),
                address = deriveLibra.getAddress().toHex(),
                coinNumber = CoinTypes.Libra.coinType(),
                mnemonic = security.encrypt(password, wordList.toString().toByteArray())
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
                mnemonic = security.encrypt(password, wordList.toString().toByteArray())
            )
        )
    }

    /**
     * 导入 EthWallet 钱包（非身份钱包）
     */
    @Throws(MnemonicException::class)
    private fun importEthWallet(
        context: Context,
        wordList: List<String>,
        walletName: String,
        password: ByteArray
    ): Long {
        val seed = Mnemonic.English()
            .toByteArray(wordList) ?: throw MnemonicException()

        val deriveBitcoin = deriveEthereum(seed)
        val security = SimpleSecurity.instance(context)

        return mAccountStorage.insert(
            AccountDO(
                privateKey = security.encrypt(password, deriveBitcoin.rawPrivateKey),
                publicKey = deriveBitcoin.publicKey,
                address = deriveBitcoin.address,
                coinNumber = CoinTypes.Ethereum.coinType(),
                mnemonic = security.encrypt(password, wordList.toString().toByteArray())
            )
        )
    }

    /**
     * 创建身份
     */
    fun createIdentity(context: Context, password: ByteArray): List<String> {
        val generate = Mnemonic.English().generate()
        importIdentity(context, generate, password)
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
        password: ByteArray
    ) {
        checkMnemonicCount(wordList)

        val seed = Mnemonic.English()
            .toByteArray(wordList) ?: throw MnemonicException()

        val deriveBitcoin = deriveBitcoin(seed)
        val deriveEthereum = deriveEthereum(seed)

        val deriveLibra = deriveLibra(wordList)
        val deriveViolas = deriveViolas(wordList)

        val security = SimpleSecurity.instance(context)

        val insertIds = mAccountStorage.insert(
            AccountDO(
                privateKey = security.encrypt(
                    password,
                    deriveViolas.keyPair.getPrivateKey().toByteArray()
                ),
                publicKey = deriveViolas.getPublicKey(),
                authKeyPrefix = deriveViolas.getAuthenticationKey().prefix().toHex(),
                address = deriveViolas.getAddress().toHex(),
                coinNumber = CoinTypes.Violas.coinType(),
                mnemonic = security.encrypt(password, wordList.toString().toByteArray()),
                accountType = AccountType.NoDollars
            ),
            AccountDO(
                privateKey = security.encrypt(
                    password,
                    deriveLibra.keyPair.getPrivateKey().toByteArray()
                ),
                publicKey = deriveLibra.getPublicKey(),
                authKeyPrefix = deriveLibra.getAuthenticationKey().prefix().toHex(),
                address = deriveLibra.getAddress().toHex(),
                coinNumber = CoinTypes.Libra.coinType(),
                mnemonic = security.encrypt(password, wordList.toString().toByteArray()),
                accountType = AccountType.NoDollars
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
                logo = "file:///android_asset/logo/ic_bitcoin_logo.png"
            ),
            AccountDO(
                privateKey = security.encrypt(password, deriveEthereum.rawPrivateKey),
                publicKey = deriveEthereum.publicKey,
                address = deriveEthereum.address,
                coinNumber = CoinTypes.Ethereum.coinType(),
                mnemonic = security.encrypt(password, wordList.toString().toByteArray()),
                logo = "file:///android_asset/logo/ic_bitcoin_logo.png"
            )
        )
        if (insertIds.size > 1) {
            mAccountTokenStorage.insert(
                TokenDo(
                    account_id = insertIds[0],
                    assetsName = "LBR",
                    name = "LBR",
                    address = "00000000000000000000000000000001",
                    enable = true,
                    logo = "file:///android_asset/logo/ic_violas_logo.png"
                ),
                TokenDo(
                    account_id = insertIds[1],
                    assetsName = "LBR",
                    name = "LBR",
                    address = "00000000000000000000000000000001",
                    enable = true,
                    logo = "file:///android_asset/logo/ic_libra_logo.png"
                )
            )
        }
        if (insertIds.isNotEmpty()) {
            switchCurrentAccount(insertIds[0])
            CommandActuator.post(RefreshAssetsAllListCommand(true))
        }
    }

    private fun deriveViolas(wordList: List<String>): org.palliums.violascore.wallet.Account {
        val keyFactory = org.palliums.violascore.crypto.KeyFactory(
            org.palliums.violascore.crypto.Seed.fromMnemonic(wordList)
        )
        return org.palliums.violascore.wallet.Account(keyFactory.generateKey(0))
    }

    private fun deriveLibra(wordList: List<String>): Account {
        val keyFactory = KeyFactory(
            Seed.fromMnemonic(wordList)
        )
        return Account(keyFactory.generateKey(0))
    }

    private fun deriveEthereum(seed: ByteArray): EthECKeyPair {
        val extendedKey = ExtendedKey.create(seed)
        val bip44Path =
            BIP44.m().purpose44().coinType(CoinTypes.Ethereum).account(0).external().address(0)
        val derive = CoinPairDerive(extendedKey).derive(bip44Path)
        return derive as EthECKeyPair
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

    suspend fun getBalance(account: AccountDO): Long {
        return when (account.coinNumber) {
            CoinTypes.Violas.coinType() -> {
                DataRepository.getViolasService().getBalanceInMicroLibra(account.address)
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
                            coroutine.resumeWithException(
                                RequestException(
                                    it
                                )
                            )
                        })
                    coroutine.invokeOnCancellation {
                        subscribe.dispose()
                    }
                }
            }
        }
    }

    suspend fun activateAccount(assets: AssetsLibraCoinVo) {
        when (assets.getCoinNumber()) {
            CoinTypes.Violas.coinType() -> {
                if (!isActivate(assets.authKey)) {
                    DataRepository.getViolasService()
                        .activateAccount(assets.address, assets.authKeyPrefix)
                }
            }
            CoinTypes.Libra.coinType() -> {
                if (!isActivate(assets.authKey)) {
                    DataRepository.getLibraBizService()
                        .activateAccount(assets.address, assets.authKeyPrefix)
                }
            }
        }
    }

    private fun isActivate(authKey: String?): Boolean {
        if (authKey == null || authKey.length < 32 || authKey.substring(
                0,
                32
            ) == "00000000000000000000000000000000"
        ) {
            return false
        }
        return true
    }

    fun getLocalAssets(): MutableList<AssetsVo> {
        val assetsFiatBalanceSharedPreferences = getContext().getSharedPreferences(
            SaveAssetsFiatBalanceCommand.sharedPreferencesFileName(),
            Context.MODE_PRIVATE
        )

        val localAssets = mutableListOf<AssetsVo>()

        val loadAll = mAccountStorage.loadAll()
        loadAll.forEach {
            when (it.coinNumber) {
                CoinTypes.Libra.coinType() -> {
                    localAssets.add(
                        AssetsLibraCoinVo(
                            it.id,
                            it.publicKey,
                            "",
                            it.authKeyPrefix,
                            it.coinNumber,
                            it.address,
                            it.amount,
                            it.logo
                        ).also {
                            it.setAssetsName(CoinTypes.Libra.coinName())
                        }
                    )
                }
                CoinTypes.Violas.coinType() -> {
                    localAssets.add(
                        AssetsLibraCoinVo(
                            it.id,
                            it.publicKey,
                            "",
                            it.authKeyPrefix,
                            it.coinNumber,
                            it.address,
                            it.amount,
                            it.logo
                        ).also {
                            it.setAssetsName(CoinTypes.Violas.coinName())
                        }
                    )
                }
                else -> {
                    localAssets.add(
                        AssetsCoinVo(
                            it.id,
                            it.publicKey,
                            it.coinNumber,
                            it.address,
                            it.amount,
                            it.accountType,
                            it.logo
                        ).also { coin ->
                            val parseCoinType = CoinTypes.parseCoinType(coin.getCoinNumber())
                            coin.setAssetsName(parseCoinType.coinName())

                            val convertDisplayUnitToAmount = convertAmountToDisplayUnit(
                                it.amount,
                                parseCoinType
                            )

                            coin.amountWithUnit.amount = convertDisplayUnitToAmount.first
                            coin.amountWithUnit.unit = coin.getAssetsName()

                            val rateAmount = assetsFiatBalanceSharedPreferences.getString(
                                SaveAssetsFiatBalanceCommand.tokenKey(
                                    coin
                                ), "0.00"
                            ) ?: "0.00"
                            coin.fiatAmountWithUnit.rate = rateAmount
                            coin.fiatAmountWithUnit.amount =
                                BigDecimal(coin.amountWithUnit.amount).multiply(
                                    BigDecimal(rateAmount)
                                ).setScale(2, RoundingMode.DOWN).stripTrailingZeros()
                                    .toPlainString()
                        }
                    )
                }
            }
        }

        val localCoinMap = loadAll.toMap { accountDO -> accountDO.id.toString() }
        val localTokenAssets = mAccountTokenStorage.loadEnableAll()

        localTokenAssets.forEach {
            if (localCoinMap.containsKey(it.account_id.toString())) {
                localAssets.add(
                    AssetsTokenVo(
                        it.id!!,
                        it.account_id,
                        localCoinMap[it.account_id.toString()]?.coinNumber ?: 0,
                        it.address,
                        it.module,
                        it.name,
                        it.enable,
                        it.amount,
                        it.logo
                    ).also { tokenVo ->
                        tokenVo.setAssetsName(it.assetsName)
                        tokenVo.amountWithUnit.amount = BigDecimal(it.amount).divide(
                            BigDecimal("1000000"),
                            6,
                            RoundingMode.DOWN
                        ).stripTrailingZeros().toPlainString()
                        tokenVo.amountWithUnit.unit = it.assetsName

                        val rateAmount = assetsFiatBalanceSharedPreferences.getString(
                            SaveAssetsFiatBalanceCommand.tokenKey(
                                tokenVo
                            ), "0.00"
                        ) ?: "0.00"
                        tokenVo.fiatAmountWithUnit.rate = rateAmount
                        tokenVo.fiatAmountWithUnit.amount =
                            BigDecimal(tokenVo.amountWithUnit.amount).multiply(
                                BigDecimal(rateAmount)
                            ).setScale(2, RoundingMode.DOWN).stripTrailingZeros().toPlainString()
                    }
                )
            }
        }

        return localAssets
    }

    suspend fun refreshAssetsAmount(localAssets: MutableList<AssetsVo>): MutableList<AssetsVo> {
        val assets = localAssets.toMutableList()
        val exceptionAsync = GlobalScope.exceptionAsync { queryBTCBalance(assets) }
        val exceptionAsync1 = GlobalScope.exceptionAsync { queryLibraBalance(assets) }
        val exceptionAsync2 = GlobalScope.exceptionAsync { queryViolasBalance(assets) }

        exceptionAsync.await()
        exceptionAsync1.await()
        exceptionAsync2.await()
        return assets
    }

    private fun queryBTCBalance(localAssets: List<AssetsVo>) {
        localAssets.filter { it is AssetsCoinVo && (it.getCoinNumber() == CoinTypes.BitcoinTest.coinType() || it.getCoinNumber() == CoinTypes.Bitcoin.coinType()) }
            .forEach { assets ->
                assets as AssetsCoinVo
                val subscribe = DataRepository.getBitcoinService()
                    .getBalance(assets.address)
                    .subscribe({ balance ->
                        assets.setAmount(balance.toLong())
                        val convertAmountToDisplayUnit = convertAmountToDisplayUnit(
                            balance.toLong(),
                            CoinTypes.parseCoinType(assets.getCoinNumber())
                        )
                        assets.amountWithUnit.amount = convertAmountToDisplayUnit.first
                        assets.amountWithUnit.unit = convertAmountToDisplayUnit.second
                    }, {
                        it.printStackTrace()
                    })
            }
    }

    private suspend fun queryLibraBalance(localAssets: MutableList<AssetsVo>) {
        val assetsFiatBalanceSharedPreferences = getContext().getSharedPreferences(
            SaveAssetsFiatBalanceCommand.sharedPreferencesFileName(),
            Context.MODE_PRIVATE
        )
        localAssets.filter { it is AssetsLibraCoinVo && it.getCoinNumber() == CoinTypes.Libra.coinType() }
            .forEach { assets ->
                try {
                    assets as AssetsLibraCoinVo
                    DataRepository.getLibraService().getAccountState(assets.address)?.let { it ->
                        assets.authKey = it.authenticationKey
                        assets.delegatedKeyRotationCapability = it.delegatedKeyRotationCapability
                        assets.delegatedWithdrawalCapability = it.delegatedWithdrawalCapability

                        val filter =
                            localAssets.filter { assetsToken -> assetsToken is AssetsTokenVo && assetsToken.getAccountId() == assets.getAccountId() }
                                .toMap { assetsToken ->
                                    (assetsToken as AssetsTokenVo).module.toUpperCase(Locale.getDefault())
                                }
                        it.balances?.forEach { balance ->
                            val assetsVo = filter[balance.currency.toUpperCase(Locale.getDefault())]
                            if (assetsVo == null) {
                                localAssets.add(HiddenTokenVo(
                                    assets.getId(),
                                    assets.getAccountId(),
                                    assets.getCoinNumber(),
                                    AccountAddress.DEFAULT.toHex(),
                                    balance.currency,
                                    balance.currency,
                                    amount = balance.amount
                                ).also { tokenVo ->
                                    tokenVo.setAssetsName(balance.currency)
                                    tokenVo.amountWithUnit.amount =
                                        BigDecimal(balance.amount).divide(
                                            BigDecimal("1000000"),
                                            6,
                                            RoundingMode.DOWN
                                        ).stripTrailingZeros().toPlainString()
                                    tokenVo.amountWithUnit.unit = balance.currency

                                    val rateAmount = assetsFiatBalanceSharedPreferences.getString(
                                        SaveAssetsFiatBalanceCommand.tokenKey(
                                            tokenVo
                                        ), "0.00"
                                    ) ?: "0.00"
                                    tokenVo.fiatAmountWithUnit.rate = rateAmount
                                    tokenVo.fiatAmountWithUnit.amount =
                                        BigDecimal(tokenVo.amountWithUnit.amount).multiply(
                                            BigDecimal(rateAmount)
                                        ).setScale(2, RoundingMode.DOWN).stripTrailingZeros()
                                            .toPlainString()
                                })
                            } else {
                                assetsVo.apply {
                                    this as AssetsTokenVo
                                    setAmount(balance.amount)
                                    val convertAmountToDisplayUnit = convertAmountToDisplayUnit(
                                        balance.amount,
                                        CoinTypes.parseCoinType(assets.getCoinNumber())
                                    )
                                    amountWithUnit.amount = convertAmountToDisplayUnit.first
                                    amountWithUnit.unit = getAssetsName()
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
    }

    private suspend fun queryViolasBalance(localAssets: MutableList<AssetsVo>) {
        val assetsFiatBalanceSharedPreferences = getContext().getSharedPreferences(
            SaveAssetsFiatBalanceCommand.sharedPreferencesFileName(),
            Context.MODE_PRIVATE
        )
        localAssets.filter { it is AssetsCoinVo && it.getCoinNumber() == CoinTypes.Violas.coinType() }
            .forEach { assets ->
                assets as AssetsLibraCoinVo
                try {
                    DataRepository.getViolasChainRpcService().getAccountState(assets.address)?.let {
                        assets.authKey = it.authenticationKey
                        assets.delegatedKeyRotationCapability = it.delegatedKeyRotationCapability
                        assets.delegatedWithdrawalCapability = it.delegatedWithdrawalCapability

                        val filter =
                            localAssets.filter { assetsToken -> assetsToken is AssetsTokenVo && assetsToken.getAccountId() == assets.getAccountId() }
                                .toMap { assetsToken ->
                                    (assetsToken as AssetsTokenVo).module.toUpperCase(Locale.getDefault())
                                }
                        it.balances?.forEach { balance ->
                            val assetsVo = filter[balance.currency.toUpperCase(Locale.getDefault())]
                            if (assetsVo == null) {
                                localAssets.add(HiddenTokenVo(
                                    assets.getId(),
                                    assets.getAccountId(),
                                    assets.getCoinNumber(),
                                    org.palliums.violascore.transaction.AccountAddress.DEFAULT.toHex(),
                                    balance.currency,
                                    balance.currency,
                                    amount = balance.amount
                                ).also { tokenVo ->
                                    tokenVo.setAssetsName(balance.currency)
                                    tokenVo.amountWithUnit.amount =
                                        BigDecimal(balance.amount).divide(
                                            BigDecimal("1000000"),
                                            6,
                                            RoundingMode.DOWN
                                        ).stripTrailingZeros().toPlainString()
                                    tokenVo.amountWithUnit.unit = balance.currency

                                    val rateAmount = assetsFiatBalanceSharedPreferences.getString(
                                        SaveAssetsFiatBalanceCommand.tokenKey(
                                            tokenVo
                                        ), "0.00"
                                    ) ?: "0.00"
                                    tokenVo.fiatAmountWithUnit.rate = rateAmount
                                    tokenVo.fiatAmountWithUnit.amount =
                                        BigDecimal(tokenVo.amountWithUnit.amount).multiply(
                                            BigDecimal(rateAmount)
                                        ).setScale(2, RoundingMode.DOWN).stripTrailingZeros()
                                            .toPlainString()
                                })
                            } else {
                                assetsVo.apply {
                                    this as AssetsTokenVo
                                    setAmount(balance.amount)
                                    val convertAmountToDisplayUnit = convertAmountToDisplayUnit(
                                        balance.amount,
                                        CoinTypes.parseCoinType(assets.getCoinNumber())
                                    )
                                    amountWithUnit.amount = convertAmountToDisplayUnit.first
                                    amountWithUnit.unit = getAssetsName()
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
    }

    suspend fun refreshFiatAssetsAmount(localAssets: MutableList<AssetsVo>): MutableList<AssetsVo> {
        val assets = localAssets.toMutableList()
        val exceptionAsync =
            GlobalScope.exceptionAsync { queryFiatBalance(assets) { it is AssetsCoinVo && it.getCoinNumber() == CoinTypes.Violas.coinType() } }
        val exceptionAsync1 =
            GlobalScope.exceptionAsync { queryFiatBalance(assets) { it is AssetsCoinVo && it.getCoinNumber() == CoinTypes.Libra.coinType() } }
        val exceptionAsync2 =
            GlobalScope.exceptionAsync { queryFiatBalance(assets) { it is AssetsCoinVo && (it.getCoinNumber() == CoinTypes.Bitcoin.coinType() || it.getCoinNumber() == CoinTypes.BitcoinTest.coinType()) } }

        exceptionAsync.await()
        exceptionAsync1.await()
        exceptionAsync2.await()
        return assets
    }

    private suspend fun queryFiatBalance(
        localAssets: List<AssetsVo>,
        filter: (AssetsVo) -> Boolean
    ) {
        localAssets.filter { filter.invoke(it) }
            .forEach { assets ->
                try {
                    assets as AssetsCoinVo
                    val fiatBalances = when (assets.getCoinNumber()) {
                        CoinTypes.BitcoinTest.coinType(),
                        CoinTypes.Bitcoin.coinType() -> {
                            DataRepository.getViolasService().getBTCChainFiatBalance(assets.address)
                        }
                        CoinTypes.Violas.coinType() -> {
                            DataRepository.getViolasService()
                                .getViolasChainFiatBalance(assets.address)
                        }
                        CoinTypes.Libra.coinType() -> {
                            DataRepository.getViolasService()
                                .getLibraChainFiatBalance(assets.address)
                        }
                        else -> null
                    }

                    val fiatBalanceMap =
                        fiatBalances?.toMap { fiatBalanceDTO -> fiatBalanceDTO.name }

                    localAssets.filter { it.getCoinNumber() == assets.getCoinNumber() }
                        .map { assetsVo ->
                            val currentAssetsFiatBalance = when (assetsVo) {
                                is AssetsTokenVo -> fiatBalanceMap?.get(assetsVo.module)
                                is AssetsCoinVo -> fiatBalanceMap?.get(
                                    CoinTypes.parseCoinType(assetsVo.getCoinNumber()).coinName()
                                )
                                else -> null
                            }
                            currentAssetsFiatBalance?.let {
                                try {
                                    if (assetsVo.amountWithUnit.amount.toDouble() == 0.0 || it.rate == 0.0) {
                                        assetsVo.fiatAmountWithUnit.amount = "0.00"
                                    } else {
                                        assetsVo.fiatAmountWithUnit.rate = it.rate.toString()
                                        assetsVo.fiatAmountWithUnit.amount =
                                            BigDecimal(assetsVo.amountWithUnit.amount).multiply(
                                                BigDecimal(it.rate.toString())
                                            ).setScale(2, RoundingMode.DOWN).toPlainString()
                                    }
                                } catch (e: Exception) {
                                    assetsVo.fiatAmountWithUnit.amount = "0.00"
                                }
                            }
                        }
                } catch (e: Exception) {
                }
            }
    }
}