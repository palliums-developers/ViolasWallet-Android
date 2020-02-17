package com.violas.wallet.ui.outsideExchange

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.AmountInputFilter
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseFragment
import com.palliums.utils.TextWatcherSimple
import com.palliums.utils.stripTrailingZeros
import com.palliums.utils.toBigDecimal
import com.violas.wallet.R
import com.violas.wallet.biz.LackOfBalanceException
import com.violas.wallet.common.EXTRA_KEY_ACCOUNT_ID
import com.violas.wallet.ui.account.AccountType
import com.violas.wallet.ui.account.operations.AccountOperationsActivity
import com.violas.wallet.widget.dialog.ExchangeMappingPasswordDialog
import kotlinx.android.synthetic.main.outside_exchange_fragment.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode

class OutsideExchangeFragment : BaseFragment() {

    companion object {
        private const val REQUEST_CODE_SELECT_ACCOUNT = 100

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
        viewModel.init(accountId)
        initViewEvent()
        handlerExchangeCoin()
        handlerExchangeRate()
        handlerReceivingAccount()
        handlerExchangeNumber()
    }

    override fun onViewClick(view: View) {
        when (view.id) {
            R.id.ivSelectAccount -> {
                AccountOperationsActivity.selectAccount(
                    this,
                    REQUEST_CODE_SELECT_ACCOUNT,
                    AccountType.VIOLAS
                )
            }

            R.id.tvOrders -> {
                // TODO
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_SELECT_ACCOUNT -> {
                if (resultCode != Activity.RESULT_OK) {
                    return
                }

                val accountId = data?.getLongExtra(EXTRA_KEY_ACCOUNT_ID, -100L)
                if (accountId == null || accountId == -100L) {
                    return
                }

                viewModel.switchReceiveAccount(accountId)
            }
        }
    }

    private fun initViewEvent() {
        editFromCoin.addTextChangedListener(mFromAmountTextWatcher)
        editToCoin.addTextChangedListener(mToAmountTextWatcher)
        editFromCoin.filters = arrayOf(AmountInputFilter(4, 6))
        editToCoin.filters = arrayOf(AmountInputFilter(4, 6))
        btnExchange.setOnClickListener {
            initiateChange()
        }
        btnCancel.setOnClickListener { finishActivity() }
        ivSelectAccount.setOnClickListener(this)
        tvOrders.setOnClickListener(this)
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
        val fromBigDecimal = editFromCoin.text.toString().toBigDecimal()
        val toBigDecimal = editToCoin.text.toString().toBigDecimal()
        if (fromBigDecimal <= BigDecimal("0") || toBigDecimal <= BigDecimal("0")) {
            showToast(getString(R.string.hint_change_number_not_zero))
            return
        }
        showPasswordDialog { sendAccountKey,
                             receiveAccountKey ->
            showProgress()
            viewModel.initiateChange(sendAccountKey, receiveAccountKey, {
                dismissProgress()
                showToast(R.string.hint_exchange_successful)
                finishActivity()
            }, {
                dismissProgress()
                it.printStackTrace()
                when (it) {
                    is LackOfBalanceException -> {
                        showToast(getString(R.string.hint_insufficient_or_trading_fees_are_confirmed))
                    }
                    else -> {
                        it.message?.let { message ->
                            showToast(message)
                        }
                    }
                }
            })
        }
    }

    private fun showPasswordDialog(callback: (ByteArray, ByteArray) -> Unit) {
        val exchangePair = viewModel.getExchangePairChainName()
        ExchangeMappingPasswordDialog()
            .setSendHint(getString(R.string.hint_input_account_password, exchangePair.first))
            .setReceiveHint(getString(R.string.hint_input_account_password, exchangePair.second))
            .setConfirmListener { sendPassword, receivePassword, dialogFragment ->
                showProgress()
                launch(Dispatchers.IO) {
                    val decryptSendAccountKey = viewModel.decryptSendAccountKey(sendPassword)
                    if (decryptSendAccountKey == null) {
                        dialogFragment.setErrorHint(
                            getString(
                                R.string.hint_input_account_password_error,
                                exchangePair.first
                            )
                        )
                        dismissProgress()
                        return@launch
                    }
                    val decryptReceiveAccountKey =
                        viewModel.decryptReceiveAccountKey(receivePassword)
                    if (decryptReceiveAccountKey == null) {
                        dialogFragment.setErrorHint(
                            getString(
                                R.string.hint_input_account_password_error,
                                exchangePair.second
                            )
                        )
                        dismissProgress()
                        return@launch
                    }
                    withContext(Dispatchers.Main) {
                        dismissProgress()
                        dialogFragment.dismiss()
                        callback(decryptSendAccountKey, decryptReceiveAccountKey)
                    }
                }
            }
            .show(childFragmentManager)
    }
}
