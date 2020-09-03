package com.violas.wallet.ui.bank.record

import android.animation.ObjectAnimator
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.enums.PopupAnimation
import com.palliums.utils.getResourceId
import com.palliums.widget.popup.EnhancedPopupCallback
import com.palliums.widget.refresh.IRefreshLayout
import com.palliums.widget.status.IStatusLayout
import com.violas.wallet.R
import com.violas.wallet.base.BasePagingActivity
import com.violas.wallet.viewModel.WalletAppViewModel
import kotlinx.android.synthetic.main.activity_bank_record.*

/**
 * Created by elephant on 2020/8/25 15:25.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 银行存款/借款记录公共页面
 */
abstract class BaseBankRecordActivity<VO> : BasePagingActivity<VO>() {

    private val coinFilterArrowAnimator by lazy {
        ObjectAnimator.ofFloat(ivCoinFilterArrow, "rotation", 0F, 180F)
            .setDuration(360)
    }
    private val stateFilterArrowAnimator by lazy {
        ObjectAnimator.ofFloat(ivStateFilterArrow, "rotation", 0F, 180F)
            .setDuration(360)
    }

    override fun getTitleStyle(): Int {
        return PAGE_STYLE_CUSTOM
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_bank_record
    }

    override fun getRecyclerView(): RecyclerView {
        return recyclerView
    }

    override fun getRefreshLayout(): IRefreshLayout? {
        return refreshLayout
    }

    override fun getStatusLayout(): IStatusLayout? {
        return statusLayout
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitleLeftImageResource(getResourceId(R.attr.iconBackTertiary, this))
        mPagingHandler.init()
        setStatusLayout(false)

        llCoinFilter.setOnClickListener {
            loadFilterDataOrShowFilterPopup(true)
        }
        llStateFilter.setOnClickListener {
            loadFilterDataOrShowFilterPopup(false)
        }

        getFilterDataLiveData(true).observe(this, Observer {
            showFilterPopup(true, it)
        })
        getFilterDataLiveData(false).observe(this, Observer {
            showFilterPopup(false, it)
        })

        getCurrFilterLiveData(true).observe(this, Observer {
            tvCoinFilterText.text = it?.second ?: getString(R.string.all_currencies)
        })
        getCurrFilterLiveData(false).observe(this, Observer {
            tvStateFilterText.text = it?.second ?: getString(R.string.all_state)
        })

        getCurrFilterLiveData(true).value = null
        getCurrFilterLiveData(false).value = null
    }

    private fun setStatusLayout(search: Boolean) {
        statusLayout.setTipsWithStatus(
            IStatusLayout.Status.STATUS_EMPTY,
            if (search) R.string.no_matching_search_results else R.string.common_status_empty
        )
        statusLayout.setImageWithStatus(
            IStatusLayout.Status.STATUS_EMPTY,
            getResourceId(
                if (search) R.attr.bankSearchEmptyDataBg else R.attr.bankListEmptyDataBg,
                this
            )
        )
    }

    private fun loadFilterDataOrShowFilterPopup(coinFilter: Boolean) {
        val filterData = getFilterDataLiveData(coinFilter).value
        if (filterData.isNullOrEmpty()) {
            loadFilterData(coinFilter)
        } else {
            showFilterPopup(coinFilter, filterData)
        }
    }

    private fun showFilterPopup(coinFilter: Boolean, filterData: MutableList<String>) {
        if (coinFilter)
            coinFilterArrowAnimator.start()
        else
            stateFilterArrowAnimator.start()

        val currPosition = getCurrFilterLiveData(coinFilter).value?.first ?: 0

        XPopup.Builder(this)
            .hasShadowBg(false)
            .popupAnimation(PopupAnimation.ScrollAlphaFromTop)
            .atView(if (coinFilter) llCoinFilter else llStateFilter)
            .setPopupCallback(
                object : EnhancedPopupCallback() {
                    override fun onDismissBefore() {
                        if (coinFilter)
                            coinFilterArrowAnimator.reverse()
                        else
                            stateFilterArrowAnimator.reverse()
                    }
                }
            )
            .asCustom(
                BankRecordFilterPopup(
                    this,
                    filterData,
                    currPosition
                ) { position, text ->
                    // 选择结果处理
                    if (position != currPosition) {
                        val currPositionAnother =
                            getCurrFilterLiveData(!coinFilter).value?.first ?: 0
                        if (currPosition == 0 && currPositionAnother == 0) {
                            setStatusLayout(true)
                        } else if (position == 0 && currPositionAnother == 0) {
                            setStatusLayout(false)
                        }

                        onChangeFilter(
                            coinFilter,
                            if (position != 0) Pair(position, text) else null
                        )
                    }
                }
            )
            .show()
    }

    abstract fun getCurrFilterLiveData(coinFilter: Boolean): MutableLiveData<Pair<Int, String>?>

    abstract fun getFilterDataLiveData(coinFilter: Boolean): MutableLiveData<MutableList<String>>

    abstract fun loadFilterData(coinFilter: Boolean)

    private fun onChangeFilter(coinFilter: Boolean, filter: Pair<Int, String>?) {
        getCurrFilterLiveData(coinFilter).value = filter
        if (WalletAppViewModel.getViewModelInstance().isExistsAccount()) {
            mPagingHandler.restart()
        }
    }
}