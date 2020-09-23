package com.violas.wallet.repository.http.ethereum

import androidx.annotation.Keep
import com.palliums.net.ApiResponse

@Keep
class BaseEthResponse<T> : ApiResponse {
    val message: String = ""
    val status: String = ""
    val result: T? = null

    override fun getSuccessCode(): Any {
        return status == "1"
    }

    override fun getErrorCode(): Any {
        return if (status != "1") true else status
    }

    override fun getErrorMsg(): Any? {
        return message
    }

    override fun getResponseData(): Any? {
        return result
    }
}



@Keep
data class GasOracleResponse(
    val FastGasPrice: String,
    val LastBlock: String,
    val ProposeGasPrice: String,
    val SafeGasPrice: String
)