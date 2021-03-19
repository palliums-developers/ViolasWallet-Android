package com.violas.wallet.biz.exchange.processor

import com.palliums.content.ContextProvider
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.exchange.*
import com.violas.wallet.common.*
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.ui.main.market.bean.IAssetMark
import com.violas.wallet.ui.main.market.bean.ITokenVo
import com.violas.wallet.ui.main.market.bean.DiemCurrencyAssetMark
import com.violas.wallet.ui.main.market.bean.StableTokenVo
import com.violas.walletconnect.extensions.hexStringToByteArray
import org.json.JSONObject
import org.palliums.violascore.crypto.KeyPair
import org.palliums.violascore.transaction.AccountAddress
import org.palliums.violascore.transaction.TransactionPayload
import org.palliums.violascore.transaction.optionTransactionPayload
import org.palliums.violascore.transaction.storage.StructTag
import org.palliums.violascore.transaction.storage.TypeTagStructTag
import org.palliums.violascore.wallet.Account
import java.lang.RuntimeException

class ViolasToAssetsMappingProcessor(
    private val supportMappingPair: HashMap<String, MappingInfo>
) : IProcessor {

    private val mViolasRpcService by lazy {
        DataRepository.getViolasChainRpcService()
    }

    private val mLibraRpcService by lazy {
        DataRepository.getDiemRpcService()
    }

    override fun hasHandleSwap(tokenFrom: ITokenVo, tokenTo: ITokenVo): Boolean {
        return tokenFrom is StableTokenVo
                && tokenFrom.coinNumber == getViolasCoinType().coinNumber()
                && supportMappingPair.containsKey(IAssetMark.convert(tokenTo).mark())
    }

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

        val payeeAddress =
            payee ?: AccountManager.getAccountByCoinNumber(tokenTo.coinNumber)?.address
            ?: throw AccountNotFindAddressException()

        // 检查 Libra 的稳定币有没有 Publish
        if (tokenTo.coinNumber == getDiemCoinType().coinNumber()) {
            tokenTo as StableTokenVo

            // 检查收款地址激活状态
            val accountState =
                mLibraRpcService.getAccountState(payeeAddress)
                    ?: throw AccountPayeeNotFindException()

            // 检查收款地址 Token 注册状态
            var isPublishToken = false
            accountState.balances?.forEach {
                if (it.currency.equals(tokenTo.module, true)) {
                    isPublishToken = true
                }
            }
            if (!isPublishToken) {
                throw AccountPayeeTokenNotActiveException(
                    getDiemCoinType(),
                    payeeAddress,
                    tokenTo
                )
            }
        }

        val simpleSecurity =
            SimpleSecurity.instance(ContextProvider.getContext())

        val fromAccount = AccountManager.getAccountByCoinNumber(tokenFrom.coinNumber)
            ?: throw AccountNotFindAddressException()
        val privateKey = simpleSecurity.decrypt(pwd, fromAccount.privateKey)
            ?: throw RuntimeException("password error")
        // 开始发起 Violas 交易
        val account = Account(KeyPair.fromSecretKey(privateKey))

        val typeTagFrom = TypeTagStructTag(
            StructTag(
                AccountAddress(tokenFrom.address.hexStringToByteArray()),
                tokenFrom.module,
                tokenFrom.name,
                arrayListOf()
            )
        )

        val subExchangeDate = JSONObject()
        subExchangeDate.put("flag", "violas")
        subExchangeDate.put(
            "type",
            supportMappingPair[IAssetMark.convert(tokenTo).mark()]?.label
        )
        if (tokenTo.coinNumber == getBitcoinCoinType().coinNumber()) {
            subExchangeDate.put("to_address", payeeAddress)
        } else {
            val authKeyPrefix = "00000000000000000000000000000000"
            subExchangeDate.put("to_address", authKeyPrefix + payeeAddress)
        }
        subExchangeDate.put("state", "start")
        subExchangeDate.put("out_amount", amountOutMin)
        subExchangeDate.put("times", 10)

        val optionTokenSwapTransactionPayload =
            TransactionPayload.optionTransactionPayload(
                ContextProvider.getContext(),
                supportMappingPair[IAssetMark.convert(tokenTo).mark()]?.receiverAddress ?: "",
                amountIn,
                metaData = subExchangeDate.toString().toByteArray(),
                typeTag = typeTagFrom
            )

        return mViolasRpcService.sendTransaction(
            optionTokenSwapTransactionPayload,
            account,
            gasCurrencyCode = typeTagFrom.value.module,
            chainId = getViolasChainId()
        ).sequenceNumber.toString()
    }

    override fun hasHandleCancel(
        fromIAssetMark: IAssetMark,
        toIAssetMark: IAssetMark
    ): Boolean {
        return fromIAssetMark is DiemCurrencyAssetMark
                && fromIAssetMark.coinNumber() == getViolasCoinType().coinNumber()
                && supportMappingPair.containsKey(toIAssetMark.mark())
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
        fromIAssetMark as DiemCurrencyAssetMark

        val simpleSecurity =
            SimpleSecurity.instance(ContextProvider.getContext())

        val fromAccount = AccountManager.getAccountByCoinNumber(fromIAssetMark.coinNumber())
            ?: throw AccountNotFindAddressException()
        val privateKey = simpleSecurity.decrypt(pwd, fromAccount.privateKey)
            ?: throw RuntimeException("password error")
        // 开始发起 Violas 交易
        val account = Account(KeyPair.fromSecretKey(privateKey))

        val typeTagFrom = TypeTagStructTag(
            StructTag(
                AccountAddress.DEFAULT,
                fromIAssetMark.module,
                fromIAssetMark.name,
                arrayListOf()
            )
        )

        val subExchangeDate = JSONObject()
        subExchangeDate.put("flag", "violas")
        subExchangeDate.put(
            "type",
            typeTag
        )
        subExchangeDate.put("tran_id", tranId)
        subExchangeDate.put("state", "stop")

        val optionTokenSwapTransactionPayload =
            TransactionPayload.optionTransactionPayload(
                ContextProvider.getContext(),
                supportMappingPair[toIAssetMark.mark()]?.receiverAddress ?: "",
                1,
                metaData = subExchangeDate.toString().toByteArray(),
                typeTag = typeTagFrom
            )

        return mViolasRpcService.sendTransaction(
            optionTokenSwapTransactionPayload,
            account,
            gasCurrencyCode = typeTagFrom.value.module,
            chainId = getViolasChainId()
        ).sequenceNumber.toString()
    }
}