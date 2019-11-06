package com.violas.wallet.base.paging

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.violas.wallet.R
import com.violas.wallet.base.BaseViewHolder
import com.violas.wallet.repository.http.LoadState
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

    constructor(retryCallback: () -> Unit, diffCallback: DiffUtil.ItemCallback<VO>) : super(
        diffCallback
    ) {
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
        when (getItemViewType(position)) {
            R.layout.item_load_more -> {
                (holder as BaseViewHolder<LoadState>).bind(position, loadMoreState)
                holder.itemView.tag = loadMoreState
            }

            else -> {
                val item = getItem(position)
                (holder as BaseViewHolder<VO>).bind(position, item)
                holder.itemView.tag = item
            }
        }
    }

    final override fun getItemCount(): Int {
        return super.getItemCount() + getOtherItemCount() + if (hasExtraRow()) 1 else 0
    }

    final override fun getItemViewType(position: Int): Int {
        return if (hasExtraRow() && position == itemCount - 1) {
            R.layout.item_load_more
        } else {
            getItemViewTypeSupport(position)
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
                notifyItemRemoved(super.getItemCount() + getOtherItemCount())
            } else {
                notifyItemInserted(super.getItemCount() + getOtherItemCount())
            }
        } else if (hasExtraRow && prevLoadMoreState != loadMoreState) {
            notifyItemChanged(itemCount - 1)
        }
    }

    abstract fun onCreateViewHolderSupport(parent: ViewGroup, viewType: Int): BaseViewHolder<VO>

    open fun getItemViewTypeSupport(position: Int): Int {
        return 0
    }

    open fun getOtherItemCount(): Int {
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

        override fun onViewBind(itemIndex: Int, itemDate: LoadState?) {
            itemDate?.let {
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
                            R.string.refresh_footer_loading

                        LoadState.Status.SUCCESS_NO_MORE ->
                            R.string.refresh_footer_no_more

                        LoadState.Status.FAILURE -> {
                            if (it.isNoNetwork()) {
                                R.string.refresh_footer_no_network
                            } else {
                                R.string.refresh_footer_failed
                            }
                        }

                        else ->
                            R.string.refresh_footer_finish
                    }
                )
            }
        }

        override fun onViewClick(view: View, itemIndex: Int, itemDate: LoadState?) {
            itemDate?.let {
                if (it.status == LoadState.Status.FAILURE) {
                    retryCallback()
                }
            }
        }
    }
}