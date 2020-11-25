package com.violas.wallet.ui.bank.repayBorrow

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import com.palliums.utils.start
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.bank.BankManager
import com.violas.wallet.biz.command.CommandActuator
import com.violas.wallet.biz.command.RefreshAssetsAllListCommand
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
import com.violas.wallet.viewModel.bean.AssetsVo
import kotlinx.android.synthetic.main.activity_bank_business.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        AccountManager().getIdentityByCoinType(CoinTypes.Violas.coinType())
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
                    getString(R.string.hint_can_repay_borrow_lines),
                    convertAmountToDisplayAmountStr(borrowedAmount),
                    tokenShowName
                )
            )
            mBankBusinessViewModel.mBusinessUserInfoLiveData.postValue(
                BusinessUserInfo(
                    getString(R.string.hint_bank_repay_borrow_business_name),
                    getString(R.string.hint_enter_repayment_amount)
                )
            )
            mBankBusinessViewModel.mBusinessParameterListLiveData.postValue(
                arrayListOf(
                    BusinessParameter(
                        getString(R.string.borrowing_rate),
                        "${borrowingRate * 100}%"
                    ),
                    BusinessParameter(
                        getString(R.string.label_miner_fees),
                        "0.00 ${tokenShowName}"
                    ),
                    BusinessParameter(
                        getString(R.string.hint_repayment_account),
                        getString(R.string.hint_borrow_wallet_balance)
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
        mBankBusinessViewModel.mPageTitleLiveData.value = getString(R.string.title_repay_borrow)
        mBankBusinessViewModel.mBusinessActionLiveData.value =
            getString(R.string.action_repay_borrowing_immediately)
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
            val amountStr = editBusinessValue.text.toString()
            val assets = mBankBusinessViewModel.mCurrentAssetsLiveData.value
            if (assets == null) {
                showToast(getString(R.string.hint_bank_load_assets_error))
                return@launch
            }
            if (amountStr.isEmpty()) {
                mBankBusinessViewModel.mBusinessActionHintLiveData.postValue(getString(R.string.hint_please_enter_the_amount_repay_borrowed))
                return@launch
            }
            val account = mAccountManager.getIdentityByCoinType(assets.getCoinNumber())
            if (account == null) {
                showToast(getString(R.string.hint_account_error))
                return@launch
            }
            val amount =
                convertDisplayUnitToAmount(amountStr, CoinTypes.parseCoinType(account.coinNumber))

            if (amount > mBankBusinessViewModel.mCurrentAssetsLiveData.value?.getAmount() ?: 0) {
                showToast(getString(R.string.hint_repay_borrow_insufficient_balance))
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
                val productId = intent.getStringExtra(EXT_BUSINESS_ID) ?: "0"
                mBankManager.repayBorrow(
                    pwd.toByteArray(),
                    account,
                    productId,
                    mark,
                    amount
                )
                dismissProgress()
                showToast(getString(R.string.hint_bank_business_borrow_success))
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
