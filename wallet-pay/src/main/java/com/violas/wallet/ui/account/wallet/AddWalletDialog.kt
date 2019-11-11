package com.violas.wallet.ui.account.wallet

import android.os.Bundle
import android.view.Gravity
import android.view.View
import com.violas.wallet.R
import com.violas.wallet.base.BaseDialogFragment
import com.violas.wallet.common.EXTRA_KEY_ACCOUNT_TYPE
import com.violas.wallet.ui.account.AccountType
import com.violas.wallet.ui.account.transformAccountType
import kotlinx.android.synthetic.main.dialog_add_wallet.*

/**
 * Created by elephant on 2019-10-30 18:05.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 添加钱包弹窗
 */
class AddWalletDialog : BaseDialogFragment() {

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

    override fun getLayoutResId(): Int {
        return R.layout.dialog_add_wallet
    }

    override fun getWindowAnimationsStyleId(): Int {
        return R.style.AnimationDefaultBottomDialog
    }

    override fun getWindowLayoutParamsGravity(): Int {
        return Gravity.BOTTOM
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

    override fun onViewClick(view: View) {
        when (view.id) {
            R.id.llCreate -> {
                CreateWalletActivity.start(
                    requireActivity(),
                    transformAccountType(accountType),
                    AddWalletActivity.REQUEST_CREATE_IMPORT
                )
                close()
            }

            R.id.llImport -> {
                ImportWalletActivity.start(
                    requireActivity(),
                    transformAccountType(accountType),
                    AddWalletActivity.REQUEST_CREATE_IMPORT
                )
                close()
            }

            R.id.tvCancel -> {
                close()
            }
        }
    }
}