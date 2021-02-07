package com.violas.wallet.biz.exchange.processor

import com.palliums.content.ContextProvider
import com.palliums.violas.smartcontract.ViolasExchangeContract
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.exchange.AccountNotFindAddressException
import com.violas.wallet.biz.exchange.AccountPayeeNotFindException
import com.violas.wallet.biz.exchange.AccountPayeeTokenNotActiveException
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.common.getViolasChainId
import com.violas.wallet.common.getViolasCoinType
import com.violas.wallet.common.isViolasTestNet
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.ui.main.market.bean.IAssetsMark
import com.violas.wallet.ui.main.market.bean.ITokenVo
import com.violas.wallet.ui.main.market.bean.StableTokenVo
import com.violas.walletconnect.extensions.hexStringToByteArray
import org.palliums.violascore.crypto.KeyPair
import org.palliums.violascore.http.ViolasException
import org.palliums.violascore.transaction.AccountAddress
import org.palliums.violascore.transaction.storage.StructTag
import org.palliums.violascore.transaction.storage.TypeTagStructTag
import org.palliums.violascore.wallet.Account

class ViolasTokenToViolasTokenProcessor : IProcessor {

    private val mViolasRpcService by lazy {
        DataRepository.getViolasChainRpcService()
    }

    private val mAccountManager by lazy {
        AccountManager()
    }

    private val mViolasExchangeContract by lazy {
        ViolasExchangeContract(isViolasTestNet())
    }

    override fun hasHandleSwap(tokenFrom: ITokenVo, tokenTo: ITokenVo): Boolean {
        val hasToken = tokenFrom is StableTokenVo && tokenTo is StableTokenVo
        val hasViolsToken =
            tokenFrom.coinNumber == getViolasCoinType().coinNumber() && tokenTo.coinNumber == getViolasCoinType().coinNumber()
        return hasToken && hasViolsToken
    }

    @Throws(
        ViolasException::class,
        AccountPayeeNotFindException::class,
        AccountPayeeTokenNotActiveException::class
    )
    override suspend fun handle(
        pwd: ByteArray,
        tokenFrom: ITokenVo,
        tokenTo: ITokenVo,
        payee: String?,
        amountIn: Long,
        amountOutMin: Long,
        path: ByteArray,
        data: ByteArray
    ): String {
        tokenFrom as StableTokenVo
        tokenTo as StableTokenVo

        val accountDo = mAccountManager.getIdentityByCoinType(getViolasCoinType().coinNumber())
            ?: throw AccountNotFindAddressException()

        val payeeAddress = payee ?: accountDo.address

        // 开始检查 Violas 账户的基本信息
        // 收款地址状态
        val accountState =
            mViolasRpcService.getAccountState(payeeAddress) ?: throw AccountPayeeNotFindException()

        var isPublishToken = false
        accountState.balances?.forEach {
            if (it.currency.equals(tokenTo.module, true)) {
                isPublishToken = true
            }
        }
        if (!isPublishToken) {
            throw AccountPayeeTokenNotActiveException(
                getViolasCoinType(),
                payeeAddress,
                tokenTo
            )
        }

        val simpleSecurity =
            SimpleSecurity.instance(ContextProvider.getContext())

        val privateKey = simpleSecurity.decrypt(pwd, accountDo.privateKey)
            ?: throw RuntimeException("password error")
        // 开始发起 Violas 交易
        val account = Account(KeyPair.fromSecretKey(privateKey))

        val minToken: ITokenVo
        val maxToken: ITokenVo
        if (tokenFrom.marketIndex > tokenTo.marketIndex) {
            minToken = tokenTo
            maxToken = tokenFrom
        } else {
            minToken = tokenFrom
            maxToken = tokenTo
        }

        val minTypeTag = TypeTagStructTag(
            StructTag(
                AccountAddress(minToken.address.hexStringToByteArray()),
                minToken.module,
                minToken.name,
                arrayListOf()
            )
        )
        val maxTypeTag = TypeTagStructTag(
            StructTag(
                AccountAddress(maxToken.address.hexStringToByteArray()),
                maxToken.module,
                maxToken.name,
                arrayListOf()
            )
        )

        val optionTokenSwapTransactionPayload =
            mViolasExchangeContract.optionTokenSwapTransactionPayload(
                minTypeTag,
                maxTypeTag,
                payeeAddress,
                amountIn,
                amountOutMin,
                path,
                data
            )

        return mViolasRpcService.sendTransaction(
            optionTokenSwapTransactionPayload,
            account,
            gasCurrencyCode = minTypeTag.value.module,
            chainId = getViolasChainId()
        ).sequenceNumber.toString()
    }

    override fun hasHandleCancel(
        fromIAssetsMark: IAssetsMark,
        toIAssetsMark: IAssetsMark
    ): Boolean {
        return false
    }

    override suspend fun cancel(
        pwd: ByteArray,
        fromIAssetsMark: IAssetsMark,
        toIAssetsMark: IAssetsMark,
        typeTag: String,
        originPayeeAddress: String,
        tranId: String?,
        sequence: String?
    ): String {
        TODO("Not yet implemented")
    }
}