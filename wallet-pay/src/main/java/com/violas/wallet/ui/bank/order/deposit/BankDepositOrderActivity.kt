package com.violas.wallet.ui.bank.order.deposit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseViewHolder
import com.palliums.extensions.getShowErrorMessage
import com.palliums.extensions.show
import com.palliums.paging.PagingViewAdapter
import com.palliums.paging.PagingViewModel
import com.palliums.utils.getResourceId
import com.palliums.utils.start
import com.palliums.widget.status.IStatusLayout
import com.violas.wallet.R
import com.violas.wallet.repository.http.bank.DepositInfoDTO
import com.violas.wallet.ui.bank.order.BaseBankOrderActivity
import com.violas.wallet.ui.bank.record.deposit.BankDepositRecordActivity
import com.violas.wallet.ui.bank.withdrawal.BankWithdrawalDialog
import com.violas.wallet.utils.convertAmountToDisplayAmountStr
import com.violas.wallet.utils.convertRateToPercentage
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
class BankDepositOrderActivity : BaseBankOrderActivity<DepositInfoDTO>() {

    companion object {

        fun start(context: Context) {
            Intent(context, BankDepositOrderActivity::class.java).start(context)
        }
    }

    private val withdrawalCallback: (DepositInfoDTO, Int) -> Unit = { depositInfo, position ->
        launch {
            try {
                showProgress()
                val depositDetails = getViewModel().getDepositDetails(depositInfo)
                dismissProgress()
                BankWithdrawalDialog.newInstance(depositInfo.productId, depositDetails)
                    .show(supportFragmentManager)
            } catch (e: Exception) {
                showToast(e.getShowErrorMessage(false))
                dismissProgress()
            }
        }
    }

    override fun lazyInitPagingViewModel(): PagingViewModel<DepositInfoDTO> {
        return ViewModelProvider(this).get(BankDepositOrderViewModel::class.java)
    }

    override fun lazyInitPagingViewAdapter(): PagingViewAdapter<DepositInfoDTO> {
        return ViewAdapter(withdrawalCallback)
    }

    private fun getViewModel(): BankDepositOrderViewModel {
        return getPagingViewModel() as BankDepositOrderViewModel
    }

    override fun onTitleRightViewClick() {
        BankDepositRecordActivity.start(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.deposit_order)
        ivIcon.setImageResource(getResourceId(R.attr.bankDepositOrderIcon, this))
        tvLabel.setText(R.string.current_deposit)

        launch {
            if (getViewModel().initAddress()) {
                getPagingHandler().start()
            } else {
                statusLayout.showStatus(IStatusLayout.Status.STATUS_EMPTY)
            }
        }
    }

    class ViewAdapter(
        private val withdrawalCallback: (DepositInfoDTO, Int) -> Unit
    ) : PagingViewAdapter<DepositInfoDTO>() {

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
        private val withdrawalCallback: (DepositInfoDTO, Int) -> Unit
    ) : BaseViewHolder<DepositInfoDTO>(view) {

        init {
            itemView.tvWithdrawal.setOnClickListener(this)
        }

        override fun onViewBind(itemPosition: Int, itemData: DepositInfoDTO?) {
            itemData?.let {
                itemView.ivLogo.loadCircleImage(
                    it.productLogo,
                    getResourceId(R.attr.iconCoinDefLogo, itemView.context)
                )
                itemView.tvName.text = it.productName
                itemView.tvPrincipal.text = convertAmountToDisplayAmountStr(it.principal)
                itemView.tvEarnings.text = convertAmountToDisplayAmountStr(it.totalEarnings)
                itemView.tvDepositYield.text = convertRateToPercentage(it.depositYield)
            }
        }

        override fun onViewClick(view: View, itemPosition: Int, itemData: DepositInfoDTO?) {
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