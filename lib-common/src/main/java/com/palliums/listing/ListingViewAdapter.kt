package com.palliums.listing

import androidx.recyclerview.widget.RecyclerView
import com.palliums.base.BaseViewHolder

/**
 * Created by elephant on 2019-11-05 15:17.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 列表适配器基类
 */
abstract class ListingViewAdapter<VO> : RecyclerView.Adapter<BaseViewHolder<VO>>() {

    private var listData = mutableListOf<VO>()

    override fun getItemCount(): Int {
        return listData.size
    }

    override fun onBindViewHolder(holder: BaseViewHolder<VO>, position: Int) {
        holder.bind(position, listData[position])
    }

    fun setListData(listData: List<VO>) {
        this.listData.clear()
        this.listData.addAll(listData)
        notifyDataSetChanged()
    }
}