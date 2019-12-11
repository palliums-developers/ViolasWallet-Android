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
            getRealPosition(positionStart, itemCount),
            itemCount
        )
    }

    override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
        adapterDataObserver.onItemRangeChanged(
            getRealPosition(positionStart, itemCount),
            itemCount,
            payload
        )
    }

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        adapterDataObserver.onItemRangeInserted(
            getRealPosition(positionStart, itemCount),
            itemCount
        )
    }

    override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
        adapterDataObserver.onItemRangeMoved(
            getRealPosition(fromPosition, itemCount),
            getRealPosition(toPosition, itemCount),
            itemCount
        )
    }

    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
        adapterDataObserver.onItemRangeRemoved(
            getRealPosition(positionStart, itemCount),
            itemCount
        )
    }

    private fun getRealPosition(position: Int, itemCount: Int): Int {
        return if (itemCount == 1 && position < headerCount) {
            position
        } else {
            position + headerCount
        }
    }
}