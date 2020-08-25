package com.violas.wallet.ui.bank.borrow

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import com.palliums.utils.getColorByAttrId
import com.quincysx.crypto.bip44.CoinType
import com.violas.wallet.R
import com.violas.wallet.ui.bank.*
import com.violas.wallet.ui.main.market.bean.IAssetsMark
import com.violas.wallet.viewModel.bean.AssetsVo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BorrowActivity : BankBusinessActivity() {
    companion object {
        fun start(
            context: Context,
            coinType: CoinType,
            module: String? = null,
            address: String? = null,
            name: String? = null
        ) {

        }
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

    }
}