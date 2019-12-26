package com.palliums.utils


class PasswordEmptyException : RuntimeException()

// 密码太短
class PasswordLengthShortException : RuntimeException()

// 密码太长
class PasswordLengthLongException : RuntimeException()

// 验证失败
open class PasswordValidationFailsException : RuntimeException()

// 存在特殊字符
class PasswordSpecialFailsException : PasswordValidationFailsException()

object PasswordCheckUtil {
    /**
     * @param text 需要检测的文本
     * @param specialFails 是否允许特殊字符
     */
    @Throws(
        PasswordLengthShortException::class,
        PasswordLengthLongException::class,
        PasswordSpecialFailsException::class,
        PasswordValidationFailsException::class
    )
    fun check(password: String?, specialFails: Boolean = true): Boolean {
        if(password.isNullOrEmpty()){
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
