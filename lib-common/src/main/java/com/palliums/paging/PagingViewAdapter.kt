package com.palliums.paging

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.palliums.R
import com.palliums.base.BaseViewHolder
import com.palliums.net.LoadState
import com.palliums.utils.RecyclerViewDataObserverProxy
import kotlinx.android.synthetic.main.item_load_more.view.*

/**
 * Created by elephant on 2019-08-14 13:58.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: PagingViewAdapter
 */
abstract class PagingViewAdapter<VO> : PagedListAdapter<VO, RecyclerView.ViewHolder> {

    private val retryCallback: () -> Unit
    private var loadMoreState: LoadState = LoadState.IDLE

    constructor(
        retryCallback: () -> Unit,
        diffCallback: DiffUtil.ItemCallback<VO>
    ) : super(diffCallback) {
        this.retryCallback = retryCallback
    }

    constructor(
        retryCallback: () -> Unit,
        differConfig: AsyncDifferConfig<VO>
    ) : super(differConfig) {
        this.retryCallback = retryCallback
    }

    final override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_load_more ->
                LoadMoreViewHolder.create(parent, retryCallback)

            else ->
                onCreateViewHolderSupport(parent, viewType)
        }
    }

    final override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val viewType = getItemViewType(position)
        when {
            viewType == R.layout.item_load_more -> {
                (holder as BaseViewHolder<LoadState>).bind(position, loadMoreState)
                holder.itemView.tag = loadMoreState
            }

            isHeaderItem(viewType) -> {
                val itemData = getHeaderItem(position, viewType)
                (holder as BaseViewHolder<Any>).bind(position, itemData)
                holder.itemView.tag = itemData
            }

            else -> {
                val itemData = getItem(position)
                (holder as BaseViewHolder<Any>).bind(position, itemData)
                holder.itemView.tag = itemData
            }
        }
    }

    final override fun getItemCount(): Int {
        return super.getItemCount() + getHeaderItemCount() + if (hasExtraRow()) 1 else 0
    }

    final override fun getItemViewType(position: Int): Int {
        return if (hasExtraRow() && position == itemCount - 1) {
            R.layout.item_load_more
        } else {
            getItemViewTypeSupport(position)
        }
    }

    override fun getItem(position: Int): VO? {
        return super.getItem(position - getHeaderItemCount())
    }

    override fun registerAdapterDataObserver(observer: RecyclerView.AdapterDataObserver) {
        val headerItemCount = getHeaderItemCount()
        if (headerItemCount <= 0) {
            super.registerAdapterDataObserver(observer)
        } else {
            super.registerAdapterDataObserver(
                RecyclerViewDataObserverProxy(
                    observer,
                    headerItemCount
                )
            )
        }
    }

    fun hasExtraRow(): Boolean {
        return loadMoreState.status != LoadState.Status.SUCCESS
                && loadMoreState.status != LoadState.Status.IDLE
    }

    fun setLoadMoreState(loadMoreState: LoadState) {
        val prevLoadMoreState = this.loadMoreState
        val hadExtraRow = hasExtraRow()

        this.loadMoreState = loadMoreState
        val hasExtraRow = hasExtraRow()

        if (hadExtraRow != hasExtraRow) {
            if (hadExtraRow) {
                notifyItemRemoved(super.getItemCount() + getHeaderItemCount())
            } else {
                notifyItemInserted(super.getItemCount() + getHeaderItemCount())
            }
        } else if (hasExtraRow && prevLoadMoreState != loadMoreState) {
            notifyItemChanged(itemCount - 1)
        }
    }

    abstract fun onCreateViewHolderSupport(parent: ViewGroup, viewType: Int): BaseViewHolder<out Any>

    open fun getItemViewTypeSupport(position: Int): Int {
        return 0
    }

    open fun isHeaderItem(viewType: Int): Boolean {
        return false
    }

    open fun getHeaderItem(position: Int, viewType: Int): Any? {
        return null
    }

    open fun getHeaderItemCount(): Int {
        return 0
    }

    class LoadMoreViewHolder(
        view: View,
        private val retryCallback: () -> Unit
    ) : BaseViewHolder<LoadState>(view) {

        companion object {
            fun create(parent: ViewGroup, retryCallback: () -> Unit): LoadMoreViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_load_more, parent, false)
                return LoadMoreViewHolder(view, retryCallback)
            }
        }

        init {
            itemView.setOnClickListener(this)
        }

        override fun onViewBind(itemPosition: Int, itemData: LoadState?) {
            itemData?.let {
                if (it.status == LoadState.Status.RUNNING) {
                    itemView.vLoadMoreProgress.visibility = View.VISIBLE
                    itemView.vLoadMoreProgress.startLoadingAnim()
                } else {
                    itemView.vLoadMoreProgress.visibility = View.GONE
                    itemView.vLoadMoreProgress.stopAnim()
                }

                itemView.vLoadMoreTips.setText(
                    when (it.status) {
                        LoadState.Status.RUNNING ->
                            R.string.common_refresh_footer_loading

                        LoadState.Status.SUCCESS_NO_MORE ->
                            R.string.common_refresh_footer_no_more

                        LoadState.Status.FAILURE -> {
                            if (it.isNoNetwork()) {
                                R.string.common_refresh_footer_no_network
                            } else {
                                R.string.common_refresh_header_failure
                            }
                        }

                        else ->
                            R.string.common_refresh_footer_finish
                    }
                )
            }
        }

        override fun onViewClick(view: View, itemPosition: Int, itemData: LoadState?) {
            itemData?.let {
                if (it.status == LoadState.Status.FAILURE) {
                    retryCallback()
                }
            }
        }
    }
}