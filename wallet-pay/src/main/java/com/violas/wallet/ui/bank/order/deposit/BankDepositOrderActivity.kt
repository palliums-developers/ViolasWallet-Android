package com.violas.wallet.ui.bank.order.deposit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseViewHolder
import com.palliums.listing.ListingViewAdapter
import com.palliums.listing.ListingViewModel
import com.palliums.utils.getResourceId
import com.violas.wallet.R
import com.violas.wallet.repository.http.bank.CoinCurrDepositDTO
import com.violas.wallet.ui.bank.order.BaseBankOrderActivity
import com.violas.wallet.utils.convertAmountToDisplayAmountStr
import com.violas.wallet.utils.loadCircleImage
import kotlinx.android.synthetic.main.activity_bank_order.*
import kotlinx.android.synthetic.main.item_bank_coin_curr_deposit.view.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Created by elephant on 2020/8/24 11:19.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 银行存款订单页面
 */
class BankDepositOrderActivity : BaseBankOrderActivity<CoinCurrDepositDTO>() {

    private val viewModel by lazy {
        ViewModelProvider(this).get(CoinCurrDepositViewModel::class.java)
    }
    private val viewAdapter by lazy {
        CoinCurrDepositViewAdapter { deposit, position ->
            // TODO 提取操作
            showToast("提取操作")
        }
    }

    override fun getViewModel(): ListingViewModel<CoinCurrDepositDTO> {
        return viewModel
    }

    override fun getViewAdapter(): ListingViewAdapter<CoinCurrDepositDTO> {
        return viewAdapter
    }

    override fun onTitleRightViewClick() {
        // TODO 进入存款记录页面
        showToast("进入存款记录页面")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.deposit_order)
        ivIcon.setImageResource(getResourceId(R.attr.bankDepositOrderIcon, this))
        tvLabel.setText(R.string.current_deposit)

        launch {
            val initResult = viewModel.initAddress()
            if (!initResult) {
                close()
                return@launch
            }

            viewModel.execute()
        }
    }
}

class CoinCurrDepositViewAdapter(
    private val withdrawalCallback: (CoinCurrDepositDTO, Int) -> Unit
) : ListingViewAdapter<CoinCurrDepositDTO>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewHolder<CoinCurrDepositDTO> {
        return CoinCurrDepositViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_bank_coin_curr_deposit, parent, false
            ),
            withdrawalCallback
        )
    }
}

class CoinCurrDepositViewHolder(
    view: View,
    private val withdrawalCallback: (CoinCurrDepositDTO, Int) -> Unit
) : BaseViewHolder<CoinCurrDepositDTO>(view) {

    init {
        itemView.tvWithdrawal.setOnClickListener(this)
    }

    override fun onViewBind(itemPosition: Int, itemData: CoinCurrDepositDTO?) {
        itemData?.let {
            itemView.ivCoinLogo.loadCircleImage(
                it.coinLogo,
                getResourceId(R.attr.iconCoinDefLogo, itemView.context)
            )
            itemView.tvCoinName.text = it.coinName
            itemView.tvPrincipal.text = convertAmountToDisplayAmountStr(it.principal)
            itemView.tvEarnings.text = convertAmountToDisplayAmountStr(it.totalEarnings)
            itemView.tvSevenDayAnnualYield.text = "${BigDecimal(it.sevenDayAnnualYield).setScale(
                2,
                RoundingMode.DOWN
            ).toPlainString()}%"
        }
    }

    override fun onViewClick(view: View, itemPosition: Int, itemData: CoinCurrDepositDTO?) {
        itemData?.let {
            when (view) {
                itemView.tvWithdrawal -> {
                    withdrawalCallback.invoke(it, itemPosition)
                }
            }
        }
    }
}