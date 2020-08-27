package com.violas.wallet.ui.bank.repayBorrow

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import com.palliums.utils.getColorByAttrId
import com.palliums.utils.start
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.bank.BankManager
import com.violas.wallet.biz.command.CommandActuator
import com.violas.wallet.biz.command.RefreshAssetsAllListCommand
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.ui.bank.*
import com.violas.wallet.ui.main.market.bean.IAssetsMark
import com.violas.wallet.ui.main.market.bean.LibraTokenAssetsMark
import com.violas.wallet.utils.authenticateAccount
import com.violas.wallet.utils.convertDisplayUnitToAmount
import kotlinx.android.synthetic.main.activity_bank_business.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by elephant on 2020/8/19 15:38.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 数字银行-还款页面
 */
class RepayBorrowActivity : BankBusinessActivity() {
    companion object {
        fun start(
            context: Context,
            coinType: CoinTypes,
            module: String? = null,
            address: String? = null,
            name: String? = null
        ) {
            Intent(context, RepayBorrowActivity::class.java).run {
                putExtra(EXT_ASSETS_COINTYPE, coinType.coinType())
                putExtra(EXT_ASSETS_MODULE, module)
                putExtra(EXT_ASSETS_ADDRESS, address)
                putExtra(EXT_ASSETS_NAME, name)
            }.start(context)
        }
    }

    private val mAccountManager by lazy {
        AccountManager()
    }

    private val mBankManager by lazy {
        BankManager()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBankBusinessViewModel.mPageTitleLiveData.value = getString(R.string.title_repay_borrow)
        mBankBusinessViewModel.mBusinessUserInfoLiveData.value = BusinessUserInfo(
            getString(R.string.hint_bank_repay_borrow_business_name), "500 V-AAA起，每1V-AAA递增",
            BusinessUserAmountInfo(
                R.drawable.icon_bank_user_amount_info,
                getString(R.string.hint_can_repay_borrow_lines),
                "800",
                "VLSUSD"
            )
        )
        mBankBusinessViewModel.mBusinessActionLiveData.value =
            getString(R.string.action_repay_borrowing_immediately)
        mBankBusinessViewModel.mBusinessParameterListLiveData.value = arrayListOf(
            BusinessParameter("借贷率", "5%"),
            BusinessParameter("矿工费用", "0.00VLS"),
            BusinessParameter("还款账户", "钱包余额")
        )

        mBankBusinessViewModel.mBusinessActionHintLiveData.value =
            getString(R.string.hint_please_enter_the_amount_repay_borrowed)
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
                mBankManager.repayBorrow(pwd.toByteArray(), account, mark, amount)
                dismissProgress()
                showToast(getString(R.string.hint_bank_business_borrow_success))
                CommandActuator.postDelay(RefreshAssetsAllListCommand(), 2000)
                finish()
            } catch (e: Exception) {
                e.message?.let { showToast(it) }
                dismissProgress()
            }
        }
    }
}
