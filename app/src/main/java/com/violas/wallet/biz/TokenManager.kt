package com.violas.wallet.biz

import com.violas.wallet.biz.bean.AssertToken
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.TokenDo

class TokenManager {
    /**
     * 本地兼容的币种
     */
    fun loadSupportToken(): List<AssertToken> {
        return arrayListOf(
            AssertToken(
                enable = true,
                fullName = "VToken",
                name = "VToken",
                isToken = false
            ),
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

    fun loadToken(accountId: Long): List<AssertToken> {
        val loadSupportToken = loadSupportToken()
        val supportTokenMap = HashMap<String, TokenDo>(loadSupportToken.size)
        DataRepository.getTokenStorage().findByAccountId(accountId).map {
            supportTokenMap[it.name] = it
        }

        loadSupportToken.forEach { token ->
            token.account_id = accountId
            supportTokenMap[token.name]?.let {
                token.enable = it.enable
            }
        }

        val mutableList = MutableList(loadSupportToken.size + 1) { AssertToken() }
        mutableList.add(
            0, AssertToken(
                id = 0,
                account_id = accountId,
                enable = true,
                isToken = false,
                name = "",
                fullName = "",
                amount = 0
            )
        )
        mutableList.addAll(loadSupportToken)
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