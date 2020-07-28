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
import com.palliums.utils.*
import com.palliums.violas.http.PoolLiquidityDTO
import com.palliums.widget.popup.EnhancedPopupCallback
import com.violas.wallet.R
import com.violas.wallet.event.SwitchMarketPoolOpModeEvent
import com.violas.wallet.ui.main.market.MarketSwitchPopupView
import com.violas.wallet.ui.main.market.MarketViewModel
import com.violas.wallet.ui.main.market.bean.ITokenVo
import com.violas.wallet.ui.main.market.bean.StableTokenVo
import com.violas.wallet.ui.main.market.pool.MarketPoolViewModel.Companion.ACTION_ADD_LIQUIDITY
import com.violas.wallet.ui.main.market.pool.MarketPoolViewModel.Companion.ACTION_GET_USER_LIQUIDITY_LIST
import com.violas.wallet.ui.main.market.pool.MarketPoolViewModel.Companion.ACTION_REMOVE_LIQUIDITY
import com.violas.wallet.ui.main.market.pool.MarketPoolViewModel.Companion.ACTION_SYNC_LIQUIDITY_RESERVE
import com.violas.wallet.ui.main.market.selectToken.CoinsBridge
import com.violas.wallet.ui.main.market.selectToken.SelectTokenDialog
import com.violas.wallet.ui.main.market.selectToken.SelectTokenDialog.Companion.ACTION_POOL_SELECT_A
import com.violas.wallet.ui.main.market.selectToken.SelectTokenDialog.Companion.ACTION_POOL_SELECT_B
import com.violas.wallet.utils.authenticateAccount
import com.violas.wallet.utils.convertAmountToDisplayAmountStr
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
class MarketPoolFragment : BaseFragment(), CoinsBridge {

    private val poolViewModel by lazy {
        ViewModelProvider(this).get(MarketPoolViewModel::class.java)
    }
    private val marketViewModel by lazy {
        ViewModelProvider(requireParentFragment()).get(MarketViewModel::class.java)
    }
    private var isInputA = true

    override fun getLayoutResId(): Int {
        return R.layout.fragment_market_pool
    }

    override fun onDestroyView() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        poolViewModel.startSyncLiquidityReserveWork()
    }

    override fun onPause() {
        super.onPause()
        poolViewModel.stopSyncLiquidityReserveWork()
        clearInputBoxFocusAndHideSoftInput()
    }

    override fun onLazyInitViewByResume(savedInstanceState: Bundle?) {
        super.onLazyInitViewByResume(savedInstanceState)
        EventBus.getDefault().register(this)

        // 设置View初始值
        handleValueNull(tvExchangeRate, R.string.exchange_rate_format)
        handleValueNull(tvPoolTokenAndPoolShare, R.string.market_my_pool_amount_format)

        // 输入框设置
        etInputBoxA.addTextChangedListener(inputTextWatcherA)
        etInputBoxB.addTextChangedListener(inputTextWatcherB)
        etInputBoxA.filters = arrayOf(AmountInputFilter(12, 6))

        // 按钮点击事件
        llSwitchOpModeGroup.setOnClickListener {
            clearInputBoxFocusAndHideSoftInput()
            showSwitchOpModePopup()
        }

        llSelectGroupA.setOnClickListener {
            clearInputBoxFocusAndHideSoftInput()
            if (poolViewModel.isTransferInMode()) {
                showSelectCoinDialog(true)
            } else {
                if (accountInvalid()) return@setOnClickListener

                poolViewModel.getLiquidityListLiveData()
                    .removeObserver(liquidityListObserver)
                poolViewModel.execute(action = ACTION_GET_USER_LIQUIDITY_LIST) {
                    poolViewModel.getLiquidityListLiveData()
                        .observe(viewLifecycleOwner, liquidityListObserver)
                }
            }
        }

        llSelectGroupB.setOnClickListener {
            clearInputBoxFocusAndHideSoftInput()
            showSelectCoinDialog(false)
        }

        btnPositive.setOnClickListener {
            clearInputBoxFocusAndHideSoftInput()
            if (accountInvalid()) return@setOnClickListener

            if (poolViewModel.isTransferInMode()) {
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
                poolViewModel.setupViolasAccount(it)
            })

        poolViewModel.getCurrOpModeLiveData().observe(viewLifecycleOwner, currOpModeObserver)
        poolViewModel.getCurrCoinALiveData().observe(viewLifecycleOwner, currCoinAObserver)
        poolViewModel.getCurrCoinBLiveData().observe(viewLifecycleOwner, currCoinBObserver)
        poolViewModel.getCurrLiquidityLiveData().observe(viewLifecycleOwner, currLiquidityObserver)
        poolViewModel.getExchangeRateLiveData()
            .observe(viewLifecycleOwner, Observer {
                if (it == null) {
                    handleValueNull(tvExchangeRate, R.string.exchange_rate_format)
                } else {
                    tvExchangeRate.text = getString(
                        R.string.exchange_rate_format,
                        "1:${it.toPlainString()}"
                    )
                }
            })
        poolViewModel.getLiquidityReserveLiveData()
            .observe(viewLifecycleOwner, Observer {
                if (poolViewModel.isTransferInMode()) {
                    poolViewModel.calculateTransferIntoAmount(
                        isInputA,
                        if (isInputA)
                            etInputBoxA.text.toString().trim()
                        else
                            etInputBoxB.text.toString().trim()
                    )
                } else {
                    poolViewModel.calculateTransferOutAmount(
                        etInputBoxA.text.toString().trim()
                    )
                }
            })
        poolViewModel.getPoolTokenAndPoolShareLiveData()
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
        poolViewModel.getInputATextLiveData().observe(viewLifecycleOwner, Observer {
            etInputBoxA.setText(it)
        })
        poolViewModel.getInputBTextLiveData().observe(viewLifecycleOwner, Observer {
            etInputBoxB.setText(it)
        })

        poolViewModel.loadState.observe(viewLifecycleOwner, Observer {
            when (it.peekData().status) {
                LoadState.Status.RUNNING -> {
                    showProgress(
                        if (it.peekData().action == ACTION_SYNC_LIQUIDITY_RESERVE)
                            getString(
                                if (poolViewModel.isTransferInMode())
                                    R.string.tips_market_add_liquidity_estimate_amount
                                else
                                    R.string.tips_market_remove_liquidity_estimate_amount
                            )
                        else
                            null
                    )
                }

                else -> {
                    dismissProgress()
                }
            }
        })
        poolViewModel.tipsMessage.observe(viewLifecycleOwner, Observer {
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
            tvInputLabelA.setText(R.string.transfer_in)
            tvInputLabelB.setText(R.string.transfer_in)
            btnPositive.setText(R.string.action_transfer_in_nbsp)

            etInputBoxB.filters = arrayOf(AmountInputFilter(12, 6))
            etInputBoxB.inputType =
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            etInputBoxB.isEnabled = true
            etInputBoxB.requestLayout()

            tvBalanceB.visibility = View.VISIBLE
            llSelectGroupB.visibility = View.VISIBLE

            ivModeIcon.setImageResource(
                getResourceId(R.attr.marketPoolAndIcon, requireContext())
            )
        } else {
            tvSwitchOpModeText.setText(R.string.transfer_out)
            tvInputLabelA.setText(R.string.pool_liquidity_token)
            tvInputLabelB.setText(R.string.transfer_out)
            btnPositive.setText(R.string.action_transfer_out_nbsp)

            etInputBoxB.filters = arrayOf()
            etInputBoxB.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            etInputBoxB.isEnabled = false
            adjustInputBoxBPaddingEnd()

            tvBalanceB.visibility = View.GONE
            llSelectGroupB.visibility = View.GONE

            ivModeIcon.setImageResource(
                getResourceId(R.attr.marketPoolOutIcon, requireContext())
            )
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
                    poolViewModel.getCurrOpModelPosition(),
                    mutableListOf(
                        getString(R.string.transfer_in),
                        getString(R.string.transfer_out)
                    )
                ) {
                    poolViewModel.switchOpModel(MarketPoolOpMode.values()[it])
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

        poolViewModel.switchOpModel(event.opMode)
    }

    //*********************************** 转出模式选择交易对逻辑 ***********************************//
    private val currLiquidityObserver = Observer<PoolLiquidityDTO?> {
        if (poolViewModel.isTransferInMode()) return@Observer

        if (it == null) {
            tvSelectTextA.text = getString(R.string.select_token_pair)
            etInputBoxB.hint = "0.00\n0.00"
            handleValueNull(tvBalanceA, R.string.market_liquidity_token_balance_format)
        } else {
            tvSelectTextA.text = "${it.coinA.displayName}/${it.coinB.displayName}"
            etInputBoxB.hint = "0.00 ${it.coinA.displayName}\n0.00 ${it.coinB.displayName}"
            tvBalanceA.text = getString(
                R.string.market_liquidity_token_balance_format,
                convertAmountToDisplayAmountStr(it.amount)
            )
        }

        adjustInputBoxAPaddingEnd()
    }

    private val liquidityListObserver = Observer<List<PoolLiquidityDTO>?> {
        if (it.isNullOrEmpty()) {
            showToast(R.string.tips_market_pool_select_token_pair_empty)
        } else {
            val displayList = it.map { item ->
                "${item.coinA.displayName}/${item.coinB.displayName}"
            } as MutableList
            showSelectLiquidityPopup(displayList)
        }
    }

    private val selectLiquidityArrowUpAnimator by lazy {
        ObjectAnimator.ofFloat(ivSelectArrowB, "rotation", 0F, 180F)
            .setDuration(360)
    }

    private val selectLiquidityArrowDownAnimator by lazy {
        ObjectAnimator.ofFloat(ivSelectArrowB, "rotation", 180F, 360F)
            .setDuration(360)
    }

    private fun showSelectLiquidityPopup(displayTokenPairs: MutableList<String>) {
        selectLiquidityArrowUpAnimator.start()
        XPopup.Builder(requireContext())
            .hasShadowBg(false)
            .popupAnimation(PopupAnimation.ScrollAlphaFromTop)
            .atView(llSelectGroupA)
            .setPopupCallback(
                object : EnhancedPopupCallback() {
                    override fun onDismissBefore() {
                        selectLiquidityArrowDownAnimator.start()
                    }
                })
            .asCustom(
                MarketSwitchPopupView(
                    requireContext(),
                    poolViewModel.getCurrLiquidityPosition(),
                    displayTokenPairs
                ) {
                    poolViewModel.selectLiquidity(it)
                }
            )
            .show()
    }

    //*********************************** 转入模式选择Token逻辑 ***********************************//
    private val currCoinAObserver = Observer<StableTokenVo?> {
        if (!poolViewModel.isTransferInMode()) return@Observer

        etInputBoxA.hint = "0.00"
        if (it == null) {
            tvSelectTextA.text = getString(R.string.select_token)
            handleValueNull(tvBalanceA, R.string.market_token_balance_format)
        } else {
            tvSelectTextA.text = it.displayName
            tvBalanceA.text = getString(
                R.string.market_token_balance_format,
                "${it.displayAmount.toPlainString()} ${it.displayName}"
            )
        }

        adjustInputBoxAPaddingEnd()
    }

    private val currCoinBObserver = Observer<StableTokenVo?> {
        if (!poolViewModel.isTransferInMode()) return@Observer

        etInputBoxB.hint = "0.00"
        if (it == null) {
            tvSelectTextB.text = getString(R.string.select_token)
            handleValueNull(tvBalanceB, R.string.market_token_balance_format)
        } else {
            tvSelectTextB.text = it.displayName
            tvBalanceB.text = getString(
                R.string.market_token_balance_format,
                "${it.displayAmount.toPlainString()} ${it.displayName}"
            )
        }

        adjustInputBoxBPaddingEnd()
    }

    private fun showSelectCoinDialog(selectCoinA: Boolean) {
        SelectTokenDialog.newInstance(
            if (selectCoinA) ACTION_POOL_SELECT_A else ACTION_POOL_SELECT_B
        ).show(childFragmentManager)
    }

    override fun onSelectCoin(action: Int, coin: ITokenVo) {
        poolViewModel.selectCoin(
            action == ACTION_POOL_SELECT_A,
            coin as StableTokenVo
        )
    }

    override fun getMarketSupportCoins(onlyNeedViolasCoins: Boolean) {
        marketViewModel.execute(onlyNeedViolasCoins)
    }

    override fun getMarketSupportCoinsLiveData(): LiveData<List<ITokenVo>?> {
        return marketViewModel.getMarketSupportCoinsLiveData()
    }

    override fun getCurrCoin(action: Int): ITokenVo? {
        return if (action == ACTION_POOL_SELECT_A)
            poolViewModel.getCurrCoinALiveData().value
        else
            poolViewModel.getCurrCoinBLiveData().value
    }

    //*********************************** 输入框逻辑 ***********************************//
    private val inputTextWatcherA = object : TextWatcherSimple() {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (!etInputBoxA.isFocused) return

            isInputA = true
            val inputText = s?.toString() ?: ""
            val amountStr = handleInputText(inputText)
            if (inputText != amountStr) {
                handleInputTextWatcher(amountStr, etInputBoxA, this)
            }

            if (poolViewModel.isTransferInMode()) {
                poolViewModel.calculateTransferIntoAmount(isInputA, amountStr)
            } else {
                poolViewModel.calculateTransferOutAmount(amountStr)
            }
        }
    }

    private val inputTextWatcherB = object : TextWatcherSimple() {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (!etInputBoxB.isFocused) return

            isInputA = false
            val inputText = s?.toString() ?: ""
            val amountStr = handleInputText(inputText)
            if (inputText != amountStr) {
                handleInputTextWatcher(amountStr, etInputBoxB, this)
            }

            if (poolViewModel.isTransferInMode()) {
                poolViewModel.calculateTransferIntoAmount(isInputA, amountStr)
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
        if (etInputBoxA.isFocused) {
            focused = true
            etInputBoxA.clearFocus()
        }
        if (etInputBoxB.isFocused) {
            focused = true
            etInputBoxB.clearFocus()
        }

        if (focused) {
            btnPositive.requestFocus()
            hideSoftInput()
        }
    }

    //*********************************** 转入转出逻辑 ***********************************//
    private fun transferInPreconditionsInvalid(): Boolean {
        val coinA = poolViewModel.getCurrCoinALiveData().value
        val coinB = poolViewModel.getCurrCoinBLiveData().value
        if (coinA == null || coinB == null) {
            showToast(R.string.tips_market_add_liquidity_coin_not_selected)
            return true
        }

        val inputAAmountStr = etInputBoxA.text.toString().trim()
        val inputBAmountStr = etInputBoxB.text.toString().trim()
        if (inputAAmountStr.isEmpty() && inputBAmountStr.isEmpty()) {
            showToast(R.string.tips_market_add_liquidity_amount_not_input)
            return true
        }

        if (inputAAmountStr.isNotEmpty()
            && BigDecimal(inputAAmountStr) > coinA.displayAmount
        ) {
            val prefix = if (inputBAmountStr.isNotEmpty()
                && BigDecimal(inputBAmountStr) > coinB.displayAmount
            ) {
                "${coinA.displayName}/${coinB.displayName}"
            } else {
                coinA.displayName
            }
            showToast(getString(R.string.tips_market_insufficient_balance_format, prefix))
            return true
        } else if (inputBAmountStr.isNotEmpty()
            && BigDecimal(inputBAmountStr) > coinB.displayAmount
        ) {
            val prefix = coinB.displayName
            showToast(getString(R.string.tips_market_insufficient_balance_format, prefix))
            return true
        }

        if (inputAAmountStr.isEmpty() || inputBAmountStr.isEmpty()) {
            poolViewModel.startSyncLiquidityReserveWork(showLoadingAndTips = true)
            return true
        }

        return false
    }

    private fun transferOutPreconditionsInvalid(): Boolean {
        val liquidity = poolViewModel.getCurrLiquidityLiveData().value
        if (liquidity == null) {
            showToast(R.string.tips_market_remove_liquidity_pair_not_selected)
            return true
        }

        val inputAAmountStr = etInputBoxA.text.toString().trim()
        if (inputAAmountStr.isEmpty()) {
            showToast(R.string.tips_market_remove_liquidity_amount_not_input)
            return true
        }

        if (convertDisplayAmountToAmount(inputAAmountStr) > liquidity.amount) {
            showToast(getString(R.string.tips_market_insufficient_balance_format, ""))
            return true
        }

        if (etInputBoxB.text.toString().isEmpty()) {
            poolViewModel.startSyncLiquidityReserveWork(showLoadingAndTips = true)
            return true
        }

        return false
    }

    private fun authenticateAccount(transferIn: Boolean) {
        authenticateAccount(
            poolViewModel.getViolasAccount()!!,
            poolViewModel.getAccountManager()
        ) { privateKey ->
            if (transferIn) {
                poolViewModel.execute(
                    privateKey,
                    etInputBoxA.text.toString().trim(),
                    etInputBoxB.text.toString().trim(),
                    action = ACTION_ADD_LIQUIDITY
                ) {
                    showToast(R.string.tips_market_add_liquidity_success)
                }
            } else {
                poolViewModel.execute(
                    privateKey,
                    etInputBoxA.text.toString().trim(),
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
        return if (poolViewModel.getViolasAccount() == null) {
            showToast(R.string.tips_create_or_import_wallet)
            true
        } else {
            false
        }
    }

    private fun adjustInputBoxAPaddingEnd() {
        etInputBoxA.post {
            val paddingRight = if (llSelectGroupA.visibility != View.VISIBLE) {
                DensityUtility.dp2px(requireContext(), 11)
            } else {
                llSelectGroupA.width + DensityUtility.dp2px(requireContext(), 11) * 2
            }
            etInputBoxA.setPadding(
                etInputBoxA.paddingLeft,
                etInputBoxA.paddingTop,
                paddingRight,
                etInputBoxA.paddingBottom
            )
        }
    }

    private fun adjustInputBoxBPaddingEnd() {
        etInputBoxB.post {
            val paddingRight = if (llSelectGroupB.visibility != View.VISIBLE) {
                DensityUtility.dp2px(requireContext(), 11)
            } else {
                llSelectGroupB.width + DensityUtility.dp2px(requireContext(), 11) * 2
            }
            etInputBoxB.setPadding(
                etInputBoxB.paddingLeft,
                etInputBoxB.paddingTop,
                paddingRight,
                etInputBoxB.paddingBottom
            )
        }
    }
}