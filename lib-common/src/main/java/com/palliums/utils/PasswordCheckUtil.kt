package com.palliums.utils

import com.palliums.R
import com.palliums.content.ContextProvider


class PasswordEmptyException : RuntimeException(getString(R.string.hint_please_password_not_empty))

// 密码太短
class PasswordLengthShortException :
    RuntimeException(getString(R.string.hint_please_minimum_password_length))

// 密码太长
class PasswordLengthLongException :
    RuntimeException(getString(R.string.hint_please_maxmum_password_length))

// 验证失败
open class PasswordValidationFailsException(msg: String = getString(R.string.hint_please_cannot_contain_special_characters)) :
    RuntimeException(msg)

// 存在特殊字符
class PasswordSpecialFailsException :
    PasswordValidationFailsException(getString(R.string.hint_please_cannot_contain_special_characters))

object PasswordCheckUtil {
    /**
     * @param password 需要检测的文本
     * @param specialFails 是否允许特殊字符
     */
    @Throws(
        PasswordEmptyException::class,
        PasswordLengthShortException::class,
        PasswordLengthLongException::class,
        PasswordSpecialFailsException::class,
        PasswordValidationFailsException::class
    )
    fun check(password: String?, specialFails: Boolean = true): Boolean {
        if (password.isNullOrEmpty()) {
            throw PasswordEmptyException()
        }
        if (password.trim().length < 8) {
            throw PasswordLengthShortException()
        }
        if (password.length > 20) {
            throw PasswordLengthLongException()
        }
        var existsUppercase = false
        var existsLowercase = false
        var existsNumber = false
        var existsIllegal = false
        password.forEach {
            when {
                it.isUpperCase() -> existsUppercase = true
                it.isLowerCase() -> existsLowercase = true
                it.isDigit() -> existsNumber = true
                else -> existsIllegal = true
            }
            if (!specialFails && existsIllegal) {
                return@forEach
            }
            if (existsUppercase && existsLowercase && existsNumber) {
                return@forEach
            }
        }
        if (!specialFails && existsIllegal) {
            throw PasswordSpecialFailsException()
        }
        if (existsUppercase && existsLowercase && existsNumber) {
            return true
        }
        throw PasswordValidationFailsException()
    }

}
