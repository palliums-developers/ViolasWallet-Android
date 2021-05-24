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

        /** 没有网络 */
        const val ERROR_CODE_NO_NETWORK = "H100"

        /** 未知主机（可能是因为无网络） */
        private const val ERROR_CODE_UNKNOWN_HOST = "H101"

        /** 服务器证书无效 */
        private const val ERROR_CODE_CERTIFICATE_INVALID = "H102"

        /** 连接异常（可能是主机拒绝了连接） */
        private const val ERROR_CODE_CONNECT_ERROR = "H103"

        /** 连接超时 */
        private const val ERROR_CODE_CONNECT_TIMEOUT = "H104"

        /** Socket超时 */
        private const val ERROR_CODE_SOCKET_TIMEOUT = "H105"

        /** 未知错误 */
        const val ERROR_CODE_UNKNOWN_ERROR = "L100"

        /** 请求参数异常 */
        private const val ERROR_CODE_REQUEST_PARAM_ERROR = "L101"

        /** 响应数据异常 */
        private const val ERROR_CODE_RESPONSE_DATA_ERROR = "L102"

        /** 主动取消 */
        const val ERROR_CODE_ACTIVE_CANCELLATION = "U100"

        fun networkUnavailable(): RequestException {
            return RequestException(
                ERROR_CODE_NO_NETWORK,
                getString(R.string.common_http_network_unavailable)
            )
        }

        fun requestParamError(errorDesc: String): RequestException {
            return RequestException(
                ERROR_CODE_REQUEST_PARAM_ERROR,
                errorDesc
            )
        }

        fun responseDataError(errorDesc: String): RequestException {
            return RequestException(
                ERROR_CODE_RESPONSE_DATA_ERROR,
                errorDesc
            )
        }
    }

    /**
     * 错误码
     * 正数表示一次http的状态码或错误码，如：404，500，502，服务器返回的非200...
     * 负数表示一个exception的错误码，如：[ERROR_CODE_CERTIFICATE_INVALID]...
     */
    val errorCode: Any?
    val errorMsg: String

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
            is UnknownHostException -> {
                // 域名无法解析
                errorCode = ERROR_CODE_UNKNOWN_HOST
                errorMsg = getString(R.string.common_http_unknown_host)
            }
            is SSLPeerUnverifiedException -> {
                // 证书错误
                errorCode = ERROR_CODE_CERTIFICATE_INVALID
                errorMsg = getString(R.string.common_http_certificate_invalid)
            }
            is ConnectException -> {
                // 连接异常
                errorCode = ERROR_CODE_CONNECT_ERROR
                errorMsg = getString(R.string.common_http_connect_exception)
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
            is retrofit2.HttpException -> {
                errorCode = exception.code()
                errorMsg = if (BuildConfig.DEBUG)
                    "HTTP $errorCode ${exception.message()}"
                else
                    getString(R.string.common_http_server_error, errorCode)
            }
            is JSONException, is JsonParseException -> {
                errorCode = ERROR_CODE_RESPONSE_DATA_ERROR
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
                "Unknown reason"
            } else {
                realErrorMsg.toString()
            }
    }

    constructor(errorCode: Any? = null, errorMsg: String? = null) {
        this.errorCode = errorCode
        this.errorMsg = errorMsg ?: "Unknown reason"
    }

    override val message: String?
        get() = errorMsg

    override fun getErrorMessage(loadAction: Boolean?, failedDesc: String?): String {
        return when {
            cause is retrofit2.HttpException ->
                errorMsg
            BuildConfig.DEBUG
                    || errorCode == ERROR_CODE_NO_NETWORK
                    || errorCode == ERROR_CODE_UNKNOWN_HOST
                    || errorCode == ERROR_CODE_CERTIFICATE_INVALID
                    || errorCode == ERROR_CODE_CONNECT_ERROR
                    || errorCode == ERROR_CODE_CONNECT_TIMEOUT
                    || errorCode == ERROR_CODE_SOCKET_TIMEOUT ->
                "${errorMsg}${getErrorCodeStr()}"
            !failedDesc.isNullOrBlank() ->
                "${failedDesc}${getErrorCodeStr()}"
            loadAction == true ->
                "${getString(R.string.common_load_fail)}${getErrorCodeStr()}"
            else ->
                "${getString(R.string.common_operation_fail)}${getErrorCodeStr()}"
        }
    }

    private fun getErrorCodeStr(): String {
        return if (errorCode != null) "($errorCode)" else ""
    }
}