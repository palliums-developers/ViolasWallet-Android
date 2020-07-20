package com.violas.wallet.biz.exchange.processor

import com.palliums.content.ContextProvider
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.exchange.AccountNotFindAddressException
import com.violas.wallet.biz.exchange.AccountPayeeNotFindException
import com.violas.wallet.biz.exchange.AccountPayeeTokenNotActiveException
import com.violas.wallet.biz.exchange.MappingInfo
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.ui.main.market.bean.IAssetsMark
import com.violas.wallet.ui.main.market.bean.ITokenVo
import com.violas.wallet.ui.main.market.bean.StableTokenVo
import com.violas.walletconnect.extensions.hexStringToByteArray
import org.json.JSONObject
import org.palliums.libracore.crypto.KeyPair
import org.palliums.libracore.transaction.AccountAddress
import org.palliums.libracore.transaction.TransactionPayload
import org.palliums.libracore.transaction.optionTransactionPayload
import org.palliums.libracore.transaction.storage.StructTag
import org.palliums.libracore.transaction.storage.TypeTagStructTag
import org.palliums.libracore.wallet.Account

class LibraToMappingAssetsProcessor(
    private val supportMappingPair: HashMap<String, MappingInfo>
) : IProcessor {

    private val mLibraRpcService by lazy {
        DataRepository.getLibraService()
    }

    private val mAccountManager by lazy {
        AccountManager()
    }

    override fun hasHandle(tokenFrom: ITokenVo, tokenTo: ITokenVo): Boolean {
        return tokenFrom is StableTokenVo
                && tokenFrom.coinNumber == CoinTypes.Libra.coinType()
                && supportMappingPair.containsKey(IAssetsMark.convert(tokenTo).mark())
    }

    override suspend fun handle(
        privateKey: ByteArray,
        tokenFrom: ITokenVo,
        tokenTo: ITokenVo,
        payee: String?,
        amountIn: Long,
        amountOutMin: Long,
        path: ByteArray,
        data: ByteArray
    ) {
        tokenFrom as StableTokenVo
        tokenTo as StableTokenVo

        val payeeAddress =
            payee ?: mAccountManager.getIdentityByCoinType(tokenTo.coinNumber)?.address
            ?: throw AccountNotFindAddressException()

        // 检查收款地址激活状态
        val accountState =
            mLibraRpcService.getAccountState(payeeAddress) ?: throw AccountPayeeNotFindException()

        // 检查收款地址 Token 注册状态
        var isPublishToken = false
        accountState.balances?.forEach {
            if (it.currency.equals(tokenTo.module, true)) {
                isPublishToken = true
            }
        }
        if (!isPublishToken) {
            throw AccountPayeeTokenNotActiveException(
                CoinTypes.Libra,
                payeeAddress,
                tokenTo
            )
        }

        // 开始交易
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
        subExchangeDate.put("flag", "libra")
        subExchangeDate.put(
            "type",
            supportMappingPair[IAssetsMark.convert(tokenTo).mark()]?.label
        )
        val authKeyPrefix = "00000000000000000000000000000000"
        subExchangeDate.put("to_address", authKeyPrefix + payeeAddress)
        subExchangeDate.put("state", "start")
        subExchangeDate.put("out_amount", amountOutMin)
        subExchangeDate.put("times", 0)

        val optionTokenSwapTransactionPayload =
            TransactionPayload.optionTransactionPayload(
                ContextProvider.getContext(),
                supportMappingPair[IAssetsMark.convert(tokenTo).mark()]?.receiverAddress ?: "",
                amountIn,
                metaData = subExchangeDate.toString().toByteArray(),
                typeTag = typeTagFrom
            )

        mLibraRpcService.sendTransaction(
            optionTokenSwapTransactionPayload,
            account,
            gasCurrencyCode = typeTagFrom.value.module
        )
    }
}