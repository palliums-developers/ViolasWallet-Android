package com.violas.wallet.ui.main.me

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.palliums.base.BaseFragment
import com.violas.wallet.R
import com.violas.wallet.ui.account.management.AccountManagementActivity
import com.violas.wallet.ui.addressBook.AddressBookActivity
import com.violas.wallet.ui.dexOrder.DexOrdersActivity
import com.violas.wallet.ui.setting.SettingActivity
import kotlinx.android.synthetic.main.fragment_me.*

/**
 * 我的页面
 */
class MeFragment : BaseFragment() {

    override fun getLayoutResId(): Int {
        return R.layout.fragment_me
    }

    override fun onLazyInitView(savedInstanceState: Bundle?) {
        super.onLazyInitView(savedInstanceState)

        mivWalletManagement.setOnClickListener(this)
        mivTransferRecord.setOnClickListener(this)
        mivAddressBook.setOnClickListener(this)
        mivSettings.setOnClickListener(this)

        mivTransferRecord.visibility = View.VISIBLE
    }

    override fun onViewClick(view: View) {
        when (view.id) {
            R.id.mivWalletManagement -> {
                startActivity(Intent(_mActivity, AccountManagementActivity::class.java))
            }

            R.id.mivTransferRecord -> {
                startActivity(Intent(_mActivity, DexOrdersActivity::class.java))
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