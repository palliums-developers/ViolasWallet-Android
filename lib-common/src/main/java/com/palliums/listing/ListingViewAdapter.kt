package com.palliums.listing

import androidx.recyclerview.widget.RecyclerView
import com.palliums.base.BaseViewHolder

/**
 * Created by elephant on 2019-11-05 15:17.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 列表适配器基类
 */
abstract class ListingViewAdapter<VO>(dataList: MutableList<VO> = ArrayList()) :
    RecyclerView.Adapter<BaseViewHolder<VO>>() {

    protected var mDataList = dataList

    override fun getItemCount(): Int {
        return mDataList.size
    }

    override fun onBindViewHolder(holder: BaseViewHolder<VO>, position: Int) {
        holder.bind(position, mDataList[position])
    }

    fun setDataList(dataList: MutableList<VO>, notify: Boolean = true) {
        if (this.mDataList.isEmpty() && dataList.isEmpty()) {
            this.mDataList = dataList
            return
        }

        this.mDataList = dataList

        if (notify) {
            notifyDataSetChanged()
        }
    }

    fun getDataList(): MutableList<VO> {
        return this.mDataList
    }

    fun addData(data: VO, notify: Boolean = true) {
        this.mDataList.add(data)

        if (notify) {
            notifyItemInserted(mDataList.size - 1)
        }
    }

    fun removeData(position: Int, notify: Boolean = true) {
        if (position < 0 || position > mDataList.size - 1) {
            return
        }

        this.mDataList.removeAt(position)

        if (notify) {
            notifyItemRemoved(position)
        }
    }

    fun removeData(data: VO, notify: Boolean = true) {
        if (mDataList.isEmpty()) {
            return
        }

        val position = mDataList.indexOf(data)
        if (position >= 0) {
            this.mDataList.remove(data)

            if (notify) {
                notifyItemRemoved(position)
            }
        }
    }
}