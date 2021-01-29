package com.palliums.exceptions

import com.google.gson.JsonParseException
import com.palliums.BuildConfig
import com.palliums.R
import com.palliums.net.ApiResponse
import com.palliums.utils.getString
import com.palliums.utils.isNetworkConnected
import kotlinx.coroutines.CancellationException
import org.apache.http.conn.ConnectTimeoutException
import org.json.JSONException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLPeerUnverifiedException

/**
 * Created by elephant on 2019-08-01 10:22.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 请求异常，统一处理网络请求中出现的exception、http error、server error、响应数据无法解析、无网络情况
 */
class RequestException : RuntimeException, BaseException {

    companion object {

        /** 未知错误 */
        const val ERROR_CODE_UNKNOWN_ERROR = "L100"

        /** 连接超时 */
        private const val ERROR_CODE_CONNECT_TIMEOUT = "L101"

        /** Socket超时 */
        private const val ERROR_CODE_SOCKET_TIMEOUT = "L102"

        /** 服务器证书无效 */
        private const val ERROR_CODE_CERTIFICATE_INVALID = "L103"

        /** 未知主机（可能是因为无网络） */
        private const val ERROR_CODE_UNKNOWN_HOST = "L104"

        /** 连接异常（可能是主机拒绝了连接） */
        private const val ERROR_CODE_CONNECT_EXCEPTION = "L105"

        /** 数据异常 */
        private const val ERROR_CODE_DATA_EXCEPTION = "L106"

        /** 没有网络 */
        const val ERROR_CODE_NO_NETWORK = "L107"

        /** 主动取消 */
        const val ERROR_CODE_ACTIVE_CANCELLATION = "L108"

        fun networkUnavailable(): RequestException {
            return RequestException(
                ERROR_CODE_NO_NETWORK,
                getString(R.string.common_http_network_unavailable)
            )
        }

        fun responseDataException(errorDesc: String): RequestException {
            return RequestException(
                ERROR_CODE_DATA_EXCEPTION,
                errorDesc
            )
        }
    }

    /**
     * 错误码
     * 正数表示一次http的状态码或错误码，如：404，500，502，服务器返回的非200...
     * 负数表示一个exception的错误码，如：[ERROR_CODE_CERTIFICATE_INVALID]...
     */
    val errorCode: Any
    val errorMsg: String?

    constructor(exception: Throwable) : super(exception) {
        if (exception is RequestException) {
            errorCode = exception.errorCode
            errorMsg = exception.errorMsg
            return
        } else if (exception is CancellationException
            || exception is java.util.concurrent.CancellationException
            || this.javaClass.name == "kotlinx.coroutines.JobCancellationException"
        ) {
            errorCode = ERROR_CODE_ACTIVE_CANCELLATION
            errorMsg = "Active Cancellation"
            return
        } else if (!isNetworkConnected()) {
            errorCode = ERROR_CODE_NO_NETWORK
            errorMsg = getString(R.string.common_http_network_unavailable)
            return
        }

        when (exception) {
            is retrofit2.HttpException -> {
                errorCode = "H${exception.code()}"
                errorMsg = if (BuildConfig.DEBUG)
                    "HTTP ${exception.code()} ${exception.message()}"
                else
                    getString(R.string.common_http_server_error, errorCode)
            }
            is ConnectTimeoutException -> {
                // 连接超时，此时并未发送数据
                errorCode = ERROR_CODE_CONNECT_TIMEOUT
                errorMsg = getString(R.string.common_http_connect_timeout)
            }
            is SocketTimeoutException -> {
                // Socket超时，此时数据已发送但服务器未应答
                errorCode = ERROR_CODE_SOCKET_TIMEOUT
                errorMsg = getString(R.string.common_http_socket_timeout)
            }
            is SSLPeerUnverifiedException -> {
                // 证书错误
                errorCode = ERROR_CODE_CERTIFICATE_INVALID
                errorMsg = getString(R.string.common_http_certificate_invalid)
            }
            is UnknownHostException -> {
                errorCode = ERROR_CODE_UNKNOWN_HOST
                errorMsg = getString(R.string.common_http_unknown_host)
            }
            is ConnectException -> {
                errorCode = ERROR_CODE_CONNECT_EXCEPTION
                errorMsg = getString(R.string.common_http_connect_exception)
            }
            is JSONException, is JsonParseException -> {
                errorCode = ERROR_CODE_DATA_EXCEPTION
                errorMsg = exception.toString()
            }
            else -> {
                if (exception is InterruptedIOException
                    && exception.message?.contains("timeout", true) == true
                ) {
                    // Socket超时，此时数据已发送但服务器未应答
                    errorCode = ERROR_CODE_SOCKET_TIMEOUT
                    errorMsg = getString(R.string.common_http_socket_timeout)
                    return
                }

                // 未知错误
                errorCode = ERROR_CODE_UNKNOWN_ERROR
                errorMsg = exception.toString()
            }
        }
    }

    constructor(response: ApiResponse) : super() {
        errorCode = response.getErrorCode()

        val realErrorMsg = response.getErrorMsg()
        errorMsg =
            if (realErrorMsg == null || (realErrorMsg is String && realErrorMsg.isEmpty())) {
                null
            } else {
                realErrorMsg.toString()
            }
    }

    private constructor(errorCode: Any, errorMsg: String) {
        this.errorCode = errorCode
        this.errorMsg = errorMsg
    }

    override val message: String?
        get() = errorMsg

    override fun getErrorMessage(loadAction: Boolean?, failedDesc: String?): String {
        if (errorCode != ERROR_CODE_DATA_EXCEPTION
            && errorCode != ERROR_CODE_UNKNOWN_ERROR
            && !errorMsg.isNullOrEmpty()
        ) {
            return errorMsg
        }

        if (!failedDesc.isNullOrBlank()) {
            return if (BuildConfig.DEBUG)
                "$failedDesc($errorCode)\n${errorMsg ?: "Unknown reason"}"
            else
                "$failedDesc($errorCode)"
        }

        if (loadAction == null) {
            return if (BuildConfig.DEBUG)
                "${getString(R.string.common_load_fail)}($errorCode)\n${
                    errorMsg ?: "Unknown reason"
                }"
            else
                "${getString(R.string.common_load_fail)}($errorCode)"
        }

        return if (BuildConfig.DEBUG)
            "${getString(R.string.common_operation_fail)}($errorCode)\n${
                errorMsg ?: "Unknown reason"
            }"
        else
            "${getString(R.string.common_operation_fail)}($errorCode)"
    }
}