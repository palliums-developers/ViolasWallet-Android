package com.violas.wallet.ui.main.quotes

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.TextWatcher
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseFragment
import com.palliums.utils.TextWatcherSimple
import com.palliums.utils.coroutineExceptionHandler
import com.palliums.utils.stripTrailingZeros
import com.palliums.utils.toBigDecimal
import com.violas.wallet.R
import com.violas.wallet.biz.WrongPasswordException
import com.violas.wallet.ui.dexOrder.DexOrdersActivity
import com.violas.wallet.ui.main.quotes.tokenList.TokenBottomSheetDialogFragment
import com.violas.wallet.widget.dialog.PasswordInputDialog
import kotlinx.android.synthetic.main.fragment_quotes.*
import kotlinx.android.synthetic.main.fragment_quotes_content.*
import kotlinx.android.synthetic.main.fragment_quotes_did_not_open.*
import kotlinx.coroutines.launch
import java.math.BigDecimal


class QuotesFragment : BaseFragment() {
    override fun getLayoutResId(): Int {
        return R.layout.fragment_quotes
    }

    private val mConstraintSet = ConstraintSet()
    private val mConstraintSet2 = ConstraintSet()

    private val mQuotesViewModel by lazy {
        ViewModelProvider(this).get(QuotesViewModel::class.java)
    }

    private val mAllOrderAdapter by lazy {
        AllOrderAdapter()
    }
    private val mMeOrderAdapter by lazy {
        MeOrderAdapter()
    }

    private var mIvEntrustOthersAnim: ObjectAnimator? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAnim()
        initViewEvent()
        handleIsEnableObserve()
        handleExchangeCoinObserve()
        handlePositiveChangeObserve()
        handleExchangeRateObserve()
        handleMeOrderObserve()
        handleAllOrderObserve()
        handleIvEntrustOthersAnimObserve()
        handleCurrentExchangeCoinObserve()
        handleEditExchangeCoinObserve()
    }

    private fun handleIvEntrustOthersAnimObserve() {
        mQuotesViewModel.isShowMoreAllOrderLiveData.observe(viewLifecycleOwner, Observer {
            if (it) {
                mIvEntrustOthersAnim?.reverse()
            } else {
                mIvEntrustOthersAnim?.start()
            }
        })
    }

    private fun handleIsEnableObserve() {
        mQuotesViewModel.isEnable.observe(viewLifecycleOwner, Observer {
            try {
                layoutNotOpen.inflate()
            } catch (e: Exception) {
            }
            if (it) {
                layoutNotOpenRoot.visibility = View.GONE
                layoutQuotesContent.visibility = View.VISIBLE
            } else {
                layoutNotOpenRoot.visibility = View.VISIBLE
                layoutQuotesContent.visibility = View.GONE
            }
        })
    }

    private fun initAnim() {
        mConstraintSet.clone(layoutCoinConversion)
        mConstraintSet2.clone(context, R.layout.fragment_quotes_coin_anim)

        mIvEntrustOthersAnim = ObjectAnimator.ofFloat(ivEntrustOthers, "rotation", 0F, 180F)
            .setDuration(400)
    }

    private fun initViewEvent() {
        ivConversion.setOnClickListener { mQuotesViewModel.clickPositiveChange() }
        recyclerViewMeOrder.adapter = mMeOrderAdapter
        recyclerViewAllOrder.adapter = mAllOrderAdapter
        layoutFromCoin.setOnClickListener { showTokenFragment(it) }
        layoutToCoin.setOnClickListener { showTokenFragment(it) }
        layoutEntrustOthers.setOnClickListener { mQuotesViewModel.clickShowMoreAllOrder() }
        editFromCoin.addTextChangedListener(mFromAmountTextWatcher)
        editToCoin.addTextChangedListener(mToAmountTextWatcher)
        tvMyAllEntrust.setOnClickListener {
            startActivity(Intent(context, DexOrdersActivity::class.java))
        }
        btnExchange.setOnClickListener {
            handleBtnExchangeClick()
        }
    }

    private fun handleBtnExchangeClick() {
        val fromBigDecimal = editFromCoin.text.toString().toBigDecimal()
        val toBigDecimal = editToCoin.text.toString().toBigDecimal()
        if(fromBigDecimal== BigDecimal("0") || toBigDecimal== BigDecimal("0") ){
            showToast(getString(R.string.hint_change_number_not_zero))
            return
        }
        PasswordInputDialog()
            .setConfirmListener { password, dialogFragment ->
                launch(coroutineExceptionHandler()) {
                    showProgress()
                    try {
                        val handleExchange =
                            mQuotesViewModel.handleExchange(password, fromBigDecimal, toBigDecimal)
                        if (handleExchange) {
                            showToast(getString(R.string.hint_exchange_successful))
                        } else {
                            showToast(getString(R.string.hint_exchange_error))
                        }
                    } catch (e: ExchangeCoinEqualException) {
                        showToast("${tvFromCoin.text}${getString(R.string.hint_unable_change)}${tvToCoin.text}")
                        e.printStackTrace()
                    } catch (e: WrongPasswordException) {
                        showToast(getString(R.string.hint_password_error))
                        e.printStackTrace()
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        showToast(getString(R.string.hint_unknown_error))
                    }
                    dismissProgress()
                }
                dialogFragment.dismiss()
            }
            .show(childFragmentManager)
    }

    private fun showTokenFragment(view: View?) {
        val sheetDialogFragment = TokenBottomSheetDialogFragment.newInstance()
        sheetDialogFragment.show(childFragmentManager, mQuotesViewModel.getTokenList()) {
            sheetDialogFragment.dismiss()
            when (view?.id) {
                R.id.layoutFromCoin -> {
                    mQuotesViewModel.currentFormCoinLiveData.postValue(it)
                }
                R.id.layoutToCoin -> {
                    mQuotesViewModel.currentToCoinLiveData.postValue(it)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun handleCurrentExchangeCoinObserve() {
        mQuotesViewModel.currentExchangeCoinLiveData.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                tvMeOrderEntrustNumber.text =
                    "${getString(R.string.label_number)}(${it.tokenUnit()})"
                tvMeOrderEntrustPrice.text = "${getString(R.string.label_price)}(${it.tokenUnit()})"
                tvAllOrderEntrustPrice.text =
                    "${getString(R.string.label_price)}(${it.tokenUnit()})"
            }
        })
    }

    private fun handleExchangeCoinObserve() {
        mQuotesViewModel.currentFormCoinLiveData.observe(viewLifecycleOwner, Observer {
            tvFromCoin.text = it.tokenName()
        })
        mQuotesViewModel.currentToCoinLiveData.observe(viewLifecycleOwner, Observer {
            tvToCoin.text = it.tokenName()
        })
    }

    private fun handleEditExchangeCoinObserve() {
        val calculate = {
            mQuotesViewModel.changeToCoinAmount(editFromCoin.text.toString())
        }
        mQuotesViewModel.exchangeRateNumberLiveData.observe(viewLifecycleOwner, Observer {
            calculate()
        })

        mQuotesViewModel.mFromCoinAmountLiveData.observe(viewLifecycleOwner, Observer {
            editFromCoin.setText(it)
        })
        mQuotesViewModel.mToCoinAmountLiveData.observe(viewLifecycleOwner, Observer {
            editToCoin.setText(it)
        })
    }

    private fun handlePositiveChangeObserve() {
        mQuotesViewModel.isPositiveChangeLiveData.observe(viewLifecycleOwner, Observer {
            val transition = AutoTransition()
            transition.duration = 500
            TransitionManager.beginDelayedTransition(layoutCoinConversion, transition)
            if (it) {
                mConstraintSet2.applyTo(layoutCoinConversion)
            } else {
                mConstraintSet.applyTo(layoutCoinConversion)
            }
        })
    }

    private fun handleExchangeRateObserve() {
        mQuotesViewModel.exchangeRateLiveData.observe(viewLifecycleOwner, Observer {
            tvParitiesInfo.text = it
        })
    }

    private fun handleMeOrderObserve() {
        mQuotesViewModel.meOrdersLiveData.observe(viewLifecycleOwner, Observer {
            mMeOrderAdapter.submitList(it)
            if (it == null || it.isEmpty()) {
                viewMeOrderNull.visibility = View.VISIBLE
                layoutMeOrder.visibility = View.GONE
            } else {
                viewMeOrderNull.visibility = View.GONE
                layoutMeOrder.visibility = View.VISIBLE
            }
        })
    }

    private fun handleAllOrderObserve() {
        mQuotesViewModel.allDisplayOrdersLiveData.observe(viewLifecycleOwner, Observer {
            mAllOrderAdapter.submitList(it)
            if (it == null || it.isEmpty()) {
                viewAllOrderNull.visibility = View.VISIBLE
                layoutAllOrder.visibility = View.GONE
            } else {
                viewAllOrderNull.visibility = View.GONE
                layoutAllOrder.visibility = View.VISIBLE
            }
        })
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

                mQuotesViewModel.changeToCoinAmount(amountStr)
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

                mQuotesViewModel.changeFromCoinAmount(amountStr)
            }
        }
    }
}

