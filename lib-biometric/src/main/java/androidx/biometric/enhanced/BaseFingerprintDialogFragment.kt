package androidx.biometric.enhanced

import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.DialogFragment
import com.palliums.biometric.R

/**
 * Created by elephant on 2020/5/25 11:16.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 *
 * This class implements a custom AlertDialog that prompts the user for fingerprint authentication.
 * This class is not meant to be preserved across process death; for security reasons, the
 * BiometricPromptCompat will automatically dismiss the dialog when the activity is no longer in the
 * foreground.
 */
abstract class BaseFingerprintDialogFragment : DialogFragment() {

    companion object {
        private const val TAG = "FingerprintDialogFrag"
        const val KEY_DIALOG_BUNDLE = "SavedBundle"

        /**
         * Error/help message will show for this amount of time, unless
         * [Utils.shouldAlwaysHideFingerprintDialogInstantly] is true.
         *
         *
         * For error messages, the dialog will also be dismissed after this amount of time. Error
         * messages will be propagated back to the application via AuthenticationCallback
         * after this amount of time.
         */
        private const val MESSAGE_DISPLAY_TIME_MS = 2000

        // Shows a temporary message in the help area
        const val MSG_SHOW_HELP = 1
        // Show an error in the help area, and dismiss the dialog afterwards
        const val MSG_SHOW_ERROR = 2
        // Dismisses the authentication dialog
        const val MSG_DISMISS_DIALOG_ERROR = 3
        // Resets the help message
        const val MSG_RESET_MESSAGE = 4
        // Dismisses the authentication dialog after success.
        const val MSG_DISMISS_DIALOG_AUTHENTICATED = 5
        // The amount of time required that this fragment be displayed for in order that
        // we show an error message on top of the UI.
        const val DISPLAYED_FOR_500_MS = 6

        // States for icon animation
        const val STATE_NONE = 0
        const val STATE_FINGERPRINT = 1
        const val STATE_FINGERPRINT_ERROR = 2
        const val STATE_FINGERPRINT_AUTHENTICATED = 3


        /**
         * @return The effective millisecond delay to wait before hiding the dialog, while respecting
         * the result of [Utils.shouldAlwaysHideFingerprintDialogInstantly].
         */
        fun getHideDialogDelay(context: Context?): Int {
            return if (context != null && Utils.shouldHideFingerprintDialog(context, Build.MODEL))
                0
            else
                MESSAGE_DISPLAY_TIME_MS
        }
    }

    internal inner class H : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_SHOW_HELP ->
                    handleShowHelp(msg.obj as CharSequence)
                MSG_SHOW_ERROR ->
                    handleShowError(msg.obj as CharSequence)
                MSG_DISMISS_DIALOG_ERROR ->
                    handleDismissDialogError(msg.obj as CharSequence?)
                MSG_DISMISS_DIALOG_AUTHENTICATED ->
                    dismissSafely()
                MSG_RESET_MESSAGE ->
                    handleResetMessage()
                DISPLAYED_FOR_500_MS -> {
                    val context: Context? = context
                    mDismissInstantly = context != null
                            && Utils.shouldHideFingerprintDialog(context, Build.MODEL)
                }
            }
        }
    }

    private val mHandler = H()
    protected var mBundle: Bundle? = null
    protected var mLastState = 0
    protected var mContext: Context? = null

    /**
     * This flag is used to control the instant dismissal of the dialog fragment. In the case where
     * the user is already locked out this dialog will not appear. In the case where the user is
     * being locked out for the first time an error message will be displayed on the UI before
     * dismissing.
     */
    private var mDismissInstantly = true

    // This should be re-set by the BiometricPromptCompat each time the lifecycle changes.
    @VisibleForTesting
    var mNegativeButtonListener: DialogInterface.OnClickListener? = null

    // This should be re-set by the BiometricPromptCompat each time the lifecycle changes.
    @VisibleForTesting
    var mPositiveButtonListener: DialogInterface.OnClickListener? = null

    // Also created once and retained.
    protected val mDeviceCredentialButtonListener =
        DialogInterface.OnClickListener { dialog, which ->
            if (which == DialogInterface.BUTTON_NEGATIVE) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    Log.e(TAG, "Failed to check device credential. Not supported prior to L.")
                    return@OnClickListener
                }
                Utils.launchDeviceCredentialConfirmation(
                    TAG, activity, mBundle
                ) {
                    // Dismiss the fingerprint dialog without forwarding errors.
                    onCancel(dialog)
                }
            }
        }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBundle(KEY_DIALOG_BUNDLE, mBundle)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = context
    }

    override fun onResume() {
        super.onResume()
        mLastState = STATE_NONE
        updateFingerprintIcon(STATE_FINGERPRINT)
    }

    override fun onPause() {
        super.onPause()
        // Remove everything since the fragment is going away.
        mHandler.removeCallbacksAndMessages(null)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        val fingerprintHelperFragment =
            fragmentManager
                ?.findFragmentByTag(BiometricPrompt.FINGERPRINT_HELPER_FRAGMENT_TAG) as FingerprintHelperFragment?
        fingerprintHelperFragment?.cancel(FingerprintHelperFragment.USER_CANCELED_FROM_USER)
    }

    fun setBundle(bundle: Bundle) {
        mBundle = bundle
    }

    /**
     * The negative button text is persisted in the fragment, not in BiometricPromptCompat. Since
     * the dialog persists through rotation, this allows us to return this as the error text for
     * ERROR_NEGATIVE_BUTTON.
     */
    protected fun getNegativeButtonText(): CharSequence? {
        return mBundle!!.getCharSequence(BiometricPrompt.KEY_NEGATIVE_TEXT)
    }

    /**
     * The positive button text is persisted in the fragment, not in BiometricPromptCompat. Since
     * the dialog persists through rotation, this allows us to return this as the error text for
     * ERROR_POSITIVE_BUTTON.
     */
    protected fun getPositiveButtonText(): CharSequence? {
        return mBundle!!.getCharSequence(BiometricPrompt.KEY_POSITIVE_TEXT)
    }

    fun setNegativeButtonListener(listener: DialogInterface.OnClickListener?) {
        mNegativeButtonListener = listener
    }

    fun setPositiveButtonListener(listener: DialogInterface.OnClickListener?) {
        mPositiveButtonListener = listener
    }

    /**
     * @return The handler; the handler is used by FingerprintHelperFragment to notify the UI of
     * changes from Fingerprint callbacks.
     */
    fun getHandler(): Handler? {
        return mHandler
    }

    /** Attempts to dismiss this fragment while avoiding potential crashes.  */
    fun dismissSafely() {
        if (fragmentManager == null) {
            Log.e(TAG, "Failed to dismiss fingerprint dialog fragment. Fragment manager was null.")
            return
        }
        dismissAllowingStateLoss()
    }

    protected fun isDeviceCredentialAllowed(): Boolean {
        return mBundle!!.getBoolean(BiometricPrompt.KEY_ALLOW_DEVICE_CREDENTIAL)
    }

    protected open fun updateFingerprintIcon(newState: Int) {

    }

    protected open fun updateErrorText(msg: CharSequence) {

    }

    private fun handleShowHelp(msg: CharSequence) {
        updateFingerprintIcon(STATE_FINGERPRINT_ERROR)
        mHandler.removeMessages(MSG_RESET_MESSAGE)

        // May be null if we're intentionally suppressing the dialog.
        updateErrorText(msg)

        // Reset the text after a delay
        mHandler.sendMessageDelayed(
            mHandler.obtainMessage(MSG_RESET_MESSAGE),
            MESSAGE_DISPLAY_TIME_MS.toLong()
        )
    }

    private fun handleShowError(msg: CharSequence) {
        updateFingerprintIcon(STATE_FINGERPRINT_ERROR)
        mHandler.removeMessages(MSG_RESET_MESSAGE)

        // May be null if we're intentionally suppressing the dialog.
        updateErrorText(msg)

        // Dismiss the dialog after a delay
        mHandler.sendMessageDelayed(
            mHandler.obtainMessage(MSG_DISMISS_DIALOG_ERROR),
            getHideDialogDelay(mContext).toLong()
        )
    }

    private fun dismissAfterDelay(msg: CharSequence?) {
        // May be null if we're intentionally suppressing the dialog.
        updateErrorText(msg ?: mContext!!.getString(R.string.fingerprint_error_lockout))

        mHandler.postDelayed(
            { dismissSafely() },
            getHideDialogDelay(mContext).toLong()
        )
    }

    private fun handleDismissDialogError(msg: CharSequence?) {
        if (mDismissInstantly) {
            dismissSafely()
        } else {
            dismissAfterDelay(msg)
        }
        // Always set this to true. In case the user tries to authenticate again the UI will not be
        // shown.
        mDismissInstantly = true
    }

    private fun handleResetMessage() {
        updateFingerprintIcon(STATE_FINGERPRINT)

        // May be null if we're intentionally suppressing the dialog.
        updateErrorText(mContext!!.getString(R.string.fingerprint_dialog_touch_sensor))
    }
}