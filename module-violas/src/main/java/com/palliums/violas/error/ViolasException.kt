package com.palliums.violas.error

import com.palliums.exceptions.BaseException
import com.palliums.utils.getString
import com.palliums.violas.BuildConfig
import com.palliums.violas.R
import com.palliums.violas.http.Response

abstract class ViolasException(
    val errorCode: String,
    val errorMsg: String?
) : RuntimeException(), BaseException {

    companion object {

        fun <T> checkViolasTransactionException(response: Response<T>) {
            if (response.errorMsg.isNullOrEmpty()) {
                throw UnknownException(response.errorCode, response.errorMsg)
            }

            when {
                response.errorMsg!!.contains("Grpc call failed") -> {
                    throw NodeResponseException()
                }

                // Script 有 TypeTag 时 1076，{"code":4000,"message":"Node runtime error: NUMBER_OF_TYPE_ARGUMENTS_MISMATCH"}
                // Script 无 TypeTag，token id 不正确时 4016，{"code":4000,"message":"Node runtime error: ABORTED"}
                // 暂时无法判断当前账户是否有某 token id 的铸币权
                /*response.errorMsg!!.contains("NUMBER_OF_TYPE_ARGUMENTS_MISMATCH") -> {
                    throw AccountNoMintableException()
                }*/

                // todo chain other error
                else -> {
                    throw UnknownException(response.errorCode, response.errorMsg)
                }
            }
        }
    }

    override val message: String?
        get() = super.message

    override fun getErrorMessage(loadAction: Boolean): String {
        if (this !is UnknownException && !errorMsg.isNullOrEmpty()) {
            return errorMsg
        }

        if (loadAction) {
            return if (BuildConfig.DEBUG)
                "${getString(R.string.common_load_fail)}($errorCode)\n${errorMsg
                    ?: "Unknown reason"}"
            else
                "${getString(R.string.common_load_fail)}($errorCode)"
        }

        return if (BuildConfig.DEBUG)
            "${getString(R.string.common_operation_fail)}($errorCode)\n${errorMsg
                ?: "Unknown reason"}"
        else
            "${getString(R.string.common_operation_fail)}($errorCode)"
    }

    /**
     * 未知错误
     */
    class UnknownException(errorCode: Int, errorMsg: String?) :
        ViolasException(errorCode.toString(), errorMsg)

    /**
     * 账户没有激活
     */
    class AccountNoActivation :
        ViolasException("V100", getString(R.string.exception_account_no_activation))

    /**
     * 提供的私钥无法控制账户
     */
    class AccountNoControl :
        ViolasException("V101", getString(R.string.exception_account_no_control))

    /**
     * 接受转账的账户没有激活
     */
    class ReceiveAccountNoActivation :
        ViolasException("V102", getString(R.string.exception_receive_account_no_activation))

    /**
     * 脚本验证错误
     */
    class ScriptValidationFailedException :
        ViolasException("V103", getString(R.string.exception_script_validation_failed))

    /**
     * SequenceNumber 已经失效
     */
    class SequenceNumberOldException :
        ViolasException("V104", getString(R.string.exception_sequence_number_old))

    /**
     * 账户余额不足
     */
    class LackOfBalanceException :
        ViolasException("V105", getString(R.string.exception_lack_of_balance))

    /**
     * 节点反应失败
     */
    class NodeResponseException :
        ViolasException("V106", getString(R.string.exception_node_response))

    /**
     * 账户没有铸币权
     */
    class AccountNoMintableException :
        ViolasException("V107", getString(R.string.exception_account_no_mintable))
}

