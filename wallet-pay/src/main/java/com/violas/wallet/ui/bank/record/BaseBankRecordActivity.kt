package com.violas.wallet.ui.bank.record

import android.animation.ObjectAnimator
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.lxj.xpopup.XPopup
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

    private var coinFilterPopup: BankRecordCoinFilterPopup? = null
    private var stateFilterPopup: BankRecordStateFilterPopup? = null

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

        mPagingHandler.init()
        setStatusLayout(false)

        llCoinFilter.setOnClickListener {
            if (coinFilterPopup?.isShow == true) {
                coinFilterPopup?.dismiss()
                return@setOnClickListener
            }
            loadFilterDataOrShowFilterPopup(true)
        }
        llStateFilter.setOnClickListener {
            if (stateFilterPopup?.isShow == true) {
                stateFilterPopup?.dismiss()
                return@setOnClickListener
            }
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
                if (search) R.attr.bgSearchEmptyData else R.attr.bgLoadEmptyData,
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

        if (coinFilter) {
            coinFilterPopup = XPopup.Builder(this)
                .atView(llCoinFilter)
                .setPopupCallback(
                    object : EnhancedPopupCallback() {
                        override fun onDismissBefore() {
                            coinFilterArrowAnimator.reverse()
                        }

                        override fun onDismiss() {
                            coinFilterPopup = null
                        }
                    }
                )
                .asCustom(
                    BankRecordCoinFilterPopup(
                        this,
                        filterData,
                        currPosition
                    ) { selPosition, text ->
                        onSelectCallback(coinFilter, currPosition, selPosition, text)
                    }
                )
                .show() as BankRecordCoinFilterPopup
        } else {
            stateFilterPopup = XPopup.Builder(this)
                .atView(llStateFilter)
                .setPopupCallback(
                    object : EnhancedPopupCallback() {
                        override fun onDismissBefore() {
                            stateFilterArrowAnimator.reverse()
                        }

                        override fun onDismiss() {
                            stateFilterPopup = null
                        }
                    }
                )
                .asCustom(
                    BankRecordStateFilterPopup(
                        this,
                        filterData,
                        currPosition
                    ) { selPosition, text ->
                        onSelectCallback(coinFilter, currPosition, selPosition, text)
                    }
                )
                .show() as BankRecordStateFilterPopup
        }
    }

    private val onSelectCallback: (Boolean, Int, Int, String) -> Unit =
        { coinFilter, currPosition, selPosition, text ->
            // 选择结果处理
            if (selPosition != currPosition) {
                val currPositionAnother =
                    getCurrFilterLiveData(!coinFilter).value?.first ?: 0
                if (currPosition == 0 && currPositionAnother == 0) {
                    setStatusLayout(true)
                } else if (selPosition == 0 && currPositionAnother == 0) {
                    setStatusLayout(false)
                }

                onChangeFilter(
                    coinFilter,
                    if (selPosition != 0) Pair(selPosition, text) else null
                )
            }
        }

    abstract fun getCurrFilterLiveData(coinFilter: Boolean): MutableLiveData<Pair<Int, String>?>

    abstract fun getFilterDataLiveData(coinFilter: Boolean): MutableLiveData<MutableList<String>>

    abstract fun loadFilterData(coinFilter: Boolean)

    private fun onChangeFilter(coinFilter: Boolean, filter: Pair<Int, String>?) {
        getCurrFilterLiveData(coinFilter).value = filter
        if (WalletAppViewModel.getViewModelInstance().isExistsAccount()) {
            mPagingHandler.restart()
        } else {
            statusLayout.showStatus(IStatusLayout.Status.STATUS_EMPTY)
        }
    }
}