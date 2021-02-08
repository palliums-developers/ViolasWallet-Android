package com.violas.wallet.ui.bank.repayBorrow

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import com.palliums.extensions.getShowErrorMessage
import com.palliums.utils.start
import com.quincysx.crypto.CoinType
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.bank.BankManager
import com.violas.wallet.biz.command.CommandActuator
import com.violas.wallet.biz.command.RefreshAssetsAllListCommand
import com.violas.wallet.common.getViolasCoinType
import com.violas.wallet.event.BankRepaymentEvent
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.bank.BorrowDetailsDTO
import com.violas.wallet.ui.bank.BankBusinessActivity
import com.violas.wallet.ui.bank.BusinessParameter
import com.violas.wallet.ui.bank.BusinessUserAmountInfo
import com.violas.wallet.ui.bank.BusinessUserInfo
import com.violas.wallet.ui.main.market.bean.IAssetsMark
import com.violas.wallet.ui.main.market.bean.LibraTokenAssetsMark
import com.violas.wallet.utils.authenticateAccount
import com.violas.wallet.utils.convertAmountToDisplayAmountStr
import com.violas.wallet.utils.convertDisplayUnitToAmount
import com.violas.wallet.utils.convertRateToPercentage
import com.violas.wallet.viewModel.bean.AssetsVo
import kotlinx.android.synthetic.main.activity_bank_business.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

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
            businessId: String,
            businessList: Array<Parcelable>? = null
        ) {
            Intent(context, RepayBorrowActivity::class.java).run {
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
        AccountManager().getIdentityByCoinType(getViolasCoinType().coinNumber())
    }

    private var mBorrowingDetails: BorrowDetailsDTO? = null

    override fun loadBusiness(businessId: String) {
        launch(Dispatchers.IO) {
            try {
                showProgress()
                mAccountDO?.address?.let {
                    mBorrowingDetails = mBankRepository.getBorrowingDetails(it, businessId)
                    refreshTryingView()
                    if (mBorrowingDetails == null) {
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
        mBorrowingDetails?.run {
            mBankBusinessViewModel.mBusinessUsableAmount.postValue(
                BusinessUserAmountInfo(
                    R.drawable.icon_bank_user_amount_info,
                    getString(R.string.bank_repayment_label_borrowed_amount),
                    convertAmountToDisplayAmountStr(borrowedAmount),
                    tokenShowName
                )
            )
            mBankBusinessViewModel.mBusinessUserInfoLiveData.postValue(
                BusinessUserInfo(
                    getString(R.string.bank_repayment_label_biz_name),
                    getString(R.string.bank_repayment_hint_repayment_amount)
                )
            )
            mBankBusinessViewModel.mBusinessParameterListLiveData.postValue(
                arrayListOf(
                    BusinessParameter(
                        getString(R.string.bank_repayment_label_borrowing_rate),
                        convertRateToPercentage(borrowingRate, 0),
                    ),
                    BusinessParameter(
                        getString(R.string.common_label_gas_fees),
                        "0.00 $tokenShowName"
                    ),
                    BusinessParameter(
                        getString(R.string.bank_repayment_label_repayment_account),
                        getString(R.string.bank_repayment_content_repayment_account)
                    )
                )
            )

            mCurrentAssertsAmountSubscriber.changeSubscriber(
                LibraTokenAssetsMark(
                    getViolasCoinType(),
                    tokenModule,
                    tokenAddress,
                    tokenName
                )
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBankBusinessViewModel.mPageTitleLiveData.value = getString(R.string.bank_repayment_title)
        mBankBusinessViewModel.mBusinessActionLiveData.value =
            getString(R.string.bank_repayment_action)
    }

    override fun onCoinAmountNotice(assetsVo: AssetsVo?) {
        assetsVo?.let {
            mBankBusinessViewModel.mCurrentAssetsLiveData.postValue(it)
        }
    }

    override fun clickSendAll() {
        mBorrowingDetails?.run {
            val convertAmountToDisplayAmountStr =
                convertAmountToDisplayAmountStr(borrowedAmount)
            editBusinessValue.setText(convertAmountToDisplayAmountStr)
        }
    }

    override fun clickExecBusiness() {
        launch(Dispatchers.IO) {
            mBankBusinessViewModel.mBusinessActionHintLiveData.postValue(null)

            val assets = mBankBusinessViewModel.mCurrentAssetsLiveData.value
            if (assets == null) {
                showToast(getString(R.string.bank_biz_tips_load_currency_error))
                return@launch
            }

            val market = IAssetsMark.convert(assets)
            if (market !is LibraTokenAssetsMark) {
                showToast(getString(R.string.bank_biz_tips_select_currency_error))
                return@launch
            }

            val account = mAccountManager.getIdentityByCoinType(assets.getCoinNumber())
            if (account == null) {
                showToast(getString(R.string.common_tips_account_error))
                return@launch
            }

            // 未输入
            val amountStr = editBusinessValue.text.toString()
            if (amountStr.isEmpty()) {
                mBankBusinessViewModel.mBusinessActionHintLiveData.postValue(
                    getString(R.string.bank_repayment_tips_repayment_amount_empty)
                )
                return@launch
            }

            // 输入0
            val amount =
                convertDisplayUnitToAmount(amountStr, CoinType.parseCoinNumber(account.coinNumber))
            if (amount <= 0) {
                mBankBusinessViewModel.mBusinessActionHintLiveData.postValue(
                    getString(R.string.bank_repayment_tips_repayment_amount_empty)
                )
                return@launch
            }

            // 超出待还金额
            if (amount > mBorrowingDetails?.borrowedAmount ?: 0){
                mBankBusinessViewModel.mBusinessActionHintLiveData.postValue(
                    getString(R.string.bank_repayment_tips_repayment_amount_excess)
                )
                return@launch
            }

            // 超出余额
            if (amount > mBankBusinessViewModel.mCurrentAssetsLiveData.value?.getAmount() ?: 0) {
                mBankBusinessViewModel.mBusinessActionHintLiveData.postValue(
                    getString(
                        R.string.common_tips_insufficient_available_balance_format,
                        assets.getAssetsName()
                    )
                )
                return@launch
            }

            // 因为银行清算服务的原因，要想全部提取填0
            val realAmount = if (amount == mBorrowingDetails?.borrowedAmount ?: 0)
                0
            else
                amount

            authenticateAccount(account, mAccountManager, passwordCallback = {
                sendTransfer(account, market, it, realAmount)
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
                val productId = intent.getStringExtra(EXT_BUSINESS_ID) ?: "0"
                mBankManager.repayBorrow(
                    pwd.toByteArray(),
                    account,
                    productId,
                    mark,
                    amount
                )
                dismissProgress()
                showToast(getString(R.string.bank_repayment_tips_repayment_success))
                EventBus.getDefault().post(BankRepaymentEvent())
                CommandActuator.postDelay(RefreshAssetsAllListCommand(), 2000)
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
                showToast(e.getShowErrorMessage(R.string.bank_repayment_tips_repayment_failure))
                dismissProgress()
            }
        }
    }
}
