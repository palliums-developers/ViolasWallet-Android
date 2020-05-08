package com.violas.wallet.ui.ssoApplication.issueToken

import android.os.Bundle
import com.violas.wallet.R

/**
 * 发行商首次申请发币视图
 */
class SSOApplyForIssueTokenFragment : BaseSSOApplyForIssueTokenFragment() {

    override fun getLayoutResId(): Int {
        return R.layout.fragment_sso_apply_for_issue_token
    }

    override fun initData(savedInstanceState: Bundle?): Boolean {
        return true
    }
}
