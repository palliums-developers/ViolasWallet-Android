package com.violas.wallet.utils

import androidx.biometric.BiometricManager
import androidx.fragment.app.Fragment
import com.palliums.base.BaseActivity
import com.palliums.base.BaseFragment
import com.palliums.base.ViewController
import com.palliums.biometric.BiometricCompat
import com.palliums.utils.*
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.ui.biometric.CustomFingerprintDialog
import com.violas.wallet.widget.dialog.PasswordInputDialog
import kotlinx.coroutines.*
import org.palliums.violascore.wallet.Account

/**
 * Created by elephant on 2020/3/10 16:07.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

/**
 * 验证账户，开启生物验证且生物识别可用时优先使用生物特征验证，反之则输入密码进行验证
 *
 * @param useFingerprintWhenBiometric 生物验证时使用指纹
 * @see BaseActivity.authenticateAccountByBiometric
 * @see BaseActivity.authenticateAccountByPassword
 */
fun BaseActivity.authenticateAccount(
    accountDO: AccountDO,
    retryWhenPwdInputError: Boolean = true,
    showLoadingWhenDecryptStart: Boolean = true,
    dismissLoadingWhenDecryptEnd: Boolean = false,
    useFingerprintWhenBiometric: Boolean = true,
    cancelCallback: (() -> Unit)? = null,
    passwordCallback: ((password: String) -> Unit)? = null,
    mnemonicCallback: ((mnemonics: List<String>) -> Unit)? = null,
    privateKeyCallback: ((privateKey: ByteArray) -> Unit)? = null
) {
    authenticateAccountByBiometric(
        accountDO = accountDO,
        showLoadingWhenDecryptStart = showLoadingWhenDecryptStart,
        dismissLoadingWhenDecryptEnd = dismissLoadingWhenDecryptEnd,
        useFingerprint = useFingerprintWhenBiometric,
        biometricErrorCallback = {
            authenticateAccountByPassword(
                accountDO = accountDO,
                retryWhenPwdInputError = retryWhenPwdInputError,
                showLoadingWhenDecryptStart = showLoadingWhenDecryptStart,
                dismissLoadingWhenDecryptEnd = dismissLoadingWhenDecryptEnd,
                cancelCallback = cancelCallback,
                passwordCallback = passwordCallback,
                mnemonicCallback = mnemonicCallback,
                privateKeyCallback = privateKeyCallback
            )
        },
        cancelCallback = cancelCallback,
        passwordCallback = passwordCallback,
        mnemonicCallback = mnemonicCallback,
        privateKeyCallback = privateKeyCallback
    )
}

/**
 * 验证账户，开启生物验证且生物识别可用时优先使用生物特征验证，反之则输入密码进行验证
 *
 * @param useFingerprintWhenBiometric 生物验证时使用指纹
 * @see Fragment.authenticateAccountByBiometric
 * @see Fragment.authenticateAccountByPassword
 */
fun Fragment.authenticateAccount(
    accountDO: AccountDO,
    retryWhenPwdInputError: Boolean = true,
    showLoadingWhenDecryptStart: Boolean = true,
    dismissLoadingWhenDecryptEnd: Boolean = false,
    useFingerprintWhenBiometric: Boolean = true,
    cancelCallback: (() -> Unit)? = null,
    passwordCallback: ((password: String) -> Unit)? = null,
    mnemonicCallback: ((mnemonics: List<String>) -> Unit)? = null,
    privateKeyCallback: ((privateKey: ByteArray) -> Unit)? = null
) {
    authenticateAccountByBiometric(
        accountDO = accountDO,
        showLoadingWhenDecryptStart = showLoadingWhenDecryptStart,
        dismissLoadingWhenDecryptEnd = dismissLoadingWhenDecryptEnd,
        useFingerprint = useFingerprintWhenBiometric,
        biometricErrorCallback = {
            authenticateAccountByPassword(
                accountDO = accountDO,
                retryWhenPwdInputError = retryWhenPwdInputError,
                showLoadingWhenDecryptStart = showLoadingWhenDecryptStart,
                dismissLoadingWhenDecryptEnd = dismissLoadingWhenDecryptEnd,
                cancelCallback = cancelCallback,
                passwordCallback = passwordCallback,
                mnemonicCallback = mnemonicCallback,
                privateKeyCallback = privateKeyCallback
            )
        },
        cancelCallback = cancelCallback,
        passwordCallback = passwordCallback,
        mnemonicCallback = mnemonicCallback,
        privateKeyCallback = privateKeyCallback
    )
}

/**
 * 通过生物特征验证账号
 *
 * @param useFingerprint            使用指纹进行生物识别
 * @param cancelCallback            取消回调
 * @param biometricErrorCallback    生物识别 error 回调
 * @see BaseFragment.decryptAccount
 */
fun BaseActivity.authenticateAccountByBiometric(
    accountDO: AccountDO,
    showLoadingWhenDecryptStart: Boolean = true,
    dismissLoadingWhenDecryptEnd: Boolean = false,
    useFingerprint: Boolean = true,
    biometricErrorCallback: (() -> Unit)? = null,
    cancelCallback: (() -> Unit)? = null,
    passwordCallback: ((password: String) -> Unit)? = null,
    mnemonicCallback: ((mnemonics: List<String>) -> Unit)? = null,
    privateKeyCallback: ((privateKey: ByteArray) -> Unit)? = null
) {
    val securityPassword = AccountManager.getSecurityPassword()
    if (securityPassword.isNullOrBlank()) {
        // 没有开启生物验证功能
        biometricErrorCallback?.invoke()
        return
    }

    val biometricCompat = BiometricCompat.Builder(this).build()
    if (biometricCompat.canAuthenticate(useFingerprint) != BiometricManager.BIOMETRIC_SUCCESS) {
        // 生物识别不可用
        biometricErrorCallback?.invoke()
        return
    }

    val promptParams =
        BiometricCompat.PromptParams.Builder(this)
            .title(getString(R.string.auth_touch_id_title))
            .negativeButtonText(getString(R.string.common_action_cancel_space))
            .positiveButtonText(getString(R.string.auth_touch_id_action_use_password))
            .customFingerprintDialogClass(CustomFingerprintDialog::class.java)
            .useFingerprint(useFingerprint)
            .build()

    val key = AccountManager.getBiometricKey()
    biometricCompat.decrypt(promptParams, key, securityPassword) { result ->
        if (result.type == BiometricCompat.Type.INFO) return@decrypt

        if (result.type == BiometricCompat.Type.SUCCESS) {
            decryptAccount(
                accountDO = accountDO,
                password = result.value,
                showLoadingWhenDecryptStart = showLoadingWhenDecryptStart,
                dismissLoadingWhenDecryptEnd = dismissLoadingWhenDecryptEnd,
                passwordFromUserInput = false,
                passwordErrorCallback = {
                    // 加密的密码异常
                    biometricErrorCallback?.invoke()
                },
                passwordCallback = passwordCallback,
                mnemonicCallback = mnemonicCallback,
                privateKeyCallback = privateKeyCallback
            )
            return@decrypt
        }

        // error case
        if (result.reason == BiometricCompat.Reason.USER_CANCELED
            || result.reason == BiometricCompat.Reason.NEGATIVE_BUTTON
        ) {
            // 用户取消操作
            cancelCallback?.invoke()
            return@decrypt
        }

        // 其它error原因
        biometricErrorCallback?.invoke()
    }
}

/**
 * 通过生物特征验证账号
 *
 * @param useFingerprint            使用指纹进行生物识别
 * @param cancelCallback            取消回调
 * @param biometricErrorCallback    生物识别 error 回调
 * @see Fragment.decryptAccount
 */
fun Fragment.authenticateAccountByBiometric(
    accountDO: AccountDO,
    showLoadingWhenDecryptStart: Boolean = true,
    dismissLoadingWhenDecryptEnd: Boolean = false,
    useFingerprint: Boolean = true,
    biometricErrorCallback: (() -> Unit)? = null,
    cancelCallback: (() -> Unit)? = null,
    passwordCallback: ((password: String) -> Unit)? = null,
    mnemonicCallback: ((mnemonics: List<String>) -> Unit)? = null,
    privateKeyCallback: ((privateKey: ByteArray) -> Unit)? = null
) {
    val securityPassword = AccountManager.getSecurityPassword()
    if (securityPassword.isNullOrBlank()) {
        // 没有开启生物验证功能
        biometricErrorCallback?.invoke()
        return
    }

    val biometricCompat = BiometricCompat.Builder(requireContext()).build()
    if (biometricCompat.canAuthenticate(useFingerprint) != BiometricManager.BIOMETRIC_SUCCESS) {
        // 生物识别不可用
        biometricErrorCallback?.invoke()
        return
    }

    val promptParams =
        BiometricCompat.PromptParams.Builder(this)
            .title(getString(R.string.auth_touch_id_title))
            .negativeButtonText(getString(R.string.common_action_cancel_space))
            .positiveButtonText(getString(R.string.auth_touch_id_action_use_password))
            .customFingerprintDialogClass(CustomFingerprintDialog::class.java)
            .useFingerprint(useFingerprint)
            .build()

    val key = AccountManager.getBiometricKey()
    biometricCompat.decrypt(promptParams, key, securityPassword) { result ->
        if (result.type == BiometricCompat.Type.INFO) return@decrypt

        if (result.type == BiometricCompat.Type.SUCCESS) {
            decryptAccount(
                accountDO = accountDO,
                password = result.value,
                showLoadingWhenDecryptStart = showLoadingWhenDecryptStart,
                dismissLoadingWhenDecryptEnd = dismissLoadingWhenDecryptEnd,
                passwordFromUserInput = false,
                passwordErrorCallback = {
                    // 加密的密码异常
                    biometricErrorCallback?.invoke()
                },
                passwordCallback = passwordCallback,
                mnemonicCallback = mnemonicCallback,
                privateKeyCallback = privateKeyCallback
            )
            return@decrypt
        }

        // error case
        if (result.reason == BiometricCompat.Reason.USER_CANCELED
            || result.reason == BiometricCompat.Reason.NEGATIVE_BUTTON
        ) {
            // 用户取消操作
            cancelCallback?.invoke()
            return@decrypt
        }

        // 其它error原因
        biometricErrorCallback?.invoke()
    }
}


/**
 * 通过密码验证账号
 *
 * @param cancelCallback            取消回调
 * @see BaseActivity.decryptAccount
 */
fun BaseActivity.authenticateAccountByPassword(
    accountDO: AccountDO,
    retryWhenPwdInputError: Boolean = true,
    showLoadingWhenDecryptStart: Boolean = true,
    dismissLoadingWhenDecryptEnd: Boolean = false,
    cancelCallback: (() -> Unit)? = null,
    passwordCallback: ((password: String) -> Unit)? = null,
    mnemonicCallback: ((mnemonics: List<String>) -> Unit)? = null,
    privateKeyCallback: ((privateKey: ByteArray) -> Unit)? = null
) {
    PasswordInputDialog()
        .setCancelListener { cancelCallback?.invoke() }
        .setConfirmListener { pwdBytes, pwdDialog ->
            pwdDialog.dismiss()

            decryptAccount(
                accountDO = accountDO,
                password = String(pwdBytes),
                showLoadingWhenDecryptStart = showLoadingWhenDecryptStart,
                dismissLoadingWhenDecryptEnd = dismissLoadingWhenDecryptEnd,
                passwordFromUserInput = true,
                passwordErrorCallback = {
                    if (retryWhenPwdInputError) {
                        authenticateAccountByPassword(
                            accountDO = accountDO,
                            retryWhenPwdInputError = retryWhenPwdInputError,
                            showLoadingWhenDecryptStart = showLoadingWhenDecryptStart,
                            dismissLoadingWhenDecryptEnd = dismissLoadingWhenDecryptEnd,
                            cancelCallback = cancelCallback,
                            passwordCallback = passwordCallback,
                            mnemonicCallback = mnemonicCallback,
                            privateKeyCallback = privateKeyCallback
                        )
                    }
                },
                passwordCallback = passwordCallback,
                mnemonicCallback = mnemonicCallback,
                privateKeyCallback = privateKeyCallback
            )
        }
        .show(supportFragmentManager)
}

/**
 * 通过密码验证账号
 *
 * @param cancelCallback            取消回调
 * @see Fragment.decryptAccount
 */
fun Fragment.authenticateAccountByPassword(
    accountDO: AccountDO,
    retryWhenPwdInputError: Boolean = true,
    showLoadingWhenDecryptStart: Boolean = true,
    dismissLoadingWhenDecryptEnd: Boolean = false,
    cancelCallback: (() -> Unit)? = null,
    passwordCallback: ((password: String) -> Unit)? = null,
    mnemonicCallback: ((mnemonics: List<String>) -> Unit)? = null,
    privateKeyCallback: ((privateKey: ByteArray) -> Unit)? = null
) {
    PasswordInputDialog()
        .setCancelListener { cancelCallback?.invoke() }
        .setConfirmListener { pwdBytes, pwdDialog ->
            pwdDialog.dismiss()

            decryptAccount(
                accountDO = accountDO,
                password = String(pwdBytes),
                showLoadingWhenDecryptStart = showLoadingWhenDecryptStart,
                dismissLoadingWhenDecryptEnd = dismissLoadingWhenDecryptEnd,
                passwordFromUserInput = true,
                passwordErrorCallback = {
                    if (retryWhenPwdInputError) {
                        authenticateAccountByPassword(
                            accountDO = accountDO,
                            retryWhenPwdInputError = retryWhenPwdInputError,
                            showLoadingWhenDecryptStart = showLoadingWhenDecryptStart,
                            dismissLoadingWhenDecryptEnd = dismissLoadingWhenDecryptEnd,
                            cancelCallback = cancelCallback,
                            passwordCallback = passwordCallback,
                            mnemonicCallback = mnemonicCallback,
                            privateKeyCallback = privateKeyCallback
                        )
                    }
                },
                passwordCallback = passwordCallback,
                mnemonicCallback = mnemonicCallback,
                privateKeyCallback = privateKeyCallback
            )
        }
        .show(childFragmentManager)
}

/**
 * 解密账号
 * [passwordCallback]、[mnemonicCallback]、[privateKeyCallback] 只会处理一个回调。
 * 密码错误时会自动 Toast, [passwordFromUserInput] 为 false 时不提示。
 *
 * @param accountDO                     本地账号
 * @param password                      明文密码
 * @param showLoadingWhenDecryptStart   解密开始时是否显示 loading，解密较耗时，默认显示 loading
 * @param dismissLoadingWhenDecryptEnd  解密结束时是否隐藏 loading，密码错误时会自动隐藏 loading
 * @param passwordFromUserInput         密码来源，true 表示来源于用户输入，false 表示来源于生物验证通过后
 *                                      解密 [AccountDO.encryptedPassword]。
 * @param passwordErrorCallback         密码错误回调，密码规则不符合或解密 [AccountDO.privateKey] 失败时回调
 * @param passwordCallback              密码回调，密码规则符合且解密 [AccountDO.privateKey] 成功时回调，
 *                                      用于验证账户后返回密码场景（如开启生物验证）
 * @param mnemonicCallback              助记词回调，解密 [AccountDO.mnemonic] 成功时回调，
 *                                      用于验证账户后返回助记词场景（如导出助记词）
 * @param privateKeyCallback            私钥回调，解密 [AccountDO.privateKey] 成功时回调，
 *                                      用于验证账户后返回私钥（如转账交易）
 */
fun BaseActivity.decryptAccount(
    accountDO: AccountDO,
    password: String?,
    showLoadingWhenDecryptStart: Boolean = true,
    dismissLoadingWhenDecryptEnd: Boolean = false,
    passwordFromUserInput: Boolean = true,
    passwordErrorCallback: (() -> Unit)? = null,
    passwordCallback: ((password: String) -> Unit)? = null,
    mnemonicCallback: ((mnemonics: List<String>) -> Unit)? = null,
    privateKeyCallback: ((privateKey: ByteArray) -> Unit)? = null
) {
    require(
        passwordCallback != null
                || mnemonicCallback != null
                || privateKeyCallback != null
    ) {
        "passwordCallback, mnemonicCallback, privateKeyCallback, cannot all be null"
    }

    if (password.isNullOrBlank()) {
        if (passwordFromUserInput) {
            showToast(getString(R.string.auth_pwd_hint_pwd_empty))
        }
        return
    }

    try {
        PasswordCheckUtil.check(password)
    } catch (e: Throwable) {
        if (passwordFromUserInput) {
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
        }

        passwordErrorCallback?.invoke()
        return
    }

    launch(Dispatchers.Main) {
        if (showLoadingWhenDecryptStart) {
            showProgress()
        }

        var privateKey: ByteArray? = null
        var mnemonics: List<String>? = null
        withContext(Dispatchers.IO) {
            val simpleSecurity =
                SimpleSecurity.instance(applicationContext)

            when {
                passwordCallback != null || privateKeyCallback != null -> {
                    privateKey = simpleSecurity.decrypt(
                        password.toByteArray(), accountDO.privateKey
                    )

                    if (privateKey == null) {
                        if (showLoadingWhenDecryptStart) {
                            delay(500)
                        }
                    }
                }

                mnemonicCallback != null -> {
                    val mnemonicBytes = simpleSecurity.decrypt(
                        password.toByteArray(), accountDO.mnemonic
                    )

                    if (mnemonicBytes != null) {
                        val mnemonicStr = String(mnemonicBytes)
                        mnemonics = mnemonicStr.substring(1, mnemonicStr.length - 1)
                            .split(",")
                            .map { it.trim() }
                    } else {
                        if (showLoadingWhenDecryptStart) {
                            delay(500)
                        }
                    }
                }
            }
        }

        if (passwordCallback != null
            && privateKey != null
        ) {
            if (showLoadingWhenDecryptStart && dismissLoadingWhenDecryptEnd) {
                dismissProgress()
            }
            passwordCallback.invoke(password)
        } else if (mnemonicCallback != null
            && !mnemonics.isNullOrEmpty()
        ) {
            if (showLoadingWhenDecryptStart && dismissLoadingWhenDecryptEnd) {
                dismissProgress()
            }
            mnemonicCallback.invoke(mnemonics!!)
        } else if (privateKeyCallback != null
            && privateKey != null
        ) {
            if (showLoadingWhenDecryptStart && dismissLoadingWhenDecryptEnd) {
                dismissProgress()
            }
            privateKeyCallback.invoke(privateKey!!)
        } else {
            if (showLoadingWhenDecryptStart) {
                dismissProgress()
            }

            if (passwordFromUserInput) {
                showToast(getString(R.string.auth_pwd_hint_pwd_error))
            }
            passwordErrorCallback?.invoke()
        }
    }
}

/**
 * 解密账号
 * [passwordCallback]、[mnemonicCallback]、[privateKeyCallback] 只会处理一个回调。
 * 密码错误时会自动 Toast, [passwordFromUserInput] 为 false 时不提示。
 *
 * @param accountDO                     本地账号
 * @param password                      明文密码
 * @param showLoadingWhenDecryptStart   解密开始时是否显示 loading，解密较耗时，默认显示 loading
 * @param dismissLoadingWhenDecryptEnd  解密结束时是否隐藏 loading，密码错误时会自动隐藏 loading
 * @param passwordFromUserInput         密码来源，true 表示来源于用户输入，false 表示来源于生物验证通过后
 *                                      解密 [AccountDO.encryptedPassword]。
 * @param passwordErrorCallback         密码错误回调，密码规则不符合或解密 [AccountDO.privateKey] 失败时回调
 * @param passwordCallback              密码回调，密码规则符合且解密 [AccountDO.privateKey] 成功时回调，
 *                                      用于验证账户后返回密码场景（如开启生物验证）
 * @param mnemonicCallback              助记词回调，解密 [AccountDO.mnemonic] 成功时回调，
 *                                      用于验证账户后返回助记词场景（如导出助记词）
 * @param privateKeyCallback            私钥回调，解密 [AccountDO.privateKey] 成功时回调，
 *                                      用于验证账户后返回私钥（如转账交易）
 */
fun Fragment.decryptAccount(
    accountDO: AccountDO,
    password: String?,
    showLoadingWhenDecryptStart: Boolean = true,
    dismissLoadingWhenDecryptEnd: Boolean = false,
    passwordFromUserInput: Boolean = true,
    passwordErrorCallback: (() -> Unit)? = null,
    passwordCallback: ((password: String) -> Unit)? = null,
    mnemonicCallback: ((mnemonics: List<String>) -> Unit)? = null,
    privateKeyCallback: ((privateKey: ByteArray) -> Unit)? = null
) {
    require(
        passwordCallback != null
                || mnemonicCallback != null
                || privateKeyCallback != null
    ) {
        "passwordCallback, mnemonicCallback, privateKeyCallback, cannot all be null"
    }

    if (password.isNullOrBlank()) {
        if (passwordFromUserInput) {
            (this as? ViewController)?.showToast(getString(R.string.auth_pwd_hint_pwd_empty))
        }
        return
    }

    try {
        PasswordCheckUtil.check(password)
    } catch (e: Throwable) {
        if (passwordFromUserInput) {
            (this as? ViewController)?.showToast(
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
        }

        passwordErrorCallback?.invoke()
        return
    }

    (this as? CoroutineScope)?.launch(Dispatchers.Main) {
        if (showLoadingWhenDecryptStart) {
            (this as? ViewController)?.showProgress()
        }

        var privateKey: ByteArray? = null
        var mnemonics: List<String>? = null
        var account: Account? = null
        withContext(Dispatchers.IO) {
            val simpleSecurity =
                SimpleSecurity.instance(requireContext().applicationContext)

            when {
                passwordCallback != null || privateKeyCallback != null -> {
                    privateKey = simpleSecurity.decrypt(
                        password.toByteArray(), accountDO.privateKey
                    )

                    if (privateKey == null) {
                        if (showLoadingWhenDecryptStart) {
                            delay(500)
                        }
                    }
                }

                mnemonicCallback != null -> {
                    val mnemonicBytes = simpleSecurity.decrypt(
                        password.toByteArray(), accountDO.mnemonic
                    )

                    if (mnemonicBytes != null) {
                        val mnemonicStr = String(mnemonicBytes)
                        mnemonics = mnemonicStr.substring(1, mnemonicStr.length - 1)
                            .split(",")
                            .map { it.trim() }
                    } else {
                        if (showLoadingWhenDecryptStart) {
                            delay(500)
                        }
                    }
                }
            }
        }

        if (passwordCallback != null
            && privateKey != null
        ) {
            if (showLoadingWhenDecryptStart && dismissLoadingWhenDecryptEnd) {
                (this as? ViewController)?.dismissProgress()
            }
            passwordCallback.invoke(password)
        } else if (mnemonicCallback != null
            && !mnemonics.isNullOrEmpty()
        ) {
            if (showLoadingWhenDecryptStart && dismissLoadingWhenDecryptEnd) {
                (this as? ViewController)?.dismissProgress()
            }
            mnemonicCallback.invoke(mnemonics!!)
        } else if (privateKeyCallback != null
            && privateKey != null
        ) {
            if (showLoadingWhenDecryptStart && dismissLoadingWhenDecryptEnd) {
                (this as? ViewController)?.dismissProgress()
            }
            privateKeyCallback.invoke(privateKey!!)
        } else {
            if (showLoadingWhenDecryptStart) {
                (this as? ViewController)?.dismissProgress()
            }

            if (passwordFromUserInput) {
                (this as? ViewController)?.showToast(getString(R.string.auth_pwd_hint_pwd_error))
            }
            passwordErrorCallback?.invoke()
        }
    }
}