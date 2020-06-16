package com.violas.wallet.biz

import androidx.annotation.WorkerThread
import androidx.collection.ArrayMap
import com.palliums.violas.http.ViolasMultiTokenRepository
import com.palliums.violas.smartcontract.ViolasMultiTokenContract
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.bean.AssertOriginateToken
import com.violas.wallet.biz.bean.TokenMark
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.database.entity.TokenDo
import org.palliums.violascore.transaction.TransactionPayload
import org.palliums.violascore.wallet.Account
import java.util.concurrent.Executors

class TokenManager {

    private val mExecutor by lazy { Executors.newFixedThreadPool(2) }

    private val mTokenStorage by lazy { DataRepository.getTokenStorage() }
    private val mAccountStorage by lazy { DataRepository.getAccountStorage() }

    private val mViolasService by lazy {
        DataRepository.getViolasService()
    }

    private val mViolasMultiTokenService by lazy {
        ViolasMultiTokenRepository(
            DataRepository.getMultiTokenContractService(),
            // todo 不同环境合约地址可能会不同
            ViolasMultiTokenContract(Vm.TestNet)
        )
    }

    fun getViolasMultiTokenContract() =
        mViolasMultiTokenService.getMultiTokenContract()

    /**
     * 本地兼容的币种
     */
    private suspend fun loadNetWorkSupportViolasToken(): List<AssertOriginateToken> {
        val list = mutableListOf<AssertOriginateToken>()
//        val supportCurrency = mViolasMultiTokenService.getSupportCurrency()
//        supportCurrency?.forEach { item ->
//            list.add(
//                AssertOriginateToken(
//                    tokenMark = TokenMark("LBR", "00000000000000000000000000000000", "T"),
//                    fullName = item.name,
//                    name = item.name,
//                    isToken = true
//                )
//            )
//        }
        return list
    }

    /**
     * 本地兼容的币种
     */
    private suspend fun loadNetWorkSupportLibraToken(): List<AssertOriginateToken> {
        val list = mutableListOf<AssertOriginateToken>()
//        val supportCurrency = mViolasMultiTokenService.getSupportCurrency()
//        supportCurrency?.forEach { item ->
//            list.add(
//                AssertToken(
//                    fullName = item.name,
//                    name = item.name,
//                    tokenIdx = item.tokenIdentity,
//                    isToken = true
//                )
//            )
//        }
        return list
    }

    @WorkerThread
    fun findTokenById(tokenId: Long) = mTokenStorage.findById(tokenId)

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

//        val coinTypes = CoinTypes.parseCoinType(CoinTypes.Bitcoin.coinType())
//        mutableList.add(
//            0, AssertOriginateToken(
//                id = 0,
//                account_id = account.id,
//                enable = true,
//                isToken = false,
//                name = coinTypes.coinName(),
//                fullName = coinTypes.fullName(),
//                amount = 0
//            )
//        )

        accounts.forEach { account ->
            var loadSupportToken: List<AssertOriginateToken>? = null
            when (account.coinNumber) {
                CoinTypes.Bitcoin.coinType(),
                CoinTypes.BitcoinTest.coinType() -> {

                }
                CoinTypes.Violas.coinType() -> {
                    loadSupportToken = loadNetWorkSupportViolasToken()
                }
                CoinTypes.Libra.coinType() -> {
                    loadSupportToken = loadNetWorkSupportLibraToken()
                }
            }

            val localSupportTokenMap = ArrayMap<TokenMark, Int>()

            loadSupportToken?.forEach { token ->
                localSupportTokenMap[token.tokenMark] = 0
                token.account_id = account.id
                supportTokenMap[CoinTokenMark(token.tokenMark, account.id)]?.let {
                    token.enable = it.enable
                }
            }


            if (loadSupportToken != null) {
                resultTokenList.addAll(loadSupportToken)
            }

            val localToken = mTokenStorage.findByAccountId(account.id)
            localToken.map {
                supportTokenMap[CoinTokenMark(
                    TokenMark(it.module, it.address, it.name),
                    it.account_id
                )] = it
            }

            localToken.map {
                val tokenMark = TokenMark(
                    it.module,
                    it.address,
                    it.name
                )
                if (it.enable && !localSupportTokenMap.contains(tokenMark)) {
                    resultTokenList.add(
                        AssertOriginateToken(
                            account_id = account.id,
                            enable = true,
                            tokenMark = tokenMark,
                            isToken = true,
                            name = it.assetsName,
                            fullName = "",
                            amount = 0
                        )
                    )
                }
            }
        }

        return resultTokenList
    }

    @WorkerThread
    fun loadEnableToken(account: AccountDO): List<AssertOriginateToken> {
        val enableToken = mTokenStorage
            .findEnableTokenByAccountId(account.id)
            .map {
//                AssertToken(
//                    id = it.id!!,
//                    account_id = it.account_id,
//                    coinType = account.coinNumber,
//                    enable = it.enable,
//                    isToken = true,
//                    tokenIdx = it.tokenIdx,
//                    name = it.name,
//                    fullName = "",
//                    amount = it.amount
//                )
            }.toList()

        val mutableList = mutableListOf<AssertOriginateToken>()
        mutableList.add(
            0, AssertOriginateToken(
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
//        mutableList.addAll(enableToken)
        return mutableList
    }

    @WorkerThread
    fun insert(checked: Boolean, accountId: Long, tokenName: String, tokenIdx: Long) {
        mTokenStorage.insert(
//            TokenDo(
//                enable = checked,
//                account_id = accountId,
//                name = tokenName,
//                tokenIdx = tokenIdx
//            )
        )
    }

    @WorkerThread
    fun insert(checked: Boolean, assertOriginateToken: AssertOriginateToken) {
        mTokenStorage.insert(
//            TokenDo(
//                enable = checked,
//                account_id = assertToken.account_id,
//                name = assertToken.name,
//                tokenIdx = assertToken.tokenIdx
//            )
        )
    }

    @WorkerThread
    fun deleteAllToken() {
        mTokenStorage.deleteAll()
    }

    suspend fun getTokenBalance(
        address: String,
        tokenDo: TokenDo
    ): Long {
//        val amount = getTokenBalance(address, tokenDo.tokenIdx)
//
//        mExecutor.submit {
//            tokenDo.amount = amount
//            mTokenStorage.update(tokenDo)
//        }
//        return amount
        return 1000
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
        amount: Long,
        date: ByteArray = byteArrayOf()
    ) {
        val publishTokenPayload = mViolasMultiTokenService.transferTokenPayload(
            tokenIdx, address, amount, date
        )
        mViolasService.sendTransaction(publishTokenPayload, account)
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