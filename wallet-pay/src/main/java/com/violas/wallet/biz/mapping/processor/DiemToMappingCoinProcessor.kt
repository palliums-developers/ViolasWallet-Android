package com.violas.wallet.biz.mapping.processor

import com.palliums.content.ContextProvider
import com.violas.wallet.biz.WrongPasswordException
import com.violas.wallet.biz.bean.DiemAppToken
import com.violas.wallet.biz.transaction.DiemTxnManager
import com.violas.wallet.biz.transaction.ViolasTxnManager
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.common.getDiemChainId
import com.violas.wallet.common.getDiemCoinType
import com.violas.wallet.common.getViolasCoinType
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.mapping.MappingCoinPairDTO
import com.violas.wallet.utils.str2CoinType
import com.violas.walletconnect.extensions.hexStringToByteArray
import org.json.JSONObject
import org.palliums.libracore.crypto.KeyPair
import org.palliums.libracore.transaction.AccountAddress
import org.palliums.libracore.transaction.TransactionPayload
import org.palliums.libracore.transaction.optionTransactionPayload
import org.palliums.libracore.transaction.storage.StructTag
import org.palliums.libracore.transaction.storage.TypeTag
import org.palliums.libracore.wallet.Account
import org.palliums.violascore.http.ViolasRpcService

/**
 * Created by elephant on 2020/8/13 17:13.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class DiemToMappingCoinProcessor(
    private val mViolasRpcService: ViolasRpcService
) : MappingProcessor {

    private val mDiemRpcService by lazy { DataRepository.getDiemRpcService() }

    override fun hasMappable(coinPair: MappingCoinPairDTO): Boolean {
        return str2CoinType(coinPair.fromCoin.chainName) == getDiemCoinType()
                && str2CoinType(coinPair.toCoin.chainName) == getViolasCoinType()
    }

    override suspend fun mapping(
        checkPayeeAccount: Boolean,
        payeeAddress: String?,
        payeeAccountDO: AccountDO?,
        payerAccountDO: AccountDO,
        password: ByteArray,
        mappingAmount: Long,
        coinPair: MappingCoinPairDTO
    ): String {
        if (checkPayeeAccount) {
            // 检查Violas收款人账户
            ViolasTxnManager().getReceiverAccountState(
                payeeAddress ?: payeeAccountDO!!.address,
                DiemAppToken.convert(coinPair.toCoin.assets)
            ) {
                mViolasRpcService.getAccountState(it)
            }
        }

        val payerPrivateKey = SimpleSecurity.instance(ContextProvider.getContext())
            .decrypt(password, payerAccountDO.privateKey) ?: throw WrongPasswordException()
        val payerAccount = Account(KeyPair.fromSecretKey(payerPrivateKey))

        // 检查Diem付款人账户
        val diemTxnManager = DiemTxnManager()
        val payerAccountState = diemTxnManager.getSenderAccountState(payerAccount) {
            mDiemRpcService.getAccountState(it)
        }

        // 计算gas info
        val gasInfo = diemTxnManager.calculateGasInfo(
            payerAccountState,
            listOf(Pair(coinPair.fromCoin.assets.module, mappingAmount))
        )

        // 构建映射协议数据
        val subMappingDate = JSONObject()
        subMappingDate.put("flag", "libra")
        subMappingDate.put("type", coinPair.mappingType)
        subMappingDate.put(
            "to_address",
            "00000000000000000000000000000000${payeeAccountDO!!.address}"
        )
        subMappingDate.put("state", "start")
        subMappingDate.put("times", 0)

        val transferTransactionPayload = TransactionPayload.optionTransactionPayload(
            ContextProvider.getContext(),
            coinPair.receiverAddress,
            mappingAmount,
            metaData = subMappingDate.toString().toByteArray(),
            typeTag = TypeTag.newStructTag(
                StructTag(
                    AccountAddress(coinPair.fromCoin.assets.address.hexStringToByteArray()),
                    coinPair.fromCoin.assets.module,
                    coinPair.fromCoin.assets.name,
                    arrayListOf()
                )
            )
        )

        return mDiemRpcService.sendTransaction(
            transferTransactionPayload,
            payerAccount,
            sequenceNumber = payerAccountState.sequenceNumber,
            gasCurrencyCode = gasInfo.gasCurrencyCode,
            maxGasAmount = gasInfo.maxGasAmount,
            gasUnitPrice = gasInfo.gasUnitPrice,
            chainId = getDiemChainId()
        ).sequenceNumber.toString()
    }
}