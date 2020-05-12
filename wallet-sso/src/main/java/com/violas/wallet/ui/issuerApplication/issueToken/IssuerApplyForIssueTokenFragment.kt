package com.violas.wallet.ui.issuerApplication.issueToken

import android.os.Bundle
import com.violas.wallet.R

/**
 * 发行商首次申请发币视图
 */
class IssuerApplyForIssueTokenFragment : BaseIssuerApplyForIssueTokenFragment() {

    override fun getLayoutResId(): Int {
        return R.layout.fragment_issuer_apply_for_issue_token
    }

    override fun initData(savedInstanceState: Bundle?): Boolean {
        return true
    }
}
