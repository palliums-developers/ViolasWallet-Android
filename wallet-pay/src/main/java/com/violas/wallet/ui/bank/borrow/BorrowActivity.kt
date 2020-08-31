package com.violas.wallet.ui.bank.borrow

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
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
 * desc: 数字银行-借款页面
 */
class BorrowActivity : BankBusinessActivity() {
    companion object {
        fun start(
            context: Context,
            businessId: String,
            businessList: Array<Parcelable>? = null
        ) {
            Intent(context, BorrowActivity::class.java).run {
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
        mBankBusinessViewModel.mPageTitleLiveData.value = getString(R.string.title_borrow)
        mBankBusinessViewModel.mBusinessUserInfoLiveData.value = BusinessUserInfo(
            getString(R.string.hint_bank_borrow_business_name), "500 V-AAA起，每1V-AAA递增",
            BusinessUserAmountInfo(
                R.drawable.icon_bank_user_amount_info,
                getString(R.string.hint_can_borrow_lines),
                "800",
                "VLSUSD",
                "1000"
            )
        )
        mBankBusinessViewModel.mBusinessActionLiveData.value =
            getString(R.string.action_borrowing_immediately)
        mBankBusinessViewModel.mBusinessParameterListLiveData.value = arrayListOf(
            BusinessParameter("借款利率", "0.50%/日", contentColor = Color.parseColor("#13B788")),
            BusinessParameter("质押率", "50%", "质押率=借贷数量/存款数量"),
            BusinessParameter("质押账户", "银行余额", "清算部分将从存款账户扣除")
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
        launch {
            mBankBusinessViewModel.mBusinessPolicyLiveData.value = buildUseBehaviorSpan()
        }
        mBankBusinessViewModel.mBusinessActionHintLiveData.value =
            getString(R.string.hint_please_enter_the_amount_borrowed)
    }

    private fun openWebPage(url: String) {
        val webpage: Uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, webpage)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    private suspend fun buildUseBehaviorSpan(): SpannableStringBuilder? =
        withContext(Dispatchers.IO) {
            val useBehavior = getString(R.string.bank_borrow_agreement_read_and_agree)
            val borrowPolicy = getString(R.string.bank_borrow_policy)
            val spannableStringBuilder = SpannableStringBuilder(useBehavior)
            val userAgreementClickSpanPrivacy = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    openWebPage(getString(R.string.bank_borrow_service_agreement_url))
                }

                override fun updateDrawState(ds: TextPaint) {
                    ds.color = getColorByAttrId(
                        R.attr.colorPrimary,
                        this@BorrowActivity
                    )
                    ds.isUnderlineText = false//去掉下划线
                }
            }

            useBehavior.indexOf(borrowPolicy).also {
                spannableStringBuilder.setSpan(
                    userAgreementClickSpanPrivacy,
                    it,
                    it + borrowPolicy.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            spannableStringBuilder
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
                mBankManager.borrow(pwd.toByteArray(), account, mark, amount)
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
