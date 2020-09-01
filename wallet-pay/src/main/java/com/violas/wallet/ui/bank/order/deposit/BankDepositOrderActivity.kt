package com.violas.wallet.ui.bank.order.deposit

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseViewHolder
import com.palliums.paging.PagingViewAdapter
import com.palliums.paging.PagingViewModel
import com.palliums.utils.getResourceId
import com.palliums.utils.start
import com.palliums.widget.status.IStatusLayout
import com.violas.wallet.R
import com.violas.wallet.repository.http.bank.AccountDepositInfoDTO
import com.violas.wallet.ui.bank.order.BaseBankOrderActivity
import com.violas.wallet.ui.bank.record.deposit.BankDepositRecordActivity
import com.violas.wallet.utils.convertAmountToDisplayAmountStr
import com.violas.wallet.utils.keepTwoDecimals
import com.violas.wallet.utils.loadCircleImage
import kotlinx.android.synthetic.main.activity_bank_order.*
import kotlinx.android.synthetic.main.item_bank_curr_deposit.view.*
import kotlinx.coroutines.launch

/**
 * Created by elephant on 2020/8/24 11:19.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 银行存款订单页面
 */
class BankDepositOrderActivity : BaseBankOrderActivity<AccountDepositInfoDTO>() {

    private val viewModel by lazy {
        ViewModelProvider(this).get(BankDepositOrderViewModel::class.java)
    }
    private val viewAdapter by lazy {
        ViewAdapter { depositInfo, position ->
            // TODO 提取操作
            showToast("提取操作")
        }
    }

    override fun getViewModel(): PagingViewModel<AccountDepositInfoDTO> {
        return viewModel
    }

    override fun getViewAdapter(): PagingViewAdapter<AccountDepositInfoDTO> {
        return viewAdapter
    }

    override fun onTitleRightViewClick() {
        Intent(this, BankDepositRecordActivity::class.java).start(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.deposit_order)
        ivIcon.setImageResource(getResourceId(R.attr.bankDepositOrderIcon, this))
        tvLabel.setText(R.string.current_deposit)

        launch {
            val hasAccount = viewModel.initAddress()
            if (hasAccount) {
                viewModel.start()
            } else {
                refreshLayout.setEnableRefresh(false)
                statusLayout.showStatus(IStatusLayout.Status.STATUS_EMPTY)
            }
        }
    }

    class ViewAdapter(
        private val withdrawalCallback: (AccountDepositInfoDTO, Int) -> Unit
    ) : PagingViewAdapter<AccountDepositInfoDTO>() {

        override fun onCreateViewHolderSupport(
            parent: ViewGroup,
            viewType: Int
        ): BaseViewHolder<out Any> {
            return ViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_bank_curr_deposit, parent, false
                ),
                withdrawalCallback
            )
        }
    }

    class ViewHolder(
        view: View,
        private val withdrawalCallback: (AccountDepositInfoDTO, Int) -> Unit
    ) : BaseViewHolder<AccountDepositInfoDTO>(view) {

        init {
            itemView.tvWithdrawal.setOnClickListener(this)
        }

        override fun onViewBind(itemPosition: Int, itemData: AccountDepositInfoDTO?) {
            itemData?.let {
                itemView.ivCoinLogo.loadCircleImage(
                    it.productLogo,
                    getResourceId(R.attr.iconCoinDefLogo, itemView.context)
                )
                itemView.tvCoinName.text = it.coinName
                itemView.tvPrincipal.text = convertAmountToDisplayAmountStr(it.principal)
                itemView.tvEarnings.text = convertAmountToDisplayAmountStr(it.totalEarnings)
                itemView.tvDepositYield.text = "${keepTwoDecimals(it.depositYield)}%"
            }
        }

        override fun onViewClick(view: View, itemPosition: Int, itemData: AccountDepositInfoDTO?) {
            itemData?.let {
                when (view) {
                    itemView.tvWithdrawal -> {
                        withdrawalCallback.invoke(it, itemPosition)
                    }
                }
            }
        }
    }
}