package com.violas.wallet.ui.main.market.swap

import android.os.Bundle
import android.text.AmountInputFilter
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseFragment
import com.palliums.extensions.expandTouchArea
import com.palliums.extensions.getShowErrorMessage
import com.palliums.extensions.show
import com.palliums.net.LoadState
import com.palliums.utils.TextWatcherSimple
import com.palliums.utils.stripTrailingZeros
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.command.CommandActuator
import com.violas.wallet.biz.command.RefreshAssetsAllListCommand
import com.violas.wallet.biz.exchange.AccountPayeeNotFindException
import com.violas.wallet.biz.exchange.AccountPayeeTokenNotActiveException
import com.violas.wallet.biz.exchange.ReserveManager
import com.violas.wallet.biz.exchange.UnsupportedTradingPairsException
import com.violas.wallet.repository.subscribeHub.BalanceSubscribeHub
import com.violas.wallet.repository.subscribeHub.BalanceSubscriber
import com.violas.wallet.ui.main.market.MarketViewModel
import com.violas.wallet.ui.main.market.bean.IAssetsMark
import com.violas.wallet.ui.main.market.bean.ITokenVo
import com.violas.wallet.ui.main.market.selectToken.SwapSelectTokenDialog
import com.violas.wallet.ui.main.market.selectToken.SwapSelectTokenDialog.Companion.ACTION_SWAP_SELECT_FROM
import com.violas.wallet.ui.main.market.selectToken.SwapSelectTokenDialog.Companion.ACTION_SWAP_SELECT_TO
import com.violas.wallet.ui.main.market.selectToken.SwapTokensDataResourcesBridge
import com.violas.wallet.utils.authenticateAccount
import com.violas.wallet.utils.convertAmountToDisplayUnit
import com.violas.wallet.utils.convertDisplayUnitToAmount
import com.violas.wallet.viewModel.WalletAppViewModel
import com.violas.wallet.viewModel.bean.AssetsVo
import com.violas.wallet.widget.dialog.PublishTokenDialog
import kotlinx.android.synthetic.main.fragment_swap.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Created by elephant on 2020/6/23 17:18.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 市场兑换视图
 */
class SwapFragment : BaseFragment(), SwapTokensDataResourcesBridge {

    private val swapViewModel by lazy {
        ViewModelProvider(this).get(SwapViewModel::class.java)
    }
    private val marketViewModel by lazy {
        ViewModelProvider(requireParentFragment()).get(MarketViewModel::class.java)
    }
    private val mAccountManager by lazy {
        AccountManager()
    }
    private var mCurrFromAssetsAmount = BigDecimal("0")
    private val mReserveManager by lazy {
        ReserveManager()
    }
    private var isInputFrom = true
    private val mSwapPath = mutableListOf<Int>()
    private var noSwapPath: Boolean? = null

    override fun getLayoutResId(): Int {
        return R.layout.fragment_swap
    }

    override fun onPause() {
        super.onPause()
        clearInputBoxFocusAndHideSoftInput()
    }

    private fun resetState(clearInputBox: Boolean) = launch {
        if (clearInputBox) {
            isInputFrom = true
            handleInputTextWatcher("", etFromInputBox, fromInputTextWatcher)
            handleInputTextWatcher("", etToInputBox, toInputTextWatcher)
        }

        noSwapPath = null
        mSwapPath.clear()
        swapViewModel.getGasFeeLiveData().value = null
        swapViewModel.getExchangeRateLiveData().value = null
        swapViewModel.getHandlingFeeRateLiveDataLiveData().value = null
    }

    private val fromAssertsAmountSubscriber = object : BalanceSubscriber(null) {
        override fun onNotice(assets: AssetsVo?) {
            launch {
                mCurrFromAssetsAmount = BigDecimal(assets?.amountWithUnit?.amount ?: "0")
                withContext(Dispatchers.IO) {
                    swapViewModel.getSupportTokensLiveData().value?.forEach {
                        if (IAssetsMark.convert(it) == getAssetsMark()) {
                            withContext(Dispatchers.Main) {
                                tvFromBalance.text = getString(
                                    R.string.market_common_label_token_balance_format,
                                    "${assets?.amountWithUnit?.amount ?: "0"} ${it.displayName}"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private val toAssertsAmountSubscriber = object : BalanceSubscriber(null) {
        override fun onNotice(assets: AssetsVo?) {
            launch {
                swapViewModel.getSupportTokensLiveData().value?.forEach {
                    if (IAssetsMark.convert(it) == getAssetsMark()) {
                        withContext(Dispatchers.Main) {
                            tvToBalance.text = getString(
                                R.string.market_common_label_token_balance_format,
                                "${assets?.amountWithUnit?.amount ?: "0"} ${it.displayName}"
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onLazyInitViewByResume(savedInstanceState: Bundle?) {
        super.onLazyInitViewByResume(savedInstanceState)

        etFromInputBox.hint = "0.00"
        etToInputBox.hint = "0.00"
        handleValueNull(tvFromBalance, R.string.market_common_label_token_balance_format)
        handleValueNull(tvToBalance, R.string.market_common_label_token_balance_format)
        handleValueNull(tvHandlingFeeRate, R.string.market_swap_label_charge_rate_format)
        handleValueNull(tvExchangeRate, R.string.market_swap_label_swap_rate_format)
        handleValueNull(tvGasFee, R.string.market_swap_label_gas_fee_format)

        mReserveManager.init(this)

        etFromInputBox.addTextChangedListener(fromInputTextWatcher)
        etToInputBox.addTextChangedListener(toInputTextWatcher)
        etFromInputBox.filters = arrayOf(AmountInputFilter(12, 6))
        etToInputBox.filters = arrayOf(AmountInputFilter(12, 6))

        llFromSelectGroup.expandTouchArea(11)
        llToSelectGroup.expandTouchArea(11)

        llFromSelectGroup.setOnClickListener {
            clearInputBoxFocusAndHideSoftInput()
            showSelectTokenDialog(true)
        }

        llToSelectGroup.setOnClickListener {
            clearInputBoxFocusAndHideSoftInput()
            showSelectTokenDialog(false)
            swapViewModel.checkTokenMapInfoAndRefresh()
        }

        btnSwap.setOnClickListener {
            clearInputBoxFocusAndHideSoftInput()

            // 没有钱包账户
            if (!WalletAppViewModel.getViewModelInstance().isExistsAccount()) {
                showToast(R.string.common_tips_account_empty)
                return@setOnClickListener
            }

            // 输入币种没有选择
            val fromToken = swapViewModel.getCurrFromTokenLiveData().value
            if (fromToken == null) {
                showToast(getString(R.string.market_swap_tips_from_token_empty))
                return@setOnClickListener
            }

            // 输出币种没有选择
            val toToken = swapViewModel.getCurrToTokenLiveData().value
            if (toToken == null) {
                showToast(getString(R.string.market_swap_tips_to_token_empty))
                return@setOnClickListener
            }

            // 填写输入金额/输出金额
            if (etFromInputBox.text?.isBlank() == true && etToInputBox.text?.isBlank() == true) {
                showToast(getString(R.string.market_swap_tips_amount_empty))
                return@setOnClickListener
            }

            // 正在估算输出金额
            if (noSwapPath == null) {
                showToast(getString(R.string.market_swap_tips_swap_estimating))
                return@setOnClickListener
            }

            // 没有兑换路径
            if (noSwapPath == true) {
                showToast(
                    getString(
                        R.string.market_swap_tips_swap_no_path_format,
                        fromToken.displayName,
                        toToken.displayName
                    )
                )
                return@setOnClickListener
            }

            // 正在估算输出金额
            if (mSwapPath.size <= 1
                || etFromInputBox.text?.isBlank() == true
                || etToInputBox.text?.isBlank() == true
            ) {
                showToast(getString(R.string.market_swap_tips_swap_estimating))
                return@setOnClickListener
            }

            // 余额不足
            if (mCurrFromAssetsAmount < BigDecimal(
                    etFromInputBox.text?.trim()?.toString() ?: "0"
                )
            ) {
                showToast(R.string.market_swap_tips_insufficient_balance)
                return@setOnClickListener
            }

            showProgress()
            launch(Dispatchers.IO) {
                try {
                    val defaultAccount = mAccountManager.getDefaultAccount()
                    dismissProgress()
                    authenticateAccount(defaultAccount, mAccountManager, passwordCallback = {
                        swap(it.toByteArray())
                    })
                } catch (e: Exception) {
                    dismissProgress()
                }
            }
        }

        swapViewModel.getCurrFromTokenLiveData().observe(viewLifecycleOwner, Observer {
            mCurrFromAssetsAmount = BigDecimal("0")
            if (it == null) {
                tvFromSelectText.text = getString(R.string.market_common_action_select_token)
                handleValueNull(tvFromBalance, R.string.market_common_label_token_balance_format)
                fromAssertsAmountSubscriber.changeSubscriber(null)
            } else {
                tvFromSelectText.text = it.displayName
                fromAssertsAmountSubscriber.changeSubscriber(IAssetsMark.convert(it))
            }
        })
        swapViewModel.getCurrToTokenLiveData().observe(viewLifecycleOwner, Observer {
            if (it == null) {
                tvToSelectText.text = getString(R.string.market_common_action_select_token)
                handleValueNull(tvToBalance, R.string.market_common_label_token_balance_format)
                handleInputTextWatcher("", etToInputBox, toInputTextWatcher)
                toAssertsAmountSubscriber.changeSubscriber(null)
            } else {
                tvToSelectText.text = it.displayName
                toAssertsAmountSubscriber.changeSubscriber(IAssetsMark.convert(it))
            }
        })
        mReserveManager.mChangeLiveData.observe(this, Observer {
            estimateOutputTokenNumber()
        })
        swapViewModel.getHandlingFeeRateLiveDataLiveData().observe(viewLifecycleOwner, Observer {
            if (it == null) {
                handleValueNull(tvHandlingFeeRate, R.string.market_swap_label_charge_rate_format)
            } else {
                tvHandlingFeeRate.text = getString(
                    R.string.market_swap_label_charge_rate_format,
                    "${it.toPlainString()}%"
                )
            }
        })
        swapViewModel.getExchangeRateLiveData().observe(viewLifecycleOwner, Observer {
            if (it == null) {
                handleValueNull(tvExchangeRate, R.string.market_swap_label_swap_rate_format)
            } else {
                tvExchangeRate.text = getString(
                    R.string.market_swap_label_swap_rate_format,
                    "1:${it.toPlainString()}"
                )
            }
        })
        swapViewModel.getGasFeeLiveData().observe(viewLifecycleOwner, Observer {
            if (it == null) {
                handleValueNull(tvGasFee, R.string.market_swap_label_gas_fee_format)
            } else {
                tvGasFee.text = getString(
                    R.string.market_swap_label_gas_fee_format,
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

        BalanceSubscribeHub.observe(this, fromAssertsAmountSubscriber)
        BalanceSubscribeHub.observe(this, toAssertsAmountSubscriber)

        marketViewModel.getMarketSupportCoinsLiveData().observe(this, Observer {
            swapViewModel.onTokenChange(it)
        })
    }

    private fun swap(pwd: ByteArray) {
        showProgress()
        launch(Dispatchers.IO) {
            try {
                swapViewModel.swap(
                    pwd,
                    etFromInputBox.text.toString(),
                    etToInputBox.text.toString(),
                    mSwapPath,
                    isInputFrom
                )
                CommandActuator.postDelay(RefreshAssetsAllListCommand(), 2000)
                resetState(true)
                showToast(getString(R.string.market_swap_tips_swap_success))
            } catch (e: UnsupportedTradingPairsException) {
                showToast(getString(R.string.market_swap_tips_unsupported_pair))
            } catch (e: AccountPayeeNotFindException) {
                showToast(getString(R.string.chain_tips_payee_account_not_active))
            } catch (e: AccountPayeeTokenNotActiveException) {
                showPublishTokenDialog(pwd, e)
            } catch (e: Exception) {
                e.printStackTrace()
                showToast(e.getShowErrorMessage(R.string.market_swap_tips_swap_failure))
            }
            dismissProgress()
        }
    }

    private fun showPublishTokenDialog(
        pwd: ByteArray,
        e: AccountPayeeTokenNotActiveException
    ) {
        PublishTokenDialog()
            .setAddCurrencyPage(false)
            .setCurrencyName(e.assetsMark.displayName)
            .setConfirmListener {
                it.dismiss()
                launch(Dispatchers.IO) {
                    showProgress()
                    try {
                        if (swapViewModel.publishToken(pwd, e.coinTypes, e.assetsMark)) {
                            swap(pwd)
                        } else {
                            showToast(R.string.txn_details_state_add_currency_failure)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        showToast(R.string.txn_details_state_add_currency_failure)
                        dismissProgress()
                    }
                }
            }.show(parentFragmentManager)
    }

    private fun estimateOutputTokenNumber() {
        launch(Dispatchers.IO) {
            try {
                val fromToken = swapViewModel.getCurrFromTokenLiveData().value
                val toToken = swapViewModel.getCurrToTokenLiveData().value

                val inputAmountStr = etFromInputBox.text.toString()
                val outputAmountStr = etToInputBox.text.toString()

                if (fromToken == null || toToken == null) {
                    return@launch
                }

                var inputAmount = if (isInputFrom) {
                    if (fromToken.coinNumber == CoinTypes.BitcoinTest.coinType() ||
                        fromToken.coinNumber == CoinTypes.Bitcoin.coinType()
                    ) {
                        convertDisplayUnitToAmount(
                            inputAmountStr,
                            CoinTypes.parseCoinType(fromToken.coinNumber)
                        ) / 100
                    } else {
                        convertDisplayUnitToAmount(
                            inputAmountStr,
                            CoinTypes.parseCoinType(fromToken.coinNumber)
                        )
                    }
                } else {
                    if (toToken.coinNumber == CoinTypes.BitcoinTest.coinType() ||
                        toToken.coinNumber == CoinTypes.Bitcoin.coinType()
                    ) {
                        convertDisplayUnitToAmount(
                            outputAmountStr,
                            CoinTypes.parseCoinType(toToken.coinNumber)
                        ) / 100
                    } else {
                        convertDisplayUnitToAmount(
                            outputAmountStr,
                            CoinTypes.parseCoinType(toToken.coinNumber)
                        )
                    }
                }

                if (!isInputFrom) {
                    inputAmount += (inputAmount * SwapViewModel.MINIMUM_PRICE_FLUCTUATION).toLong()
                }

                val outputEdit = if (isInputFrom) {
                    etToInputBox
                } else {
                    etFromInputBox
                }

                if (inputAmount == 0L) {
                    withContext(Dispatchers.Main) {
                        withContext(Dispatchers.Main) {
                            handleInputTextWatcher(
                                "", outputEdit, if (isInputFrom) {
                                    toInputTextWatcher
                                } else {
                                    fromInputTextWatcher
                                }
                            )
                            swapViewModel.getExchangeRateLiveData().value = null
                            swapViewModel.getHandlingFeeRateLiveDataLiveData().value = null
                        }
                    }
                    return@launch
                }

                val tradeExact =
                    mReserveManager.tradeExact(fromToken, toToken, inputAmount, isInputFrom)
                if (tradeExact == null) {
                    withContext(Dispatchers.Main) {
                        noSwapPath = true
                        mSwapPath.clear()
                        swapViewModel.getExchangeRateLiveData().value =
                            BigDecimal.valueOf(0)
                        swapViewModel.getHandlingFeeRateLiveDataLiveData().value =
                            BigDecimal.valueOf(0)
                        handleInputTextWatcher(
                            "", outputEdit, if (isInputFrom) {
                                toInputTextWatcher
                            } else {
                                fromInputTextWatcher
                            }
                        )
                    }
                } else {
                    // 获取应该输出的币种信息
                    val outputCoin = if (isInputFrom) {
                        toToken
                    } else {
                        fromToken
                    }
                    // 获取计算出来的金额
                    var outputAmount = tradeExact.amount
                    if (isInputFrom) {
                        if (toToken.coinNumber == CoinTypes.BitcoinTest.coinType() ||
                            toToken.coinNumber == CoinTypes.Bitcoin.coinType()
                        ) {
                            outputAmount *= 100
                        }
                    } else {
                        if (fromToken.coinNumber == CoinTypes.BitcoinTest.coinType() ||
                            fromToken.coinNumber == CoinTypes.Bitcoin.coinType()
                        ) {
                            outputAmount *= 100
                        }
                    }

                    // 根据币种信息转换计算出来的金额单位
                    val outputAmountByCoin = convertAmountToDisplayUnit(
                        outputAmount,
                        CoinTypes.parseCoinType(outputCoin.coinNumber)
                    ).first

                    // 计算手续费率
                    val handlingFeeRate = if (isInputFrom) {
                        BigDecimal(tradeExact.fee).divide(
                            BigDecimal(inputAmount),
                            6,
                            RoundingMode.HALF_UP
                        )
                    } else {
                        BigDecimal(tradeExact.fee).divide(
                            BigDecimal(outputAmount),
                            6,
                            RoundingMode.HALF_UP
                        )
                    }.multiply(BigDecimal("100"))

                    // 计算兑换率
                    val exchangeRate = if (isInputFrom) {
                        BigDecimal(outputAmountByCoin).divide(
                            BigDecimal(inputAmountStr).divide(
                                BigDecimal(1),
                                6,
                                RoundingMode.HALF_UP
                            ),
                            6,
                            RoundingMode.HALF_UP
                        )
                    } else {
                        try {
                            BigDecimal(outputAmountStr).divide(
                                BigDecimal(outputAmountByCoin).divide(
                                    BigDecimal(1),
                                    6,
                                    RoundingMode.HALF_UP
                                ),
                                6,
                                RoundingMode.HALF_UP
                            )
                        } catch (e: Exception) {
                            BigDecimal("0")
                        }
                    }

                    withContext(Dispatchers.Main) {
                        noSwapPath = false
                        mSwapPath.clear()
                        mSwapPath.addAll(tradeExact.path)
                        swapViewModel.getExchangeRateLiveData().value = exchangeRate
                        swapViewModel.getHandlingFeeRateLiveDataLiveData().value = handlingFeeRate
                        handleInputTextWatcher(
                            outputAmountByCoin, outputEdit, if (isInputFrom) {
                                toInputTextWatcher
                            } else {
                                fromInputTextWatcher
                            }
                        )
                    }
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    //*********************************** 选择Token逻辑 ***********************************//
    private fun showSelectTokenDialog(selectFrom: Boolean) {
        SwapSelectTokenDialog
            .newInstance(
                if (selectFrom) ACTION_SWAP_SELECT_FROM else ACTION_SWAP_SELECT_TO
            )
            .setCallback { action, iToken ->
                launch {
                    resetState(false)
                    swapViewModel.selectToken(ACTION_SWAP_SELECT_FROM == action, iToken)
                    estimateOutputTokenNumber()
                }
            }
            .show(childFragmentManager)
    }

    override fun getMarketSupportFromTokensLiveData(): LiveData<List<ITokenVo>?> {
        return swapViewModel.getSupportTokensLiveData()
    }

    override fun loadMarketSupportFromTokens(failureCallback: (error: Throwable) -> Unit) {
        marketViewModel.execute(failureCallback = failureCallback)
    }

    override suspend fun getMarketSupportToTokens(): List<ITokenVo>? {
        return try {
            swapViewModel.getSwapToTokenList()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun getCurrCoin(action: Int): ITokenVo? {
        return if (action == ACTION_SWAP_SELECT_FROM)
            swapViewModel.getCurrFromTokenLiveData().value
        else
            swapViewModel.getCurrToTokenLiveData().value
    }

    //*********************************** 输入框逻辑 ***********************************//
    private val fromInputTextWatcher = object : TextWatcherSimple() {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (!etFromInputBox.isFocused) return

            isInputFrom = true
            val inputText = s?.toString() ?: ""
            val amountStr = handleInputText(inputText)
            if (inputText != amountStr) {
                handleInputTextWatcher(amountStr, etFromInputBox, this)
            }
            estimateOutputTokenNumber()
        }
    }

    private val toInputTextWatcher = object : TextWatcherSimple() {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (!etToInputBox.isFocused) return

            isInputFrom = false
            val inputText = s?.toString() ?: ""
            val amountStr = handleInputText(inputText)
            if (inputText != amountStr) {
                handleInputTextWatcher(amountStr, etToInputBox, this)
            }
            estimateOutputTokenNumber()
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
        textView.text = getString(formatResId, getString(R.string.common_desc_value_null))
    }
}