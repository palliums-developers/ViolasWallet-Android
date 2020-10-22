package com.violas.wallet.ui.main.market.pool

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.lxj.xpopup.core.BottomPopupView
import com.palliums.base.BaseViewHolder
import com.palliums.listing.ListingViewAdapter
import com.palliums.utils.DensityUtility
import com.palliums.utils.getString
import com.palliums.widget.popup.EnhancedPopupCallback
import com.violas.wallet.R
import kotlinx.android.synthetic.main.item_market_pool_liquidity_select.view.*
import kotlinx.android.synthetic.main.popup_market_pool_liquidity_select.view.*

/**
 * Created by elephant on 2020/10/22 14:29.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 资金池流动性选择弹窗
 */
class MarketPoolLiquiditySelectPopup(
    context: Context,
    private val checkedPosition: Int,
    private val dataList: MutableList<String>,
    private val selectCallback: (Int) -> Unit
) : BottomPopupView(context) {

    override fun getImplLayoutId(): Int {
        return R.layout.popup_market_pool_liquidity_select
    }

    override fun getMaxHeight(): Int {
        return (DensityUtility.getScreenHeight(context) * 0.85f).toInt()
    }

    override fun onCreate() {
        super.onCreate()

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = ViewAdapter(dataList, checkedPosition) {
            dismiss()
            selectCallback.invoke(it)
        }
    }

    override fun doAfterShow() {
        popupInfo?.xPopupCallback?.run {
            if (this is EnhancedPopupCallback) {
                onShowBefore()
            }
        }

        super.doAfterShow()
    }

    override fun doAfterDismiss() {
        popupInfo?.xPopupCallback?.run {
            if (this is EnhancedPopupCallback) {
                onDismissBefore()
            }
        }

        super.doAfterDismiss()
    }

    class ViewAdapter(
        dataList: MutableList<String>,
        private val checkedPosition: Int,
        private val selectCallback: (Int) -> Unit
    ) : ListingViewAdapter<String>(dataList) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<String> {
            return ViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_market_pool_liquidity_select,
                    parent,
                    false
                ),
                getDataList().size,
                checkedPosition,
                selectCallback
            )
        }
    }

    class ViewHolder(
        view: View,
        private val itemSize: Int,
        private val checkedPosition: Int,
        private val selectCallback: (Int) -> Unit
    ) : BaseViewHolder<String>(view) {

        init {
            itemView.setOnClickListener(this)
        }

        override fun onViewBind(itemPosition: Int, itemData: String?) {
            itemData?.let {
                itemView.tvText.text = it
                itemView.tvText.typeface = Typeface.create(
                    getString(
                        if (itemPosition == checkedPosition)
                            R.string.font_family_title
                        else
                            R.string.font_family_normal
                    ),
                    Typeface.NORMAL
                )
                itemView.vDivider.visibility =
                    if (itemPosition == itemSize - 1) View.GONE else View.VISIBLE
            }
        }

        override fun onViewClick(view: View, itemPosition: Int, itemData: String?) {
            selectCallback.invoke(itemPosition)
        }
    }
}