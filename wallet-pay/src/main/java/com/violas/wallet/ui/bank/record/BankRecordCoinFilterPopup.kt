package com.violas.wallet.ui.bank.record

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.lxj.xpopup.impl.PartShadowPopupView
import com.palliums.base.BaseViewHolder
import com.palliums.listing.ListingViewAdapter
import com.palliums.utils.DensityUtility
import com.palliums.widget.popup.EnhancedPopupCallback
import com.violas.wallet.R
import kotlinx.android.synthetic.main.item_bank_record_coin_filter.view.*
import kotlinx.android.synthetic.main.popup_bank_record_coin_filter.view.*

/**
 * Created by elephant on 2020/10/21 16:30.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 银行存款借款币种过滤器弹窗
 */
@SuppressLint("ViewConstructor")
class BankRecordCoinFilterPopup(
    context: Context,
    private val dataList: MutableList<String>,
    private val checkedPosition: Int,
    private val selectCallback: (Int, String) -> Unit
) : PartShadowPopupView(context) {

    override fun getImplLayoutId(): Int {
        return R.layout.popup_bank_record_coin_filter
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


    override fun onCreate() {
        super.onCreate()
        // 设置 recyclerView 最大高度
        /*recyclerView.layoutManager = object : LinearLayoutManager(context) {
            override fun setMeasuredDimension(childrenBounds: Rect?, wSpec: Int, hSpec: Int) {
                super.setMeasuredDimension(
                    childrenBounds,
                    wSpec,
                    MeasureSpec.makeMeasureSpec(
                        DensityUtility.dp2px(context, 400),
                        MeasureSpec.AT_MOST
                    )
                )
            }
        }*/
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter =
            ViewAdapter(dataList, checkedPosition) { position, text ->
                dismiss()
                selectCallback.invoke(position, text)
            }
    }

    class ViewAdapter(
        dataList: MutableList<String>,
        private val checkedPosition: Int,
        private val selectCallback: (Int, String) -> Unit
    ) : ListingViewAdapter<String>(dataList) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<String> {
            return ViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_bank_record_coin_filter,
                    parent,
                    false
                ), checkedPosition, selectCallback
            )
        }
    }

    class ViewHolder(
        view: View,
        private val checkedPosition: Int,
        private val selectCallback: (Int, String) -> Unit
    ) : BaseViewHolder<String>(view) {

        init {
            itemView.setOnClickListener(this)
        }

        override fun onViewBind(itemPosition: Int, itemData: String?) {
            itemData?.let {
                itemView.tvText.text = it
                itemView.tvText.typeface =
                    if (itemPosition == checkedPosition) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }
        }

        override fun onViewClick(view: View, itemPosition: Int, itemData: String?) {
            itemData?.let {
                selectCallback.invoke(itemPosition, it)
            }
        }
    }
}