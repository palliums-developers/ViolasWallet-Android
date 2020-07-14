package com.violas.wallet.ui.main.market.fundPool

import android.animation.ObjectAnimator
import android.os.Bundle
import android.text.AmountInputFilter
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.enums.PopupAnimation
import com.palliums.base.BaseFragment
import com.palliums.extensions.show
import com.palliums.net.LoadState
import com.palliums.utils.TextWatcherSimple
import com.palliums.utils.getColorByAttrId
import com.palliums.utils.stripTrailingZeros
import com.palliums.widget.popup.EnhancedPopupCallback
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.event.SwitchFundPoolOpModeEvent
import com.violas.wallet.ui.main.market.MarketSwitchPopupView
import com.violas.wallet.ui.main.market.MarketViewModel
import com.violas.wallet.ui.main.market.bean.ITokenVo
import com.violas.wallet.ui.main.market.bean.StableTokenVo
import com.violas.wallet.ui.main.market.fundPool.FundPoolViewModel.Companion.ACTION_GET_TOKEN_PAIRS
import com.violas.wallet.ui.main.market.selectToken.SelectTokenDialog
import com.violas.wallet.ui.main.market.selectToken.SelectTokenDialog.Companion.ACTION_POOL_SELECT_FIRST
import com.violas.wallet.ui.main.market.selectToken.SelectTokenDialog.Companion.ACTION_POOL_SELECT_SECOND
import com.violas.wallet.ui.main.market.selectToken.TokensBridge
import com.violas.wallet.utils.convertAmountToDisplayUnit
import kotlinx.android.synthetic.main.fragment_fund_pool.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

/**
 * Created by elephant on 2020/6/23 17:18.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 市场资金池视图
 */
class FundPoolFragment : BaseFragment(), TokensBridge {

    private val fundPoolViewModel by lazy {
        ViewModelProvider(this).get(FundPoolViewModel::class.java)
    }
    private val marketViewModel by lazy {
        ViewModelProvider(requireParentFragment()).get(MarketViewModel::class.java)
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

        handleValueNull(tvExchangeRate, R.string.exchange_rate_format)
        handleValueNull(tvPoolTokenAndPoolShare, R.string.my_fund_pool_amount_format)

        etFirstInputBox.addTextChangedListener(firstInputTextWatcher)
        etSecondInputBox.addTextChangedListener(secondInputTextWatcher)
        etFirstInputBox.filters = arrayOf(AmountInputFilter(12, 2))
        etSecondInputBox.filters = arrayOf(AmountInputFilter(12, 2))

        llSwitchOpModeGroup.setOnClickListener {
            showSwitchOpModePopup()
        }

        llFirstSelectGroup.setOnClickListener {
            if (fundPoolViewModel.isTransferInMode()) {
                showSelectTokenDialog(true)
            } else {
                fundPoolViewModel.getTokenPairsLiveData()
                    .removeObserver(tokenPairsObserver)
                fundPoolViewModel.execute(action = ACTION_GET_TOKEN_PAIRS) {
                    fundPoolViewModel.getTokenPairsLiveData()
                        .observe(viewLifecycleOwner, tokenPairsObserver)
                }
            }
        }

        llSecondSelectGroup.setOnClickListener {
            showSelectTokenDialog(false)
        }

        btnPositive.setOnClickListener {
            // TODO 转入转出逻辑
        }

        fundPoolViewModel.getCurrOpModeLiveData().observe(viewLifecycleOwner, currOpModeObserver)
        fundPoolViewModel.getExchangeRateLiveData().observe(viewLifecycleOwner, Observer {
            if (it == null) {
                handleValueNull(tvExchangeRate, R.string.exchange_rate_format)
            } else {
                tvExchangeRate.text = getString(
                    R.string.exchange_rate_format,
                    "1:${it.stripTrailingZeros().toPlainString()}"
                )
            }
        })
        fundPoolViewModel.getPoolTokenAndPoolShareLiveData().observe(viewLifecycleOwner, Observer {
            if (it == null) {
                handleValueNull(tvPoolTokenAndPoolShare, R.string.my_fund_pool_amount_format)
            } else {
                tvPoolTokenAndPoolShare.text = getString(
                    R.string.my_fund_pool_amount_format,
                    it.first
                )
            }
        })

        fundPoolViewModel.loadState.observe(viewLifecycleOwner, Observer {
            when (it.peekData().status) {
                LoadState.Status.RUNNING -> {
                    showProgress()
                }

                else -> {
                    dismissProgress()
                }
            }
        })
        fundPoolViewModel.tipsMessage.observe(viewLifecycleOwner, Observer {
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

    //*********************************** 切换转入转出模式逻辑 ***********************************//
    private val currOpModeObserver = Observer<FundPoolOpMode> {
        if (it == FundPoolOpMode.TransferIn) {
            tvSwitchOpModeText.setText(R.string.transfer_in)
            tvFirstInputLabel.setText(R.string.transfer_in)
            tvSecondInputLabel.setText(R.string.transfer_in)
            btnPositive.setText(R.string.action_transfer_in_nbsp)
            etSecondInputBox.isEnabled = true
            etSecondInputBox.requestLayout()
            tvSecondBalance.visibility = View.VISIBLE
            llSecondSelectGroup.visibility = View.VISIBLE

            fundPoolViewModel.getCurrFirstTokenLiveData()
                .observe(viewLifecycleOwner, currFirstTokenObserver)
            fundPoolViewModel.getCurrSecondTokenLiveData()
                .observe(viewLifecycleOwner, currSecondTokenObserver)
            fundPoolViewModel.getCurrTokenPairLiveData()
                .removeObserver(currTokenPairObserver)
            fundPoolViewModel.getTokenPairsLiveData()
                .removeObserver(tokenPairsObserver)
        } else {
            tvSwitchOpModeText.setText(R.string.transfer_out)
            tvFirstInputLabel.setText(R.string.fund_pool_token)
            tvSecondInputLabel.setText(R.string.transfer_out)
            btnPositive.setText(R.string.action_transfer_out_nbsp)
            etSecondInputBox.isEnabled = false
            tvSecondBalance.visibility = View.GONE
            llSecondSelectGroup.visibility = View.GONE

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
    private val currTokenPairObserver = Observer<Pair<StableTokenVo, StableTokenVo>?> {
        if (it == null) {
            tvFirstSelectText.text = getString(R.string.select_token_pair)
            etSecondInputBox.hint = "0.00\n0.00"
            handleValueNull(tvFirstBalance, R.string.market_liquidity_token_balance_format)
        } else {
            tvFirstSelectText.text = "${it.first.displayName}/${it.second.displayName}"
            etSecondInputBox.hint = "0.00${it.first.displayName}\n0.00${it.second.displayName}"
            tvFirstBalance.text = getString(
                R.string.market_liquidity_token_balance_format,
                "10"
            )
        }
    }

    private val tokenPairsObserver = Observer<List<Pair<StableTokenVo, StableTokenVo>>?> {
        if (it.isNullOrEmpty()) {
            showToast(R.string.tips_fund_pool_select_token_pair_empty)
        } else {
            val displayList = it.map { item ->
                "${item.first.displayName}/${item.second.displayName}"
            } as MutableList
            showSelectTokenPairPopup(displayList)
        }
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
            .atView(llFirstSelectGroup)
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

    //*********************************** 转入模式选择Token逻辑 ***********************************//
    private val currFirstTokenObserver = Observer<StableTokenVo?> {
        etFirstInputBox.hint = "0.00"
        if (it == null) {
            tvFirstSelectText.text = getString(R.string.select_token)
            handleValueNull(tvFirstBalance, R.string.market_token_balance_format)
        } else {
            tvFirstSelectText.text = it.displayName
            val amountWithUnit =
                convertAmountToDisplayUnit(it.amount, CoinTypes.parseCoinType(it.coinNumber))
            tvFirstBalance.text = getString(
                R.string.market_token_balance_format,
                "${amountWithUnit.first} ${it.displayName}"
            )
        }
    }

    private val currSecondTokenObserver = Observer<StableTokenVo?> {
        etSecondInputBox.hint = "0.00"
        if (it == null) {
            tvSecondSelectText.text = getString(R.string.select_token)
            handleValueNull(tvSecondBalance, R.string.market_token_balance_format)
        } else {
            tvSecondSelectText.text = it.displayName
            val amountWithUnit =
                convertAmountToDisplayUnit(it.amount, CoinTypes.parseCoinType(it.coinNumber))
            tvSecondBalance.text = getString(
                R.string.market_token_balance_format,
                "${amountWithUnit.first} ${it.displayName}"
            )
        }
    }

    private fun showSelectTokenDialog(selectFirst: Boolean) {
        SelectTokenDialog
            .newInstance(
                if (selectFirst) ACTION_POOL_SELECT_FIRST else ACTION_POOL_SELECT_SECOND
            )
            .setCallback {
                fundPoolViewModel.selectToken(selectFirst, it as StableTokenVo)
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
        return if (action == ACTION_POOL_SELECT_FIRST)
            fundPoolViewModel.getCurrFirstTokenLiveData().value
        else
            fundPoolViewModel.getCurrSecondTokenLiveData().value
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

    //*********************************** 其它逻辑 ***********************************//
    private val handleValueNull: (TextView, Int) -> Unit = { textView, formatResId ->
        textView.text = getString(formatResId, getString(R.string.market_value_null))
    }
}