package com.violas.wallet.ui.main.market.fundPool

import android.animation.ObjectAnimator
import android.os.Bundle
import android.text.AmountInputFilter
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.enums.PopupAnimation
import com.palliums.base.BaseFragment
import com.palliums.net.LoadState
import com.palliums.utils.TextWatcherSimple
import com.palliums.utils.getColorByAttrId
import com.palliums.utils.stripTrailingZeros
import com.palliums.widget.popup.EnhancedPopupCallback
import com.violas.wallet.R
import com.violas.wallet.event.SwitchFundPoolOpModeEvent
import com.violas.wallet.ui.main.market.MarketSwitchPopupView
import com.violas.wallet.ui.main.market.fundPool.FundPoolViewModel.Companion.ACTION_GET_TOKEN_PAIRS
import kotlinx.android.synthetic.main.fragment_fund_pool.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

/**
 * Created by elephant on 2020/6/23 17:18.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 市场资金池视图
 */
class FundPoolFragment : BaseFragment() {

    private val fundPoolViewModel by lazy {
        ViewModelProvider(this).get(FundPoolViewModel::class.java)
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_fund_pool
    }

    override fun onDestroyView() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        super.onDestroyView()
    }

    override fun onLazyInitViewByResume(savedInstanceState: Bundle?) {
        super.onLazyInitViewByResume(savedInstanceState)
        EventBus.getDefault().register(this)

        tvExchangeRate.text = getString(R.string.exchange_rate_format, "- -")
        tvShareOfPool.text = getString(R.string.my_fund_pool_amount_format, "- -")

        llSwitchOpModeGroup.setOnClickListener {
            showSwitchOpModePopup()
        }

        llFirstSelectGroup.setOnClickListener {
            showSelectTokenDialog(true)
        }

        llSecondSelectGroup.setOnClickListener {
            if (fundPoolViewModel.isTransferInMode()) {
                showSelectTokenDialog(false)
            } else {
                fundPoolViewModel.execute(action = ACTION_GET_TOKEN_PAIRS)
            }
        }

        btnPositive.setOnClickListener {

        }

        etFirstInputBox.addTextChangedListener(firstInputTextWatcher)
        etSecondInputBox.addTextChangedListener(secondInputTextWatcher)
        etFirstInputBox.filters = arrayOf(AmountInputFilter(12, 2))
        etSecondInputBox.filters = arrayOf(AmountInputFilter(12, 2))

        fundPoolViewModel.getCurrOpModeLiveData()
            .observe(viewLifecycleOwner, currOpModeObserver)
        fundPoolViewModel.getDisplayTokenPairsLiveData()
            .observe(viewLifecycleOwner, displayTokenPairsObserver)

        fundPoolViewModel.loadState.observe(this, Observer {
            when (it.peekData().status) {
                LoadState.Status.RUNNING -> {
                    showProgress()
                }

                else -> {
                    dismissProgress()
                }
            }
        })

        fundPoolViewModel.tipsMessage.observe(this, Observer {
            it.getDataIfNotHandled()?.let { msg ->
                if (msg.isNotEmpty()) {
                    showToast(msg)
                }
            }
        })
    }

    //*********************************** 切换转入转出模式逻辑 ***********************************//
    private val currOpModeObserver = Observer<FundPoolOpMode> {
        if (it == FundPoolOpMode.TransferIn) {
            tvSwitchOpModeText.setText(R.string.transfer_in)
            tvFirstInputLabel.setText(R.string.transfer_in)
            tvSecondInputLabel.setText(R.string.transfer_in)
            btnPositive.setText(R.string.action_transfer_in_nbsp)
            etSecondInputBox.isEnabled = true
            etSecondInputBox.requestLayout()
            llFirstSelectGroup.visibility = View.VISIBLE

            fundPoolViewModel.getCurrFirstTokenLiveData()
                .observe(viewLifecycleOwner, currFirstTokenObserver)
            fundPoolViewModel.getCurrSecondTokenLiveData()
                .observe(viewLifecycleOwner, currSecondTokenObserver)
            fundPoolViewModel.getCurrTokenPairLiveData()
                .removeObserver(currTokenPairObserver)
        } else {
            tvSwitchOpModeText.setText(R.string.transfer_out)
            tvFirstInputLabel.setText(R.string.fund_pool_token)
            tvSecondInputLabel.setText(R.string.transfer_out)
            btnPositive.setText(R.string.action_transfer_out_nbsp)
            etSecondInputBox.isEnabled = false
            llFirstSelectGroup.visibility = View.GONE

            fundPoolViewModel.getCurrFirstTokenLiveData()
                .removeObserver(currFirstTokenObserver)
            fundPoolViewModel.getCurrSecondTokenLiveData()
                .removeObserver(currSecondTokenObserver)
            fundPoolViewModel.getCurrTokenPairLiveData()
                .observe(viewLifecycleOwner, currTokenPairObserver)
        }
    }

    private val switchOpModeArrowUpAnimator by lazy {
        ObjectAnimator.ofFloat(ivSwitchOpModeArrow, "rotation", 0F, 180F)
            .setDuration(360)
    }
    private val switchOpModeArrowDownAnimator by lazy {
        ObjectAnimator.ofFloat(ivSwitchOpModeArrow, "rotation", 180F, 360F)
            .setDuration(360)
    }

    private fun showSwitchOpModePopup() {
        setSwitchOpModeViewBgAndTextColor(true)
        switchOpModeArrowUpAnimator.start()
        XPopup.Builder(requireContext())
            .hasShadowBg(false)
            .popupAnimation(PopupAnimation.ScrollAlphaFromTop)
            .atView(llSwitchOpModeGroup)
            .setPopupCallback(
                object : EnhancedPopupCallback() {
                    override fun onDismissBefore() {
                        setSwitchOpModeViewBgAndTextColor(false)
                        switchOpModeArrowDownAnimator.start()
                    }
                })
            .asCustom(
                MarketSwitchPopupView(
                    requireContext(),
                    fundPoolViewModel.getCurrOpModelPosition(),
                    mutableListOf(
                        getString(R.string.transfer_in),
                        getString(R.string.transfer_out)
                    )
                ) {
                    fundPoolViewModel.switchOpModel(FundPoolOpMode.values()[it])
                }
            )
            .show()
    }

    private fun setSwitchOpModeViewBgAndTextColor(pressed: Boolean) {
        if (pressed) {
            llSwitchOpModeGroup.setBackgroundResource(R.drawable.bg_market_switch_pressed)
            ivSwitchOpModeArrow.setBackgroundResource(R.drawable.shape_market_downward_pressed)
            tvSwitchOpModeText.setTextColor(
                getColorByAttrId(R.attr.colorOnPrimary, requireContext())
            )
        } else {
            llSwitchOpModeGroup.setBackgroundResource(R.drawable.sel_bg_market_switch)
            ivSwitchOpModeArrow.setBackgroundResource(R.drawable.sel_market_downward)
            tvSwitchOpModeText.setTextColor(
                ContextCompat.getColorStateList(requireContext(), R.color.sel_color_market_switch)
            )
        }
    }

    @Subscribe(sticky = true)
    fun onSwitchFundPoolOpModeEvent(event: SwitchFundPoolOpModeEvent) {
        try {
            EventBus.getDefault().removeStickyEvent(event)
        } catch (ignore: Exception) {
        }

        fundPoolViewModel.switchOpModel(event.opMode)
    }

    //*********************************** 转出模式选择交易对逻辑 ***********************************//
    private val currTokenPairObserver = Observer<Pair<String, String>?> {
        if (it == null) {
            tvSecondSelectText.text = getString(R.string.select_token_pair)
            etSecondInputBox.hint = "0.00\n0.00"
        } else {
            tvSecondSelectText.text = "${it.first}/${it.second}"
            etSecondInputBox.hint = "0.00${it.first}\n0.00${it.second}"
        }
        etSecondInputBox.clearComposingText()
    }

    private val displayTokenPairsObserver = Observer<MutableList<String>> {
        if (it.isEmpty()) {
            showToast(R.string.tips_fund_pool_select_token_pair_empty)
            return@Observer
        }
        showSelectTokenPairPopup(it)
    }

    private val selectTokenPairArrowUpAnimator by lazy {
        ObjectAnimator.ofFloat(ivSecondSelectArrow, "rotation", 0F, 180F)
            .setDuration(360)
    }
    private val selectTokenPairArrowDownAnimator by lazy {
        ObjectAnimator.ofFloat(ivSecondSelectArrow, "rotation", 180F, 360F)
            .setDuration(360)
    }

    private fun showSelectTokenPairPopup(displayTokenPairs: MutableList<String>) {
        val currPosition = fundPoolViewModel.getCurrTokenPairPosition()
        selectTokenPairArrowUpAnimator.start()
        XPopup.Builder(requireContext())
            .hasShadowBg(false)
            .popupAnimation(PopupAnimation.ScrollAlphaFromTop)
            .atView(llSecondSelectGroup)
            .setPopupCallback(
                object : EnhancedPopupCallback() {
                    override fun onDismissBefore() {
                        selectTokenPairArrowDownAnimator.start()
                    }
                })
            .asCustom(
                MarketSwitchPopupView(
                    requireContext(),
                    currPosition,
                    displayTokenPairs
                ) {
                    fundPoolViewModel.selectTokenPair(it, currPosition)
                }
            )
            .show()
    }

    //*********************************** 转入模式选择通证逻辑 ***********************************//
    private val currFirstTokenObserver = Observer<String?> {
        tvFirstSelectText.text = if (it.isNullOrBlank()) getString(R.string.select_token) else it
        etFirstInputBox.hint = "0.00"
        etFirstInputBox.clearComposingText()
    }

    private val currSecondTokenObserver = Observer<String?> {
        tvSecondSelectText.text = if (it.isNullOrBlank()) getString(R.string.select_token) else it
        etSecondInputBox.hint = "0.00"
        etSecondInputBox.clearComposingText()
    }

    private fun showSelectTokenDialog(selectFirst: Boolean) {

    }

    //*********************************** 输入框逻辑 ***********************************//
    private val firstInputTextWatcher = object : TextWatcherSimple() {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (!etFirstInputBox.isFocused) return

            val inputText = s?.toString() ?: ""
            val amountStr = handleInputText(inputText)
            if (inputText != amountStr) {
                handleInputTextWatcher(amountStr, etFirstInputBox, this)
            }
        }
    }

    private val secondInputTextWatcher = object : TextWatcherSimple() {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (!etSecondInputBox.isFocused) return

            val inputText = s?.toString() ?: ""
            val amountStr = handleInputText(inputText)
            if (inputText != amountStr) {
                handleInputTextWatcher(amountStr, etSecondInputBox, this)
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