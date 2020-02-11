package com.violas.wallet.widget.dialog

import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.palliums.utils.CustomMainScope
import com.violas.wallet.R
import kotlinx.android.synthetic.main.dialog_exchange_mapping_password.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

class ExchangeMappingPasswordDialog : DialogFragment(), CoroutineScope by CustomMainScope() {
    private var mRootView: View? = null

    private var confirmCallback: ((String, String, ExchangeMappingPasswordDialog) -> Unit)? = null
    private var cancelCallback: (() -> Unit)? = null

    private var mSendHint: String? = null
    private var mReceiveHint: String? = null

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

        mRootView = inflater.inflate(R.layout.dialog_exchange_mapping_password, container, false)
        isCancelable = false
        mRootView?.btnConfirm?.setOnClickListener {
            if (mRootView?.etReceivePassword?.text.toString().isEmpty()) {
                mRootView?.tvHintError?.text = getString(R.string.hint_please_input_password)
                return@setOnClickListener
            }
            if (mRootView?.etSendPassword?.text.toString().isEmpty()) {
                mRootView?.tvHintError?.text = getString(R.string.hint_please_input_password)
                return@setOnClickListener
            }
            confirmCallback?.invoke(
                mRootView?.etSendPassword?.text.toString().trim(),
                mRootView?.etReceivePassword?.text.toString().trim(),
                this
            )
        }
        mRootView?.tvCancel?.setOnClickListener {
            dismiss()
            cancelCallback?.invoke()
        }
        mRootView?.tvSendPasswordHint?.text = mSendHint
        mRootView?.tvReceiveHint?.text = mReceiveHint
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
        cancel()
        super.onDestroy()
    }

    fun show(manager: FragmentManager) {
        show(manager, "validation")
    }

    fun setConfirmListener(callback: (String, String, ExchangeMappingPasswordDialog) -> Unit): ExchangeMappingPasswordDialog {
        confirmCallback = callback
        return this
    }

    fun setCancelListener(callback: () -> Unit): ExchangeMappingPasswordDialog {
        cancelCallback = callback
        return this
    }

    fun setErrorHint(msg: String) {
        mRootView?.tvHintError?.text = msg
    }

    fun setSendHint(msg: String): ExchangeMappingPasswordDialog {
        mSendHint = msg
        return this
    }

    fun setReceiveHint(msg: String): ExchangeMappingPasswordDialog {
        mReceiveHint = msg
        return this
    }
}