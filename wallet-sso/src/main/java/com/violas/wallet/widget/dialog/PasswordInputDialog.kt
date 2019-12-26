package com.violas.wallet.widget.dialog

import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.palliums.utils.*
import com.violas.wallet.R
import kotlinx.android.synthetic.main.dialog_password_input.view.*

class PasswordInputDialog : DialogFragment() {
    private lateinit var mRootView: View

    private var confirmCallback: ((ByteArray, DialogFragment) -> Unit)? = null
    private var cancelCallback: (() -> Unit)? = null

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
            params?.y = (point.y * 0.23).toInt()
            dialog?.window?.attributes = params
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        }
        mRootView = inflater.inflate(R.layout.dialog_password_input, container)
        isCancelable = false
        mRootView.btnConfirm.setOnClickListener {
            val trim = mRootView.editPassword.text.toString().trim()
            if (trim.isEmpty()) {
                showToast(getString(R.string.hint_please_input_password))
                return@setOnClickListener
            }
            try {
                PasswordCheckUtil.check(trim)
                confirmCallback?.invoke(trim.toByteArray(), this)
            } catch (e: PasswordLengthShortException) {
                showToast(getString(R.string.hint_please_minimum_password_length))
            } catch (e: PasswordLengthLongException) {
                showToast(getString(R.string.hint_please_maxmum_password_length))
            } catch (e: PasswordSpecialFailsException) {
                showToast(getString(R.string.hint_please_cannot_contain_special_characters))
            } catch (e: PasswordValidationFailsException) {
                showToast(getString(R.string.hint_please_password_rules_are_wrong))
            } catch (e: PasswordEmptyException) {
                showToast(getString(R.string.hint_please_password_not_empty))
            }
        }
        mRootView.btnCancel.setOnClickListener {
            dismiss()
            cancelCallback?.invoke()
        }
        return mRootView
    }

    private fun showToast(msg: String) {
        Toast.makeText(
            context,
            msg,
            Toast.LENGTH_SHORT
        ).show()
    }

    fun show(manager: FragmentManager) {
        show(manager, "password")
    }

    fun setConfirmListener(callback: (ByteArray, DialogFragment) -> Unit): PasswordInputDialog {
        confirmCallback = callback
        return this
    }

    fun setCancelListener(callback: () -> Unit): PasswordInputDialog {
        cancelCallback = callback
        return this
    }
}