package com.violas.wallet.biz.exchange.processor

import com.palliums.content.ContextProvider
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.WrongPasswordException
import com.violas.wallet.biz.bean.DiemAppToken
import com.violas.wallet.biz.exchange.AccountNotFindAddressException
import com.violas.wallet.biz.exchange.MappingInfo
import com.violas.wallet.biz.transaction.DiemTxnManager
import com.violas.wallet.biz.transaction.ViolasTxnManager
import com.violas.wallet.common.*
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.ui.main.market.bean.DiemCurrencyAssetMark
import com.violas.wallet.ui.main.market.bean.IAssetMark
import com.violas.wallet.ui.main.market.bean.ITokenVo
import com.violas.wallet.ui.main.market.bean.StableTokenVo
import com.violas.walletconnect.extensions.hexStringToByteArray
import org.json.JSONObject
import org.palliums.violascore.crypto.KeyPair
import org.palliums.violascore.transaction.AccountAddress
import org.palliums.violascore.transaction.TransactionPayload
import org.palliums.violascore.transaction.optionTransactionPayload
import org.palliums.violascore.transaction.storage.StructTag
import org.palliums.violascore.transaction.storage.TypeTag
import org.palliums.violascore.wallet.Account

class ViolasToAssetsMappingProcessor(
    private val mSupportMappingPair: HashMap<String, MappingInfo>
) : IProcessor {

    private val mViolasRpcService by lazy {
        DataRepository.getViolasRpcService()
    }

    private val mDiemRpcService by lazy {
        DataRepository.getDiemRpcService()
    }

    override fun hasHandleSwap(tokenFrom: ITokenVo, tokenTo: ITokenVo): Boolean {
        return tokenFrom is StableTokenVo
                && tokenFrom.coinNumber == getViolasCoinType().coinNumber()
                && mSupportMappingPair.containsKey(IAssetMark.convert(tokenTo).mark())
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

        if (tokenTo.coinNumber == getDiemCoinType().coinNumber()) {
            tokenTo as StableTokenVo

            // 检查Diem收款人账户
            DiemTxnManager().getReceiverAccountState(
                payeeAddress,
                DiemAppToken.convert(tokenTo)
            ) {
                mDiemRpcService.getAccountState(it)
            }
        }

        val fromAccount = AccountManager.getAccountByCoinNumber(tokenFrom.coinNumber)
            ?: throw AccountNotFindAddressException()
        val privateKey = SimpleSecurity.instance(ContextProvider.getContext())
            .decrypt(pwd, fromAccount.privateKey) ?: throw WrongPasswordException()
        val payerAccount = Account(KeyPair.fromSecretKey(privateKey))

        // 检查Violas付款人账户
        val violasTxnManager = ViolasTxnManager()
        val payerAccountState = violasTxnManager.getSenderAccountState(payerAccount) {
            mViolasRpcService.getAccountState(it)
        }

        // 计算gas info
        val gasInfo = violasTxnManager.calculateGasInfo(
            payerAccountState,
            listOf(Pair(tokenFrom.module, amountIn))
        )

        // 构建跨链兑换协议数据
        val subExchangeDate = JSONObject()
        subExchangeDate.put("flag", "violas")
        subExchangeDate.put(
            "type",
            mSupportMappingPair[IAssetMark.convert(tokenTo).mark()]?.label
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

        val transferTransactionPayload = TransactionPayload.optionTransactionPayload(
            ContextProvider.getContext(),
            mSupportMappingPair[IAssetMark.convert(tokenTo).mark()]?.receiverAddress ?: "",
            amountIn,
            metaData = subExchangeDate.toString().toByteArray(),
            typeTag = TypeTag.newStructTag(
                StructTag(
                    AccountAddress(tokenFrom.address.hexStringToByteArray()),
                    tokenFrom.module,
                    tokenFrom.name,
                    arrayListOf()
                )
            )
        )

        return mViolasRpcService.sendTransaction(
            transferTransactionPayload,
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
        return fromIAssetMark is DiemCurrencyAssetMark
                && fromIAssetMark.coinNumber() == getViolasCoinType().coinNumber()
                && mSupportMappingPair.containsKey(toIAssetMark.mark())
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

        val fromAccount = AccountManager.getAccountByCoinNumber(fromIAssetMark.coinNumber())
            ?: throw AccountNotFindAddressException()
        val privateKey = SimpleSecurity.instance(ContextProvider.getContext())
            .decrypt(pwd, fromAccount.privateKey) ?: throw WrongPasswordException()
        val payerAccount = Account(KeyPair.fromSecretKey(privateKey))

        // 检查Violas付款人账户
        val violasTxnManager = ViolasTxnManager()
        val payerAccountState = violasTxnManager.getSenderAccountState(payerAccount) {
            mViolasRpcService.getAccountState(it)
        }

        // 计算gas info
        val amount = 1L
        val gasInfo = violasTxnManager.calculateGasInfo(
            payerAccountState,
            listOf(Pair(fromIAssetMark.module, amount))
        )

        // 构建取消跨链兑换协议数据
        val subExchangeDate = JSONObject()
        subExchangeDate.put("flag", "violas")
        subExchangeDate.put(
            "type",
            typeTag
        )
        subExchangeDate.put("tran_id", tranId)
        subExchangeDate.put("state", "stop")

        val transferTransactionPayload = TransactionPayload.optionTransactionPayload(
            ContextProvider.getContext(),
            mSupportMappingPair[toIAssetMark.mark()]?.receiverAddress ?: "",
            amount,
            metaData = subExchangeDate.toString().toByteArray(),
            typeTag = TypeTag.newStructTag(
                StructTag(
                    AccountAddress(fromIAssetMark.address.hexStringToByteArray()),
                    fromIAssetMark.module,
                    fromIAssetMark.name,
                    arrayListOf()
                )
            )
        )

        return mViolasRpcService.sendTransaction(
            transferTransactionPayload,
            payerAccount,
            sequenceNumber = payerAccountState.sequenceNumber,
            gasCurrencyCode = gasInfo.gasCurrencyCode,
            maxGasAmount = gasInfo.maxGasAmount,
            gasUnitPrice = gasInfo.gasUnitPrice,
            chainId = getViolasChainId()
        ).sequenceNumber.toString()
    }
}