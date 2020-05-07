package com.violas.wallet.ui.main.applyFor

import android.os.Bundle
import android.view.View
import com.palliums.base.BaseFragment
import com.violas.wallet.R
import com.violas.wallet.ui.ssoApplication.SSOApplicationActivity
import kotlinx.android.synthetic.main.fragment_verify_user_info.*

/**
 * 验证发行商用户信息成功视图
 */
class SSOVerifySuccessFragment : BaseFragment() {

    override fun getLayoutResId(): Int {
        return R.layout.fragment_sso_verify_success
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnGo.setOnClickListener {
            activity?.let { it1 ->
                SSOApplicationActivity.start(it1)
            }
        }
    }
}
