package com.palliums.utils

// 密码太短
class PasswordLengthShortException : RuntimeException()

// 密码太长
class PasswordLengthLongException : RuntimeException()

// 验证失败
open class PasswordValidationFailsException : RuntimeException()

// 存在特殊字符
class PasswordSpecialFailsException : PasswordValidationFailsException()

object PasswordCheckUtil {
    @Throws(
        PasswordLengthShortException::class,
        PasswordLengthLongException::class,
        PasswordSpecialFailsException::class,
        PasswordValidationFailsException::class
    )
    fun check(text: String?): Boolean {
        val password = text?.trim() ?: ""
        if (password?.trim()?.length ?: 0 < 8) {
            throw PasswordLengthShortException()
        }
        if (password?.length ?: 0 > 20) {
            throw PasswordLengthLongException()
        }
        var existsUppercase = false
        var existsLowercase = false
        var existsNumber = false
        var existsIllegal = false
        password?.forEach {
            when {
                it.isUpperCase() -> existsUppercase = true
                it.isLowerCase() -> existsLowercase = true
                it.isDigit() -> existsNumber = true
                else -> existsIllegal = true
            }
            if (existsIllegal) {
                return@forEach
            }
            if (existsUppercase && existsLowercase && existsNumber) {
                return@forEach
            }
        }
        if (existsIllegal) {
            throw PasswordSpecialFailsException()
        }
        if (existsUppercase && existsLowercase && existsNumber) {
            return true
        }
        throw PasswordValidationFailsException()
    }

}
