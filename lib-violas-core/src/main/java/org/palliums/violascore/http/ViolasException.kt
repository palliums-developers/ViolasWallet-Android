package org.palliums.violascore.http

import java.lang.RuntimeException

abstract class ViolasException : RuntimeException() {
    /**
     * 接受转账的账户没有激活
     */
    class ReceiveAccountNoActivation() : ViolasException()

    /**
     * 账户没有激活
     */
    class AccountNoActivation() : ViolasException()

    /**
     * 提供的私钥无法控制账户
     */
    class AccountNoControl() : ViolasException()

    /**
     * 未知错误
     */
    class UnknownException : ViolasException()

    /**
     * 脚本验证错误
     */
    class ScriptValidationFailedException : ViolasException()

    /**
     * SequenceNumber 已经失效
     */
    class SequenceNumberOldException : ViolasException()

    /**
     * 账户余额不足
     */
    class LackOfBalanceException : ViolasException()

    /**
     * 节点反应失败
     */
    class NodeResponseException : ViolasException()

    /**
     * 币种不存在
     */
    class CurrencyNotExistException : ViolasException()
}

