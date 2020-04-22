package org.palliums.libracore.wallet

import org.palliums.libracore.crypto.KeyFactory
import org.palliums.libracore.crypto.KeyPair
import org.palliums.libracore.crypto.Seed
import org.palliums.libracore.mnemonic.English
import org.palliums.libracore.mnemonic.Mnemonic
import org.palliums.libracore.mnemonic.WordCount

/**
 * Created by elephant on 2019-09-20 11:35.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

class WalletConfig(
    var mnemonic: List<String>? = null,
    var salt: String? = null
)

class LibraWallet {

    companion object {
        private const val MNEMONIC_SALT_DEFAULT = "LIBRA"

        fun generateMnemonic(words: WordCount = WordCount.TWELVE): ArrayList<String> {
            return Mnemonic(English.INSTANCE).generate(words)
        }
    }

    val config: WalletConfig
    private var lastChild = 0L
    private val keyFactory: KeyFactory
    private val accounts = LinkedHashMap<String, Account>()

    constructor(config: WalletConfig? = null) {
        this.config = config ?: WalletConfig()

        val mnemonic: List<String> = this.config.mnemonic ?: generateMnemonic()
        this.config.mnemonic = mnemonic

        val salt: String = this.config.salt ?: MNEMONIC_SALT_DEFAULT
        this.config.salt = salt

        val seed: Seed = Seed.fromMnemonic(mnemonic, salt)
        this.keyFactory = KeyFactory(seed)
    }

    fun newAccount(): Account {
        val newAccount: Account = this.generateAccount(this.lastChild)
        this.lastChild++
        return newAccount
    }

    fun generateAccount(depth: Long): Account {
        val keyPair: KeyPair = this.keyFactory.generateKey(depth)
        val account = Account(keyPair)
        this.addAccount(account)
        return account
    }

    fun addAccount(account: Account) {
        this.accounts[account.getAddress().toHex()] = account
    }

    fun getAccount(address: String): Account? {
        return accounts[address]
    }
}