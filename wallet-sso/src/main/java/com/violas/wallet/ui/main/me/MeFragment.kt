package com.violas.wallet.ui.main.me

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.palliums.base.BaseFragment
import com.violas.wallet.R
import com.violas.wallet.ui.addressBook.AddressBookActivity
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

        vAuthentication.setOnClickListener(this)
        vPhoneVerification.setOnClickListener(this)
        vEmailVerification.setOnClickListener(this)
        vAddressBook.setOnClickListener(this)
        vSettings.setOnClickListener(this)
    }

    override fun onViewClick(view: View) {
        when (view.id) {
            R.id.vAuthentication -> {

            }

            R.id.vPhoneVerification -> {

            }

            R.id.vEmailVerification -> {

            }

            R.id.vAddressBook -> {
                AddressBookActivity.start(_mActivity)
            }

            R.id.vSettings -> {
                startActivity(Intent(_mActivity, SettingActivity::class.java))
            }
        }
    }
}