package com.violas.wallet.repository.http.jsonRpc

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.palliums.exceptions.RequestException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


class RPCService(private val url: String, private val okHttpClient: OkHttpClient) {
    companion object {
        val JSONMediaType: MediaType = "application/json; charset=utf-8".toMediaType()
    }

    private val mGson = Gson()

    suspend fun <T> call(body: String): T? {
        return suspendCancellableCoroutine {
            try {
                val request: Request = Request.Builder()
                    .url(url)
                    .post(body.toRequestBody(JSONMediaType))
                    .build()
                val newCall = okHttpClient.newCall(request)
                it.invokeOnCancellation {
                    newCall.cancel()
                }
                val response: Response = newCall.execute()

                if (!response.isSuccessful) {
                    it.resumeWithException(RequestException.responseDataException("http error"))
                } else if (response.body == null) {
                    it.resumeWithException(RequestException.responseDataException("data is null"))
                } else {

                    val type = object : TypeToken<JsonRPCResponseDTO<T>>() {}.type
                    val fromJson =
                        mGson.fromJson<JsonRPCResponseDTO<T>>(response.body!!.string(), type)
                    if (fromJson.error != null) {
                        it.resumeWithException(
                            RequestException(
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