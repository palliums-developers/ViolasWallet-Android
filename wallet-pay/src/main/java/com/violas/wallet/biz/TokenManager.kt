package com.violas.wallet.biz

import androidx.annotation.WorkerThread
import com.palliums.content.ContextProvider
import com.palliums.violas.bean.TokenMark
import com.palliums.violas.http.ViolasMultiTokenRepository
import com.palliums.violas.smartcontract.ViolasMultiTokenContract
import com.quincysx.crypto.CoinType
import com.violas.wallet.biz.bean.AssertOriginateToken
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

    private val mViolasService by lazy {
        DataRepository.getViolasService()
    }

    private val mViolasMultiTokenService by lazy {
        ViolasMultiTokenRepository(
            DataRepository.getMultiTokenContractService(),
            ViolasMultiTokenContract(isViolasTestNet())
        )
    }

    fun getViolasMultiTokenContract() =
        mViolasMultiTokenService.getMultiTokenContract()

    /**
     * 本地兼容的币种
     */
    private suspend fun loadNetWorkSupportViolasToken(): List<AssertOriginateToken> {
        val list = mutableListOf<AssertOriginateToken>()
        try {
            DataRepository.getViolasService().getCurrencies()?.forEach {
                list.add(
                    AssertOriginateToken(
                        tokenMark = TokenMark(it.module, it.address, it.name),
                        name = it.showName,
                        fullName = it.showName,
                        isToken = true,
                        logo = it.showLogo,
                        coinType = getViolasCoinType().coinNumber()
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
    private suspend fun loadNetWorkSupportLibraToken(): List<AssertOriginateToken> {
        val list = mutableListOf<AssertOriginateToken>()
        try {
            DataRepository.getLibraBizService().getCurrencies()?.forEach {
                list.add(
                    AssertOriginateToken(
                        tokenMark = TokenMark(it.module, it.address, it.name),
                        name = it.showName,
                        fullName = it.showName,
                        isToken = true,
                        logo = it.showLogo,
                        coinType = getDiemCoinType().coinNumber()
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
        val tokenMark: TokenMark?,
        val accountId: Long
    ) {
        override fun hashCode(): Int {
            var result = accountId.hashCode()
            return result * 31 + tokenMark.hashCode()
        }
    }

    suspend fun loadSupportToken(): List<AssertOriginateToken> {
        val accounts = mAccountStorage.loadAll()

        val supportTokenMap = HashMap<CoinTokenMark, TokenDo>()

        val resultTokenList = mutableListOf<AssertOriginateToken>()

        accounts.forEach { account ->

            if (account.accountType == AccountType.Normal) {
                val coinTypes = CoinType.parseCoinNumber(account.coinNumber)
                resultTokenList.add(
                    0, AssertOriginateToken(
                        id = 0,
                        account_id = account.id,
                        enable = true,
                        isToken = false,
                        name = coinTypes.coinName(),
                        fullName = coinTypes.chainName(),
                        coinType = coinTypes.coinNumber(),
                        amount = 0,
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
                    loadSupportToken = loadNetWorkSupportLibraToken()
                }
            }

            val localSupportTokenMap = HashMap<TokenMark?, Int>()

            val localToken = mTokenStorage.findByAccountId(account.id)
            localToken.map {
                supportTokenMap[CoinTokenMark(
                    TokenMark(it.module, it.address, it.name),
                    it.account_id
                )] = it
            }

            loadSupportToken?.forEach { token ->
                localSupportTokenMap[token.tokenMark] = 0
                token.account_id = account.id
//                token.logo = logo
                supportTokenMap[CoinTokenMark(token.tokenMark, account.id)]?.let {
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
                account_id = assertOriginateToken.account_id,
                assetsName = assertOriginateToken.name,
                name = assertOriginateToken.tokenMark?.name ?: "",
                address = assertOriginateToken.tokenMark?.address ?: "",
                module = assertOriginateToken.tokenMark?.module ?: "",
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
        tokenMark: TokenMark
    ): Boolean {
        when (coinType.coinNumber()) {
            getViolasCoinType().coinNumber() -> {
                val violasChainRpcService = DataRepository.getViolasChainRpcService()

                val transactionResult = violasChainRpcService.addCurrency(
                    ContextProvider.getContext(),
                    Account(
                        KeyPair.fromSecretKey(privateKey)
                    ),
                    tokenMark.address,
                    tokenMark.module,
                    tokenMark.name,
                    chainId = getViolasChainId()
                )

                for (item in 1 until 4) {
                    delay(item * 1000L)
                    val transaction = violasChainRpcService.getTransaction(
                        transactionResult.payerAddress,
                        transactionResult.sequenceNumber
                    )
                    if (transaction?.isSuccessExecuted() == true) {
                        return true
                    }
                }
            }

            getDiemCoinType().coinNumber() -> {
                val libraService = DataRepository.getLibraRpcService()

                val transactionResult = libraService.addCurrency(
                    ContextProvider.getContext(),
                    org.palliums.libracore.wallet.Account(
                        org.palliums.libracore.crypto.KeyPair.fromSecretKey(privateKey)
                    ),
                    tokenMark.address,
                    tokenMark.module,
                    tokenMark.name,
                    chainId = getDiemChainId()
                )

                for (item in 1 until 4) {
                    delay(item * 1000L)
                    val transaction = libraService.getTransaction(
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
    suspend fun publishToken(accountId: Long, account: ByteArray, tokenMark: TokenMark): Boolean {
        mAccountStorage.findById(accountId)?.let {
            return publishToken(CoinType.parseCoinNumber(it.coinNumber), account, tokenMark)
        }
        return false
    }

    @Deprecated("删除")
    suspend fun isPublish(address: String): Boolean {
        return mViolasMultiTokenService.getRegisterToken(address)
    }

    suspend fun isPublish(accountId: Long, tokenMark: TokenMark): Boolean {
        return mAccountStorage.findById(accountId)?.let {
            var isPublish = false
            when (it.coinNumber) {
                getViolasCoinType().coinNumber() -> {
                    DataRepository.getViolasChainRpcService()
                        .getAccountState(it.address)?.balances?.forEach { accountBalance ->
                            if (tokenMark.module == accountBalance.currency) {
                                isPublish = true
                            }
                        }
                }
                getDiemCoinType().coinNumber() -> {
                    DataRepository.getLibraRpcService()
                        .getAccountState(it.address)?.balances?.forEach { accountBalance ->
                            if (tokenMark.module == accountBalance.currency) {
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