package com.violas.wallet.biz

import androidx.annotation.WorkerThread
import com.palliums.content.ContextProvider
import com.palliums.violas.bean.TokenMark
import com.palliums.violas.http.ViolasMultiTokenRepository
import com.palliums.violas.smartcontract.ViolasMultiTokenContract
import com.quincysx.crypto.CoinType
import com.violas.wallet.biz.bean.AssertOriginateToken
import com.violas.wallet.biz.bean.DiemCurrency
import com.violas.wallet.biz.transaction.DiemTxnManager
import com.violas.wallet.biz.transaction.ViolasTxnManager
import com.violas.wallet.common.*
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountType
import com.violas.wallet.repository.database.entity.TokenDo
import kotlinx.coroutines.delay
import org.palliums.violascore.crypto.KeyPair
import org.palliums.violascore.transaction.TransactionPayload
import org.palliums.violascore.wallet.Account

class TokenManager {

    private val mTokenStorage by lazy { DataRepository.getTokenStorage() }
    private val mAccountStorage by lazy { DataRepository.getAccountStorage() }

    private val mViolasMultiTokenService by lazy {
        ViolasMultiTokenRepository(
            DataRepository.getMultiTokenContractService(),
            ViolasMultiTokenContract(isViolasTestNet())
        )
    }

    /**
     * 本地兼容的币种
     */
    private suspend fun loadNetWorkSupportViolasToken(): List<AssertOriginateToken> {
        val list = mutableListOf<AssertOriginateToken>()
        try {
            DataRepository.getViolasService().getCurrencies()?.forEach {
                list.add(
                    AssertOriginateToken(
                        currency = DiemCurrency(it.module, it.name, it.address),
                        name = it.showName,
                        fullName = it.showName,
                        isToken = true,
                        logo = it.showLogo,
                        coinNumber = getViolasCoinType().coinNumber()
                    )
                )
            }
        } catch (e: Exception) {

        }
        return list
    }

    /**
     * 本地兼容的币种
     */
    private suspend fun loadNetWorkSupportDiemToken(): List<AssertOriginateToken> {
        val list = mutableListOf<AssertOriginateToken>()
        try {
            DataRepository.getDiemBizService().getCurrencies()?.forEach {
                list.add(
                    AssertOriginateToken(
                        currency = DiemCurrency(it.module, it.name, it.address),
                        name = it.showName,
                        fullName = it.showName,
                        isToken = true,
                        logo = it.showLogo,
                        coinNumber = getDiemCoinType().coinNumber()
                    )
                )
            }
        } catch (e: Exception) {

        }
        return list
    }

    @WorkerThread
    fun findTokenById(tokenId: Long) = mTokenStorage.findById(tokenId)

    @WorkerThread
    fun loadTokensByAccountId(accountId: Long): List<TokenDo> =
        mTokenStorage.findByAccountId(accountId)

    @WorkerThread
    fun findTokenByName(accountId: Long, tokenName: String) =
        mTokenStorage.findByName(accountId, tokenName)

    data class CoinTokenMark(
        val currency: DiemCurrency?,
        val accountId: Long
    ) {
        override fun hashCode(): Int {
            var result = accountId.hashCode()
            return result * 31 + currency.hashCode()
        }
    }

    suspend fun loadSupportToken(): List<AssertOriginateToken> {
        val accounts = mAccountStorage.loadAll()

        val supportTokenMap = HashMap<CoinTokenMark, TokenDo>()

        val resultTokenList = mutableListOf<AssertOriginateToken>()

        accounts.forEach { account ->

            if (account.accountType == AccountType.Normal) {
                val coinType = CoinType.parseCoinNumber(account.coinNumber)
                resultTokenList.add(
                    0, AssertOriginateToken(
                        id = 0,
                        accountId = account.id,
                        enable = true,
                        isToken = false,
                        name = coinType.coinName(),
                        fullName = coinType.chainName(),
                        coinNumber = coinType.coinNumber(),
                        amount = account.amount,
                        logo = account.logo
                    )
                )
            }

            var loadSupportToken: List<AssertOriginateToken>? = null
            when (account.coinNumber) {
                getBitcoinCoinType().coinNumber() -> {

                }
                getViolasCoinType().coinNumber() -> {
                    loadSupportToken = loadNetWorkSupportViolasToken()
                }
                getDiemCoinType().coinNumber() -> {
                    loadSupportToken = loadNetWorkSupportDiemToken()
                }
            }

            val localSupportTokenMap = HashMap<DiemCurrency?, Int>()

            val localToken = mTokenStorage.findByAccountId(account.id)
            localToken.map {
                supportTokenMap[CoinTokenMark(
                    DiemCurrency(it.module, it.name, it.address),
                    it.accountId
                )] = it
            }

            loadSupportToken?.forEach { token ->
                localSupportTokenMap[token.currency] = 0
                token.accountId = account.id
//                token.logo = logo
                supportTokenMap[CoinTokenMark(token.currency, account.id)]?.let {
                    token.enable = it.enable
                }
            }

            if (loadSupportToken != null) {
                resultTokenList.addAll(loadSupportToken)
            }
        }

        return resultTokenList
    }


    @WorkerThread
    fun insert(checked: Boolean, assertOriginateToken: AssertOriginateToken) {
        mTokenStorage.insert(
            TokenDo(
                enable = checked,
                accountId = assertOriginateToken.accountId,
                assetsName = assertOriginateToken.name,
                name = assertOriginateToken.currency?.name ?: "",
                address = assertOriginateToken.currency?.address ?: "",
                module = assertOriginateToken.currency?.module ?: "",
                logo = assertOriginateToken.logo
            )
        )
    }

    @WorkerThread
    fun deleteAllToken() {
        mTokenStorage.deleteAll()
    }

    suspend fun getTokenBalance(
        address: String,
        tokenIdx: Long
    ): Long {
        val tokens =
            mViolasMultiTokenService.getBalance(address, arrayListOf(tokenIdx))?.modules

        var amount = 0L
        tokens?.forEach { token ->
            if (token.id == tokenIdx) {
                amount = token.balance
                return@forEach
            }
        }
        return amount
    }

    @Throws(RuntimeException::class)
    @WorkerThread
    suspend fun publishToken(
        coinType: CoinType,
        privateKey: ByteArray,
        currency: DiemCurrency
    ): Boolean {
        when (coinType.coinNumber()) {
            getViolasCoinType().coinNumber() -> {
                val violasRpcService = DataRepository.getViolasRpcService()
                val violasTxnManager = ViolasTxnManager()

                // 检查发送人账户
                val senderAccount = Account(KeyPair.fromSecretKey(privateKey))
                val senderAccountState =
                    violasTxnManager.getSenderAccountState(senderAccount) {
                        violasRpcService.getAccountState(it)
                    }

                // 计算gas info
                val gasInfo = violasTxnManager.calculateGasInfo(
                    senderAccountState,
                    null
                )

                val transactionResult = violasRpcService.addCurrency(
                    ContextProvider.getContext(),
                    senderAccount,
                    currency.address,
                    currency.module,
                    currency.name,
                    sequenceNumber = senderAccountState.sequenceNumber,
                    gasCurrencyCode = gasInfo.gasCurrencyCode,
                    maxGasAmount = gasInfo.maxGasAmount,
                    gasUnitPrice = gasInfo.gasUnitPrice,
                    chainId = getViolasChainId()
                )

                for (item in 1 until 4) {
                    delay(item * 1000L)
                    val transaction = violasRpcService.getTransaction(
                        transactionResult.payerAddress,
                        transactionResult.sequenceNumber
                    )
                    if (transaction?.isSuccessExecuted() == true) {
                        return true
                    }
                }
            }

            getDiemCoinType().coinNumber() -> {
                val diemRpcService = DataRepository.getDiemRpcService()
                val diemTxnManager = DiemTxnManager()

                // 检查发送人账户
                val senderAccount = org.palliums.libracore.wallet.Account(
                    org.palliums.libracore.crypto.KeyPair.fromSecretKey(privateKey)
                )
                val senderAccountState =
                    diemTxnManager.getSenderAccountState(senderAccount) {
                        diemRpcService.getAccountState(it)
                    }

                // 计算gas info
                val gasInfo = diemTxnManager.calculateGasInfo(
                    senderAccountState,
                    null
                )

                val transactionResult = diemRpcService.addCurrency(
                    ContextProvider.getContext(),
                    senderAccount,
                    currency.address,
                    currency.module,
                    currency.name,
                    sequenceNumber = senderAccountState.sequenceNumber,
                    gasCurrencyCode = gasInfo.gasCurrencyCode,
                    maxGasAmount = gasInfo.maxGasAmount,
                    gasUnitPrice = gasInfo.gasUnitPrice,
                    chainId = getDiemChainId()
                )

                for (item in 1 until 4) {
                    delay(item * 1000L)
                    val transaction = diemRpcService.getTransaction(
                        transactionResult.payerAddress,
                        transactionResult.sequenceNumber
                    )
                    if (transaction?.isSuccessExecuted() == true) {
                        return true
                    }
                }
            }

            else -> throw RuntimeException("error")
        }
        return false
    }

    @Throws(RuntimeException::class)
    @WorkerThread
    suspend fun publishToken(
        accountId: Long,
        privateKey: ByteArray,
        currency: DiemCurrency
    ): Boolean {
        mAccountStorage.findById(accountId)?.let {
            return publishToken(CoinType.parseCoinNumber(it.coinNumber), privateKey, currency)
        }
        return false
    }

    @Deprecated("删除")
    suspend fun isPublish(address: String): Boolean {
        return mViolasMultiTokenService.getRegisterToken(address)
    }

    suspend fun isPublish(accountId: Long, diemCurrency: DiemCurrency): Boolean {
        return mAccountStorage.findById(accountId)?.let {
            var isPublish = false
            when (it.coinNumber) {
                getViolasCoinType().coinNumber() -> {
                    DataRepository.getViolasRpcService()
                        .getAccountState(it.address)?.balances?.forEach { accountBalance ->
                            if (diemCurrency.module == accountBalance.currency) {
                                isPublish = true
                            }
                        }
                }
                getDiemCoinType().coinNumber() -> {
                    DataRepository.getDiemRpcService()
                        .getAccountState(it.address)?.balances?.forEach { accountBalance ->
                            if (diemCurrency.module == accountBalance.currency) {
                                isPublish = true
                            }
                        }
                }
            }
            isPublish
        } ?: false
    }

    fun transferTokenPayload(
        tokenIdx: Long,
        address: String,
        amount: Long,
        date: ByteArray = byteArrayOf()
    ): TransactionPayload {
        return mViolasMultiTokenService.transferTokenPayload(
            tokenIdx, address, amount, date
        )
    }
}