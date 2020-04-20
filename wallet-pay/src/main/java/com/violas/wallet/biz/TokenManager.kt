package com.violas.wallet.biz

import androidx.annotation.WorkerThread
import androidx.collection.ArrayMap
import com.palliums.utils.coroutineExceptionHandler
import com.palliums.violas.http.ViolasMultiTokenRepository
import com.palliums.violas.smartcontract.ViolasMultiTokenContract
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.bean.AssertToken
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.database.entity.TokenDo
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.palliums.violascore.wallet.Account
import java.util.concurrent.Executors

class TokenManager {

    private val mExecutor by lazy { Executors.newFixedThreadPool(2) }

    private val mTokenStorage by lazy { DataRepository.getTokenStorage() }

    private val mViolasService by lazy {
        DataRepository.getViolasService()
    }

    private val mViolasMultiTokenService by lazy {
        ViolasMultiTokenRepository(
            DataRepository.getMultiTokenContractService(),
            // todo 不同环境合约地址可能会不同
            ViolasMultiTokenContract()
        )
    }

    /**
     * 本地兼容的币种
     */
    private suspend fun loadSupportToken(): List<AssertToken> {
        val list = mutableListOf<AssertToken>()
        val supportCurrency = mViolasMultiTokenService.getSupportCurrency()
        supportCurrency?.forEach { item ->
            list.add(
                AssertToken(
                    fullName = item.name,
                    name = item.name,
                    tokenIdx = item.tokenIdentity,
                    isToken = true
                )
            )
        }
        return list
    }

    @WorkerThread
    fun findTokenById(tokenId: Long) = mTokenStorage.findById(tokenId)

    @WorkerThread
    fun findTokenByName(accountId: Long, tokenName: String) =
        mTokenStorage.findByName(accountId, tokenName)

    suspend fun loadSupportToken(account: AccountDO): List<AssertToken> {
        val loadSupportToken = loadSupportToken()

        val supportTokenMap = HashMap<String, TokenDo>(loadSupportToken.size)
        val localToken = mTokenStorage.findByAccountId(account.id)
        localToken.map {
            supportTokenMap[it.name] = it
        }

        val localSupportTokenMap = ArrayMap<Long, Int>()

        loadSupportToken.forEach { token ->

            localSupportTokenMap[token.tokenIdx] = 0
            token.account_id = account.id
            supportTokenMap[token.name]?.let {
                token.enable = it.enable
            }
        }

        val mutableList = mutableListOf<AssertToken>().also {
            val coinTypes = CoinTypes.parseCoinType(account.coinNumber)
            it.add(
                0, AssertToken(
                    id = 0,
                    account_id = account.id,
                    enable = true,
                    isToken = false,
                    name = coinTypes.coinName(),
                    fullName = coinTypes.fullName(),
                    amount = 0
                )
            )
            it.addAll(loadSupportToken)
        }

        localToken.map {
            if (it.enable && !localSupportTokenMap.contains(it.tokenIdx)) {
                mutableList.add(
                    AssertToken(
                        account_id = account.id,
                        enable = true,
                        tokenIdx = it.tokenIdx,
                        isToken = true,
                        name = it.name,
                        fullName = "",
                        amount = 0
                    )
                )
            }
        }

        return mutableList
    }

    @WorkerThread
    fun loadEnableToken(account: AccountDO): List<AssertToken> {
        val enableToken = mTokenStorage
            .findEnableTokenByAccountId(account.id)
            .map {
                AssertToken(
                    id = it.id!!,
                    account_id = it.account_id,
                    coinType = account.coinNumber,
                    enable = it.enable,
                    isToken = true,
                    tokenIdx = it.tokenIdx,
                    name = it.name,
                    fullName = "",
                    amount = it.amount
                )
            }.toList()

        val mutableList = mutableListOf<AssertToken>()
        mutableList.add(
            0, AssertToken(
                id = 0,
                account_id = account.id,
                coinType = account.coinNumber,
                enable = true,
                isToken = false,
                name = CoinTypes.parseCoinType(account.coinNumber).coinName(),
                fullName = "",
                amount = account.amount
            )
        )
        mutableList.addAll(enableToken)
        return mutableList
    }

    @WorkerThread
    fun insert(checked: Boolean, accountId: Long, tokenName: String, tokenIdx: Long) {
        mTokenStorage.insert(
            TokenDo(
                enable = checked,
                account_id = accountId,
                name = tokenName,
                tokenIdx = tokenIdx
            )
        )
    }

    @WorkerThread
    fun insert(checked: Boolean, assertToken: AssertToken) {
        mTokenStorage.insert(
            TokenDo(
                enable = checked,
                account_id = assertToken.account_id,
                name = assertToken.name,
                tokenIdx = assertToken.tokenIdx
            )
        )
    }

    suspend fun getTokenBalance(
        address: String,
        tokenDo: TokenDo
    ): Long {
        val amount = getTokenBalance(address, tokenDo.tokenIdx)

        mExecutor.submit {
            tokenDo.amount = amount
            mTokenStorage.update(tokenDo)
        }
        return amount
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

    data class RefreshBalanceResult(
        var accountBalance: Long,
        var assertTokens: List<AssertToken>
    )

    suspend fun refreshBalance(
        address: String,
        enableTokens: List<AssertToken>
    ): RefreshBalanceResult {
        val tokenAddressList = arrayListOf<Long>()
        enableTokens.forEach {
            if (it.isToken) {
                tokenAddressList.add(it.tokenIdx)
            }
        }
        val tokenBalance = mViolasMultiTokenService.getBalance(address, tokenAddressList)

        val accountBalance = tokenBalance?.balance ?: 0
        val tokens = tokenBalance?.modules

        val tokenMap = mutableMapOf<Long, Long>()
        tokens?.forEach { token ->
            tokenMap[token.id] = token.balance
        }

        enableTokens.forEach {
            if (!it.isToken) {
                it.amount = accountBalance
            } else if (tokenMap.contains(it.tokenIdx)) {
                it.amount = tokenMap[it.tokenIdx]!!
            } else {
                it.amount = 0
            }
        }

        // 更新本地token资产余额，钱包资产余额交由AccountManager更新
        mExecutor.submit {
            val tokens = enableTokens
                .filter {
                    it.isToken
                }.map {
                    TokenDo(
                        id = it.id,
                        account_id = it.account_id,
                        tokenIdx = it.tokenIdx,
                        name = it.name,
                        enable = it.enable,
                        amount = it.amount
                    )
                }
            mTokenStorage.update(*tokens.toTypedArray())
        }

        return RefreshBalanceResult(accountBalance, enableTokens)
    }

    suspend fun publishToken(account: Account) {
        val publishTokenPayload = mViolasMultiTokenService.publishTokenPayload()
        mViolasService.sendTransaction(publishTokenPayload, account)
    }

    suspend fun isPublish(address: String): Boolean {
        return mViolasMultiTokenService.getRegisterToken(address)
    }

    suspend fun sendViolasToken(
        tokenIdx: Long,
        account: Account,
        address: String,
        amount: Long
    ) {
        val publishTokenPayload = mViolasMultiTokenService.transferTokenPayload(
            tokenIdx, address, amount, byteArrayOf()
        )
        mViolasService.sendTransaction(publishTokenPayload, account)
    }
}