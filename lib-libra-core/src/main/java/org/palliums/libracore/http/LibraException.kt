package org.palliums.libracore.http

import java.lang.RuntimeException

abstract class LibraException : RuntimeException() {
    /**
     * 接受转账的账户没有激活
     */
    class ReceiveAccountNoActivation() : LibraException()

    /**
     * 账户没有激活
     */
    class AccountNoActivation() : LibraException()

    /**
     * 提供的私钥无法控制账户
     */
    class AccountNoControl() : LibraException()

    /**
     * 未知错误
     */
    class UnknownException : LibraException()

    /**
     * 脚本验证错误
     */
    class ScriptValidationFailedException : LibraException()

    /**
     * SequenceNumber 已经失效
     */
    class SequenceNumberOldException : LibraException()

    /**
     * 账户余额不足
     */
    class LackOfBalanceException : LibraException()

    /**
     * 节点反应失败
     */
    class NodeResponseException : LibraException()

    /**
     * 币种不存在
     */
    class CurrencyNotExistException : LibraException()
}

