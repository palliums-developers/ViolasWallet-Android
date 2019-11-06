package com.violas.wallet.base.listing

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.scwang.smartrefresh.layout.SmartRefreshLayout
import com.violas.wallet.base.ViewController
import com.violas.wallet.repository.http.LoadState
import com.violas.wallet.widget.DataLoadStatusControl
import com.violas.wallet.widget.DataLoadStatusLayout

/**
 * Created by elephant on 2019-11-05 18:20.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class ListingHandler<VO>(
    private val mAdapter: BaseListingAdapter<VO>,
    private val mViewModel: BaseListingViewModel<VO>,
    private val mLifecycleOwner: LifecycleOwner,
    private val mViewController: ViewController,
    private val vRecyclerView: RecyclerView,
    private val vRefreshLayout: SmartRefreshLayout?,
    private val vStatusLayout: DataLoadStatusLayout?
) {

    init {
        mViewModel.loadState.observe(mLifecycleOwner, Observer {
            when (it.status) {
                LoadState.Status.RUNNING -> {
                    if (mViewController.loadingUseDialog()) {
                        mViewController.showProgress()
                    } else {
                        vRefreshLayout?.autoRefreshAnimationOnly()
                    }
                }

                LoadState.Status.SUCCESS -> {
                    if (mViewController.loadingUseDialog()) {
                        mViewController.dismissProgress()
                    } else {
                        vRefreshLayout?.finishRefresh(true)
                    }
                    vStatusLayout?.showStatus(DataLoadStatusControl.Status.STATUS_NONE)
                }

                LoadState.Status.FAILED -> {
                    if (mViewController.loadingUseDialog()) {
                        mViewController.dismissProgress()
                    } else {
                        vRefreshLayout?.finishRefresh(false)
                    }

                    when {
                        mAdapter.itemCount > 0 -> {
                            vStatusLayout?.showStatus(DataLoadStatusControl.Status.STATUS_NONE)
                        }

                        it.isNoNetwork() -> {
                            vStatusLayout?.showStatus(
                                DataLoadStatusControl.Status.STATUS_NO_NETWORK,
                                it.getErrorMsg()
                            )
                        }

                        else -> {
                            vStatusLayout?.showStatus(
                                DataLoadStatusControl.Status.STATUS_FAIL,
                                it.getErrorMsg()
                            )
                        }
                    }
                }
            }
        })

        mViewModel.tipsMessage.observe(mLifecycleOwner, Observer {
            if (it.isNotEmpty()) {
                mViewController.showToast(it)
            }
        })

        mViewModel.listData.observe(mLifecycleOwner, Observer {
            mAdapter.setListData(it)
        })

        vRefreshLayout?.let {
            it.setEnableLoadMore(false)
            it.setEnableOverScrollBounce(true)
            it.setEnableOverScrollDrag(true)
            if (mViewController.loadingUseDialog()) {
                it.setEnableRefresh(false)
            }
        }

        vStatusLayout?.showStatus(DataLoadStatusControl.Status.STATUS_NONE)

        vRecyclerView.adapter = mAdapter
    }
}