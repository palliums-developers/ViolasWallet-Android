package com.violas.wallet.repository.http.ethereum

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.palliums.net.await
import com.palliums.net.checkResponse
import com.violas.wallet.repository.http.jsonRpc.JsonRPCRequestDTO
import com.violas.wallet.repository.http.jsonRpc.RPCService
import okhttp3.OkHttpClient
import okhttp3.internal.toHexString
import org.palliums.libracore.serialization.toHex
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.math.BigInteger

class EtherscanRepository(private val okHttpClient: OkHttpClient, testNet: Boolean) {

    private val mUrl by lazy {
        if (testNet) {
            "https://ropsten.infura.io/v3/c433d19bc15943d6b8c978081bd652de"
        } else {
            "https://mainnet.infura.io/v3/c433d19bc15943d6b8c978081bd652de"
        }
    }
    private val mService by lazy {
        RPCService(mUrl, okHttpClient)
    }

    private val mGson by lazy {
        Gson()
    }

    private fun String.addHexPrefix(): String {
        return if (this.length >= 2 && (this[0] == '0' && this[1] == 'x')) {
            this
        } else {
            "0x$this"
        }
    }

    private fun String.removeHexPrefix(): String {
        return if (this[0] == '0' && this[1] == 'x') {
            return this.removePrefix("0x")
        } else {
            this
        }
    }

    private fun JsonRPCRequestDTO.toJson(): String {
        return mGson.toJson(this)
    }

    suspend fun getBalance(
        address: String
    ): BigInteger {
        return mService.call<String>(
            JsonRPCRequestDTO(
                "eth_getBalance",
                arrayListOf(
                    address.addHexPrefix(),
                    "latest"
                )
            ).toJson()
        )?.removeHexPrefix()?.toBigInteger(16) ?: BigInteger.ZERO
    }

    suspend fun getGasPrice(): BigInteger {
        return mService.call<String>(
            JsonRPCRequestDTO(
                "eth_gasPrice",
                arrayListOf()
            ).toJson()
        )?.removeHexPrefix()?.toBigInteger(16) ?: BigInteger.ZERO
    }

    suspend fun getNonce(
        address: String
    ): BigInteger {
        return mService.call<String>(
            JsonRPCRequestDTO(
                "eth_getTransactionCount",
                arrayListOf(
                    address.addHexPrefix(),
                    "latest"
                )
            ).toJson()
        )?.removeHexPrefix()?.toBigInteger(16) ?: BigInteger.ZERO
    }

    suspend fun sendRawTransaction(
        hexTx: String
    ): String? {
        return mService.call<String>(
            JsonRPCRequestDTO(
                "eth_sendRawTransaction",
                arrayListOf(
                    hexTx.addHexPrefix()
                )
            ).toJson()
        )
    }

    data class ContractDTO(
        val from: String? = null,
        val to: String,
        val gas: String? = null,
        val gasPrice: String? = null,
        val value: String? = null,
        val `data`: String? = null
    )

    suspend fun contractCall(
        to: String,
        from: String? = null,
        gas: BigInteger? = null,
        gasPrice: BigInteger? = null,
        value: BigInteger? = null,
        data: ByteArray? = null
    ): String? {
        val contractDTO = ContractDTO(
            from = from?.addHexPrefix(),
            to = to.addHexPrefix(),
            gas = gas?.toString(16)?.addHexPrefix(),
            gasPrice = gasPrice?.toString(16)?.addHexPrefix(),
            value = value?.toString(16)?.addHexPrefix(),
            data = data?.toHex()?.addHexPrefix()
        )

        return mService.call<String>(
            JsonRPCRequestDTO(
                "eth_call",
                arrayListOf(
                    contractDTO,
                    "latest"
                )
            ).toJson()
        )
    }

    suspend fun estimateGas(
        to: String,
        from: String? = null,
        gas: BigInteger? = null,
        gasPrice: BigInteger? = null,
        value: BigInteger? = null,
        data: ByteArray? = null
    ): BigInteger {
        val contractDTO = ContractDTO(
            to = to.addHexPrefix(),
            from = from?.addHexPrefix(),
            gas = gas?.toString(16)?.addHexPrefix(),
            gasPrice = gasPrice?.toString(16)?.addHexPrefix(),
            value = value?.toString(16)?.addHexPrefix(),
            data = data?.toHex()?.addHexPrefix()
        )
        return mService.call<String>(
            JsonRPCRequestDTO(
                "eth_estimateGas",
                arrayListOf(
                    contractDTO
                )
            ).toJson()
        )?.removeHexPrefix()?.toBigInteger(16) ?: BigInteger.ZERO
    }
}