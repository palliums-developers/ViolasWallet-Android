package com.violas.wallet.ui.main.market.swap

import android.os.Bundle
import android.text.AmountInputFilter
import android.text.TextWatcher
import android.widget.EditText
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseFragment
import com.palliums.extensions.show
import com.palliums.net.LoadState
import com.palliums.utils.TextWatcherSimple
import com.palliums.utils.stripTrailingZeros
import com.violas.wallet.R
import com.violas.wallet.ui.main.market.bean.ITokenVo
import com.violas.wallet.ui.main.market.selectToken.SelectTokenDialog
import com.violas.wallet.ui.main.market.selectToken.SelectTokenDialog.Companion.ACTION_SWAP_SELECT_FROM
import com.violas.wallet.ui.main.market.selectToken.SelectTokenDialog.Companion.ACTION_SWAP_SELECT_TO
import com.violas.wallet.ui.main.market.selectToken.TokensBridge
import com.violas.wallet.viewModel.MarketTokensViewModel
import kotlinx.android.synthetic.main.fragment_swap.*

/**
 * Created by elephant on 2020/6/23 17:18.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 市场兑换视图
 */
class SwapFragment : BaseFragment(), TokensBridge {

    private val swapViewModel by lazy {
        ViewModelProvider(this).get(SwapViewModel::class.java)
    }
    private val marketTokensViewModel by lazy {
        ViewModelProvider(requireParentFragment()).get(MarketTokensViewModel::class.java)
    }


    override fun getLayoutResId(): Int {
        return R.layout.fragment_swap
    }

    override fun onLazyInitViewByResume(savedInstanceState: Bundle?) {
        super.onLazyInitViewByResume(savedInstanceState)

        etFromInputBox.hint = "0.00"
        etToInputBox.hint = "0.00"
        tvHandlingFeeRate.text = getString(R.string.handling_fee_rate_format, "- -")
        tvExchangeRate.text = getString(R.string.exchange_rate_format, "- -")
        tvGasFee.text = getString(R.string.gas_fee_format, "- -")

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
            tvFromSelectText.text = it?.displayName ?: getString(R.string.select_token)
        })
        swapViewModel.getCurrToTokenLiveData().observe(viewLifecycleOwner, Observer {
            tvToSelectText.text = it?.displayName ?: getString(R.string.select_token)
        })
        swapViewModel.getHandlingFeeRateLiveDataLiveData().observe(viewLifecycleOwner, Observer {
            tvHandlingFeeRate.text = getString(
                R.string.handling_fee_rate_format,
                if (it != null) "${it.toPlainString()}%" else "- -"
            )
        })
        swapViewModel.getExchangeRateLiveData().observe(viewLifecycleOwner, Observer {
            tvExchangeRate.text = getString(
                R.string.exchange_rate_format,
                if (it != null) "1:${it.toPlainString()}" else "- -"
            )
        })
        swapViewModel.getGasFeeLiveData().observe(viewLifecycleOwner, Observer {
            tvGasFee.text = getString(
                R.string.gas_fee_format,
                it?.toString() ?: "- -"
            )
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

        if (!marketTokensViewModel.tipsMessage.hasObservers()) {
            marketTokensViewModel.tipsMessage.observe(viewLifecycleOwner, Observer {
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

    override fun getMarketSupportTokens() {
        marketTokensViewModel.execute()
    }

    override fun getMarketSupportTokensLiveData(): LiveData<List<ITokenVo>?> {
        return marketTokensViewModel.getMarketSupportTokensLiveData()
    }

    override fun getCurrToken(action: Int): ITokenVo? {
        return if (action == ACTION_SWAP_SELECT_FROM)
            swapViewModel.getCurrFromTokenLiveData().value
        else
            swapViewModel.getCurrToTokenLiveData().value
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
}