package com.violas.wallet.biz

import androidx.collection.ArrayMap
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.bean.AssertToken
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.database.entity.TokenDo
import io.reactivex.disposables.Disposable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class TokenManager {

    private val mExecutor by lazy { Executors.newFixedThreadPool(2) }

    private val mTokenStorage by lazy { DataRepository.getTokenStorage() }

    /**
     * 本地兼容的币种
     */
    fun loadSupportToken(): List<AssertToken> {
//        return arrayListOf(
////            AssertToken(
////                enable = true,
////                fullName = "VToken",
////                name = "VToken",
////                isToken = false
////            ),
//            AssertToken(
//                fullName = "ZCoin",
//                name = "ZCoin",
//                tokenAddress = "7257c2417e4d1038e1817c8f283ace2e1041b3396cdbb099eb357bbee024d614",
//                isToken = true
//            ),
//            AssertToken(
//                fullName = "BCoin",
//                name = "BCoin",
//                tokenAddress = "c20bca7777142dc8ada8ea6ced1c02519aac0b1f0d27149eb0bbe02ee22f5cd9",
//                isToken = true
//            ),
//            AssertToken(
//                fullName = "ACoin",
//                name = "ACoin",
//                tokenAddress = "6fa7359891bef85cc7aa12787b17e85d3f75c78525ffd39cb77eb3453eb3bb75",
//                isToken = true
//            )
//        )
        val countDownLatch = CountDownLatch(1)
        val list = mutableListOf<AssertToken>()
        DataRepository.getViolasService().getSupportCurrency {
            it.forEach { item ->
                list.add(
                    AssertToken(
                        fullName = item.description,
                        name = item.name,
                        tokenAddress = item.address,
                        isToken = true
                    )
                )
            }
            countDownLatch.countDown()
        }
        countDownLatch.await()
        return list
    }

    fun findTokenById(tokenId: Long) = mTokenStorage.findById(tokenId)

    fun findTokenByName(accountId: Long, tokenName: String) =
        mTokenStorage.findByName(accountId, tokenName)

    fun loadSupportToken(account: AccountDO): List<AssertToken> {
        val loadSupportToken = loadSupportToken()

        val onlineTokenMap = ArrayMap<String, Int>()
        DataRepository.getViolasService().checkTokenRegister(
            account.address
        ) {
            it.forEach { item ->
                onlineTokenMap[item] = 0
            }
        }

        val supportTokenMap = HashMap<String, TokenDo>(loadSupportToken.size)
        val localToken = mTokenStorage.findByAccountId(account.id)
        localToken.map {
            supportTokenMap[it.name] = it
        }

        val localSupportTokenMap = ArrayMap<String, Int>()

        loadSupportToken.forEach { token ->
            if (onlineTokenMap.contains(token.tokenAddress)) {
                token.netEnable = true
            }

            localSupportTokenMap[token.tokenAddress] = 0
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
            if (it.enable && !localSupportTokenMap.contains(it.tokenAddress)) {
                mutableList.add(
                    AssertToken(
                        account_id = account.id,
                        enable = true,
                        tokenAddress = it.tokenAddress,
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
                    tokenAddress = it.tokenAddress,
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
                tokenAddress = assertToken.tokenAddress
            )
        )
    }

    fun getTokenBalance(
        address: String,
        tokenDo: TokenDo,
        call: (tokenBalance: Long, result: Boolean) -> Unit
    ): Disposable {
        return DataRepository.getViolasService()
            .getBalance(address) { accountBalance, tokens, result ->
                var amount = 0L
                tokens?.forEach {
                    if (it.address == tokenDo.tokenAddress) {
                        amount = it.balance
                        return@forEach
                    }
                }

                if (result) {
                    mExecutor.submit {
                        tokenDo.amount = amount
                        mTokenStorage.update(tokenDo)
                    }
                }

                call.invoke(amount, result)
            }
    }

    fun refreshBalance(
        address: String,
        enableTokens: List<AssertToken>,
        call: (accountBalance: Long, assertTokens: List<AssertToken>) -> Unit
    ): Disposable {
        return DataRepository.getViolasService()
            .getBalance(address) { accountBalance, tokens, result ->
                val tokenMap = mutableMapOf<String, Long>()
                tokens?.forEach {
                    tokenMap[it.address] = it.balance
                }

                enableTokens.forEach {
                    if (!it.isToken) {
                        it.amount = accountBalance
                    } else if (tokenMap.contains(it.tokenAddress)) {
                        it.amount = tokenMap[it.tokenAddress]!!
                    } else {
                        it.amount = 0
                    }
                }

                // 更新本地token资产余额，钱包资产余额交由AccountManager更新
                if (result) {
                    mExecutor.submit {
                        val tokens = enableTokens
                            .filter {
                                it.isToken
                            }.map {
                                TokenDo(
                                    id = it.id,
                                    account_id = it.account_id,
                                    tokenAddress = it.tokenAddress,
                                    name = it.name,
                                    enable = it.enable,
                                    amount = it.amount
                                )
                            }
                        mTokenStorage.update(*tokens.toTypedArray())
                    }
                }

                call.invoke(accountBalance, enableTokens)
            }
    }
}