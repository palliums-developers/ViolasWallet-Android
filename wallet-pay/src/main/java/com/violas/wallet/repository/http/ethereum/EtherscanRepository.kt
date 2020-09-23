package com.violas.wallet.repository.http.ethereum

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.palliums.net.await
import com.palliums.net.checkResponse
import com.violas.wallet.repository.http.jsonRpc.JsonRPCRequestDTO
import com.violas.wallet.repository.http.jsonRpc.RPCService
import okhttp3.OkHttpClient
import okhttp3.internal.toHexString
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

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

    private fun String.hex2Long(): Long {
        return this.replace("0x", "").toLong(16)
    }

    private fun String.addHexPrefix(): String {
        return if (this[0] == '0' && this[1] == 'x') {
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
    ): Long {
        return mService.call<String>(
            JsonRPCRequestDTO(
                "eth_getBalance",
                arrayListOf(
                    address.addHexPrefix(),
                    "latest"
                )
            ).toJson()
        )?.hex2Long() ?: 0L
    }

    suspend fun getGasPrice(): Long {
        return mService.call<String>(
            JsonRPCRequestDTO(
                "eth_gasPrice",
                arrayListOf()
            ).toJson()
        )?.hex2Long() ?: 0L
    }

    suspend fun getNonce(
        address: String
    ): Long {
        return mService.call<String>(
            JsonRPCRequestDTO(
                "eth_getTransactionCount",
                arrayListOf(
                    address.addHexPrefix(),
                    "latest"
                )
            ).toJson()
        )?.hex2Long() ?: 0L
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
        gas: Long? = null,
        gasPrice: Long? = null,
        value: Long? = null,
        data: String? = null
    ): String? {
        val contractDTO = ContractDTO(
            from = from?.addHexPrefix(),
            to = to.addHexPrefix(),
            gas = gas?.toHexString()?.addHexPrefix(),
            gasPrice = gasPrice?.toHexString()?.addHexPrefix(),
            value = value?.toHexString()?.addHexPrefix(),
            data = data?.addHexPrefix()
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
        contractAddress: String,
        from: String? = null,
        gas: Long? = null,
        gasPrice: Long? = null,
        value: Long? = null,
        data: String? = null
    ): Long {
        val contractDTO = ContractDTO(
            to = contractAddress,
            from = from?.addHexPrefix(),
            gas = gas?.toHexString()?.addHexPrefix(),
            gasPrice = gasPrice?.toHexString()?.addHexPrefix(),
            value = value?.toHexString()?.addHexPrefix(),
            data = data?.addHexPrefix()
        )
        return mService.call<String>(
            JsonRPCRequestDTO(
                "eth_estimateGas",
                arrayListOf(
                    contractDTO
                )
            ).toJson()
        )?.hex2Long() ?: 0L
    }
}