package com.violas.wallet.repository.http.jsonRpc

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.palliums.net.ApiResponse
import java.util.*
import kotlin.math.abs

@Keep
class JsonRPCResponseDTO<T> {
    val jsonrpc: String = "2.0"
    val id: Int = 0
    val result: T? = null
    val error: Error? = null

    class Error {
        val code: Int = 0
        val message: String = ""
    }
}

@Keep
data class JsonRPCRequestDTO(
    val method: String,
    val params: List<Any>,
    @SerializedName("jsonrpc")
    val jsonRPC: String = "2.0",
    val id: String = abs(Random().nextInt()).toString()
)