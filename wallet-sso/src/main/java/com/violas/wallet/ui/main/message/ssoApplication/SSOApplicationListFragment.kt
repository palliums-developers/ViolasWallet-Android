package com.violas.wallet.ui.main.message.ssoApplication

import android.os.Bundle
import com.palliums.base.BaseFragment
import com.violas.wallet.R

/**
 * Created by elephant on 2020/2/28 20:31.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: SSO发币申请列表页面
 */
class SSOApplicationListFragment : BaseFragment() {

    override fun getLayoutResId(): Int {
        return R.layout.fragment_sso_application_list
    }

    override fun onLazyInitView(savedInstanceState: Bundle?) {
        super.onLazyInitView(savedInstanceState)

        // TODO
    }
}