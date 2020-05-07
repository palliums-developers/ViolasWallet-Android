package com.violas.wallet.ui.ssoApplication

import android.os.Bundle
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity

class ApplyForIssueTokenActivity : BaseAppActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.title_apply_issue_sso)
    }

    override fun getLayoutResId() = R.layout.activity_apply_fot_sso_submit
}
