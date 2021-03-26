package com.violas.wallet.biz.exchange.processor

import com.palliums.content.ContextProvider
import com.quincysx.crypto.bitcoin.BitcoinOutputStream
import com.quincysx.crypto.bitcoin.script.Script
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.LackOfBalanceException
import com.violas.wallet.biz.TransferUnknownException
import com.violas.wallet.biz.WrongPasswordException
import com.violas.wallet.biz.bean.DiemAppToken
import com.violas.wallet.biz.btc.TransactionManager
import com.violas.wallet.biz.exchange.AccountNotFindAddressException
import com.violas.wallet.biz.exchange.MappingInfo
import com.violas.wallet.biz.transaction.DiemTxnManager
import com.violas.wallet.biz.transaction.ViolasTxnManager
import com.violas.wallet.common.*
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.http.bitcoinChainApi.request.BitcoinChainApi
import com.violas.wallet.ui.main.market.bean.*
import com.violas.walletconnect.extensions.hexStringToByteArray
import com.violas.walletconnect.extensions.toHex
import kotlinx.coroutines.suspendCancellableCoroutine
import org.palliums.violascore.serialization.hexToBytes
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class BTCToMappingAssetsProcessor(
    private val contractAddress: String,
    private val supportMappingPair: HashMap<String, MappingInfo>
) : IProcessor {

    private val mViolasRpcService by lazy {
        DataRepository.getViolasRpcService()
    }

    private val mDiemRpcService by lazy {
        DataRepository.getDiemRpcService()
    }

    override fun hasHandleSwap(tokenFrom: ITokenVo, tokenTo: ITokenVo): Boolean {
        return tokenFrom is PlatformTokenVo
                && tokenFrom.coinNumber == getBitcoinCoinType().coinNumber()
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
        tokenTo as StableTokenVo

        val payeeAddress =
            payee ?: AccountManager.getAccountByCoinNumber(tokenTo.coinNumber)?.address
            ?: throw AccountNotFindAddressException()

        if (tokenTo.coinNumber == getViolasCoinType().coinNumber()) {
            // 检查Violas收款人账户
            ViolasTxnManager().getReceiverAccountState(
                payeeAddress,
                DiemAppToken.convert(tokenTo)
            ) {
                mViolasRpcService.getAccountState(it)
            }
        } else if (tokenTo.coinNumber == getDiemCoinType().coinNumber()) {
            // 检查Diem收款人账户
            DiemTxnManager().getReceiverAccountState(
                payeeAddress,
                DiemAppToken.convert(tokenTo)
            ) {
                mDiemRpcService.getAccountState(it)
            }
        }

        val senderAccount = AccountManager.getAccountByCoinNumber(tokenFrom.coinNumber)
            ?: throw AccountNotFindAddressException()
        val privateKey = SimpleSecurity.instance(ContextProvider.getContext())
            .decrypt(pwd, senderAccount.privateKey) ?: throw WrongPasswordException()

        val mTransactionManager = TransactionManager(arrayListOf(senderAccount.address))
        val checkBalance =
            mTransactionManager.checkBalance(amountIn / 100000000.0, 3)
        if (!checkBalance) throw LackOfBalanceException()

        return suspendCancellableCoroutine { coroutine ->
            val subscribe = mTransactionManager.obtainTransaction(
                privateKey,
                senderAccount.publicKey.hexStringToByteArray(),
                checkBalance,
                supportMappingPair[IAssetMark.convert(tokenTo).mark()]?.receiverAddress ?: "",
                senderAccount.address,
                ViolasOutputScript().requestExchange(
                    supportMappingPair[IAssetMark.convert(tokenTo).mark()]?.label ?: "",
                    payeeAddress.hexStringToByteArray(),
                    contractAddress.hexStringToByteArray(),
                    amountOutMin
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
                coroutine.resume(it)
            }, {
                coroutine.resumeWithException(it)
            })
            coroutine.invokeOnCancellation {
                subscribe.dispose()
            }
        }
    }

    override fun hasHandleCancel(
        fromIAssetMark: IAssetMark,
        toIAssetMark: IAssetMark
    ): Boolean {
        return fromIAssetMark is CoinAssetMark
                && fromIAssetMark.coinNumber() == getBitcoinCoinType().coinNumber()
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
        toIAssetMark as DiemCurrencyAssetMark

        val senderAccount = AccountManager.getAccountByCoinNumber(fromIAssetMark.coinNumber())
            ?: throw AccountNotFindAddressException()
        val privateKey = SimpleSecurity.instance(ContextProvider.getContext())
            .decrypt(pwd, senderAccount.privateKey) ?: throw WrongPasswordException()

        val mTransactionManager = TransactionManager(arrayListOf(senderAccount.address))
        val checkBalance =
            mTransactionManager.checkBalance(500 / 100000000.0, 3)
        if (!checkBalance) throw LackOfBalanceException()

        return suspendCancellableCoroutine { coroutine ->
            val subscribe = mTransactionManager.obtainTransaction(
                privateKey,
                senderAccount.publicKey.hexStringToByteArray(),
                checkBalance,
                supportMappingPair[toIAssetMark.mark()]?.receiverAddress ?: "",
                senderAccount.address,
                ViolasOutputScript().cancelExchange(
                    typeTag,
                    originPayeeAddress.hexStringToByteArray(),
                    sequence = sequence?.toLong() ?: 0
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
                coroutine.resume(it)
            }, {
                coroutine.resumeWithException(it)
            })
            coroutine.invokeOnCancellation {
                subscribe.dispose()
            }
        }
    }
}

class ViolasOutputScript {
    companion object {
        const val OP_VER: Int = 0x0004
        val OP_TYPE_START: ByteArray = byteArrayOf(0x30, 0x00)
        val OP_TYPE_END: ByteArray = byteArrayOf(0x30, 0x01)
        val TYPE_CANCEL: ByteArray = byteArrayOf(0x30, 0x02)
    }

    /**
     * 创建映射交易
     */
    fun requestMapping(
        type: String,
        payeeAddress: ByteArray,
        contractAddress: ByteArray,
        sequence: Long = System.currentTimeMillis()
    ): Script {
        val dataStream = BitcoinOutputStream()
        dataStream.write("violas".toByteArray())
        // 此处非 BTC 小端字节规则，需要注意
        dataStream.writeInt16WithBigEndian(OP_VER)

        dataStream.write(type.replace("0x", "").hexToBytes())
        dataStream.write(payeeAddress)
        dataStream.write(
            ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(sequence).array()
        )
        dataStream.write(contractAddress)
        dataStream.write(
            ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(0).array()
        )
        dataStream.writeInt16WithBigEndian(0)
        dataStream.writeWithBigEndian(getViolasChainId())

        val scriptStream = BitcoinOutputStream()
        scriptStream.write(Script.OP_RETURN.toInt())
        Script.writeBytes(dataStream.toByteArray(), scriptStream)
        return Script(scriptStream.toByteArray())
    }

    /**
     * 创建跨链兑换交易
     * @param address 接收地址
     * @param vtokenAddress Token 地址
     */
    fun requestExchange(
        lable: String,
        payeeAddress: ByteArray,
        vtokenAddress: ByteArray,
        miniOutputAmount: Long,
        sequence: Long = System.currentTimeMillis()
    ): Script {
        val dataStream = BitcoinOutputStream()
        dataStream.write("violas".toByteArray())
        // 此处非 BTC 小端字节规则，需要注意
        dataStream.writeInt16WithBigEndian(OP_VER)

        dataStream.write(lable.replace("0x", "").hexToBytes())
        dataStream.write(payeeAddress)
        dataStream.write(
            ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(sequence).array()
        )
        dataStream.write(vtokenAddress)
        dataStream.write(
            ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(miniOutputAmount).array()
        )
        dataStream.writeInt16WithBigEndian(10)
        dataStream.writeWithBigEndian(getViolasChainId())

        val scriptStream = BitcoinOutputStream()
        scriptStream.write(Script.OP_RETURN.toInt())
        Script.writeBytes(dataStream.toByteArray(), scriptStream)
        return Script(scriptStream.toByteArray())
    }

    fun cancelExchange(
        lable: String,
        address: ByteArray,
        sequence: Long = System.currentTimeMillis()
    ): Script {
        val dataStream = BitcoinOutputStream()
        dataStream.write("violas".toByteArray())
        // 此处非 BTC 小端字节规则，需要注意
        dataStream.writeInt16WithBigEndian(OP_VER)

        dataStream.write(TYPE_CANCEL)
        dataStream.write(lable.replace("0x", "").hexToBytes())
        dataStream.write(address)
        dataStream.write(
            ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(sequence).array()
        )

        val scriptStream = BitcoinOutputStream()
        scriptStream.write(Script.OP_RETURN.toInt())
        Script.writeBytes(dataStream.toByteArray(), scriptStream)
        return Script(scriptStream.toByteArray())
    }
}