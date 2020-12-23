package com.violas.wallet.biz.bean

import org.palliums.libracore.common.CURRENCY_DEFAULT_CODE
import org.palliums.libracore.transaction.AccountAddress

/**
 * Libra 标准 Token
 */
data class LibraToken(
    var model: String = CURRENCY_DEFAULT_CODE,
    var name: String = CURRENCY_DEFAULT_CODE,
    var address: AccountAddress = AccountAddress.DEFAULT
) {
    fun equals(token: LibraToken): Boolean {
        return token.model == model && token.name == name && token.address == address
    }
}
