package com.palliums.utils

import androidx.recyclerview.widget.RecyclerView

/**
 * Created by elephant on 2019-12-10 17:29.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class RecyclerViewDataObserverProxy(
    private val adapterDataObserver: RecyclerView.AdapterDataObserver,
    private val headerCount: Int
) : RecyclerView.AdapterDataObserver() {

    override fun onChanged() {
        adapterDataObserver.onChanged()
    }

    override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
        adapterDataObserver.onItemRangeChanged(
            if (isHeaderItem(positionStart)) positionStart else positionStart + headerCount,
            itemCount
        )
    }

    override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
        adapterDataObserver.onItemRangeChanged(
            if (isHeaderItem(positionStart)) positionStart else positionStart + headerCount,
            itemCount,
            payload
        )
    }

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        adapterDataObserver.onItemRangeInserted(
            if (isHeaderItem(positionStart)) positionStart else positionStart + headerCount,
            itemCount
        )
    }

    override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
        adapterDataObserver.onItemRangeMoved(
            if (isHeaderItem(fromPosition)) fromPosition else fromPosition + headerCount,
            if (isHeaderItem(toPosition)) toPosition else toPosition + headerCount,
            itemCount
        )
    }

    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
        adapterDataObserver.onItemRangeRemoved(
            if (isHeaderItem(positionStart)) positionStart else positionStart + headerCount,
            itemCount
        )
    }

    private fun isHeaderItem(position: Int): Boolean {
        return position < headerCount
    }
}