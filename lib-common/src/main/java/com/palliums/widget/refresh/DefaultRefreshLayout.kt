package com.palliums.widget.refresh

import android.content.Context
import android.util.AttributeSet
import com.palliums.extensions.lazyLogError
import com.scwang.smartrefresh.layout.SmartRefreshLayout
import com.scwang.smartrefresh.layout.constant.RefreshState

/**
 * Created by elephant on 2019-11-06 14:22.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 默认的刷新布局
 */
class DefaultRefreshLayout : SmartRefreshLayout, IRefreshLayout {

    companion object {
        private const val TAG = "RefreshLayout"
    }

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    /**
     * [autoRefresh] 首次加载数据后，再手动触发下拉刷新，会出现 [mState] 一直为 [RefreshState.Refreshing]，
     * OnRefreshListener 不回调，因此覆写 [overSpinner] 方法进行处理，在下拉后当 [mState] or [mViceState]
     * 处于 [RefreshState.ReleaseToRefresh] 且 [mEnableRefresh] 为 true 时就进行
     * mKernel.setState(RefreshState.Refreshing)操作
     */
    override fun overSpinner() {
        lazyLogError(TAG) { "overSpinner, state(${mState.name}), vice state(${mViceState.name})" }

        if (mState == RefreshState.TwoLevel) {
            val thisView = this
            if (mCurrentVelocity > -1000 && mSpinner > thisView.measuredHeight / 2) {
                val animator = mKernel.animSpinner(thisView.measuredHeight)
                if (animator != null) {
                    animator.duration = mFloorDuration.toLong()
                }
            } else if (mIsBeingDragged) {
                mKernel.finishTwoLevel()
            }
        } else if (mState == RefreshState.Loading
            || (mEnableFooterFollowWhenNoMoreData
                    && mFooterNoMoreData
                    && mFooterNoMoreDataEffective
                    && mSpinner < 0
                    && isEnableRefreshOrLoadMore(mEnableLoadMore))
        ) {
            if (mSpinner < -mFooterHeight) {
                mKernel.animSpinner(-mFooterHeight)
            } else if (mSpinner > 0) {
                mKernel.animSpinner(0)
            }
        } else if (mState == RefreshState.ReleaseToRefresh
            || (mViceState == RefreshState.ReleaseToRefresh
                    && mState != RefreshState.Refreshing
                    && mEnableRefresh)
        ) {
            mKernel.setState(RefreshState.Refreshing)
        } else if (mState == RefreshState.Refreshing) {
            if (mSpinner > mHeaderHeight) {
                mKernel.animSpinner(mHeaderHeight)
            } else if (mSpinner < 0) {
                mKernel.animSpinner(0)
            }
        } else if (mState == RefreshState.PullDownToRefresh) {
            mKernel.setState(RefreshState.PullDownCanceled)
        } else if (mState == RefreshState.PullUpToLoad) {
            mKernel.setState(RefreshState.PullUpCanceled)
        } else if (mState == RefreshState.ReleaseToLoad) {
            mKernel.setState(RefreshState.Loading)
        } else if (mState == RefreshState.ReleaseToTwoLevel) {
            mKernel.setState(RefreshState.TwoLevelReleased)
        } else if (mState == RefreshState.RefreshReleased) {
            if (reboundAnimator == null) {
                mKernel.animSpinner(mHeaderHeight)
            }
        } else if (mState == RefreshState.LoadReleased) {
            if (reboundAnimator == null) {
                mKernel.animSpinner(-mFooterHeight)
            }
        } else if (mSpinner != 0) {
            mKernel.animSpinner(0)
        }
    }
}