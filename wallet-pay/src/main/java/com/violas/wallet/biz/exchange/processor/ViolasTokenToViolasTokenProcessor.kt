package com.violas.wallet.biz.exchange.processor

import com.palliums.violas.smartcontract.ViolasExchangeContract
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.exchange.AccountPayeeNotFindException
import com.violas.wallet.biz.exchange.AccountPayeeTokenNotActiveException
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.DataRepository
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

    private val mViolasExchangeContract by lazy {
        ViolasExchangeContract(Vm.TestNet)
    }

    override fun hasHandle(tokenFrom: ITokenVo, tokenTo: ITokenVo): Boolean {
        val hasToken = tokenFrom is StableTokenVo && tokenTo is StableTokenVo
        val hasViolsToken =
            tokenFrom.coinNumber == CoinTypes.Violas.coinType() && tokenTo.coinNumber == CoinTypes.Violas.coinType()
        return hasToken && hasViolsToken
    }

    @Throws(
        ViolasException::class,
        AccountPayeeNotFindException::class,
        AccountPayeeTokenNotActiveException::class
    )
    override suspend fun handle(
        privateKey: ByteArray,
        tokenFrom: ITokenVo,
        tokenTo: ITokenVo,
        payee: String,
        amountIn: Long,
        amountOutMin: Long,
        path: ByteArray,
        data: ByteArray
    ) {
        tokenFrom as StableTokenVo
        tokenTo as StableTokenVo

        // 收款地址状态
        val accountState =
            mViolasRpcService.getAccountState(payee) ?: throw AccountPayeeNotFindException()

        var isPublishToken = false
        accountState.balances?.forEach {
            if (it.currency.equals(tokenTo.module, true)) {
                isPublishToken = true
            }
        }
        if (!isPublishToken) {
            throw AccountPayeeTokenNotActiveException()
        }

        val account = Account(KeyPair.fromSecretKey(privateKey))

        val typeTagFrom = TypeTagStructTag(
            StructTag(
                AccountAddress(tokenFrom.address.hexStringToByteArray()),
                tokenFrom.module,
                tokenFrom.name,
                arrayListOf()
            )
        )
        val typeTagTo = TypeTagStructTag(
            StructTag(
                AccountAddress(tokenTo.address.hexStringToByteArray()),
                tokenTo.module,
                tokenTo.name,
                arrayListOf()
            )
        )

        val optionTokenSwapTransactionPayload =
            mViolasExchangeContract.optionTokenSwapTransactionPayload(
                typeTagFrom,
                typeTagTo,
                payee,
                amountIn,
                amountOutMin,
                path,
                data
            )

        mViolasRpcService.sendTransaction(
            optionTokenSwapTransactionPayload,
            account,
            gasCurrencyCode = typeTagFrom.value.module
        )
    }
}