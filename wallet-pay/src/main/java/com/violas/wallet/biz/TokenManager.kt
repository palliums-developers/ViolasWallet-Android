package com.violas.wallet.biz

import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.bean.AssertToken
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.database.entity.TokenDo
import java.util.concurrent.CountDownLatch

class TokenManager {
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
            it.forEach {
                list.add(
                    AssertToken(
                        fullName = it.description,
                        name = it.name,
                        tokenAddress = it.address,
                        isToken = true
                    )
                )
            }
            countDownLatch.countDown()
        }
        countDownLatch.await()
        return list
    }

    fun loadSupportToken(account: AccountDO): List<AssertToken> {
        val loadSupportToken = loadSupportToken()

        val supportTokenMap = HashMap<String, TokenDo>(loadSupportToken.size)
        DataRepository.getTokenStorage().findByAccountId(account.id).map {
            supportTokenMap[it.name] = it
        }

        loadSupportToken.forEach { token ->
            token.account_id = account.id
            supportTokenMap[token.name]?.let {
                token.enable = it.enable
            }
        }

        val mutableList = mutableListOf<AssertToken>()
        mutableList.add(
            0, AssertToken(
                id = 0,
                account_id = account.id,
                enable = true,
                isToken = false,
                name = CoinTypes.parseCoinType(account.coinNumber).coinName(),
                fullName = "",
                amount = 0
            )
        )
        mutableList.addAll(loadSupportToken)
        return mutableList
    }

    fun loadEnableToken(account: AccountDO): List<AssertToken> {
        val enableToken = DataRepository.getTokenStorage()
            .findEnableTokenByAccountId(account.id)
            .map {
                AssertToken(
                    id = it.id!!,
                    account_id = it.account_id,
                    coinType = account.coinNumber,
                    enable = it.enable,
                    isToken = false,
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
                amount = 0
            )
        )
        mutableList.addAll(enableToken)
        return mutableList
    }

    fun insert(checked: Boolean, assertToken: AssertToken) {
        DataRepository.getTokenStorage().insert(
            TokenDo(
                enable = checked,
                account_id = assertToken.account_id,
                name = assertToken.name
            )
        )
    }
}