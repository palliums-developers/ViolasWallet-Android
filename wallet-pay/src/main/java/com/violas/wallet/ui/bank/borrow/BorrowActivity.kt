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
import com.violas.wallet.event.BankBorrowEvent
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.bank.BorrowProductDetailsDTO
import com.violas.wallet.ui.bank.*
import com.violas.wallet.ui.main.market.bean.IAssetsMark
import com.violas.wallet.ui.main.market.bean.LibraTokenAssetsMark
import com.violas.wallet.utils.authenticateAccount
import com.violas.wallet.utils.convertAmountToDisplayAmountStr
import com.violas.wallet.utils.convertDisplayUnitToAmount
import com.violas.wallet.viewModel.bean.AssetsVo
import kotlinx.android.synthetic.main.activity_bank_business.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

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
                putExtra(EXT_BUSINESS_DTO, businessList)
            }.start(context)
        }
    }

    private val mAccountManager by lazy {
        AccountManager()
    }

    private val mBankManager by lazy {
        BankManager()
    }

    private val mBankRepository by lazy {
        DataRepository.getBankService()
    }

    private val mAccountDO by lazy {
        AccountManager().getIdentityByCoinType(CoinTypes.Violas.coinType())
    }

    private var mBorrowProductDetails: BorrowProductDetailsDTO? = null
    override fun loadBusiness(businessId: String) {
        launch(Dispatchers.IO) {
            showProgress()
            try {
                mAccountDO?.address?.let {
                    mBorrowProductDetails = mBankRepository.getBorrowProductDetails(businessId, it)
                    refreshTryingView()
                    if (mBorrowProductDetails == null) {
                        loadedFailure()
                    } else {
                        loadedSuccess()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                loadedFailure()
            } finally {
                dismissProgress()
            }
        }
    }

    private fun refreshTryingView() {
        mBorrowProductDetails?.run {
//            setCurrentCoin(tokenModule, tokenAddress, tokenName, CoinTypes.Violas.coinType())
            mBankBusinessViewModel.mBusinessUsableAmount.postValue(
                BusinessUserAmountInfo(
                    R.drawable.icon_bank_user_amount_info,
                    getString(R.string.hint_can_borrow_lines),
                    convertAmountToDisplayAmountStr(quotaLimit - quotaUsed),
                    tokenShowName,
                    convertAmountToDisplayAmountStr(quotaLimit)
                )
            )
            mBankBusinessViewModel.mBusinessUserInfoLiveData.postValue(
                BusinessUserInfo(
                    getString(R.string.hint_bank_borrow_business_name),
                    getString(
                        R.string.hint_bank_limit_amount,
                        convertAmountToDisplayAmountStr(minimumAmount),
                        tokenShowName,
                        convertAmountToDisplayAmountStr(minimumStep),
                        tokenShowName
                    )
                )
            )
            mBankBusinessViewModel.mProductExplanationListLiveData.postValue(intor.map {
                ProductExplanation(it.title, it.text)
            })

            mBankBusinessViewModel.mFAQListLiveData.postValue(question.map {
                FAQ(it.title, it.text)
            })
            mBankBusinessViewModel.mBusinessParameterListLiveData.postValue(
                arrayListOf(
                    BusinessParameter(
                        getString(R.string.hint_borrowing_rates),
                        "${rate * 100}%/日",
                        contentColor = Color.parseColor("#13B788")
                    ),
                    BusinessParameter(
                        getString(R.string.hint_pledge_rate),
                        "${pledgeRate * 100}%",
                        getString(R.string.hint_borrow_pledge_algorithm)
                    ),
                    BusinessParameter(
                        getString(R.string.hint_pledge_account),
                        getString(R.string.hint_bank_balance),
                        getString(
                            R.string.hint_borrow_liquidation
                        )
                    )
                )
            )

            mCurrentAssertsAmountSubscriber.changeSubscriber(
                LibraTokenAssetsMark(
                    CoinTypes.Violas,
                    tokenModule,
                    tokenAddress,
                    tokenName
                )
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBankBusinessViewModel.mPageTitleLiveData.value = getString(R.string.title_borrow)

        mBankBusinessViewModel.mBusinessActionLiveData.value =
            getString(R.string.action_borrowing_immediately)

        launch {
            mBankBusinessViewModel.mBusinessPolicyLiveData.value = buildUseBehaviorSpan()
        }
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
        mBorrowProductDetails?.run {
            val convertAmountToDisplayAmountStr =
                convertAmountToDisplayAmountStr(quotaLimit - quotaUsed)
            editBusinessValue.setText(convertAmountToDisplayAmountStr)
        }
    }

    override fun onCoinAmountNotice(assetsVo: AssetsVo?) {
        assetsVo?.let {
            mBankBusinessViewModel.mCurrentAssetsLiveData.postValue(it)
        }
    }

    override fun clickExecBusiness() {
        launch(Dispatchers.IO) {
            mBankBusinessViewModel.mBusinessActionHintLiveData.postValue(null)
            val amountStr = editBusinessValue.text.toString()
            val assets = mBankBusinessViewModel.mCurrentAssetsLiveData.value
            if (assets == null) {
                showToast(getString(R.string.hint_bank_load_assets_error))
                return@launch
            }
            val account = mAccountManager.getIdentityByCoinType(assets.getCoinNumber())
            if (account == null) {
                showToast(getString(R.string.hint_account_error))
                return@launch
            }
            if (amountStr.isEmpty()) {
                mBankBusinessViewModel.mBusinessActionHintLiveData.postValue(getString(R.string.hint_please_enter_the_amount_borrowed))
                return@launch
            }
            if (!btnHasAgreePolicy.isChecked) {
                mBankBusinessViewModel.mBusinessActionHintLiveData.postValue(getString(R.string.hint_please_read_and_agree_pledge_service_agreement))
                return@launch
            }
            val amount =
                convertDisplayUnitToAmount(amountStr, CoinTypes.parseCoinType(account.coinNumber))

            if (amount < mBorrowProductDetails?.minimumAmount ?: 0) {
                mBankBusinessViewModel.mBusinessActionHintLiveData.postValue(
                    getString(
                        R.string.hint_borrow_amount_too_small,
                        convertAmountToDisplayAmountStr(mBorrowProductDetails?.minimumAmount ?: 0),
                        mBorrowProductDetails?.tokenShowName ?: ""
                    )
                )
                return@launch
            }

            val limitAmount =
                (mBorrowProductDetails?.quotaLimit ?: 0) - (mBorrowProductDetails?.quotaUsed ?: 0)
            if (amount > limitAmount) {
                mBankBusinessViewModel.mBusinessActionHintLiveData.postValue(
                    getString(R.string.hint_not_exceed_quota_today)
                )
                return@launch
            }

            val maxAmount = (mBorrowProductDetails?.quotaLimit ?: 0)
            if (amount > maxAmount) {
                mBankBusinessViewModel.mBusinessActionHintLiveData.postValue(
                    getString(
                        R.string.hint_borrow_amount_too_big,
                        convertAmountToDisplayAmountStr(maxAmount),
                        mBorrowProductDetails?.tokenShowName ?: ""
                    )
                )
                return@launch
            }

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
                mBankManager.borrow(
                    pwd.toByteArray(), account,
                    mBorrowProductDetails!!.id, mark, amount
                )
                dismissProgress()
                showToast(getString(R.string.hint_bank_business_borrow_success))
                EventBus.getDefault().post(BankBorrowEvent())
                CommandActuator.postDelay(RefreshAssetsAllListCommand(), 2000)
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
                e.message?.let { showToast(it) }
                dismissProgress()
            }
        }
    }
}
