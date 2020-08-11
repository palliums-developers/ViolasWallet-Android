package com.violas.wallet.ui.mapping

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.palliums.utils.StatusBarUtil
import com.palliums.utils.getResourceId
import com.palliums.utils.start
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity

/**
 * Created by elephant on 2020/8/11 16:15.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 映射页面
 */
class MappingActivity : BaseAppActivity() {

    companion object {
        fun start(context: Context) {
            Intent(context, MappingActivity::class.java).start(context)
        }
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_mapping
    }

    override fun getTitleStyle(): Int {
        return PAGE_STYLE_CUSTOM
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        StatusBarUtil.setLightStatusBarMode(window, false)
        setTopBackgroundResource(getResourceId(R.attr.homeFragmentTopBg, this))
        setTitleLeftImageResource(getResourceId(R.attr.iconBackSecondary, this))
        setTitleRightImageResource(getResourceId(R.attr.mappingMenuIcon, this))
        titleColor = getResourceId(R.attr.colorOnPrimary, this)
        setTitle(R.string.mapping)
    }

    override fun onTitleRightViewClick() {
        showToast("进入兑换记录页面")
    }
}