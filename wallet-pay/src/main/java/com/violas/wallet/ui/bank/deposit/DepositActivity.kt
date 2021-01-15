package com.violas.wallet.ui.bank.deposit

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import com.palliums.utils.start
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.bank.BankManager
import com.violas.wallet.biz.command.CommandActuator
import com.violas.wallet.biz.command.RefreshAssetsAllListCommand
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
        AccountManager().getIdentityByCoinType(CoinTypes.Violas.coinType())
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
                    getString(R.string.hint_bank_deposit_business_name),
                    getString(
                        R.string.hint_bank_limit_amount,
                        convertAmountToDisplayAmountStr(minimumAmount),
                        tokenShowName,
                        convertAmountToDisplayAmountStr(minimumStep),
                        tokenShowName
                    ),
                    BusinessUserAmountInfo(
                        R.drawable.icon_bank_user_amount_limit,
                        getString(R.string.hint_bank_deposit_daily_limit),
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
                        getString(R.string.hint_annual_deposit_rate),
                        "${rate * 100}%",
                        contentColor = Color.parseColor("#13B788")
                    ),
                    BusinessParameter(
                        getString(R.string.hint_pledge_rate),
                        "${pledgeRate * 100}%",
                        getString(R.string.hint_borrow_pledge_algorithm)
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
        mBankBusinessViewModel.mPageTitleLiveData.value = getString(R.string.title_deposit)

        mBankBusinessViewModel.mBusinessActionLiveData.value =
            getString(R.string.action_deposit_immediately)
    }

    override fun onCoinAmountNotice(assetsVo: AssetsVo?) {
        assetsVo?.let {
            mBankBusinessViewModel.mCurrentAssetsLiveData.postValue(it)
            mBankBusinessViewModel.mBusinessUsableAmount.postValue(
                BusinessUserAmountInfo(
                    R.drawable.icon_bank_user_amount_info,
                    getString(R.string.hint_bank_deposit_available_balance),
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
            val amountStr = editBusinessValue.text.toString()
            val assets = mBankBusinessViewModel.mCurrentAssetsLiveData.value
            if (assets == null) {
                showToast(getString(R.string.hint_bank_load_assets_error))
                return@launch
            }
            if (amountStr.isEmpty()) {
                mBankBusinessViewModel.mBusinessActionHintLiveData.postValue(getString(R.string.hint_please_enter_deposit_quantity))
                return@launch
            }
            val account = mAccountManager.getIdentityByCoinType(assets.getCoinNumber())
            if (account == null) {
                showToast(getString(R.string.hint_account_error))
                return@launch
            }
            val amount =
                convertDisplayUnitToAmount(amountStr, CoinTypes.parseCoinType(account.coinNumber))

            if (amount < mDepositProductDetails?.minimumAmount ?: 0) {
                mBankBusinessViewModel.mBusinessActionHintLiveData.postValue(
                    getString(
                        R.string.hint_deposit_amount_too_small,
                        convertAmountToDisplayAmountStr(mDepositProductDetails?.minimumAmount ?: 0),
                        mDepositProductDetails?.tokenShowName ?: ""
                    )
                )
                return@launch
            }

            val limitAmount =
                (mDepositProductDetails?.quotaLimit ?: 0) - (mDepositProductDetails?.quotaUsed ?: 0)
            if (amount > limitAmount) {
                mBankBusinessViewModel.mBusinessActionHintLiveData.postValue(
                    getString(R.string.hint_not_exceed_quota_today)
                )
                return@launch
            }

            val maxAmount = (mDepositProductDetails?.quotaLimit ?: 0)
            if (amount > maxAmount) {
                mBankBusinessViewModel.mBusinessActionHintLiveData.postValue(
                    getString(
                        R.string.hint_deposit_amount_too_big,
                        convertAmountToDisplayAmountStr(maxAmount),
                        mDepositProductDetails?.tokenShowName ?: ""
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
                showToast(getString(R.string.hint_bank_business_deposit_success))
                EventBus.getDefault().post(BankDepositEvent())
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