package com.violas.wallet.ui.account.wallet

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.*
import androidx.fragment.app.DialogFragment
import com.quincysx.crypto.bip44.CoinType
import com.violas.wallet.R
import com.violas.wallet.base.BaseActivity
import com.violas.wallet.common.EXTRA_KEY_ACCOUNT_TYPE
import com.violas.wallet.ui.account.AccountType
import kotlinx.android.synthetic.main.dialog_add_wallet.*

/**
 * Created by elephant on 2019-10-30 18:05.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 添加钱包弹窗
 */
class AddWalletDialog : DialogFragment(), View.OnClickListener {

    companion object {

        fun newInstance(@AccountType accountType: Int): AddWalletDialog {
            val args = Bundle().apply {
                putInt(EXTRA_KEY_ACCOUNT_TYPE, accountType)
            }

            return AddWalletDialog().apply {
                arguments = args
            }
        }
    }

    private var accountType: Int = AccountType.VIOLAS

    init {
        setStyle(STYLE_NORMAL, R.style.ThemeDefaultBottomDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_add_wallet, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        llCreate.setOnClickListener(this)
        llImport.setOnClickListener(this)
        tvCancel.setOnClickListener(this)

        arguments?.let {
            accountType = it.getInt(EXTRA_KEY_ACCOUNT_TYPE, AccountType.VIOLAS)
        }
    }


    override fun onStart() {
        dialog?.window?.let {
            val displayMetrics = DisplayMetrics()
            activity?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)

            val attributes = it.attributes
            attributes.width = WindowManager.LayoutParams.MATCH_PARENT
            attributes.height = WindowManager.LayoutParams.WRAP_CONTENT
            attributes.gravity = Gravity.BOTTOM
            it.attributes = attributes

            it.setWindowAnimations(R.style.AnimationDefaultBottomDialog)
        }

        dialog?.setCancelable(true)
        dialog?.setCanceledOnTouchOutside(true)

        super.onStart()
    }

    override fun onClick(view: View) {
        if (!BaseActivity.isFastMultiClick(view)) {
            when (view.id) {
                R.id.llCreate -> {
                    // TODO 跳转到创建钱包页面
                }

                R.id.llImport -> {
                    // TODO 跳转到导入钱包页面
                }

                R.id.tvCancel -> {
                    if (!isDetached && !isRemoving && fragmentManager != null) {
                        dismissAllowingStateLoss()
                    }
                }
            }
        }
    }

    fun transform(): CoinType? {
        return null
    }
}