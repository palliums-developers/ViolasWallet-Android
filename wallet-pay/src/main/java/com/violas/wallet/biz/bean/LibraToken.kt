package com.violas.wallet.biz.bean

import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import org.palliums.libracore.transaction.AccountAddress

/**
 * Libra 标准 Token
 */
data class LibraToken(
    var model: String = "LBR",
    var name: String = "T",
    var address: AccountAddress = AccountAddress.DEFAULT
) {
    fun equals(token: LibraToken): Boolean {
        return token.model == model && token.name == name && token.address == address
    }
}
