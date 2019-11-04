package com.violas.wallet.paging

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.violas.wallet.R
import com.violas.wallet.base.BaseActivity
import com.violas.wallet.base.BaseViewHolder
import com.violas.wallet.repository.http.LoadState
import com.violas.wallet.widget.PullRefreshLayoutProgress

/**
 * Created by elephant on 2019-08-14 13:58.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: PagingAdapter
 */
abstract class PagingAdapter<Vo> : PagedListAdapter<Vo, RecyclerView.ViewHolder> {

    private val retryCallback: () -> Unit
    private var loadMoreState: LoadState = LoadState.IDLE

    constructor(retryCallback: () -> Unit, diffCallback: DiffUtil.ItemCallback<Vo>) : super(
        diffCallback
    ) {
        this.retryCallback = retryCallback
    }

    constructor(
        retryCallback: () -> Unit,
        differConfig: AsyncDifferConfig<Vo>
    ) : super(differConfig) {
        this.retryCallback = retryCallback
    }

    final override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_load_more -> LoadMoreViewHolder.create(parent, retryCallback)
            else -> onCreateContentViewHolder(parent, viewType)
        }
    }

    final override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            R.layout.item_load_more -> {
                (holder as LoadMoreViewHolder).bind(loadMoreState)
                holder.itemView.tag = loadMoreState
            }
            else -> {
                val item = getItem(position)
                (holder as BaseViewHolder<Vo>).bind(position, item)
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
            getItemType(position)
        }
    }

    private fun hasExtraRow(): Boolean {
        return loadMoreState.status != LoadState.Status.SUCCESS
                && loadMoreState.status != LoadState.Status.IDLE
    }

    fun setLoadMoreState(newLoadMoreState: LoadState) {
        val prevLoadMoreState = this.loadMoreState
        val hadExtraRow = hasExtraRow()
        this.loadMoreState = newLoadMoreState
        val hasExtraRow = hasExtraRow()
        if (hadExtraRow != hasExtraRow) {
            if (hadExtraRow) {
                notifyItemRemoved(super.getItemCount() + getOtherItemCount())
            } else {
                notifyItemInserted(super.getItemCount() + getOtherItemCount())
            }
        } else if (hasExtraRow && prevLoadMoreState != newLoadMoreState) {
            notifyItemChanged(itemCount - 1)
        }
    }

    abstract fun onCreateContentViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<Vo>

    fun getItemType(position: Int): Int {
        return 0
    }

    fun getOtherItemCount(): Int {
        return 0
    }

    class LoadMoreViewHolder(
        view: View,
        private val retryCallback: () -> Unit
    ) : RecyclerView.ViewHolder(view) {

        companion object {
            fun create(parent: ViewGroup, retryCallback: () -> Unit): LoadMoreViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_load_more, parent, false)
                return LoadMoreViewHolder(view, retryCallback)
            }
        }

        private val process: PullRefreshLayoutProgress =
            itemView.findViewById(R.id.load_more_progress)
        private val tips: TextView = itemView.findViewById(R.id.load_more_tips)
        private var loadMoreState: LoadState = LoadState.IDLE

        init {
            itemView.setOnClickListener {
                if (!BaseActivity.isFastMultiClick(it)
                    && loadMoreState.status == LoadState.Status.FAILED
                ) {
                    retryCallback()
                }
            }
        }

        fun bind(loadMoreState: LoadState) {
            this.loadMoreState = loadMoreState

            if (loadMoreState.status == LoadState.Status.RUNNING) {
                process.visibility = View.VISIBLE
                process.startLoadingAnim()
            } else {
                process.visibility = View.GONE
                process.stopAnim()
            }

            tips.setText(
                when (loadMoreState.status) {
                    LoadState.Status.RUNNING ->
                        R.string.refresh_footer_loading
                    LoadState.Status.SUCCESS_NO_MORE ->
                        R.string.refresh_footer_no_more
                    LoadState.Status.FAILED -> {
                        if (loadMoreState.isNoNetwork()) {
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
}