package com.palliums.widget.recyclerview

import androidx.recyclerview.widget.DiffUtil

/**
 * Created by elephant on 2020/8/28 11:16.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class DefaultDiffCallback<VO> : DiffUtil.ItemCallback<VO>() {
    override fun areContentsTheSame(oldItem: VO, newItem: VO): Boolean {
        return oldItem.hashCode() == newItem.hashCode()
    }

    override fun areItemsTheSame(oldItem: VO, newItem: VO): Boolean {
        return oldItem == newItem
    }
}