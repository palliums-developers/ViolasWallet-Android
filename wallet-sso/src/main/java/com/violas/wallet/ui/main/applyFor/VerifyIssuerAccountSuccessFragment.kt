package com.violas.wallet.ui.main.applyFor

import android.os.Bundle
import android.view.View
import com.palliums.base.BaseFragment
import com.violas.wallet.R
import com.violas.wallet.ui.issuerApplication.IssuerApplicationActivity
import kotlinx.android.synthetic.main.fragment_verify_issuer_account_success.*

/**
 * 验证发行商账户成功视图
 */
class VerifyIssuerAccountSuccessFragment : BaseFragment() {

    override fun getLayoutResId(): Int {
        return R.layout.fragment_verify_issuer_account_success
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnGo.setOnClickListener {
            activity?.let { it1 ->
                IssuerApplicationActivity.start(it1)
            }
        }
    }
}
