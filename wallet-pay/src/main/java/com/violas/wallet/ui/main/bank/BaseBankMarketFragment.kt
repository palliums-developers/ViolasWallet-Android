package com.violas.wallet.ui.main.bank

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.palliums.base.BaseFragment
import com.palliums.base.BaseViewHolder
import com.palliums.listing.ListingViewAdapter
import com.palliums.utils.DensityUtility
import com.palliums.utils.getResourceId
import com.palliums.utils.isNetworkConnected
import com.palliums.widget.dividers.RecyclerViewItemDividers
import com.palliums.widget.status.IStatusLayout
import com.violas.wallet.R
import com.violas.wallet.viewModel.WalletAppViewModel
import kotlinx.android.synthetic.main.layout_list.*

/**
 * Created by elephant on 2020/8/31 11:42.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 银行存款/借款市场公共视图
 */
abstract class BaseBankMarketFragment<VO> : BaseFragment() {

    private val appViewModel by lazy {
        WalletAppViewModel.getViewModelInstance(requireContext())
    }

    private val viewAdapter by lazy {
        ViewAdapter<VO>(
            bindViewCallback = { itemData, itemView ->
                onBindView(itemData, itemView)
            }
        ) { itemData, itemPosition ->
            if (appViewModel.isExistsAccount()) {
                onItemClick(itemData, itemPosition)
            } else {
                showToast(R.string.tips_create_or_import_wallet)
            }
        }
    }

    override fun getLayoutResId(): Int {
        return R.layout.layout_list
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        statusLayout.setImageWithStatus(
            IStatusLayout.Status.STATUS_EMPTY,
            getResourceId(R.attr.bankListEmptyDataBg, requireContext())
        )
        statusLayout.showStatus(IStatusLayout.Status.STATUS_LOADING)

        recyclerView.addItemDecoration(
            RecyclerViewItemDividers(
                top = DensityUtility.dp2px(requireContext(), 5),
                bottom = DensityUtility.dp2px(requireContext(), 5),
                left = DensityUtility.dp2px(requireContext(), 5),
                right = DensityUtility.dp2px(requireContext(), 5)
            )
        )
        recyclerView.adapter = viewAdapter
    }

    fun setData(data: List<VO>?) {
        if (data == null) {
            statusLayout.showStatus(
                when {
                    !viewAdapter.getDataList().isNullOrEmpty() ->
                        IStatusLayout.Status.STATUS_NONE
                    isNetworkConnected() ->
                        IStatusLayout.Status.STATUS_FAILURE
                    else ->
                        IStatusLayout.Status.STATUS_NO_NETWORK
                }
            )
            return
        }

        statusLayout.showStatus(
            if (data.isNotEmpty())
                IStatusLayout.Status.STATUS_NONE
            else
                IStatusLayout.Status.STATUS_EMPTY
        )
        viewAdapter.setDataList(data)
    }

    abstract fun onBindView(itemData: VO, itemView: View)

    abstract fun onItemClick(itemData: VO, itemPosition: Int)

    class ViewAdapter<VO>(
        private val bindViewCallback: (VO, View) -> Unit,
        private val itemClickCallback: (VO, Int) -> Unit
    ) : ListingViewAdapter<VO>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<VO> {
            return ViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_home_bank_product, parent, false
                ),
                bindViewCallback,
                itemClickCallback
            )
        }
    }

    class ViewHolder<VO>(
        view: View,
        private val bindViewCallback: (VO, View) -> Unit,
        private val itemClickCallback: (VO, Int) -> Unit
    ) : BaseViewHolder<VO>(view) {

        init {
            itemView.setOnClickListener(this)
        }

        override fun onViewBind(itemPosition: Int, itemData: VO?) {
            itemData?.let { bindViewCallback.invoke(it, itemView) }
        }

        override fun onViewClick(view: View, itemPosition: Int, itemData: VO?) {
            itemData?.let { itemClickCallback.invoke(it, itemPosition) }
        }
    }
}