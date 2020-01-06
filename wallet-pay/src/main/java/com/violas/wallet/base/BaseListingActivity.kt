package com.violas.wallet.base

import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import com.palliums.listing.ListingController
import com.palliums.listing.ListingHandler
import com.palliums.widget.refresh.IRefreshLayout
import com.palliums.widget.status.IStatusLayout
import com.violas.wallet.R
import kotlinx.android.synthetic.main.activity_base_list.*

/**
 * Created by elephant on 2019-11-05 10:34.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 列表页面基类
 */
abstract class BaseListingActivity<VO> : BaseAppActivity(), ListingController<VO> {

    protected val mListingHandler by lazy {
        ListingHandler(this, this, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mListingHandler
    }

    /**
     * 子类通过覆写[getLayoutResId]返回自定义布局，必须包含RecyclerView
     */
    override fun getLayoutResId(): Int {
        return R.layout.activity_base_list
    }

    /**
     * 子类覆写[getLayoutResId]后，需覆写[getRecyclerView]返回RecyclerView
     */
    override fun getRecyclerView(): RecyclerView {
        return vRecyclerView
    }

    /**
     * 子类覆写[getLayoutResId]后，需覆写[getRefreshLayout]返回SmartRefreshLayout
     */
    override fun getRefreshLayout(): IRefreshLayout? {
        return vRefreshLayout
    }

    /**
     * 子类覆写[getLayoutResId]后，需覆写[getStatusLayout]返回DataLoadStatusLayout
     */
    override fun getStatusLayout(): IStatusLayout? {
        return vStatusLayout
    }
}