package com.violas.wallet.repository.http

import com.google.gson.JsonParseException
import com.violas.wallet.R
import com.violas.wallet.getString
import com.violas.wallet.utils.isNetworkConnected
import org.apache.http.conn.ConnectTimeoutException
import org.json.JSONException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.*
import javax.net.ssl.SSLPeerUnverifiedException

/**
 * Created by elephant on 2019-08-01 10:22.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 网络异常，统一处理网络请求出现的exception、error code
 */
class HttpException : RuntimeException {

    companion object {
        /** 连接超时 */
        private const val ERROR_CODE_CONNECT_TIMEOUT = -101
        /** Socket超时 */
        private const val ERROR_CODE_SOCKET_TIMEOUT = -102
        /** 服务器证书无效 */
        private const val ERROR_CODE_CERTIFICATE_INVALID = -103
        /** 未知主机（可能是因为无网络） */
        private const val ERROR_CODE_UNKNOWN_HOST = -104
        /** 连接异常（可能是主机拒绝了连接） */
        private const val ERROR_CODE_CONNECT_EXCEPTION = -105
        /** 数据异常 */
        private const val ERROR_CODE_DATA_EXCEPTION = -106
        /** 没有网络 */
        const val ERROR_CODE_NO_NETWORK = -107
        /** 未知错误 */
        const val ERROR_CODE_UNKNOWN_ERROR = -100

        fun networkUnavailable(): HttpException {
            return HttpException(
                ERROR_CODE_NO_NETWORK,
                getString(R.string.http_network_unavailable)
            )
        }

        fun responseDataException(): HttpException {
            return HttpException(
                ERROR_CODE_DATA_EXCEPTION,
                getString(R.string.http_data_exception)
            )
        }
    }

    /**
     * 错误码
     * 正数表示一次http的状态码或错误码，如：404，500，502，服务器返回的非200...
     * 负数表示一个exception的错误码，如：[ERROR_CODE_CERTIFICATE_INVALID]...
     */
    val errorCode: Any
    val errorMsg: String

    constructor(exception: Throwable) : super(exception) {
        if (!isNetworkConnected()) {
            errorCode = ERROR_CODE_NO_NETWORK
            errorMsg = getString(R.string.http_network_unavailable)
            return
        }

        when (exception) {
            is retrofit2.HttpException -> {
                errorCode = exception.code()
                errorMsg = String.format(
                    Locale.ENGLISH,
                    "HTTP %s %s",
                    exception.code(),
                    exception.message()
                )
            }
            is ConnectTimeoutException -> {
                // 连接超时，此时并未发送数据
                errorCode = ERROR_CODE_CONNECT_TIMEOUT
                errorMsg = getString(R.string.http_connect_timeout)
            }
            is SocketTimeoutException -> {
                // Socket超时，此时数据已发送但服务器未应答
                errorCode = ERROR_CODE_SOCKET_TIMEOUT
                errorMsg = getString(R.string.http_socket_timeout)
            }
            is SSLPeerUnverifiedException -> {
                // 证书错误
                errorCode = ERROR_CODE_CERTIFICATE_INVALID
                errorMsg = getString(R.string.http_certificate_invalid)
            }
            is UnknownHostException -> {
                errorCode = ERROR_CODE_UNKNOWN_HOST
                errorMsg = getString(R.string.http_unknown_host)
            }
            is ConnectException -> {
                errorCode = ERROR_CODE_CONNECT_EXCEPTION
                errorMsg = getString(R.string.http_connect_exception)
            }
            is JSONException, is JsonParseException -> {
                errorCode = ERROR_CODE_DATA_EXCEPTION
                errorMsg = getString(R.string.http_data_exception)
            }
            else -> {
                // 未知错误
                errorCode = ERROR_CODE_UNKNOWN_ERROR
                errorMsg = String.format(
                    Locale.ENGLISH, "%s, %s",
                    getString(R.string.http_unknown_error),
                    exception.message
                )
            }
        }
    }

    constructor(response: ApiResponse) : super() {

        val msg: String = if (response.getErrorMsg() == null) {
            getString(R.string.http_request_fail)
        } else {
            response.getErrorMsg().toString()
        }

        errorCode = response.getErrorCode()
        errorMsg = String.format(Locale.ENGLISH, "%s", msg)
    }

    private constructor(errorCode: Int, errorMsg: String) {
        this.errorCode = errorCode
        this.errorMsg = errorMsg
    }

    override val message: String?
        get() = errorMsg
}