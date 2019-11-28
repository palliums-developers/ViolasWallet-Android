package com.violas.wallet.ui.main.me

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.palliums.base.BaseFragment
import com.violas.wallet.R
import com.violas.wallet.ui.addressBook.AddressBookActivity
import com.violas.wallet.ui.authentication.IDAuthenticationActivity
import com.violas.wallet.ui.setting.SettingActivity
import com.violas.wallet.ui.verification.EmailVerificationActivity
import com.violas.wallet.ui.verification.PhoneVerificationActivity
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
                startActivity(Intent(_mActivity, IDAuthenticationActivity::class.java))
            }

            R.id.vPhoneVerification -> {
                startActivity(Intent(_mActivity, PhoneVerificationActivity::class.java))
            }

            R.id.vEmailVerification -> {
                startActivity(Intent(_mActivity, EmailVerificationActivity::class.java))
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