package com.violas.wallet.ui.outsideExchange

import android.os.Bundle
import android.text.AmountInputFilter
import android.text.TextWatcher
import android.widget.EditText
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseFragment
import com.palliums.utils.TextWatcherSimple
import com.palliums.utils.stripTrailingZeros
import com.violas.wallet.R
import kotlinx.android.synthetic.main.outside_exchange_fragment.*
import java.math.BigDecimal
import java.math.RoundingMode

class OutsideExchangeFragment : BaseFragment() {

    companion object {
        fun newInstance(accountId: Long): OutsideExchangeFragment {
            val fragment = OutsideExchangeFragment()
            fragment.accountId = accountId
            return fragment
        }
    }

    private var accountId: Long = -1
    private lateinit var viewModel: OutsideExchangeViewModel

    override fun getLayoutResId(): Int {
        return R.layout.outside_exchange_fragment
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(OutsideExchangeViewModel::class.java)
        viewModel.exchange(accountId)
        initViewEvent()
        handlerExchangeCoin()
        handlerExchangeRate()
        handlerReceivingAccount()
        handlerExchangeNumber()
    }

    private fun initViewEvent() {
        editFromCoin.addTextChangedListener(mFromAmountTextWatcher)
        editToCoin.addTextChangedListener(mToAmountTextWatcher)
        editFromCoin.filters = arrayOf(AmountInputFilter(3, 8))
        editToCoin.filters = arrayOf(AmountInputFilter(3, 8))
        btnExchange.setOnClickListener {
            initiateChange()
        }
    }

    private fun handlerExchangeNumber() {
        viewModel.mFromCoinAmountLiveData.observe(viewLifecycleOwner, Observer {
            if (it == BigDecimal("0")) {
                editFromCoin.setText("")
                return@Observer
            }
            editFromCoin.setText(
                it.setScale(
                    8,
                    RoundingMode.DOWN
                ).stripTrailingZeros().toPlainString()
            )
        })
        viewModel.mToCoinAmountLiveData.observe(viewLifecycleOwner, Observer {
            if (it == BigDecimal("0")) {
                editToCoin.setText("")
                return@Observer
            }
            editToCoin.setText(
                it.setScale(
                    8,
                    RoundingMode.DOWN
                ).stripTrailingZeros().toPlainString()
            )
        })
    }

    private fun handlerReceivingAccount() {
        viewModel.stableCurrencyReceivingAccountLiveData.observe(viewLifecycleOwner, Observer {
            tvReceiveAddressInfo.text = it.address
        })
    }

    private fun handlerExchangeRate() {
        viewModel.exchangeRateValueLiveData.observe(viewLifecycleOwner, Observer {
            tvParitiesInfo.text = it
        })
    }

    private fun handlerExchangeCoin() {
        viewModel.exchangeFromCoinLiveData.observe(viewLifecycleOwner, Observer {
            tvFromCoin.text = it
        })
        viewModel.exchangeToCoinLiveData.observe(viewLifecycleOwner, Observer {
            tvToCoin.text = it
        })
    }

    // =========  处理输入金额 ====== //

    private val mHandlerAmountEditTextWatcher: (String, EditText, TextWatcher) -> Unit =
        { amountStr, editAmountCoin, textWatcher ->
            editAmountCoin.removeTextChangedListener(textWatcher)

            editAmountCoin.setText(amountStr)
            editAmountCoin.setSelection(amountStr.length)

            editAmountCoin.addTextChangedListener(textWatcher)
        }

    private val mFromAmountTextWatcher = object : TextWatcherSimple() {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (editFromCoin.isFocused) {
                val inputText = s?.toString() ?: ""
                val amountStr = mHandlerAmountEdit(inputText)

                if (inputText != amountStr) {
                    mHandlerAmountEditTextWatcher(amountStr, editFromCoin, this)
                }

                viewModel.changeToCoinAmount(amountStr)
            }
        }
    }

    private val mToAmountTextWatcher = object : TextWatcherSimple() {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (editToCoin.isFocused) {
                val inputText = s?.toString() ?: ""
                val amountStr = mHandlerAmountEdit(inputText)

                if (inputText != amountStr) {
                    mHandlerAmountEditTextWatcher(amountStr, editToCoin, this)
                }

                viewModel.changeFromCoinAmount(amountStr)
            }
        }
    }

    private val mHandlerAmountEdit: (String) -> String = { inputText ->
        var amoutStr = inputText
        if (inputText.startsWith(".")) {
            amoutStr = "0$inputText"
        } else if (inputText.isNotEmpty()) {
            amoutStr = (inputText + 1).stripTrailingZeros()
            amoutStr = amoutStr.substring(0, amoutStr.length - 1)
            if (amoutStr.isEmpty()) {
                amoutStr = "0"
            }
        }
        amoutStr
    }

    // ======= 发起兑换的验证 ====== //
    private fun initiateChange() {
        viewModel.initiateChange()
    }
}
