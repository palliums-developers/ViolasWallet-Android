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
import com.palliums.extensions.show
import com.palliums.net.LoadState
import com.palliums.utils.TextWatcherSimple
import com.palliums.utils.stripTrailingZeros
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.biz.ExchangeManager
import com.violas.wallet.ui.main.market.MarketViewModel
import com.violas.wallet.ui.main.market.bean.ITokenVo
import com.violas.wallet.ui.main.market.selectToken.SelectTokenDialog
import com.violas.wallet.ui.main.market.selectToken.SelectTokenDialog.Companion.ACTION_SWAP_SELECT_FROM
import com.violas.wallet.ui.main.market.selectToken.SelectTokenDialog.Companion.ACTION_SWAP_SELECT_TO
import com.violas.wallet.ui.main.market.selectToken.TokensBridge
import com.violas.wallet.ui.main.market.selectToken.TokensSwapFilterBridge
import com.violas.wallet.utils.convertAmountToDisplayUnit
import kotlinx.android.synthetic.main.fragment_swap.*

/**
 * Created by elephant on 2020/6/23 17:18.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 市场兑换视图
 */
class SwapFragment : BaseFragment(), TokensBridge, TokensSwapFilterBridge {

    private val swapViewModel by lazy {
        ViewModelProvider(this).get(SwapViewModel::class.java)
    }
    private val marketViewModel by lazy {
        ViewModelProvider(requireParentFragment()).get(MarketViewModel::class.java)
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_swap
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
            showSelectTokenDialog(true)
        }

        llToSelectGroup.setOnClickListener {
            showSelectTokenDialog(false)
        }

        btnSwap.setOnClickListener {
            // TODO 兑换逻辑
        }

        swapViewModel.getCurrFromTokenLiveData().observe(viewLifecycleOwner, Observer {
            if (it == null) {
                tvFromSelectText.text = getString(R.string.select_token)
                handleValueNull(tvFromBalance, R.string.market_token_balance_format)
            } else {
                tvFromSelectText.text = it.displayName
                val amountWithUnit =
                    convertAmountToDisplayUnit(it.amount, CoinTypes.parseCoinType(it.coinNumber))
                tvFromBalance.text = getString(
                    R.string.market_token_balance_format,
                    "${amountWithUnit.first} ${it.displayName}"
                )
            }
        })
        swapViewModel.getCurrToTokenLiveData().observe(viewLifecycleOwner, Observer {
            if (it == null) {
                tvToSelectText.text = getString(R.string.select_token)
                handleValueNull(tvToBalance, R.string.market_token_balance_format)
            } else {
                tvToSelectText.text = it.displayName
                val amountWithUnit =
                    convertAmountToDisplayUnit(it.amount, CoinTypes.parseCoinType(it.coinNumber))
                tvToBalance.text = getString(
                    R.string.market_token_balance_format,
                    "${amountWithUnit.first} ${it.displayName}"
                )
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
    }

    //*********************************** 选择Token逻辑 ***********************************//
    private fun showSelectTokenDialog(selectFrom: Boolean) {
        SelectTokenDialog
            .newInstance(
                if (selectFrom) ACTION_SWAP_SELECT_FROM else ACTION_SWAP_SELECT_TO
            )
            .setCallback {
                swapViewModel.selectToken(selectFrom, it)
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

    override fun filter(currToken: ITokenVo?, it: ITokenVo): Boolean {
        if (currToken == null) {
            return false
        }
        return swapViewModel.filter(currToken, it)
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

    //*********************************** 其它逻辑 ***********************************//
    private val handleValueNull: (TextView, Int) -> Unit = { textView, formatResId ->
        textView.text = getString(formatResId, getString(R.string.market_value_null))
    }
}