package com.violas.wallet.biz

import androidx.collection.ArrayMap
import com.palliums.violas.http.ViolasMultiTokenRepository
import com.palliums.violas.smartcontract.ViolasMultiTokenContract
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.bean.AssertToken
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.database.entity.TokenDo
import org.palliums.violascore.wallet.Account
import java.util.concurrent.Executors

class TokenManager {

    private val mExecutor by lazy { Executors.newFixedThreadPool(2) }

    private val mTokenStorage by lazy { DataRepository.getTokenStorage() }

    val mViolasService by lazy {
        DataRepository.getViolasService()
    }

    private val mViolasMultiTokenService by lazy {
        ViolasMultiTokenRepository(
            DataRepository.getMultiTokenContractService(),
            // todo 不同环境合约地址可能会不同
            ViolasMultiTokenContract(Vm.TestNet)
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

    fun findTokenById(tokenId: Long) =
        mTokenStorage.findById(tokenId)

    fun findTokenByName(accountId: Long, tokenName: String) =
        mTokenStorage.findByName(accountId, tokenName)

    fun findTokenByTokenAddress(accountId: Long, tokenIdx: Long) =
        mTokenStorage.findByTokenAddress(accountId, tokenIdx)

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

    suspend fun refreshBalance(
        address: String,
        enableTokens: List<AssertToken>
    ): Pair<Long, List<AssertToken>> {

        val tokenIds = enableTokens
            .filter { it.isToken }
            .map { it.tokenIdx }
        val tokenBalance =
            mViolasMultiTokenService.getBalance(address, tokenIds)

        val accountBalance = tokenBalance?.balance ?: 0
        val remoteTokens = tokenBalance?.modules
        val remoteTokenMap = mutableMapOf<Long, Long>()
        remoteTokens?.forEach { token ->
            remoteTokenMap[token.id] = token.balance
        }

        enableTokens.forEach {
            if (!it.isToken) {
                it.amount = accountBalance
            } else if (remoteTokenMap.contains(it.tokenIdx)) {
                it.amount = remoteTokenMap[it.tokenIdx]!!
            } else {
                it.amount = 0
            }
        }

        val localTokens = enableTokens
            .filter { it.isToken }
            .map {
                TokenDo(
                    id = it.id,
                    account_id = it.account_id,
                    tokenIdx = it.tokenIdx,
                    name = it.name,
                    enable = it.enable,
                    amount = it.amount
                )
            }

        // 更新本地token资产余额，钱包资产余额交由AccountManager更新
        mExecutor.submit {
            mTokenStorage.update(*localTokens.toTypedArray())
        }

        return Pair(accountBalance, enableTokens)
    }

    suspend fun publishToken(account: Account) {
        val publishTokenPayload = mViolasMultiTokenService.publishTokenPayload()
        mViolasService.sendTransaction(publishTokenPayload, account)
    }

    suspend fun isPublish(address: String): Boolean {
        return mViolasMultiTokenService.getRegisterToken(address)
    }

    suspend fun sendToken(
        account: Account,
        tokenIdx: Long,
        address: String,
        amount: Long,
        data: ByteArray = byteArrayOf()
    ) {
        val publishTokenPayload =
            mViolasMultiTokenService.transferTokenPayload(tokenIdx, address, amount, data)
        mViolasService.sendTransaction(publishTokenPayload, account)
    }

    /**
     * 铸币
     * @param account 铸币账户
     * @param tokenIdx 稳定币索引
     * @param address 接收地址
     * @param amount 铸造数量
     * @param data 额外数据
     */
    suspend fun mintToken(
        account: Account,
        tokenIdx: Long,
        address: String,
        amount: Long,
        data: ByteArray = byteArrayOf()
    ) {
        val mintTokenPayload =
            mViolasMultiTokenService.mintTokenPayload(tokenIdx, address, amount, data)
        mViolasService.sendTransaction(mintTokenPayload, account)
    }
}