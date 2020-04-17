package com.palliums.violas.error

import com.palliums.violas.http.Response
import java.lang.RuntimeException

open class TransactionException : RuntimeException {
    class UnknownException : TransactionException()
    class ScriptValidationFailedException : TransactionException()
    class SequenceNumberOldException : TransactionException()
    class BalanceInsufficientException : TransactionException()

    constructor() : super()
    constructor(t: Throwable) : super(t)

    companion object {
        fun <T> checkViolasTransactionException(t: Response<T>): T? {
            if (t.getErrorCode() == t.getSuccessCode()) {
                return t.data
            } else {
                //todo chain error
                when(t.errorMsg){

                }
                throw TransactionException()
            }
        }
    }
}