package com.violas.wallet.widget.dialog

import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.violas.wallet.R
import com.violas.wallet.utils.CountDownTimerUtils
import kotlinx.android.synthetic.main.dialog_email_phone_validation.*
import kotlinx.android.synthetic.main.dialog_email_phone_validation.view.*

class EmailPhoneValidationDialog : DialogFragment() {
    private lateinit var mRootView: View

    private var confirmCallback: ((String, String, DialogFragment) -> Unit)? = null
    private var cancelCallback: (() -> Unit)? = null
    private val mPhoneCountDownTimerUtils by lazy {
        CountDownTimerUtils(mRootView.tvPhoneGetVerificationCode, 1000 * 60 * 3, 1000)
    }
    private val mEmailCountDownTimerUtils by lazy {
        CountDownTimerUtils(mRootView.tvEmailGetVerificationCode, 1000 * 60 * 3, 1000)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.TOP or Gravity.CENTER)

            val params = attributes;
            val windowManager = windowManager
            val display = windowManager.defaultDisplay
            val point = Point()
            display.getSize(point)
            params.gravity = Gravity.BOTTOM
            dialog?.window?.attributes = params
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        }

        mRootView = inflater.inflate(R.layout.dialog_email_phone_validation, container, false)
        isCancelable = false
        mRootView.btnConfirm.setOnClickListener {
            if (mRootView.etEmailVerificationCode.text.toString().isEmpty()) {
                Toast.makeText(context, R.string.hint_enter_verification_code, Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            if (mRootView.etPhoneVerificationCode.text.toString().isEmpty()) {
                Toast.makeText(context, R.string.hint_enter_verification_code, Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            confirmCallback?.invoke(
                mRootView.etPhoneVerificationCode.text.toString().trim(),
                mRootView.etEmailVerificationCode.text.toString().trim(),
                this
            )
        }
        mRootView.tvCancel.setOnClickListener {
            dismiss()
            cancelCallback?.invoke()
        }
        mRootView.tvPhoneGetVerificationCode.setOnClickListener {
            mPhoneCountDownTimerUtils.start()
        }
        mRootView.tvEmailGetVerificationCode.setOnClickListener {
            mEmailCountDownTimerUtils.start()
        }
        return mRootView
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            val params = attributes;
            params.width = WindowManager.LayoutParams.MATCH_PARENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            setLayout(params.width, params.height);
        }
    }

    override fun onDestroy() {
        mPhoneCountDownTimerUtils.cancel()
        mEmailCountDownTimerUtils.cancel()
        super.onDestroy()
    }

    fun show(manager: FragmentManager) {
        show(manager, "validation")
    }

    fun setConfirmListener(callback: (String, String, DialogFragment) -> Unit): EmailPhoneValidationDialog {
        confirmCallback = callback
        return this
    }

    fun setCancelListener(callback: () -> Unit): EmailPhoneValidationDialog {
        cancelCallback = callback
        return this
    }
}