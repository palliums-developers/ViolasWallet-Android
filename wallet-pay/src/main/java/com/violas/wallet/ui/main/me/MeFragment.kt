package com.violas.wallet.ui.main.me

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.palliums.base.BaseFragment
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.ui.account.walletmanager.WalletManagerActivity
import com.violas.wallet.ui.addressBook.AddressBookActivity
import com.violas.wallet.ui.setting.SettingActivity
import com.violas.wallet.viewModel.WalletAppViewModel
import kotlinx.android.synthetic.main.fragment_me.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 我的页面
 */
class MeFragment : BaseFragment() {

    private val mAccountManager by lazy { AccountManager() }

    private val mWalletAppViewModel by lazy {
        context?.let { WalletAppViewModel.getViewModelInstance(it) }
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_me
    }

    override fun onLazyInitView(savedInstanceState: Bundle?) {
        super.onLazyInitView(savedInstanceState)

        mivWalletManagement.setOnClickListener(this)
        mivAddressBook.setOnClickListener(this)
        mivSettings.setOnClickListener(this)
    }

    override fun onViewClick(view: View) {
        when (view.id) {
            R.id.mivWalletManagement -> {
                if (mWalletAppViewModel?.isExistsAccount() != true) {
                    showToast(R.string.tips_create_or_import_wallet)
                    return
                }
                WalletManagerActivity.start(this@MeFragment)
            }

            R.id.mivAddressBook -> {
                AddressBookActivity.start(_mActivity)
            }

            R.id.mivSettings -> {
                startActivity(Intent(_mActivity, SettingActivity::class.java))
            }
        }
    }
}