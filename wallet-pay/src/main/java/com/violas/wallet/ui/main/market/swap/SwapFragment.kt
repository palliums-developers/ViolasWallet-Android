package com.violas.wallet.ui.main.market.swap

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.AmountInputFilter
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseFragment
import com.palliums.extensions.show
import com.palliums.net.LoadState
import com.palliums.utils.TextWatcherSimple
import com.palliums.utils.stripTrailingZeros
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.exchange.AccountPayeeNotFindException
import com.violas.wallet.biz.exchange.AccountPayeeTokenNotActiveException
import com.violas.wallet.repository.subscribeHub.BalanceSubscribeHub
import com.violas.wallet.repository.subscribeHub.BalanceSubscriber
import com.violas.wallet.ui.main.market.MarketViewModel
import com.violas.wallet.ui.main.market.bean.IAssetsMark
import com.violas.wallet.ui.main.market.bean.ITokenVo
import com.violas.wallet.ui.main.market.selectToken.SwapSelectTokenDialog.Companion.ACTION_SWAP_SELECT_FROM
import com.violas.wallet.ui.main.market.selectToken.SwapSelectTokenDialog.Companion.ACTION_SWAP_SELECT_TO
import com.violas.wallet.ui.main.market.selectToken.SwapSelectTokenDialog
import com.violas.wallet.ui.main.market.selectToken.TokensBridge
import com.violas.wallet.ui.main.market.selectToken.SwapTokensDataResourcesBridge
import com.violas.wallet.utils.authenticateAccount
import com.violas.wallet.utils.convertAmountToDisplayUnit
import com.violas.wallet.viewModel.WalletAppViewModel
import com.violas.wallet.viewModel.bean.AssetsVo
import com.violas.wallet.widget.dialog.PublishTokenDialog
import kotlinx.android.synthetic.main.fragment_swap.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.palliums.libracore.http.LibraException
import org.palliums.violascore.http.ViolasException

/**
 * Created by elephant on 2020/6/23 17:18.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 市场兑换视图
 */
class SwapFragment : BaseFragment(), TokensBridge, SwapTokensDataResourcesBridge {

    private val swapViewModel by lazy {
        ViewModelProvider(this).get(SwapViewModel::class.java)
    }
    private val marketViewModel by lazy {
        ViewModelProvider(requireParentFragment()).get(MarketViewModel::class.java)
    }
    private val mAccountManager by lazy {
        AccountManager()
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_swap
    }

    override fun onPause() {
        super.onPause()
        clearInputBoxFocusAndHideSoftInput()
    }

    private val fromAssertsAmountSubscriber = object : BalanceSubscriber(null) {
        override fun onNotice(assets: AssetsVo) {
            launch {
                tvFromBalance.text = getString(
                    R.string.market_token_balance_format,
                    "${assets.amountWithUnit.amount} ${assets.getAssetsName()}"
                )
            }
        }
    }
    private val toAssertsAmountSubscriber = object : BalanceSubscriber(null) {
        override fun onNotice(assets: AssetsVo) {
            launch {
                tvToBalance.text = getString(
                    R.string.market_token_balance_format,
                    "${assets.amountWithUnit.amount} ${assets.getAssetsName()}"
                )
            }
        }
    }

    override fun onLazyInitViewByResume(savedInstanceState: Bundle?) {
        super.onLazyInitViewByResume(savedInstanceState)

        etFromInputBox.hint = "0.00"
        etToInputBox.hint = "0.00"
        handleValueNull(tvFromBalance, R.string.market_token_balance_format)
        handleValueNull(tvToBalance, R.string.market_token_balance_format)
        handleValueNull(tvHandlingFeeRate, R.string.handling_fee_rate_format)
        handleValueNull(tvExchangeRate, R.string.exchange_rate_format)
        handleValueNull(tvGasFee, R.string.gas_fee_format)

        etFromInputBox.addTextChangedListener(fromInputTextWatcher)
        etToInputBox.addTextChangedListener(toInputTextWatcher)
        etFromInputBox.filters = arrayOf(AmountInputFilter(12, 2))
        etToInputBox.filters = arrayOf(AmountInputFilter(12, 2))

        llFromSelectGroup.setOnClickListener {
            clearInputBoxFocusAndHideSoftInput()
            showSelectTokenDialog(true)
        }

        llToSelectGroup.setOnClickListener {
            clearInputBoxFocusAndHideSoftInput()
            showSelectTokenDialog(false)
        }

        btnSwap.setOnClickListener {
            clearInputBoxFocusAndHideSoftInput()
            if (swapViewModel.getCurrFromTokenLiveData().value == null) {
                // 输入币种没有选择
                showToast(getString(R.string.hint_swap_input_assets_not_select))
                return@setOnClickListener
            }
            if (swapViewModel.getCurrToTokenLiveData().value == null) {
                // 输出币种没有选择
                showToast(getString(R.string.hint_swap_output_assets_not_select))
                return@setOnClickListener
            }
            if (etFromInputBox.text.isEmpty()) {
                // 填写输入金额
                showToast(getString(R.string.hint_swap_input_assets_amount_input))
                return@setOnClickListener
            }
            if (etToInputBox.text.isEmpty()) {
                // 请稍后正在估算输出金额
                showToast(getString(R.string.hint_swap_output_assets_amount_input))
                estimateOutputTokenNumber()
                return@setOnClickListener
            }
            showProgress()
            launch(Dispatchers.IO) {
                try {
                    val defaultAccount = mAccountManager.getDefaultAccount()
                    dismissProgress()
                    authenticateAccount(defaultAccount, mAccountManager) {
                        swap(it)
                    }
                } catch (e: Exception) {
                    dismissProgress()
                }
            }
        }

        swapViewModel.getCurrFromTokenLiveData().observe(viewLifecycleOwner, Observer {
            if (it == null) {
                tvFromSelectText.text = getString(R.string.select_token)
                handleValueNull(tvFromBalance, R.string.market_token_balance_format)
                fromAssertsAmountSubscriber.changeSubscriber(null)
            } else {
                tvFromSelectText.text = it.displayName
                fromAssertsAmountSubscriber.changeSubscriber(IAssetsMark.convert(it))
            }
        })
        swapViewModel.getCurrToTokenLiveData().observe(viewLifecycleOwner, Observer {
            if (it == null) {
                tvToSelectText.text = getString(R.string.select_token)
                handleValueNull(tvToBalance, R.string.market_token_balance_format)
                toAssertsAmountSubscriber.changeSubscriber(null)
            } else {
                tvToSelectText.text = it.displayName
                toAssertsAmountSubscriber.changeSubscriber(IAssetsMark.convert(it))
            }
        })
        swapViewModel.getHandlingFeeRateLiveDataLiveData().observe(viewLifecycleOwner, Observer {
            if (it == null) {
                handleValueNull(tvHandlingFeeRate, R.string.handling_fee_rate_format)
            } else {
                tvHandlingFeeRate.text = getString(
                    R.string.handling_fee_rate_format,
                    "${it.toPlainString()}%"
                )
            }
        })
        swapViewModel.getExchangeRateLiveData().observe(viewLifecycleOwner, Observer {
            if (it == null) {
                handleValueNull(tvExchangeRate, R.string.exchange_rate_format)
            } else {
                tvExchangeRate.text = getString(
                    R.string.exchange_rate_format,
                    "1:${it.toPlainString()}"
                )
            }
        })
        swapViewModel.getGasFeeLiveData().observe(viewLifecycleOwner, Observer {
            if (it == null) {
                handleValueNull(tvGasFee, R.string.gas_fee_format)
            } else {
                tvGasFee.text = getString(
                    R.string.gas_fee_format,
                    it.toString()
                )
            }
        })

        swapViewModel.loadState.observe(viewLifecycleOwner, Observer {
            when (it.peekData().status) {
                LoadState.Status.RUNNING -> {
                    showProgress()
                }

                else -> {
                    dismissProgress()
                }
            }
        })
        swapViewModel.tipsMessage.observe(viewLifecycleOwner, Observer {
            it.getDataIfNotHandled()?.let { msg ->
                if (msg.isNotEmpty()) {
                    showToast(msg)
                }
            }
        })

        if (!marketViewModel.tipsMessage.hasObservers()) {
            marketViewModel.tipsMessage.observe(viewLifecycleOwner, Observer {
                it.getDataIfNotHandled()?.let { msg ->
                    if (msg.isNotEmpty()) {
                        showToast(msg)
                    }
                }
            })
        }

        BalanceSubscribeHub.observe(this, fromAssertsAmountSubscriber)
        BalanceSubscribeHub.observe(this, toAssertsAmountSubscriber)

        // 资产变化
        WalletAppViewModel.getViewModelInstance().mAssetsListLiveData.observe(this, Observer {
            refreshBalances()
        })
    }

    /**
     * 刷新余额
     */
    private fun refreshBalances() {

    }

    private fun swap(key: ByteArray) {
        showProgress()
        launch(Dispatchers.IO) {
            try {
                swapViewModel.swap(
                    key,
                    etFromInputBox.text.toString(),
                    etToInputBox.text.toString()
                )
            } catch (e: ViolasException) {
                e.printStackTrace()
            } catch (e: LibraException) {
                e.printStackTrace()
            } catch (e: AccountPayeeNotFindException) {
                showToast(getString(R.string.hint_payee_account_not_active))
            } catch (e: AccountPayeeTokenNotActiveException) {
                showPublishTokenDialog(key, e)
            } catch (e: Exception) {
                showToast(getString(R.string.hint_unknown_error))
            }
            dismissProgress()
        }
    }

    private fun showPublishTokenDialog(
        key: ByteArray,
        e: AccountPayeeTokenNotActiveException
    ) {
        PublishTokenDialog().setConfirmListener {
            it.dismiss()
            launch(Dispatchers.IO) {
                showProgress()
                try {
                    if (swapViewModel.publishToken(key, e.coinTypes, e.assetsMark)) {
                        swap(key)
                    } else {
                        showToast(R.string.desc_transaction_state_add_currency_failure)
                    }
                } catch (e: Exception) {
                    showToast(R.string.desc_transaction_state_add_currency_failure)
                    e.printStackTrace()
                }
            }
        }.show(parentFragmentManager)
    }

    /**
     * 估算输出金额
     */
    private fun estimateOutputTokenNumber() {
        swapViewModel.exchangeSwapTrial(etFromInputBox.text.toString()) { amount, fee ->
            handleValue(tvExchangeRate, R.string.exchange_rate_format, fee)
            etToInputBox.setText(amount)
        }
    }

    private val handler = Handler(Looper.getMainLooper(), Handler.Callback {
        when (it.what) {
            ExchangeSwapTrialWhat -> {
                estimateOutputTokenNumber()
            }
        }
        true
    })
    private val ExchangeSwapTrialWhat = 1

    /**
     * 延迟估算
     */
    private fun delayEstimateOutputTokenNumber() {
        handler.removeMessages(ExchangeSwapTrialWhat)
        handler.sendEmptyMessageAtTime(ExchangeSwapTrialWhat, 800)
    }

    //*********************************** 选择Token逻辑 ***********************************//
    private fun showSelectTokenDialog(selectFrom: Boolean) {
        SwapSelectTokenDialog
            .newInstance(
                if (selectFrom) ACTION_SWAP_SELECT_FROM else ACTION_SWAP_SELECT_TO
            )
            .setCallback { action, iToken ->
                swapViewModel.selectToken(ACTION_SWAP_SELECT_FROM == action, iToken)
                // todo 可能会有问题，选择刷洗问题，因为 livedata 使用 postValue
                estimateOutputTokenNumber()
            }
            .show(childFragmentManager)
    }

    override fun getMarketSupportTokens(recreateLiveData: Boolean) {
        marketViewModel.execute(recreateLiveData)
    }

    override fun getMarketSupportTokensLiveData(): LiveData<List<ITokenVo>?> {
        return marketViewModel.getMarketSupportTokensLiveData()
    }

    override fun getCurrToken(action: Int): ITokenVo? {
        return if (action == ACTION_SWAP_SELECT_FROM)
            swapViewModel.getCurrFromTokenLiveData().value
        else
            swapViewModel.getCurrToTokenLiveData().value
    }

    override fun getMarketSupportTokens(action: Int): List<ITokenVo>? {
        return if (action == ACTION_SWAP_SELECT_FROM) {
            swapViewModel.getSupportTokensLiveData().value
        } else {
            swapViewModel.getSwapToTokenList()
        }
    }

    //*********************************** 输入框逻辑 ***********************************//
    private val fromInputTextWatcher = object : TextWatcherSimple() {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (!etFromInputBox.isFocused) return

            val inputText = s?.toString() ?: ""
            val amountStr = handleInputText(inputText)
            if (inputText != amountStr) {
                handleInputTextWatcher(amountStr, etFromInputBox, this)
            }
            delayEstimateOutputTokenNumber()
        }
    }

    private val toInputTextWatcher = object : TextWatcherSimple() {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (!etToInputBox.isFocused) return

            val inputText = s?.toString() ?: ""
            val amountStr = handleInputText(inputText)
            if (inputText != amountStr) {
                handleInputTextWatcher(amountStr, etToInputBox, this)
            }
        }
    }

    private val handleInputText: (String) -> String = { inputText ->
        var amountStr = inputText
        if (inputText.startsWith(".")) {
            amountStr = "0$inputText"
        } else if (inputText.isNotEmpty()) {
            amountStr = (inputText + 1).stripTrailingZeros()
            amountStr = amountStr.substring(0, amountStr.length - 1)
            if (amountStr.isEmpty()) {
                amountStr = "0"
            }
        }
        amountStr
    }

    private val handleInputTextWatcher: (String, EditText, TextWatcher) -> Unit =
        { amountStr, inputBox, textWatcher ->
            inputBox.removeTextChangedListener(textWatcher)

            inputBox.setText(amountStr)
            inputBox.setSelection(amountStr.length)

            inputBox.addTextChangedListener(textWatcher)
        }

    private fun clearInputBoxFocusAndHideSoftInput() {
        var focused = false
        if (etFromInputBox.isFocused) {
            focused = true
            etFromInputBox.clearFocus()
        }
        if (etToInputBox.isFocused) {
            focused = true
            etToInputBox.clearFocus()
        }

        if (focused) {
            btnSwap.requestFocus()
            hideSoftInput()
        }
    }

    //*********************************** 其它逻辑 ***********************************//
    private val handleValueNull: (TextView, Int) -> Unit = { textView, formatResId ->
        textView.text = getString(formatResId, getString(R.string.value_null))
    }

    private val handleValue: (TextView, Int, String) -> Unit = { textView, formatResId, value ->
        textView.text = getString(formatResId, value)
    }
}