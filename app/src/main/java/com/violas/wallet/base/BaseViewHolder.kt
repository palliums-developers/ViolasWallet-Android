package com.violas.wallet.base

import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Created by elephant on 2019-10-24 10:01.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Base RecyclerView.ViewHolder
 */
abstract class BaseViewHolder<VO>(view: View) :
    RecyclerView.ViewHolder(view), View.OnClickListener {

    private var itemIndex: Int = -1
    private var itemDate: VO? = null

    final override fun onClick(view: View) {
        if (!BaseActivity.isFastMultiClick(view)) {
            onViewClick(view, itemIndex, itemDate)
        }
    }

    fun bind(itemIndex: Int, itemDate: VO?) {
        this.itemIndex = itemIndex
        this.itemDate = itemDate
        onViewBind(itemIndex, itemDate)
    }

    abstract fun onViewBind(itemIndex: Int, itemDate: VO?)

    abstract fun onViewClick(view: View, itemIndex: Int, itemDate: VO?)
}