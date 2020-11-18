package com.violas.wallet.widget.popup

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.palliums.base.BaseViewHolder
import com.palliums.listing.ListingViewAdapter
import com.palliums.widget.popup.EnhancedAttachPopupView
import com.violas.wallet.R
import kotlinx.android.synthetic.main.item_menu_popup.view.*
import kotlinx.android.synthetic.main.popup_menu.view.*

/**
 * Created by elephant on 2020/8/21 16:00.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
@SuppressLint("ViewConstructor")
class MenuPopup(
    context: Context,
    private val dataList: MutableList<Pair<Int, Int>>,
    private val selectCallback: (Int) -> Unit
) : EnhancedAttachPopupView(context) {

    override fun getImplLayoutId(): Int {
        return R.layout.popup_menu
    }

    override fun initPopupContent() {
        super.initPopupContent()
        if (!popupInfo.hasShadowBg && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            attachPopupContainer.elevation = 0f
        }
    }

    override fun onCreate() {
        super.onCreate()
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = ViewAdapter(dataList) {
            dismissWith {
                selectCallback.invoke(it)
            }
        }
    }

    class ViewAdapter(
        dataList: MutableList<Pair<Int, Int>>,
        private val selectCallback: (Int) -> Unit
    ) : ListingViewAdapter<Pair<Int, Int>>(dataList) {

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): BaseViewHolder<Pair<Int, Int>> {
            return ViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_menu_popup,
                    parent,
                    false
                ),
                selectCallback
            )
        }
    }

    class ViewHolder(
        view: View,
        private val selectCallback: (Int) -> Unit
    ) : BaseViewHolder<Pair<Int, Int>>(view) {

        init {
            itemView.setOnClickListener(this)
        }

        override fun onViewBind(itemPosition: Int, itemData: Pair<Int, Int>?) {
            itemData?.let {
                itemView.ivIcon.setImageResource(it.first)
                itemView.tvText.setText(it.second)
            }
        }

        override fun onViewClick(view: View, itemPosition: Int, itemData: Pair<Int, Int>?) {
            selectCallback.invoke(itemPosition)
        }
    }
}