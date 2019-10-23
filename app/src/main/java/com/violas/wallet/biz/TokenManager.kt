package com.violas.wallet.biz

import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.bean.AssertToken
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.database.entity.TokenDo

class TokenManager {
    /**
     * 本地兼容的币种
     */
    fun loadSupportToken(): List<AssertToken> {
        return arrayListOf(
//            AssertToken(
//                enable = true,
//                fullName = "VToken",
//                name = "VToken",
//                isToken = false
//            ),
            AssertToken(
                enable = false,
                fullName = "ZCoin",
                name = "ZCoin",
                isToken = true
            ),
            AssertToken(
                enable = false,
                fullName = "ACoin",
                name = "ACoin",
                isToken = true
            )
        )
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