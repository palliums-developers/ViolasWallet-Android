package com.violas.wallet.ui.authentication

import android.os.Bundle
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity

/**
 * Created by elephant on 2019-11-29 09:49.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 身份信息页面
 */
class IDInformationActivity : BaseAppActivity() {

    override fun getLayoutResId(): Int {
        return R.layout.activity_id_infomation
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.title_id_information)
    }
}