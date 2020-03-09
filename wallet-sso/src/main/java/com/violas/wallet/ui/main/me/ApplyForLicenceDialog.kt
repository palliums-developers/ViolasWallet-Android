package com.violas.wallet.ui.main.me

import android.graphics.Point
import android.os.Bundle
import android.view.Gravity
import android.view.View
import com.palliums.base.BaseDialogFragment
import com.palliums.net.getErrorTipsMsg
import com.palliums.utils.CustomMainScope
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.GovernorManager
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.event.RefreshGovernorApplicationProgressEvent
import com.violas.wallet.widget.dialog.PasswordInputDialog
import kotlinx.android.synthetic.main.dialog_apply_for_licence.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.palliums.libracore.wallet.KeyPair
import org.palliums.violascore.wallet.Account

/**
 * Created by elephant on 2020/3/9 17:13.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class ApplyForLicenceDialog : BaseDialogFragment(), CoroutineScope by CustomMainScope() {

    private val mAccountManager by lazy { AccountManager() }
    private val mGovernorManager by lazy { GovernorManager() }

    override fun getLayoutResId(): Int {
        return R.layout.dialog_apply_for_licence
    }

    override fun getWindowAnimationsStyleId(): Int {
        return R.style.AnimationDefaultCenterDialog
    }

    override fun getWindowLayoutParamsGravity(): Int {
        return Gravity.CENTER or Gravity.TOP
    }

    override fun canceledOnTouchOutside(): Boolean {
        return false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnApplyFor.setOnClickListener {
            showPasswordInputDialog()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let {
            val attributes = it.attributes
            val point = Point()
            it.windowManager.defaultDisplay.getSize(point)
            attributes.y = (point.y * 0.2).toInt()
            it.attributes = attributes
        }
    }

    private fun showPasswordInputDialog() {
        PasswordInputDialog()
            .setConfirmListener { password, dialogFragment ->
                dialogFragment.dismiss()
                showProgress()

                launch(Dispatchers.Main) {
                    val account = withContext(Dispatchers.IO) {
                        val simpleSecurity =
                            SimpleSecurity.instance(requireContext().applicationContext)
                        val privateKey = simpleSecurity.decrypt(
                            password, mAccountManager.currentAccount().privateKey
                        )
                        return@withContext if (privateKey == null)
                            null
                        else
                            Account(KeyPair.fromSecretKey(privateKey))
                    }

                    if (account != null) {
                        publishVStake(account)
                    } else {
                        dismissProgress()
                        showToast(getString(R.string.hint_password_error))
                    }
                }
            }
            .show(childFragmentManager)
    }

    private fun publishVStake(account: Account) {
        launch(Dispatchers.Main) {
            try {
                mGovernorManager.publishVStake(requireContext(), account)
                EventBus.getDefault().post(RefreshGovernorApplicationProgressEvent())

                dismissProgress()
                showToast(R.string.tips_apply_for_licence_success)
                close()
            } catch (e: Exception) {
                dismissProgress()
                showToast(e.getErrorTipsMsg())
            }
        }
    }
}