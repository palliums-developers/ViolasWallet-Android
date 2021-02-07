package com.violas.wallet.ui.bank.deposit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.palliums.extensions.getShowErrorMessage
import com.palliums.utils.getColorByAttrId
import com.palliums.utils.start
import com.quincysx.crypto.CoinType
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.bank.BankManager
import com.violas.wallet.biz.command.CommandActuator
import com.violas.wallet.biz.command.RefreshAssetsAllListCommand
import com.violas.wallet.common.getViolasCoinType
import com.violas.wallet.event.BankDepositEvent
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.bank.DepositProductDetailsDTO
import com.violas.wallet.ui.bank.*
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
import java.math.BigDecimal

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
            businessId: String
        ) {
            Intent(context, DepositActivity::class.java).run {
                putExtra(EXT_BUSINESS_ID, businessId)
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

    private var mDepositProductDetails: DepositProductDetailsDTO? = null

    override fun loadBusiness(businessId: String) {
        launch(Dispatchers.IO) {
            showProgress()
            try {
                mAccountDO?.address?.let {
                    mDepositProductDetails =
                        mBankRepository.getDepositProductDetails(businessId, it)
                    refreshTryingView()
                    if (mDepositProductDetails == null) {
                        loadedFailure()
                    } else {
                        loadedSuccess()
                    }
                }
            } catch (e: Exception) {
                loadedFailure()
            } finally {
                dismissProgress()
            }
        }
    }

    private fun refreshTryingView() {
        mDepositProductDetails?.run {
            mBankBusinessViewModel.mBusinessUserInfoLiveData.postValue(
                BusinessUserInfo(
                    getString(R.string.bank_deposit_label_biz_name),
                    getString(
                        R.string.bank_biz_hint_limit_amount_format,
                        convertAmountToDisplayAmountStr(minimumAmount),
                        tokenShowName,
                        convertAmountToDisplayAmountStr(minimumStep),
                        tokenShowName
                    ),
                    BusinessUserAmountInfo(
                        R.drawable.icon_bank_user_amount_limit,
                        getString(R.string.bank_deposit_label_daily_limit),
                        convertAmountToDisplayAmountStr(quotaLimit - quotaUsed),
                        tokenShowName,
                        convertAmountToDisplayAmountStr(quotaLimit)
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
                        getString(R.string.bank_deposit_label_annual_deposit_rate),
                        convertRateToPercentage(rate),
                        contentColor = getColorByAttrId(
                            R.attr.bankProductRateTextColor,
                            this@DepositActivity
                        )
                    ),
                    BusinessParameter(
                        getString(R.string.bank_biz_label_pledge_rate),
                        convertRateToPercentage(pledgeRate, 0),
                        getString(R.string.bank_biz_hint_pledge_rate_algorithm)
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
        mBankBusinessViewModel.mPageTitleLiveData.value = getString(R.string.bank_deposit_title)

        mBankBusinessViewModel.mBusinessActionLiveData.value =
            getString(R.string.bank_deposit_action_deposit)
    }

    override fun onCoinAmountNotice(assetsVo: AssetsVo?) {
        assetsVo?.let {
            mBankBusinessViewModel.mCurrentAssetsLiveData.postValue(it)
            mBankBusinessViewModel.mBusinessUsableAmount.postValue(
                BusinessUserAmountInfo(
                    R.drawable.icon_bank_user_amount_info,
                    getString(R.string.common_label_available_balance),
                    assetsVo.amountWithUnit.amount,
                    assetsVo.amountWithUnit.unit
                )
            )
        }
    }

    override fun clickSendAll() {
        mDepositProductDetails?.run {
            val balance =
                BigDecimal(mBankBusinessViewModel.mBusinessUsableAmount.value?.value1 ?: "0")
            val useAmount = BigDecimal(convertAmountToDisplayAmountStr(quotaLimit - quotaUsed))

            val inputAmount = if (balance > useAmount) {
                useAmount
            } else {
                balance
            }

            editBusinessValue.setText(inputAmount.toPlainString())
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
                    getString(R.string.bank_deposit_tips_deposit_amount_empty)
                )
                return@launch
            }

            // 输入0
            val amount =
                convertDisplayUnitToAmount(amountStr, CoinType.parseCoinNumber(account.coinNumber))
            if (amount <= 0) {
                mBankBusinessViewModel.mBusinessActionHintLiveData.postValue(
                    getString(R.string.bank_deposit_tips_deposit_amount_empty)
                )
                return@launch
            }

            // 小于最小存款额
            val minimumAmount = mDepositProductDetails?.minimumAmount ?: 0
            if (amount < minimumAmount) {
                mBankBusinessViewModel.mBusinessActionHintLiveData.postValue(
                    getString(
                        R.string.bank_deposit_tips_deposit_amount_too_small_format,
                        convertAmountToDisplayAmountStr(minimumAmount),
                        mDepositProductDetails?.tokenShowName ?: ""
                    )
                )
                return@launch
            }

            // 超出每日限额
            val limitAmount =
                (mDepositProductDetails?.quotaLimit ?: 0) - (mDepositProductDetails?.quotaUsed ?: 0)
            if (amount > limitAmount) {
                mBankBusinessViewModel.mBusinessActionHintLiveData.postValue(
                    getString(R.string.bank_biz_tips_greater_than_daily_limit)
                )
                return@launch
            }

            // 超出最大存款额
            val maxAmount = mDepositProductDetails?.quotaLimit ?: 0
            if (amount > maxAmount) {
                mBankBusinessViewModel.mBusinessActionHintLiveData.postValue(
                    getString(
                        R.string.bank_deposit_tips_deposit_amount_too_big_format,
                        convertAmountToDisplayAmountStr(maxAmount),
                        mDepositProductDetails?.tokenShowName ?: ""
                    )
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

            authenticateAccount(account, mAccountManager, passwordCallback = {
                sendTransfer(account, mDepositProductDetails!!.id, market, it, amount)
            })
        }
    }

    private fun sendTransfer(
        account: AccountDO,
        productId: String,
        mark: LibraTokenAssetsMark,
        pwd: String,
        amount: Long
    ) {
        launch(Dispatchers.IO) {
            try {
                showProgress()
                mBankManager.lock(pwd.toByteArray(), account, productId, mark, amount)
                dismissProgress()
                showToast(getString(R.string.bank_deposit_tips_deposit_success))
                EventBus.getDefault().post(BankDepositEvent())
                CommandActuator.postDelay(RefreshAssetsAllListCommand(), 2000)
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
                showToast(e.getShowErrorMessage(R.string.bank_deposit_tips_deposit_failure))
                dismissProgress()
            }
        }
    }
}