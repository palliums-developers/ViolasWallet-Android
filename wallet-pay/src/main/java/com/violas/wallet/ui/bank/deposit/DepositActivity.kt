package com.violas.wallet.ui.bank.deposit

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
import com.palliums.utils.start
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.bank.BankManager
import com.violas.wallet.biz.command.CommandActuator
import com.violas.wallet.biz.command.RefreshAssetsAllListCommand
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.ui.bank.*
import com.violas.wallet.ui.bank.borrow.BorrowActivity
import com.violas.wallet.ui.main.market.bean.IAssetsMark
import com.violas.wallet.ui.main.market.bean.LibraTokenAssetsMark
import com.violas.wallet.utils.authenticateAccount
import com.violas.wallet.utils.convertDisplayUnitToAmount
import kotlinx.android.synthetic.main.activity_bank_business.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created by elephant on 2020/8/19 15:38.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 数字银行-存款页面
 */
class DepositActivity : BankBusinessActivity() {
    companion object {
        fun start(
            context: Context,
            businessId: String,
            businessList: Array<Parcelable>? = null
        ) {
            Intent(context, DepositActivity::class.java).run {
                putExtra(EXT_BUSINESS_ID, businessId)
                putExtra(EXT_BUSINESS_LIST, businessList)
            }.start(context)
        }
    }

    private val mAccountManager by lazy {
        AccountManager()
    }

    private val mBankManager by lazy {
        BankManager()
    }

    override fun loadBusiness(businessId: String) {
        
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBankBusinessViewModel.mPageTitleLiveData.value = getString(R.string.title_deposit)
        mBankBusinessViewModel.mBusinessUserInfoLiveData.value = BusinessUserInfo(
            getString(R.string.hint_bank_deposit_business_name), "500 V-AAA起，每1V-AAA递增",
            BusinessUserAmountInfo(
                R.drawable.icon_bank_user_amount_info,
                getString(R.string.hint_bank_deposit_available_balance),
                "0",
                "VLSUSD"
            ),
            BusinessUserAmountInfo(
                R.drawable.icon_bank_user_amount_limit,
                getString(R.string.hint_bank_deposit_daily_limit),
                "1000",
                "VLSUSD",
                "1000"
            )
        )
        mBankBusinessViewModel.mBusinessActionLiveData.value =
            getString(R.string.action_deposit_immediately)
        mBankBusinessViewModel.mBusinessParameterListLiveData.value = arrayListOf(
            BusinessParameter("存款年利率", "0.50%", contentColor = Color.parseColor("#13B788")),
            BusinessParameter("质押率", "50%", "质押率=借贷数量/存款数量")
        )
        mBankBusinessViewModel.mProductExplanationListLiveData.value = arrayListOf(
            ProductExplanation(
                "清算率",
                "清算率是指***************************************************************"
            ),
            ProductExplanation(
                "清算罚金",
                "清算发生时，对借贷债务额外收取的罚金清算发生时，对借贷债务额外收取的罚金清算发生时，对借贷债务额外收取的罚金清算发生时，对借贷债务额外收取的罚金清算发生时，对借贷债务额外收取的罚金"
            )
        )
        mBankBusinessViewModel.mFAQListLiveData.value = arrayListOf(
            FAQ(
                "清算率",
                "清算率是指***************************************************************"
            ),
            FAQ(
                "清算罚金",
                "清算发生时，对借贷债务额外收取的罚金清算发生时，对借贷债务额外收取的罚金清算发生时，对借贷债务额外收取的罚金清算发生时，对借贷债务额外收取的罚金清算发生时，对借贷债务额外收取的罚金"
            )
        )
    }

    override fun clickSendAll() {
        launch(Dispatchers.IO) {
            val amountStr = editBusinessValue.text.toString()
            val assets = mBankBusinessViewModel.mCurrentAssetsLiveData.value
            if (assets == null) {
                showToast(getString(R.string.hint_bank_business_select_assets))
                return@launch
            }
            val account = mAccountManager.getIdentityByCoinType(assets.getCoinNumber())
            if (account == null) {
                showToast(getString(R.string.hint_bank_business_account_error))
                return@launch
            }
            val amount =
                convertDisplayUnitToAmount(amountStr, CoinTypes.parseCoinType(account.coinNumber))

            val market = IAssetsMark.convert(assets)

            if (market !is LibraTokenAssetsMark) {
                showToast(getString(R.string.hint_bank_business_assets_error))
                return@launch
            }

            authenticateAccount(account, mAccountManager, passwordCallback = {
                sendTransfer(account, market, it, amount)
            })
        }
    }

    private fun sendTransfer(
        account: AccountDO,
        mark: LibraTokenAssetsMark,
        pwd: String,
        amount: Long
    ) {
        launch(Dispatchers.IO) {
            try {
                showProgress()
                mBankManager.lock(pwd.toByteArray(), account, mark, amount)
                dismissProgress()
                showToast(getString(R.string.hint_bank_business_deposit_success))
                CommandActuator.postDelay(RefreshAssetsAllListCommand(), 2000)
                finish()
            } catch (e: Exception) {
                e.message?.let { showToast(it) }
                dismissProgress()
            }
        }
    }
}