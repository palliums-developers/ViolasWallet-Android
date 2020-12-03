package com.palliums.listing

import androidx.lifecycle.LifecycleOwner
import com.palliums.base.ViewController
import com.palliums.net.LoadState
import com.palliums.widget.status.IStatusLayout

/**
 * Created by elephant on 2019-11-05 18:20.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class ListingHandler<VO>(
    private val mLifecycleOwner: LifecycleOwner,
    private val mViewController: ViewController,
    private val mListingController: ListingController<VO>
) {

    fun init() {
        mListingController.getListingViewModel().listData.observe(mLifecycleOwner) {
            mListingController.getListingViewAdapter().setDataList(it)
        }

        mListingController.getListingViewModel().loadState.observe(mLifecycleOwner) {
            when (it.peekData().status) {
                LoadState.Status.RUNNING -> {
                    if (mListingController.loadingUseDialog()) {
                        mViewController.showProgress()
                    } else {
                        //mListingController.getRefreshLayout()?.autoRefreshAnimationOnly()
                    }
                }

                LoadState.Status.SUCCESS,
                LoadState.Status.SUCCESS_NO_MORE -> {
                    if (mListingController.loadingUseDialog()) {
                        mViewController.dismissProgress()
                    } else {
                        mListingController.getRefreshLayout()?.run {
                            finishRefresh(true)
                            setEnableRefresh(mListingController.enableRefresh())
                        }
                    }

                    mListingController.getStatusLayout()?.showStatus(
                        IStatusLayout.Status.STATUS_NONE
                    )
                }

                LoadState.Status.SUCCESS_EMPTY -> {
                    if (mListingController.loadingUseDialog()) {
                        mViewController.dismissProgress()
                    } else {
                        mListingController.getRefreshLayout()?.run {
                            finishRefresh(true)
                            setEnableRefresh(mListingController.enableRefresh())
                        }
                    }

                    mListingController.getStatusLayout()?.showStatus(
                        IStatusLayout.Status.STATUS_EMPTY
                    )
                }

                LoadState.Status.FAILURE -> {
                    if (mListingController.loadingUseDialog()) {
                        mViewController.dismissProgress()
                    } else {
                        mListingController.getRefreshLayout()?.run {
                            finishRefresh(false)
                            setEnableRefresh(true)
                        }
                    }

                    when {
                        mListingController.getListingViewAdapter().itemCount > 0 -> {
                            mListingController.getStatusLayout()?.showStatus(
                                IStatusLayout.Status.STATUS_NONE
                            )
                        }

                        it.peekData().isNoNetwork() -> {
                            mListingController.getStatusLayout()?.showStatus(
                                IStatusLayout.Status.STATUS_NO_NETWORK,
                                it.peekData().getErrorMsg()
                            )
                        }

                        else -> {
                            mListingController.getStatusLayout()?.showStatus(
                                IStatusLayout.Status.STATUS_FAILURE,
                                it.peekData().getErrorMsg()
                            )
                        }
                    }
                }
            }
        }

        mListingController.getListingViewModel().tipsMessage.observe(mLifecycleOwner) {
            it.getDataIfNotHandled()?.let { msg ->
                if (msg.isNotEmpty()) {
                    mViewController.showToast(msg)
                }
            }
        }

        mListingController.getRefreshLayout()?.let {
            it.setEnableRefresh(false)
            it.setEnableLoadMore(false)
            it.setEnableOverScrollDrag(true)
            it.setEnableOverScrollBounce(false)
        }

        if (mListingController.loadingUseDialog()) {
            mListingController.getStatusLayout()?.showStatus(IStatusLayout.Status.STATUS_NONE)
        } else {
            mListingController.getStatusLayout()?.showStatus(IStatusLayout.Status.STATUS_LOADING)
        }

        mListingController.getRecyclerView().adapter = mListingController.getListingViewAdapter()
    }
}