package com.violas.wallet.biz.exchange.processor

import com.palliums.content.ContextProvider
import com.palliums.violas.smartcontract.ViolasExchangeContract
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.ReceiverAccountCurrencyNotAddException
import com.violas.wallet.biz.ReceiverAccountNotActivationException
import com.violas.wallet.biz.WrongPasswordException
import com.violas.wallet.biz.bean.DiemAppToken
import com.violas.wallet.biz.exchange.AccountNotFindAddressException
import com.violas.wallet.biz.transaction.ViolasTxnManager
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.common.getViolasChainId
import com.violas.wallet.common.getViolasCoinType
import com.violas.wallet.common.isViolasTestNet
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.ui.main.market.bean.IAssetMark
import com.violas.wallet.ui.main.market.bean.ITokenVo
import com.violas.wallet.ui.main.market.bean.StableTokenVo
import com.violas.walletconnect.extensions.hexStringToByteArray
import org.palliums.violascore.crypto.KeyPair
import org.palliums.violascore.http.ViolasException
import org.palliums.violascore.transaction.AccountAddress
import org.palliums.violascore.transaction.storage.StructTag
import org.palliums.violascore.transaction.storage.TypeTag
import org.palliums.violascore.wallet.Account

class ViolasTokenToViolasTokenProcessor : IProcessor {

    private val mViolasRpcService by lazy {
        DataRepository.getViolasRpcService()
    }

    private val mViolasExchangeContract by lazy {
        ViolasExchangeContract(isViolasTestNet())
    }

    override fun hasHandleSwap(tokenFrom: ITokenVo, tokenTo: ITokenVo): Boolean {
        val hasToken = tokenFrom is StableTokenVo && tokenTo is StableTokenVo
        val hasViolsToken = tokenFrom.coinNumber == getViolasCoinType().coinNumber()
                && tokenTo.coinNumber == getViolasCoinType().coinNumber()
        return hasToken && hasViolsToken
    }

    @Throws(
        ViolasException::class,
        ReceiverAccountNotActivationException::class,
        ReceiverAccountCurrencyNotAddException::class
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

        val violasAccount = AccountManager.getAccountByCoinNumber(getViolasCoinType().coinNumber())
            ?: throw AccountNotFindAddressException()

        // 检查Violas收款人账户
        val payeeAddress = payee ?: violasAccount.address
        val violasTxnManager = ViolasTxnManager()
        val payeeAccountState = violasTxnManager.getReceiverAccountState(
            payeeAddress,
            DiemAppToken.convert(tokenTo)
        ) {
            mViolasRpcService.getAccountState(it)
        }

        val privateKey = SimpleSecurity.instance(ContextProvider.getContext())
            .decrypt(pwd, violasAccount.privateKey) ?: throw WrongPasswordException()
        val payerAccount = Account(KeyPair.fromSecretKey(privateKey))

        // 检查Violas付款人账户
        val payerAddress = payerAccount.getAddress().toHex()
        val payerAccountState = if (payerAddress == payeeAddress)
            payeeAccountState
        else
            violasTxnManager.getSenderAccountState(payerAccount, payerAddress) {
                mViolasRpcService.getAccountState(it)
            }

        // 计算gas info
        val gasInfo = violasTxnManager.calculateGasInfo(
            payerAccountState,
            listOf(Pair(tokenFrom.module, amountIn))
        )

        val minToken: ITokenVo
        val maxToken: ITokenVo
        if (tokenFrom.marketIndex > tokenTo.marketIndex) {
            minToken = tokenTo
            maxToken = tokenFrom
        } else {
            minToken = tokenFrom
            maxToken = tokenTo
        }

        val swapTransactionPayload = mViolasExchangeContract.optionTokenSwapTransactionPayload(
            TypeTag.newStructTag(
                StructTag(
                    AccountAddress(minToken.address.hexStringToByteArray()),
                    minToken.module,
                    minToken.name,
                    arrayListOf()
                )
            ),
            TypeTag.newStructTag(
                StructTag(
                    AccountAddress(maxToken.address.hexStringToByteArray()),
                    maxToken.module,
                    maxToken.name,
                    arrayListOf()
                )
            ),
            payeeAddress,
            amountIn,
            amountOutMin,
            path,
            data
        )

        return mViolasRpcService.sendTransaction(
            swapTransactionPayload,
            payerAccount,
            sequenceNumber = payerAccountState.sequenceNumber,
            gasCurrencyCode = gasInfo.gasCurrencyCode,
            maxGasAmount = gasInfo.maxGasAmount,
            gasUnitPrice = gasInfo.gasUnitPrice,
            chainId = getViolasChainId()
        ).sequenceNumber.toString()
    }

    override fun hasHandleCancel(
        fromIAssetMark: IAssetMark,
        toIAssetMark: IAssetMark
    ): Boolean {
        return false
    }

    override suspend fun cancel(
        pwd: ByteArray,
        fromIAssetMark: IAssetMark,
        toIAssetMark: IAssetMark,
        typeTag: String,
        originPayeeAddress: String,
        tranId: String?,
        sequence: String?
    ): String {
        TODO("Not yet implemented")
    }
}