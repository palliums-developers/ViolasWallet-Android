package androidx.biometric.enhanced

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.palliums.biometric.R
import java.lang.ref.WeakReference

/**
 * Created by elephant on 2020/5/25 13:34.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class DefaultFingerprintDialogFragment : BaseFingerprintDialogFragment() {

    companion object {
        private const val TAG = "D-FingerprintDialogFrag"
    }

    private var mErrorColor = 0
    private var mTextColor = 0
    private var mFingerprintIcon: ImageView? = null
    private var mErrorText: TextView? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (savedInstanceState != null && mBundle == null) {
            mBundle = savedInstanceState.getBundle(KEY_DIALOG_BUNDLE)
        }

        val builder = AlertDialog.Builder(
            context!!,
            R.style.ThemeOverlay_AppCompat_Dialog_Alert_Custom
        )
        builder.setTitle(mBundle!!.getCharSequence(BiometricPrompt.KEY_TITLE))

        // We have to use builder.getContext() instead of the usual getContext() in order to get
        // the appropriately themed context for this dialog.
        val layout = LayoutInflater.from(builder.context)
            .inflate(R.layout.fingerprint_dialog_layout, null)
        builder.setView(layout)

        val subtitleView = layout.findViewById<TextView>(R.id.fingerprint_subtitle)
        val subtitle = mBundle!!.getCharSequence(BiometricPrompt.KEY_SUBTITLE)
        if (TextUtils.isEmpty(subtitle)) {
            subtitleView.visibility = View.GONE
        } else {
            subtitleView.visibility = View.VISIBLE
            subtitleView.text = subtitle
        }

        val descriptionView = layout.findViewById<TextView>(R.id.fingerprint_description)
        val description = mBundle!!.getCharSequence(BiometricPrompt.KEY_DESCRIPTION)
        if (TextUtils.isEmpty(description)) {
            descriptionView.visibility = View.GONE
        } else {
            descriptionView.visibility = View.VISIBLE
            descriptionView.text = description
        }

        mFingerprintIcon = layout.findViewById(R.id.fingerprint_icon)
        mErrorText = layout.findViewById(R.id.fingerprint_error)

        val negativeButtonText = getNegativeButtonText()!!
        builder.setNegativeButton(negativeButtonText) { dialog, which ->
            when {
                mNegativeButtonListener != null -> {
                    mNegativeButtonListener!!.onClick(dialog, which)
                }
                else -> {
                    Log.w(TAG, "No suitable negative button listener.")
                }
            }
        }

        val positiveButtonText = when {
            isDeviceCredentialAllowed() ->
                getString(R.string.confirm_device_credential_password)
            isReactivateWhenLockoutPermanent() -> {
                val setText = getPositiveButtonText()
                if (setText.isNullOrBlank()) {
                    getString(R.string.action_fingerprint_error_lockout)
                } else {
                    setText
                }
            }
            else -> getPositiveButtonText()
        }
        if (!TextUtils.isEmpty(positiveButtonText)) {
            builder.setPositiveButton(positiveButtonText) { dialog, which ->
                when {
                    isDeviceCredentialAllowed() -> {
                        mDeviceCredentialButtonListener.onClick(dialog, which)
                    }
                    isReactivateWhenLockoutPermanent() -> {
                        mReactivateBiometricButtonListener.onClick(dialog, which)
                    }
                    mPositiveButtonListener != null -> {
                        mPositiveButtonListener!!.onClick(dialog, which)
                    }
                    else -> {
                        Log.w(TAG, "No suitable positive button listener.")
                    }
                }
            }
        }

        val dialog: AlertDialog = builder.create()
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        if (isReactivateWhenLockoutPermanent()) {
            dialog.setOnShowListener {
                mFingerprintIcon?.postDelayed({
                    disableAlertDialogAutoDismiss(dialog)
                    updatePositiveBtn(false, dialog)
                }, 50)
            }
        }

        return dialog
    }

    private fun disableAlertDialogAutoDismiss(dialog: AlertDialog) {
        try {
            val mAlertField = dialog.javaClass.getDeclaredField("mAlert")
            mAlertField.isAccessible = true
            val obj = mAlertField.get(dialog)
            mAlertField.isAccessible = false

            val mHandlerField = obj.javaClass.getDeclaredField("mHandler")
            mHandlerField.isAccessible = true
            mHandlerField.set(obj, ButtonHandler(dialog))
            mHandlerField.isAccessible = false
        } catch (e: Exception) {
            Log.w(TAG, "Failed to disable AlertDialog auto dismiss, $e")
        }
    }

    private class ButtonHandler(dialog: DialogInterface) : Handler() {
        private val mDialog: WeakReference<DialogInterface> = WeakReference(dialog)

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                DialogInterface.BUTTON_POSITIVE,
                DialogInterface.BUTTON_NEGATIVE,
                DialogInterface.BUTTON_NEUTRAL ->
                    (msg.obj as DialogInterface.OnClickListener).onClick(
                        mDialog.get(),
                        msg.what
                    )
            }
        }
    }

    private fun getThemedColorFor(attr: Int): Int {
        val tv = TypedValue()
        val theme = mContext!!.theme
        theme.resolveAttribute(attr, tv, true /* resolveRefs */)
        val arr = activity!!.obtainStyledAttributes(tv.data, intArrayOf(attr))
        val color = arr.getColor(0 /* index */, 0 /* defValue */)
        arr.recycle()
        return color
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mErrorColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getThemedColorFor(android.R.attr.colorError)
        } else {
            ContextCompat.getColor(mContext!!, R.color.biometric_error_color)
        }
        mTextColor = getThemedColorFor(android.R.attr.textColorSecondary)
    }

    override fun onShowLockoutPermanent(msg: CharSequence) {
        super.onShowLockoutPermanent(msg)
        (dialog as AlertDialog?)?.let {
            updatePositiveBtn(true, it)
        }
    }

    override fun onBiometricReactivated() {
        super.onBiometricReactivated()
        (dialog as AlertDialog?)?.let {
            updatePositiveBtn(false, it)
        }
    }

    private fun updatePositiveBtn(show: Boolean, dialog: AlertDialog) {
        val positiveBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
        positiveBtn.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun updateFingerprintIcon(newState: Int) {
        super.updateFingerprintIcon(newState)

        // May be null if we're intentionally suppressing the dialog.
        if (mFingerprintIcon == null) {
            return
        }
        // Devices older than this do not have FP support (and also do not support SVG), so it's
        // fine for this to be a no-op. An error is returned immediately and the dialog is not
        // shown.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val icon =
                getAnimationForTransition(mLastState, newState) ?: return
            val animation =
                if (icon is AnimatedVectorDrawable) icon else null
            mFingerprintIcon!!.setImageDrawable(icon)
            if (animation != null && shouldAnimateForTransition(mLastState, newState)) {
                animation.start()
            }
            mLastState = newState
        }
    }

    override fun updateErrorText(msg: CharSequence) {
        super.updateErrorText(msg)
        if (mErrorText != null) {
            mErrorText!!.setTextColor(mErrorColor)
            mErrorText!!.text = msg
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
            R.drawable.fingerprint_dialog_fp_to_error
        } else if (oldState == STATE_FINGERPRINT && newState == STATE_FINGERPRINT_ERROR) {
            R.drawable.fingerprint_dialog_fp_to_error
        } else if (oldState == STATE_FINGERPRINT_ERROR && newState == STATE_FINGERPRINT) {
            R.drawable.fingerprint_dialog_error_to_fp
        } else if (oldState == STATE_FINGERPRINT && newState == STATE_FINGERPRINT_AUTHENTICATED) {
            // TODO(b/77328470): add animation when fingerprint is authenticated
            R.drawable.fingerprint_dialog_error_to_fp
        } else {
            return null
        }
        return mContext!!.getDrawable(iconRes)
    }
}