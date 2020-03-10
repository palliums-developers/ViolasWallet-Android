package com.violas.wallet.utils

import com.palliums.base.BaseActivity
import com.palliums.base.BaseFragment
import com.violas.wallet.R
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.widget.dialog.PasswordInputDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.palliums.libracore.wallet.KeyPair
import org.palliums.violascore.wallet.Account

/**
 * Created by elephant on 2020/3/10 16:07.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

fun BaseFragment.showPwdInputDialog(
    accountDO: AccountDO,
    retryWhenPwdInputError: Boolean = true,
    showLoadingWhenVerifyPwd: Boolean = true,
    accountCallback: ((account: Account) -> Unit)? = null,
    mnemonicCallback: ((mnemonics: List<String>) -> Unit)? = null,
    accountMnemonicCallback: ((account: Account, mnemonics: List<String>) -> Unit)? = null
) {
    require(
        accountCallback != null
                || mnemonicCallback != null
                || accountMnemonicCallback != null
    ) {
        "accountCallback, mnemonicCallback, accountMnemonicCallback cannot all be null"
    }

    PasswordInputDialog()
        .setConfirmListener { pwdBytes, pwdDialog ->
            pwdDialog.dismiss()

            launch(Dispatchers.Main) {
                if (showLoadingWhenVerifyPwd) {
                    showProgress()
                }

                var account: Account? = null
                var mnemonics: List<String>? = null
                withContext(Dispatchers.IO) {
                    val simpleSecurity =
                        SimpleSecurity.instance(requireContext().applicationContext)

                    when {
                        accountCallback != null -> {
                            val privateKeyBytes = simpleSecurity.decrypt(
                                pwdBytes, accountDO.privateKey
                            )

                            if (privateKeyBytes != null) {
                                account = Account(KeyPair.fromSecretKey(privateKeyBytes))
                            } else {
                                delay(500)
                            }
                        }

                        mnemonicCallback != null -> {
                            val mnemonicBytes = simpleSecurity.decrypt(
                                pwdBytes, accountDO.mnemonic
                            )

                            if (mnemonicBytes != null) {
                                val mnemonicStr = String(mnemonicBytes)
                                mnemonics = mnemonicStr.substring(1, mnemonicStr.length - 1)
                                    .split(",")
                                    .map { it.trim() }
                            } else {
                                delay(500)
                            }
                        }

                        accountMnemonicCallback != null -> {
                            val privateKeyBytes = simpleSecurity.decrypt(
                                pwdBytes, accountDO.privateKey
                            )
                            val mnemonicBytes = simpleSecurity.decrypt(
                                pwdBytes, accountDO.mnemonic
                            )

                            if (privateKeyBytes != null && mnemonicBytes != null) {
                                account = Account(KeyPair.fromSecretKey(privateKeyBytes))

                                val mnemonicStr = String(mnemonicBytes)
                                mnemonics = mnemonicStr.substring(1, mnemonicStr.length - 1)
                                    .split(",")
                                    .map { it.trim() }
                            } else {
                                delay(500)
                            }
                        }
                    }
                }

                if (accountCallback != null
                    && account != null
                ) {
                    accountCallback.invoke(account!!)
                } else if (mnemonicCallback != null
                    && !mnemonics.isNullOrEmpty()
                ) {
                    mnemonicCallback.invoke(mnemonics!!)
                } else if (accountMnemonicCallback != null
                    && account != null
                    && !mnemonics.isNullOrEmpty()
                ) {
                    accountMnemonicCallback.invoke(account!!, mnemonics!!)
                } else {
                    if (showLoadingWhenVerifyPwd) {
                        dismissProgress()
                    }

                    if (retryWhenPwdInputError) {
                        showPwdInputDialog(
                            accountDO,
                            retryWhenPwdInputError,
                            showLoadingWhenVerifyPwd,
                            accountCallback,
                            mnemonicCallback,
                            accountMnemonicCallback
                        )
                    }

                    showToast(getString(R.string.hint_password_error))
                }
            }
        }
        .show(childFragmentManager)
}

fun BaseActivity.showPwdInputDialog(
    accountDO: AccountDO,
    retryWhenPwdInputError: Boolean = true,
    showLoadingWhenVerifyPwd: Boolean = true,
    accountCallback: ((account: Account) -> Unit)? = null,
    mnemonicCallback: ((mnemonics: List<String>) -> Unit)? = null,
    accountMnemonicCallback: ((account: Account, mnemonics: List<String>) -> Unit)? = null
) {
    require(
        accountCallback != null
                || mnemonicCallback != null
                || accountMnemonicCallback != null
    ) {
        "accountCallback, mnemonicCallback, accountMnemonicCallback cannot all be null"
    }

    PasswordInputDialog()
        .setConfirmListener { pwdBytes, pwdDialog ->
            pwdDialog.dismiss()

            launch(Dispatchers.Main) {
                if (showLoadingWhenVerifyPwd) {
                    showProgress()
                }

                var account: Account? = null
                var mnemonics: List<String>? = null
                withContext(Dispatchers.IO) {
                    val simpleSecurity =
                        SimpleSecurity.instance(applicationContext)

                    when {
                        accountCallback != null -> {
                            val privateKeyBytes = simpleSecurity.decrypt(
                                pwdBytes, accountDO.privateKey
                            )

                            if (privateKeyBytes != null) {
                                account = Account(KeyPair.fromSecretKey(privateKeyBytes))
                            } else {
                                delay(500)
                            }
                        }

                        mnemonicCallback != null -> {
                            val mnemonicBytes = simpleSecurity.decrypt(
                                pwdBytes, accountDO.mnemonic
                            )

                            if (mnemonicBytes != null) {
                                val mnemonicStr = String(mnemonicBytes)
                                mnemonics = mnemonicStr.substring(1, mnemonicStr.length - 1)
                                    .split(",")
                                    .map { it.trim() }
                            } else {
                                delay(500)
                            }
                        }

                        accountMnemonicCallback != null -> {
                            val privateKeyBytes = simpleSecurity.decrypt(
                                pwdBytes, accountDO.privateKey
                            )
                            val mnemonicBytes = simpleSecurity.decrypt(
                                pwdBytes, accountDO.mnemonic
                            )

                            if (privateKeyBytes != null && mnemonicBytes != null) {
                                account = Account(KeyPair.fromSecretKey(privateKeyBytes))

                                val mnemonicStr = String(mnemonicBytes)
                                mnemonics = mnemonicStr.substring(1, mnemonicStr.length - 1)
                                    .split(",")
                                    .map { it.trim() }
                            } else {
                                delay(500)
                            }
                        }
                    }
                }

                if (accountCallback != null
                    && account != null
                ) {
                    accountCallback.invoke(account!!)
                } else if (mnemonicCallback != null
                    && !mnemonics.isNullOrEmpty()
                ) {
                    mnemonicCallback.invoke(mnemonics!!)
                } else if (accountMnemonicCallback != null
                    && account != null
                    && !mnemonics.isNullOrEmpty()
                ) {
                    accountMnemonicCallback.invoke(account!!, mnemonics!!)
                } else {
                    if (showLoadingWhenVerifyPwd) {
                        dismissProgress()
                    }

                    if (retryWhenPwdInputError) {
                        showPwdInputDialog(
                            accountDO,
                            retryWhenPwdInputError,
                            showLoadingWhenVerifyPwd,
                            accountCallback,
                            mnemonicCallback,
                            accountMnemonicCallback
                        )
                    }

                    showToast(getString(R.string.hint_password_error))
                }
            }
        }
        .show(supportFragmentManager)
}