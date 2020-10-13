package org.palliums.lib.jsonrpc

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class RPCService(private val url: String, private val okHttpClient: OkHttpClient) {
    companion object {
        val JSONMediaType: MediaType = "application/json".toMediaType()
    }

    val mGson = Gson()

    inline fun <reified T> Gson.fromJson(json: String) =
        this.fromJson<T>(json, object : TypeToken<T>() {}.type)

    fun getHttpUrl() = url
    fun getOkHttpClient() = okHttpClient

    suspend inline fun <reified T> call(body: JsonRPCRequestDTO): T? {
        return suspendCancellableCoroutine {
            try {
                val request: Request = Request.Builder()
                    .url(getHttpUrl())
                    .header("content-type", "application/json")
                    .post(mGson.toJson(body).toByteArray().toRequestBody(JSONMediaType))
                    .build()
                val newCall = getOkHttpClient().newCall(request)
                it.invokeOnCancellation {
                    try {
                        newCall.cancel()
                    } catch (e: Exception) {
                    }
                }
                val response: Response = newCall.execute()

                if (!response.isSuccessful) {
                    it.resumeWithException(ResponseExceptions(-1, "http error"))
                } else if (response.body == null) {
                    it.resumeWithException(ResponseExceptions(-1, "data is null"))
                } else {
                    val type = object : TypeToken<JsonRPCResponseDTO<T>>() {}.type
                    val fromJson =
                        mGson.fromJson<JsonRPCResponseDTO<T>>(response.body!!.string())
                    if (fromJson.error != null) {
                        it.resumeWithException(
                            ResponseExceptions(
                                fromJson.error.code,
                                fromJson.error.message
                            )
                        )
                    } else {
                        it.resume(fromJson.result)
                    }
                }
            } catch (e: Exception) {
                it.resumeWithException(RequestException(e))
            }
        }
    }
}