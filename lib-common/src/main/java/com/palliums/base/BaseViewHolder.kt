package com.palliums.base

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.palliums.utils.isFastMultiClick

/**
 * Created by elephant on 2019-10-24 10:01.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Base RecyclerView.ViewHolder
 */
abstract class BaseViewHolder<VO>(view: View) :
    RecyclerView.ViewHolder(view), View.OnClickListener {

    protected var itemData: VO? = null

    /**
     * 防重点击处理，子类复写[onViewClick]来响应事件
     */
    final override fun onClick(view: View) {
        if (!isFastMultiClick(view, 200)) {
            var itemPosition = adapterPosition
            if (itemPosition == RecyclerView.NO_POSITION) {
                itemPosition = layoutPosition
            }
            onViewClick(view, itemPosition, itemData)
        }
    }

    fun bind(itemPosition: Int, itemData: VO?) {
        this.itemData = itemData
        onViewBind(itemPosition, itemData)
    }

    abstract fun onViewBind(itemPosition: Int, itemData: VO?)

    /**
     * View点击回调，已防重点击处理
     * @param view
     * @param itemPosition
     * @param itemData
     */
    open fun onViewClick(view: View, itemPosition: Int, itemData: VO?) {

    }
}