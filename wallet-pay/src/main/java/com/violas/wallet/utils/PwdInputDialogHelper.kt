package com.violas.wallet.utils

import com.palliums.base.BaseActivity
import com.palliums.base.BaseFragment
import com.palliums.utils.*
import com.violas.wallet.R
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.widget.dialog.PasswordInputDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.palliums.violascore.crypto.KeyPair
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
    passwordCallback: ((password: String) -> Unit)? = null,
    accountCallback: ((account: Account) -> Unit)? = null,
    mnemonicCallback: ((mnemonics: List<String>) -> Unit)? = null,
    accountMnemonicCallback: ((account: Account, mnemonics: List<String>) -> Unit)? = null
) {
    require(
        passwordCallback != null
                || accountCallback != null
                || mnemonicCallback != null
                || accountMnemonicCallback != null
    ) {
        "passwordCallback, accountCallback, mnemonicCallback, accountMnemonicCallback cannot all be null"
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
                        passwordCallback != null || accountCallback != null -> {
                            val privateKeyBytes = simpleSecurity.decrypt(
                                pwdBytes, accountDO.privateKey
                            )

                            if (privateKeyBytes != null) {
                                account = Account(KeyPair.fromSecretKey(privateKeyBytes))
                            } else {
                                if (showLoadingWhenVerifyPwd) {
                                    delay(500)
                                }
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
                                if (showLoadingWhenVerifyPwd) {
                                    delay(500)
                                }
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
                                if (showLoadingWhenVerifyPwd) {
                                    delay(500)
                                }
                            }
                        }
                    }
                }

                if (passwordCallback != null
                    && account != null
                ) {
                    passwordCallback.invoke(String(pwdBytes))
                } else if (accountCallback != null
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
                            passwordCallback,
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
    cancelCallback: (() -> Unit)? = null,
    passwordCallback: ((password: String) -> Unit)? = null,
    accountCallback: ((account: Account) -> Unit)? = null,
    mnemonicCallback: ((mnemonics: List<String>) -> Unit)? = null,
    accountMnemonicCallback: ((account: Account, mnemonics: List<String>) -> Unit)? = null
) {
    require(
        passwordCallback != null
                || accountCallback != null
                || mnemonicCallback != null
                || accountMnemonicCallback != null
    ) {
        "passwordCallback, accountCallback, mnemonicCallback, accountMnemonicCallback cannot all be null"
    }

    PasswordInputDialog()
        .setCancelListener {
            cancelCallback?.invoke()
        }
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
                        passwordCallback != null || accountCallback != null -> {
                            val privateKeyBytes = simpleSecurity.decrypt(
                                pwdBytes, accountDO.privateKey
                            )

                            if (privateKeyBytes != null) {
                                account = Account(KeyPair.fromSecretKey(privateKeyBytes))
                            } else {
                                if (showLoadingWhenVerifyPwd) {
                                    delay(500)
                                }
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
                                if (showLoadingWhenVerifyPwd) {
                                    delay(500)
                                }
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
                                if (showLoadingWhenVerifyPwd) {
                                    delay(500)
                                }
                            }
                        }
                    }
                }

                if (passwordCallback != null
                    && account != null
                ) {
                    passwordCallback.invoke(String(pwdBytes))
                } else if (accountCallback != null
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
                            cancelCallback,
                            passwordCallback,
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

fun BaseActivity.decryptAccount(
    accountDO: AccountDO,
    pwd: String,
    showLoadingWhenVerifyPwd: Boolean = true,
    pwdErrorCallback: (() -> Unit)? = null,
    accountCallback: (account: Account) -> Unit
) {
    if (pwd.isEmpty()) {
        showToast(getString(R.string.hint_please_input_password))
        return
    }

    try {
        PasswordCheckUtil.check(pwd)
    } catch (e: Throwable) {
        showToast(
            when (e) {
                is PasswordLengthShortException ->
                    R.string.hint_please_minimum_password_length
                is PasswordLengthLongException ->
                    R.string.hint_please_maxmum_password_length
                is PasswordSpecialFailsException ->
                    R.string.hint_please_cannot_contain_special_characters
                is PasswordValidationFailsException ->
                    R.string.hint_please_password_rules_are_wrong
                is PasswordEmptyException ->
                    R.string.hint_please_password_not_empty
                else ->
                    R.string.hint_please_password_not_empty
            }
        )

        pwdErrorCallback?.invoke()
        return
    }

    launch(Dispatchers.Main) {
        if (showLoadingWhenVerifyPwd) {
            showProgress()
        }

        val account: Account? = withContext(Dispatchers.IO) {
            val simpleSecurity =
                SimpleSecurity.instance(applicationContext)

            val privateKeyBytes = simpleSecurity.decrypt(
                pwd.toByteArray(), accountDO.privateKey
            )

            if (privateKeyBytes != null) {
                return@withContext Account(KeyPair.fromSecretKey(privateKeyBytes))
            }

            if (showLoadingWhenVerifyPwd) {
                delay(500)
            }
            return@withContext null
        }

        if (account != null) {
            accountCallback.invoke(account)
        } else {
            if (showLoadingWhenVerifyPwd) {
                dismissProgress()
            }

            pwdErrorCallback?.invoke()
            showToast(getString(R.string.hint_password_error))
        }
    }
}