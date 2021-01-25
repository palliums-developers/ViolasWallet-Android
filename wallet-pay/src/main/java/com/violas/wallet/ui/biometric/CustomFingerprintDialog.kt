package com.violas.wallet.ui.biometric

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.biometric.enhanced.BaseFingerprintDialogFragment
import androidx.biometric.enhanced.BiometricPrompt
import com.violas.wallet.R
import kotlinx.android.synthetic.main.dialog_custom_fingerprint.*

/**
 * Created by elephant on 2020/5/25 17:53.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class CustomFingerprintDialog : BaseFingerprintDialogFragment() {

    companion object {
        private const val TAG = "C-FingerprintDialogFrag"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_custom_fingerprint, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState != null && mBundle == null) {
            mBundle = savedInstanceState.getBundle(KEY_DIALOG_BUNDLE)
        }

        tvNegativeBtn.setOnClickListener {
            when {
                mNegativeButtonListener != null -> {
                    mNegativeButtonListener!!.onClick(
                        dialog,
                        DialogInterface.BUTTON_NEGATIVE
                    )
                }
                else -> {
                    Log.w(TAG, "No suitable negative button listener.")
                }
            }
        }

        val positiveButtonText = when {
            isDeviceCredentialAllowed() ->
                getString(R.string.auth_touch_id_action_use_password)
            isReactivateWhenLockoutPermanent() -> {
                val setText = getPositiveButtonText()
                if (setText.isNullOrBlank()) {
                    getString(R.string.auth_touch_id_action_enable_now)
                } else {
                    setText
                }
            }
            else -> getPositiveButtonText()
        }
        if (!positiveButtonText.isNullOrBlank()) {
            tvPositiveBtn.text = positiveButtonText
            if (isReactivateWhenLockoutPermanent()) {
                updatePositiveBtn(false)
            }
            tvPositiveBtn.setOnClickListener {
                when {
                    isDeviceCredentialAllowed() -> {
                        mDeviceCredentialButtonListener.onClick(
                            dialog,
                            DialogInterface.BUTTON_POSITIVE
                        )
                    }
                    isReactivateWhenLockoutPermanent() -> {
                        mReactivateBiometricButtonListener.onClick(
                            dialog,
                            DialogInterface.BUTTON_POSITIVE
                        )
                    }
                    mPositiveButtonListener != null -> {
                        mPositiveButtonListener!!.onClick(
                            dialog,
                            DialogInterface.BUTTON_POSITIVE
                        )
                    }
                    else -> {
                        Log.w(TAG, "No suitable positive button listener.")
                    }
                }
            }
        } else {
            updatePositiveBtn(false)
        }
    }

    override fun onStart() {
        dialog?.window?.let {
            val point = Point()
            it.windowManager.defaultDisplay.getSize(point)

            val attributes = it.attributes
            attributes.y = (point.y * 0.23).toInt()
            it.attributes = attributes

            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL)
        }

        dialog?.setCancelable(false)
        dialog?.setCanceledOnTouchOutside(false)

        super.onStart()
    }

    override fun onShowFailed(msg: CharSequence) {
        super.onShowFailed(getString(R.string.auth_touch_id_desc_try_again))
        updateTitleText(getString(R.string.auth_touch_id_desc_auth_failure))
    }

    override fun onShowHelp(msg: CharSequence) {
        super.onShowHelp(getString(R.string.auth_touch_id_desc_try_again))
        updateTitleText(getString(R.string.auth_touch_id_desc_auth_failure))
    }

    override fun onShowError(msg: CharSequence, errorId: Int) {
        if (errorId == BiometricPrompt.ERROR_LOCKOUT) {
            super.onShowError(
                getString(R.string.auth_touch_id_desc_lockout),
                errorId
            )
        } else {
            super.onShowError(msg, errorId)
        }
        updateTitleText(getString(R.string.auth_touch_id_desc_auth_failure))
    }

    override fun onResetMessage() {
        updateFingerprintIcon(STATE_FINGERPRINT)
        updateErrorText(getString(R.string.auth_touch_id_desc_try_again))
        updateTitleText(getString(R.string.auth_touch_id_desc_auth_start))
    }

    override fun onShowLockoutPermanent(msg: CharSequence) {
        super.onShowLockoutPermanent(getString(R.string.auth_touch_id_desc_lockout_permanent))
        updateTitleText(getString(R.string.auth_touch_id_desc_auth_failure))
        updatePositiveBtn(true)
    }

    override fun onBiometricReactivated() {
        updateFingerprintIcon(STATE_FINGERPRINT)
        updateErrorText("")
        updateTitleText(getString(R.string.auth_touch_id_desc_auth_start))
        updatePositiveBtn(false)
    }

    private fun updatePositiveBtn(show: Boolean) {
        if (show) {
            vVerticalLine1.visibility = View.VISIBLE
            tvPositiveBtn.visibility = View.VISIBLE
        } else {
            vVerticalLine1.visibility = View.GONE
            tvPositiveBtn.visibility = View.GONE
        }
    }

    override fun updateErrorText(msg: CharSequence) {
        super.updateErrorText(msg)
        tvFingerprintDesc?.let {
            it.text = msg
        }
    }

    private fun updateTitleText(msg: CharSequence) {
        tvFingerprintTitle?.let {
            it.text = msg
        }
    }

    override fun updateFingerprintIcon(newState: Int) {
        super.updateFingerprintIcon(newState)

        if (ivFingerprintIcon == null) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val icon =
                getAnimationForTransition(mLastState, newState) ?: return
            val animation =
                if (icon is AnimatedVectorDrawable) icon else null
            ivFingerprintIcon!!.setImageDrawable(icon)
            if (animation != null && shouldAnimateForTransition(mLastState, newState)) {
                animation.start()
            }
            mLastState = newState
        }
    }

    private fun shouldAnimateForTransition(oldState: Int, newState: Int): Boolean {
        if (oldState == STATE_NONE && newState == STATE_FINGERPRINT) {
            return false
        } else if (oldState == STATE_FINGERPRINT && newState == STATE_FINGERPRINT_ERROR) {
            return true
        } else if (oldState == STATE_FINGERPRINT_ERROR && newState == STATE_FINGERPRINT) {
            return true
        } else if (oldState == STATE_FINGERPRINT && newState == STATE_FINGERPRINT_AUTHENTICATED) {
            // TODO(b/77328470): add animation when fingerprint is authenticated
            return false
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getAnimationForTransition(oldState: Int, newState: Int): Drawable? {
        val iconRes: Int = if (oldState == STATE_NONE && newState == STATE_FINGERPRINT) {
            R.drawable.ic_fingerprint_start
        } else if (oldState == STATE_FINGERPRINT && newState == STATE_FINGERPRINT_ERROR) {
            R.drawable.ic_fingerprint_error
        } else if (oldState == STATE_FINGERPRINT_ERROR && newState == STATE_FINGERPRINT) {
            R.drawable.ic_fingerprint_start
        } else if (oldState == STATE_FINGERPRINT && newState == STATE_FINGERPRINT_AUTHENTICATED) {
            // TODO(b/77328470): add animation when fingerprint is authenticated
            R.drawable.ic_fingerprint_start
        } else {
            return null
        }
        return mContext!!.getDrawable(iconRes)
    }
}