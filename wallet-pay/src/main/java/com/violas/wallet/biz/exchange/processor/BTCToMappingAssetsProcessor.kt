package com.violas.wallet.biz.exchange.processor

import com.palliums.content.ContextProvider
import com.quincysx.crypto.CoinTypes
import com.quincysx.crypto.bitcoin.BitcoinOutputStream
import com.quincysx.crypto.bitcoin.script.Script
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.LackOfBalanceException
import com.violas.wallet.biz.TransferUnknownException
import com.violas.wallet.biz.btc.TransactionManager
import com.violas.wallet.biz.exchange.AccountNotFindAddressException
import com.violas.wallet.biz.exchange.AccountPayeeNotFindException
import com.violas.wallet.biz.exchange.AccountPayeeTokenNotActiveException
import com.violas.wallet.biz.exchange.MappingInfo
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.http.bitcoinChainApi.request.BitcoinChainApi
import com.violas.wallet.ui.main.market.bean.*
import com.violas.walletconnect.extensions.hexStringToByteArray
import com.violas.walletconnect.extensions.toHex
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class BTCToMappingAssetsProcessor(
    private val contractAddress: String,
    private val supportMappingPair: HashMap<String, MappingInfo>
) : IProcessor {

    private val mAccountManager by lazy {
        AccountManager()
    }
    private val mViolasRpcService by lazy {
        DataRepository.getViolasChainRpcService()
    }

    override fun hasHandle(tokenFrom: ITokenVo, tokenTo: ITokenVo): Boolean {
        return tokenFrom is PlatformTokenVo
                && tokenFrom.coinNumber == if (Vm.TestNet) {
            CoinTypes.BitcoinTest
        } else {
            CoinTypes.Bitcoin
        }.coinType()
                && supportMappingPair.containsKey(IAssetsMark.convert(tokenTo).mark())
                && tokenTo.coinNumber == CoinTypes.Violas.coinType()
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
        tokenTo as StableTokenVo

        val sendAccount = mAccountManager.getIdentityByCoinType(tokenFrom.coinNumber)
            ?: throw AccountNotFindAddressException()

        // 开始检查 Violas 账户的基本信息
        val payeeAddress =
            payee ?: mAccountManager.getIdentityByCoinType(tokenTo.coinNumber)?.address
            ?: throw AccountNotFindAddressException()

        // 检查收款地址激活状态
        val accountState =
            mViolasRpcService.getAccountState(payeeAddress) ?: throw AccountPayeeNotFindException()

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

        val simpleSecurity =
            SimpleSecurity.instance(ContextProvider.getContext())

        val fromAccount = mAccountManager.getIdentityByCoinType(tokenFrom.coinNumber)
            ?: throw AccountNotFindAddressException()
        val privateKey = simpleSecurity.decrypt(pwd, fromAccount.privateKey)
            ?: throw RuntimeException("password error")

        val mTransactionManager: TransactionManager =
            TransactionManager(arrayListOf(sendAccount.address))
        val checkBalance = mTransactionManager.checkBalance(amountIn / 100000000.0, 3)
        val violasOutputScript = ViolasOutputScript()

        if (!checkBalance) {
            throw LackOfBalanceException()
        }

        return suspendCancellableCoroutine { coroutin ->
            val subscribe = mTransactionManager.obtainTransaction(
                privateKey,
                sendAccount.publicKey.hexStringToByteArray(),
                checkBalance,
                supportMappingPair[IAssetsMark.convert(tokenTo).mark()]?.receiverAddress ?: "",
                sendAccount.address,
                violasOutputScript.requestExchange(
                    payeeAddress.hexStringToByteArray(),
                    contractAddress.hexStringToByteArray()
                )
            ).flatMap {
                try {
                    BitcoinChainApi.get()
                        .pushTx(it.signBytes.toHex())
                } catch (e: Exception) {
                    e.printStackTrace()
                    throw TransferUnknownException()
                }
            }.subscribe({
                coroutin.resume(it)
            }, {
                coroutin.resumeWithException(it)
            })
            coroutin.invokeOnCancellation {
                subscribe.dispose()
            }
        }
    }
}

class ViolasOutputScript {
    companion object {
        const val OP_VER: Int = 0x0002
        val OP_TYPE_START: ByteArray = byteArrayOf(0x30, 0x00)
        val OP_TYPE_END: ByteArray = byteArrayOf(0x30, 0x01)
        val TYPE_CANCEL: ByteArray = byteArrayOf(0x30, 0x02)
    }

    /**
     * 创建跨链兑换交易
     * @param address 接收地址
     * @param vtokenAddress Token 地址
     */
    fun requestExchange(
        payeeAddress: ByteArray,
        vtokenAddress: ByteArray,
        sequence: Long = System.currentTimeMillis()
    ): Script {
        val authKeyPrefix = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        val dataStream = BitcoinOutputStream()
        dataStream.write("violas".toByteArray())
        //dataStream.writeInt16(OP_VER)
        // 此处非 BTC 小端字节规则，需要注意
        writeInt16(OP_VER, dataStream)

        dataStream.write(OP_TYPE_START)
        dataStream.write(authKeyPrefix + payeeAddress)
        dataStream.write(
            ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(sequence).array()
        )
        dataStream.write(authKeyPrefix + vtokenAddress)

        val scriptStream = BitcoinOutputStream()
        scriptStream.write(Script.OP_RETURN.toInt())
        Script.writeBytes(dataStream.toByteArray(), scriptStream)
        return Script(scriptStream.toByteArray())
    }

    fun cancelExchange(address: ByteArray, sequence: Long = System.currentTimeMillis()): Script {
        val dataStream = BitcoinOutputStream()
        dataStream.write("violas".toByteArray())
        //dataStream.writeInt16(OP_VER)
        // 此处非 BTC 小端字节规则，需要注意
        writeInt16(OP_VER, dataStream)

        dataStream.write(TYPE_CANCEL)
        dataStream.write(address)
        dataStream.write(
            ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(sequence).array()
        )

        val scriptStream = BitcoinOutputStream()
        scriptStream.write(Script.OP_RETURN.toInt())
        Script.writeBytes(dataStream.toByteArray(), scriptStream)
        return Script(scriptStream.toByteArray())
    }

    // 大端顺序 write Int16
    private fun writeInt16(value: Int, output: BitcoinOutputStream) {
        output.write(OP_VER shr 8 and 0xff)
        output.write(OP_VER and 0xff)
    }
}