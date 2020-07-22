package com.violas.wallet.ui.main.market.pool

import android.animation.ObjectAnimator
import android.os.Bundle
import android.text.AmountInputFilter
import android.text.InputType
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
import com.palliums.utils.DensityUtility
import com.palliums.utils.TextWatcherSimple
import com.palliums.utils.getColorByAttrId
import com.palliums.utils.stripTrailingZeros
import com.palliums.violas.http.LiquidityTokenDTO
import com.palliums.widget.popup.EnhancedPopupCallback
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.event.SwitchMarketPoolOpModeEvent
import com.violas.wallet.ui.main.market.MarketSwitchPopupView
import com.violas.wallet.ui.main.market.MarketViewModel
import com.violas.wallet.ui.main.market.bean.ITokenVo
import com.violas.wallet.ui.main.market.bean.StableTokenVo
import com.violas.wallet.ui.main.market.pool.MarketPoolViewModel.Companion.ACTION_ADD_LIQUIDITY
import com.violas.wallet.ui.main.market.pool.MarketPoolViewModel.Companion.ACTION_GET_USER_LIQUIDITY_TOKENS
import com.violas.wallet.ui.main.market.pool.MarketPoolViewModel.Companion.ACTION_REMOVE_LIQUIDITY
import com.violas.wallet.ui.main.market.selectToken.SelectTokenDialog
import com.violas.wallet.ui.main.market.selectToken.SelectTokenDialog.Companion.ACTION_POOL_SELECT_FIRST
import com.violas.wallet.ui.main.market.selectToken.SelectTokenDialog.Companion.ACTION_POOL_SELECT_SECOND
import com.violas.wallet.ui.main.market.selectToken.TokensBridge
import com.violas.wallet.utils.authenticateAccount
import com.violas.wallet.utils.convertAmountToDisplayAmount
import com.violas.wallet.utils.convertAmountToDisplayUnit
import com.violas.wallet.utils.convertDisplayAmountToAmount
import com.violas.wallet.viewModel.WalletAppViewModel
import kotlinx.android.synthetic.main.fragment_market_pool.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.math.BigDecimal

/**
 * Created by elephant on 2020/6/23 17:18.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 市场资金池视图
 */
class MarketPoolFragment : BaseFragment(), TokensBridge {

    private val marketPoolViewModel by lazy {
        ViewModelProvider(this).get(MarketPoolViewModel::class.java)
    }
    private val marketViewModel by lazy {
        ViewModelProvider(requireParentFragment()).get(MarketViewModel::class.java)
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_market_pool
    }

    override fun onDestroyView() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        super.onDestroyView()
    }

    override fun onPause() {
        super.onPause()
        clearInputBoxFocusAndHideSoftInput()
    }

    override fun onLazyInitViewByResume(savedInstanceState: Bundle?) {
        super.onLazyInitViewByResume(savedInstanceState)
        EventBus.getDefault().register(this)

        // 设置View初始值
        handleValueNull(tvExchangeRate, R.string.exchange_rate_format)
        handleValueNull(tvPoolTokenAndPoolShare, R.string.market_my_pool_amount_format)

        // 输入框设置
        etFirstInputBox.addTextChangedListener(firstInputTextWatcher)
        etSecondInputBox.addTextChangedListener(secondInputTextWatcher)
        etFirstInputBox.filters = arrayOf(AmountInputFilter(12, 6))

        // 按钮点击事件
        llSwitchOpModeGroup.setOnClickListener {
            clearInputBoxFocusAndHideSoftInput()
            showSwitchOpModePopup()
        }

        llFirstSelectGroup.setOnClickListener {
            clearInputBoxFocusAndHideSoftInput()
            if (marketPoolViewModel.isTransferInMode()) {
                showSelectTokenDialog(true)
            } else {
                if (accountInvalid()) return@setOnClickListener

                marketPoolViewModel.getLiquidityTokensLiveData()
                    .removeObserver(liquidityTokensObserver)
                marketPoolViewModel.execute(action = ACTION_GET_USER_LIQUIDITY_TOKENS) {
                    marketPoolViewModel.getLiquidityTokensLiveData()
                        .observe(viewLifecycleOwner, liquidityTokensObserver)
                }
            }
        }

        llSecondSelectGroup.setOnClickListener {
            clearInputBoxFocusAndHideSoftInput()
            showSelectTokenDialog(false)
        }

        btnPositive.setOnClickListener {
            clearInputBoxFocusAndHideSoftInput()
            if (accountInvalid()) return@setOnClickListener

            if (marketPoolViewModel.isTransferInMode()) {
                if (transferInPreconditionsInvalid()) return@setOnClickListener
                authenticateAccount(true)
            } else {
                if (transferOutPreconditionsInvalid()) return@setOnClickListener
                authenticateAccount(false)
            }
        }

        // 数据观察
        WalletAppViewModel.getViewModelInstance().mExistsAccountLiveData
            .observe(viewLifecycleOwner, Observer {
                marketPoolViewModel.setupViolasAccount(it)
            })

        marketPoolViewModel.getCurrOpModeLiveData().observe(viewLifecycleOwner, currOpModeObserver)
        marketPoolViewModel.getExchangeRateLiveData().observe(viewLifecycleOwner, Observer {
            if (it == null) {
                handleValueNull(tvExchangeRate, R.string.exchange_rate_format)
            } else {
                tvExchangeRate.text = getString(
                    R.string.exchange_rate_format,
                    "1:${it.toPlainString()}"
                )

                // 兑换率获取后，尝试估算已输入的金额
                val firstInputAmount = etFirstInputBox.text.toString().trim()
                if (firstInputAmount.isNotEmpty()) {
                    if (marketPoolViewModel.isTransferInMode()) {
                        marketPoolViewModel.estimateSecondTokenTransferIntoAmount(firstInputAmount)
                    } else {
                        marketPoolViewModel.estimateTokensTransferOutAmount(firstInputAmount)
                    }
                    return@Observer
                }

                val secondInputAmount = etSecondInputBox.text.toString().trim()
                if (firstInputAmount.isNotEmpty()) {
                    if (marketPoolViewModel.isTransferInMode()) {
                        marketPoolViewModel.estimateFirstTokenTransferIntoAmount(secondInputAmount)
                    }
                }
            }
        })
        marketPoolViewModel.getPoolTokenAndPoolShareLiveData()
            .observe(viewLifecycleOwner, Observer {
                if (it == null) {
                    handleValueNull(tvPoolTokenAndPoolShare, R.string.market_my_pool_amount_format)
                } else {
                    tvPoolTokenAndPoolShare.text = getString(
                        R.string.market_my_pool_amount_format,
                        it.first
                    )
                }
            })
        marketPoolViewModel.getFirstInputTextLiveData().observe(viewLifecycleOwner, Observer {
            etFirstInputBox.setText(it)
        })
        marketPoolViewModel.getSecondInputTextLiveData().observe(viewLifecycleOwner, Observer {
            etSecondInputBox.setText(it)
        })

        marketPoolViewModel.loadState.observe(viewLifecycleOwner, Observer {
            when (it.peekData().status) {
                LoadState.Status.RUNNING -> {
                    showProgress()
                }

                else -> {
                    dismissProgress()
                }
            }
        })
        marketPoolViewModel.tipsMessage.observe(viewLifecycleOwner, Observer {
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
    private val currOpModeObserver = Observer<MarketPoolOpMode> {
        if (it == MarketPoolOpMode.TransferIn) {
            tvSwitchOpModeText.setText(R.string.transfer_in)
            tvFirstInputLabel.setText(R.string.transfer_in)
            tvSecondInputLabel.setText(R.string.transfer_in)
            btnPositive.setText(R.string.action_transfer_in_nbsp)
            etSecondInputBox.filters = arrayOf(AmountInputFilter(12, 6))
            etSecondInputBox.inputType = InputType.TYPE_CLASS_NUMBER
            etSecondInputBox.isEnabled = true
            etSecondInputBox.requestLayout()
            tvSecondBalance.visibility = View.VISIBLE
            llSecondSelectGroup.visibility = View.VISIBLE

            marketPoolViewModel.getCurrFirstTokenLiveData()
                .observe(viewLifecycleOwner, currFirstTokenObserver)
            marketPoolViewModel.getCurrSecondTokenLiveData()
                .observe(viewLifecycleOwner, currSecondTokenObserver)
            marketPoolViewModel.getCurrLiquidityTokenLiveData()
                .removeObserver(currLiquidityTokenObserver)
            marketPoolViewModel.getLiquidityTokensLiveData()
                .removeObserver(liquidityTokensObserver)
        } else {
            tvSwitchOpModeText.setText(R.string.transfer_out)
            tvFirstInputLabel.setText(R.string.pool_liquidity_token)
            tvSecondInputLabel.setText(R.string.transfer_out)
            btnPositive.setText(R.string.action_transfer_out_nbsp)
            etSecondInputBox.filters = arrayOf()
            etSecondInputBox.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            etSecondInputBox.isEnabled = false
            tvSecondBalance.visibility = View.GONE
            llSecondSelectGroup.visibility = View.GONE
            adjustSecondInputBoxPaddingEnd()

            marketPoolViewModel.getCurrFirstTokenLiveData()
                .removeObserver(currFirstTokenObserver)
            marketPoolViewModel.getCurrSecondTokenLiveData()
                .removeObserver(currSecondTokenObserver)
            marketPoolViewModel.getCurrLiquidityTokenLiveData()
                .observe(viewLifecycleOwner, currLiquidityTokenObserver)
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
                    marketPoolViewModel.getCurrOpModelPosition(),
                    mutableListOf(
                        getString(R.string.transfer_in),
                        getString(R.string.transfer_out)
                    )
                ) {
                    marketPoolViewModel.switchOpModel(MarketPoolOpMode.values()[it])
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
    fun onSwitchMarketPoolOpModeEvent(event: SwitchMarketPoolOpModeEvent) {
        try {
            EventBus.getDefault().removeStickyEvent(event)
        } catch (ignore: Exception) {
        }

        marketPoolViewModel.switchOpModel(event.opMode)
    }

    //*********************************** 转出模式选择交易对逻辑 ***********************************//
    private val currLiquidityTokenObserver = Observer<LiquidityTokenDTO?> {
        if (it == null) {
            tvFirstSelectText.text = getString(R.string.select_token_pair)
            etSecondInputBox.hint = "0.00\n0.00"
            handleValueNull(tvFirstBalance, R.string.market_liquidity_token_balance_format)
        } else {
            tvFirstSelectText.text = "${it.coinAName}/${it.coinBName}"
            etSecondInputBox.hint = "0.00 ${it.coinAName}\n0.00 ${it.coinBName}"
            tvFirstBalance.text = getString(
                R.string.market_liquidity_token_balance_format,
                convertAmountToDisplayAmount(it.amount)
            )
        }

        adjustFirstInputBoxPaddingEnd()
    }

    private val liquidityTokensObserver = Observer<List<LiquidityTokenDTO>?> {
        if (it.isNullOrEmpty()) {
            showToast(R.string.tips_market_pool_select_token_pair_empty)
        } else {
            val displayList = it.map { item ->
                "${item.coinAName}/${item.coinBName}"
            } as MutableList
            showSelectLiquidityTokenPopup(displayList)
        }
    }

    private val selectLiquidityTokenArrowUpAnimator by lazy {
        ObjectAnimator.ofFloat(ivSecondSelectArrow, "rotation", 0F, 180F)
            .setDuration(360)
    }

    private val selectLiquidityTokenArrowDownAnimator by lazy {
        ObjectAnimator.ofFloat(ivSecondSelectArrow, "rotation", 180F, 360F)
            .setDuration(360)
    }

    private fun showSelectLiquidityTokenPopup(displayTokenPairs: MutableList<String>) {
        val currPosition = marketPoolViewModel.getCurrLiquidityTokenPosition()
        selectLiquidityTokenArrowUpAnimator.start()
        XPopup.Builder(requireContext())
            .hasShadowBg(false)
            .popupAnimation(PopupAnimation.ScrollAlphaFromTop)
            .atView(llFirstSelectGroup)
            .setPopupCallback(
                object : EnhancedPopupCallback() {
                    override fun onDismissBefore() {
                        selectLiquidityTokenArrowDownAnimator.start()
                    }
                })
            .asCustom(
                MarketSwitchPopupView(
                    requireContext(),
                    currPosition,
                    displayTokenPairs
                ) {
                    marketPoolViewModel.selectLiquidityToken(it, currPosition)
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

        adjustFirstInputBoxPaddingEnd()
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

        adjustSecondInputBoxPaddingEnd()
    }

    private fun showSelectTokenDialog(selectFirst: Boolean) {
        SelectTokenDialog
            .newInstance(
                if (selectFirst) ACTION_POOL_SELECT_FIRST else ACTION_POOL_SELECT_SECOND
            )
            .setCallback {
                marketPoolViewModel.selectToken(selectFirst, it as StableTokenVo)
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
            marketPoolViewModel.getCurrFirstTokenLiveData().value
        else
            marketPoolViewModel.getCurrSecondTokenLiveData().value
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

            if (marketPoolViewModel.isTransferInMode()) {
                marketPoolViewModel.estimateSecondTokenTransferIntoAmount(amountStr)
            } else {
                marketPoolViewModel.estimateTokensTransferOutAmount(amountStr)
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

            if (marketPoolViewModel.isTransferInMode()) {
                marketPoolViewModel.estimateFirstTokenTransferIntoAmount(amountStr)
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
        if (etFirstInputBox.isFocused) {
            focused = true
            etFirstInputBox.clearFocus()
        }
        if (etSecondInputBox.isFocused) {
            focused = true
            etSecondInputBox.clearFocus()
        }

        if (focused) {
            btnPositive.requestFocus()
            hideSoftInput()
        }
    }

    //*********************************** 转入转出逻辑 ***********************************//
    private fun transferInPreconditionsInvalid(): Boolean {
        val firstCoin = marketPoolViewModel.getCurrFirstTokenLiveData().value
        val secondCoin = marketPoolViewModel.getCurrSecondTokenLiveData().value
        if (firstCoin == null || secondCoin == null) {
            showToast(R.string.tips_market_add_liquidity_coin_not_selected)
            return true
        }

        val firstAmount = etFirstInputBox.text.toString().trim()
        val secondAmount = etSecondInputBox.text.toString().trim()
        if (firstAmount.isEmpty() && secondAmount.isEmpty()) {
            showToast(R.string.tips_market_add_liquidity_amount_not_input)
            return true
        }

        if (convertDisplayAmountToAmount(firstAmount) > BigDecimal(firstCoin.amount)) {
            val prefix =
                if (convertDisplayAmountToAmount(secondAmount) > BigDecimal(secondCoin.amount)) {
                    "${firstCoin.module}/${secondCoin.module}"
                } else {
                    firstCoin.module
                }
            showToast(getString(R.string.tips_market_insufficient_balance_format, prefix))
            return true
        } else if (convertDisplayAmountToAmount(secondAmount) > BigDecimal(secondCoin.amount)) {
            val prefix = firstCoin.module
            showToast(getString(R.string.tips_market_insufficient_balance_format, prefix))
            return true
        }

        return false
    }

    private fun transferOutPreconditionsInvalid(): Boolean {
        val liquidityToken =
            marketPoolViewModel.getCurrLiquidityTokenLiveData().value
        if (liquidityToken == null) {
            showToast(R.string.tips_market_remove_liquidity_pair_not_selected)
            return true
        }

        val liquidityAmount = etFirstInputBox.text.toString().trim()
        if (liquidityAmount.isEmpty()) {
            showToast(R.string.tips_market_remove_liquidity_amount_not_input)
            return true
        }

        if (convertDisplayAmountToAmount(liquidityAmount) > liquidityToken.amount) {
            showToast(getString(R.string.tips_market_insufficient_balance_format, ""))
            return true
        }

        return false
    }

    private fun authenticateAccount(transferIn: Boolean) {
        authenticateAccount(
            marketPoolViewModel.getViolasAccount()!!,
            marketPoolViewModel.getAccountManager()
        ) { privateKey ->
            if (transferIn) {
                marketPoolViewModel.execute(
                    privateKey,
                    etFirstInputBox.text.toString().trim(),
                    etSecondInputBox.text.toString().trim(),
                    action = ACTION_ADD_LIQUIDITY
                ) {
                    showToast(R.string.tips_market_add_liquidity_success)
                }
            } else {
                marketPoolViewModel.execute(
                    privateKey,
                    etFirstInputBox.text.toString().trim(),
                    action = ACTION_REMOVE_LIQUIDITY
                ) {
                    showToast(R.string.tips_market_remove_liquidity_success)
                }
            }
        }
    }

    //*********************************** 其它逻辑 ***********************************//
    private val handleValueNull: (TextView, Int) -> Unit = { textView, formatResId ->
        textView.text = getString(formatResId, getString(R.string.value_null))
    }

    private fun accountInvalid(): Boolean {
        return if (marketPoolViewModel.getViolasAccount() == null) {
            showToast(R.string.tips_create_or_import_wallet)
            true
        } else {
            false
        }
    }

    private fun adjustFirstInputBoxPaddingEnd() {
        etFirstInputBox.post {
            val paddingRight = if (llFirstSelectGroup.visibility != View.VISIBLE) {
                DensityUtility.dp2px(requireContext(), 11)
            } else {
                llFirstSelectGroup.width + DensityUtility.dp2px(requireContext(), 11) * 2
            }
            etFirstInputBox.setPadding(
                etFirstInputBox.paddingLeft,
                etFirstInputBox.paddingTop,
                paddingRight,
                etFirstInputBox.paddingBottom
            )
        }
    }

    private fun adjustSecondInputBoxPaddingEnd() {
        etSecondInputBox.post {
            val paddingRight = if (llSecondSelectGroup.visibility != View.VISIBLE) {
                DensityUtility.dp2px(requireContext(), 11)
            } else {
                llSecondSelectGroup.width + DensityUtility.dp2px(requireContext(), 11) * 2
            }
            etSecondInputBox.setPadding(
                etSecondInputBox.paddingLeft,
                etSecondInputBox.paddingTop,
                paddingRight,
                etSecondInputBox.paddingBottom
            )
        }
    }
}