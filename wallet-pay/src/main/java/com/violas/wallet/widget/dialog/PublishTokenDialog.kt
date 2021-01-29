package com.violas.wallet.widget.dialog

import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.violas.wallet.R
import kotlinx.android.synthetic.main.dialog_password_input.view.*
import kotlinx.android.synthetic.main.dialog_password_input.view.btnCancel
import kotlinx.android.synthetic.main.dialog_password_input.view.btnConfirm
import kotlinx.android.synthetic.main.dialog_publish_token.view.*

/**
 * 添加币种到账户弹窗
 */
class PublishTokenDialog : DialogFragment() {
    private lateinit var mRootView: View

    private var addCurrencyPage: Boolean = true
    private var currencyName: String? = null
    private var confirmCallback: ((DialogFragment) -> Unit)? = null
    private var cancelCallback: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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
        mRootView = inflater.inflate(R.layout.dialog_publish_token, container)
        isCancelable = false
        mRootView.btnConfirm.setOnClickListener {
            confirmCallback?.invoke(this)
        }
        mRootView.btnCancel.setOnClickListener {
            dismiss()
            cancelCallback?.invoke()
        }
        mRootView.tvContent.text =
            getString(R.string.add_currency_first_desc_format, currencyName ?: "")
        mRootView.tvTitle.setText(
            if (addCurrencyPage)
                R.string.add_currency_first_title_1
            else
                R.string.add_currency_first_title_2
        )
        return mRootView
    }

    fun show(manager: FragmentManager) {
        show(manager, "managerAssert")
    }

    fun setAddCurrencyPage(addCurrencyPage: Boolean): PublishTokenDialog {
        this.addCurrencyPage = addCurrencyPage
        return this
    }

    fun setCurrencyName(currencyName: String?): PublishTokenDialog {
        this.currencyName = currencyName
        return this
    }

    fun setConfirmListener(callback: (DialogFragment) -> Unit): PublishTokenDialog {
        confirmCallback = callback
        return this
    }

    fun setCancelListener(callback: () -> Unit): PublishTokenDialog {
        cancelCallback = callback
        return this
    }
}