package androidx.biometric.enhanced

import android.app.Dialog
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
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
        val subtitleView = layout.findViewById<TextView>(R.id.fingerprint_subtitle)
        val descriptionView = layout.findViewById<TextView>(R.id.fingerprint_description)
        val subtitle = mBundle!!.getCharSequence(
            BiometricPrompt.KEY_SUBTITLE
        )
        if (TextUtils.isEmpty(subtitle)) {
            subtitleView.visibility = View.GONE
        } else {
            subtitleView.visibility = View.VISIBLE
            subtitleView.text = subtitle
        }
        val description = mBundle!!.getCharSequence(
            BiometricPrompt.KEY_DESCRIPTION
        )
        if (TextUtils.isEmpty(description)) {
            descriptionView.visibility = View.GONE
        } else {
            descriptionView.visibility = View.VISIBLE
            descriptionView.text = description
        }
        mFingerprintIcon = layout.findViewById(R.id.fingerprint_icon)
        mErrorText = layout.findViewById(R.id.fingerprint_error)
        val negativeButtonText = if (isDeviceCredentialAllowed()) getString(
            R.string.confirm_device_credential_password
        ) else
            mBundle!!.getCharSequence(BiometricPrompt.KEY_NEGATIVE_TEXT)!!
        builder.setNegativeButton(negativeButtonText) { dialog, which ->
            when {
                isDeviceCredentialAllowed() -> {
                    mDeviceCredentialButtonListener.onClick(dialog, which)
                }
                mNegativeButtonListener != null -> {
                    mNegativeButtonListener!!.onClick(dialog, which)
                }
                else -> {
                    Log.w(TAG, "No suitable negative button listener.")
                }
            }
        }
        val positiveButtonText = getPositiveButtonText()
        if (!TextUtils.isEmpty(positiveButtonText)) {
            builder.setPositiveButton(positiveButtonText) { dialog, which ->
                if (mPositiveButtonListener != null) {
                    mPositiveButtonListener!!.onClick(dialog, which)
                } else {
                    Log.w(TAG, "No suitable positive button listener.")
                }
            }
        }
        builder.setView(layout)
        val dialog: Dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        return dialog
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

    private fun getThemedColorFor(attr: Int): Int {
        val tv = TypedValue()
        val theme = mContext!!.theme
        theme.resolveAttribute(attr, tv, true /* resolveRefs */)
        val arr = activity!!.obtainStyledAttributes(tv.data, intArrayOf(attr))
        val color = arr.getColor(0 /* index */, 0 /* defValue */)
        arr.recycle()
        return color
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