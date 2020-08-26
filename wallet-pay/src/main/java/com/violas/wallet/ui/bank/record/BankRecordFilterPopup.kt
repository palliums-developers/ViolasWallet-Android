package com.violas.wallet.ui.bank.record

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.palliums.base.BaseViewHolder
import com.palliums.listing.ListingViewAdapter
import com.palliums.utils.DensityUtility
import com.palliums.utils.getColorByAttrId
import com.palliums.widget.popup.EnhancedAttachPopupView
import com.violas.wallet.R
import kotlinx.android.synthetic.main.item_market_select_popup.view.*
import kotlinx.android.synthetic.main.popup_market_select.view.*

/**
 * Created by elephant on 2020/6/29 19:16.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 银行记录过滤器弹窗
 */
@SuppressLint("ViewConstructor")
class BankRecordFilterPopup(
    context: Context,
    private val dataList: MutableList<String>,
    private val checkedPosition: Int,
    private val selectCallback: (Int, String) -> Unit
) : EnhancedAttachPopupView(context) {

    override fun getImplLayoutId(): Int {
        return R.layout.popup_bank_record_filter
    }

    override fun initPopupContent() {
        super.initPopupContent()
        if (!popupInfo.hasShadowBg && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            attachPopupContainer.elevation = DensityUtility.dp2px(context, 5).toFloat()
        }
    }

    override fun onCreate() {
        super.onCreate()
        recyclerView.layoutManager = object : LinearLayoutManager(context) {
            override fun setMeasuredDimension(childrenBounds: Rect?, wSpec: Int, hSpec: Int) {
                super.setMeasuredDimension(
                    childrenBounds,
                    wSpec,
                    MeasureSpec.makeMeasureSpec(
                        DensityUtility.dp2px(context, 120),
                        MeasureSpec.AT_MOST
                    )
                )
            }
        }
        recyclerView.adapter = ViewAdapter(dataList, checkedPosition) { position, text ->
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
                    R.layout.item_bank_record_filter_popup,
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
                if (checkedPosition == itemPosition) {
                    itemView.tvText.setBackgroundColor(
                        getColorByAttrId(
                            R.attr.bankFilterPopupSelectedBgColor,
                            itemView.context
                        )
                    )
                    itemView.tvText.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
                } else {
                    itemView.tvText.background = null
                    itemView.tvText.setTypeface(Typeface.DEFAULT, Typeface.NORMAL)
                }
            }
        }

        override fun onViewClick(view: View, itemPosition: Int, itemData: String?) {
            itemData?.let {
                selectCallback.invoke(itemPosition, it)
            }
        }
    }
}