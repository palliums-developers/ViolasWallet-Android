package com.violas.wallet.ui.mapping

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.AmountInputFilter
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.palliums.utils.*
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.command.CommandActuator
import com.violas.wallet.biz.command.RefreshAssetsAllListCommand
import com.violas.wallet.repository.subscribeHub.BalanceSubscribeHub
import com.violas.wallet.repository.subscribeHub.BalanceSubscriber
import com.violas.wallet.ui.main.market.bean.CoinAssetsMark
import com.violas.wallet.ui.main.market.bean.LibraTokenAssetsMark
import com.violas.wallet.ui.mapping.MappingViewModel.Companion.ACTION_MAPPING
import com.violas.wallet.utils.*
import com.violas.wallet.viewModel.bean.AssetsVo
import kotlinx.android.synthetic.main.activity_mapping.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

/**
 * Created by elephant on 2020/8/11 16:15.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 映射页面
 */
class MappingActivity : BaseAppActivity() {

    companion object {
        fun start(context: Context) {
            Intent(context, MappingActivity::class.java).start(context)
        }
    }

    private val mappingViewModel by lazy {
        ViewModelProvider(this).get(MappingViewModel::class.java)
    }
    private var coinBalance = BigDecimal.ZERO

    override fun getLayoutResId(): Int {
        return R.layout.activity_mapping
    }

    override fun getTitleStyle(): Int {
        return PAGE_STYLE_CUSTOM
    }

    override fun onTitleRightViewClick() {
        showToast("进入兑换记录页面")
    }

    // <editor-fold defaultState="collapsed" desc="初始化View、Event、Observer">
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置页面
        StatusBarUtil.setLightStatusBarMode(window, false)
        setTopBackgroundResource(getResourceId(R.attr.homeFragmentTopBg, this))
        setTitleLeftImageResource(getResourceId(R.attr.iconBackSecondary, this))
        setTitleRightImageResource(getResourceId(R.attr.mappingMenuIcon, this))
        titleColor = getResourceId(R.attr.colorOnPrimary, this)
        setTitle(R.string.mapping)

        // 设置View初始值
        reset()

        // 输入框设置
        etFromInputBox.addTextChangedListener(fromInputTextWatcher)
        etFromInputBox.filters = arrayOf(AmountInputFilter(12, 6))

        // 按钮点击事件
        llFromSelectGroup.setOnClickListener {
            clearInputBoxFocusAndHideSoftInput()
            showSelectCoinDialog()
        }

        btnMapping.setOnClickListener {
            clearInputBoxFocusAndHideSoftInput()
            if (mappingPreconditionsInvalid()) return@setOnClickListener
            authenticateAccountAndMapping()
        }

        // 数据观察
        mappingViewModel.getCurrMappingCoinPairLiveData().observe(this, Observer {
            if (it == null) {
                reset()
                coinBalanceSubscriber.changeSubscriber(null)
            } else {
                tvExchangeRate.text =
                    "1 ${it.fromCoin.assets.displayName} = 1 ${it.toCoin.assets.displayName}"
                tvFromSelectText.text = it.fromCoin.assets.displayName
                tvToCoinName.text = it.toCoin.assets.displayName

                val coinType = str2CoinType(it.fromCoin.chainName)
                val assetsMark =
                    if (coinType == CoinTypes.Bitcoin || coinType == CoinTypes.BitcoinTest) {
                        CoinAssetsMark(coinType)
                    } else {
                        LibraTokenAssetsMark(
                            coinType,
                            it.fromCoin.assets.module,
                            it.fromCoin.assets.address,
                            it.fromCoin.assets.name
                        )
                    }
                coinBalanceSubscriber.changeSubscriber(assetsMark)
            }

            adjustInputBoxPaddingEnd()
        })

        BalanceSubscribeHub.observe(this, coinBalanceSubscriber)
    }
    // </editor-fold>

    // <editor-fold defaultState="collapsed" desc="选择币种相关逻辑">
    private fun showSelectCoinDialog() {
        // TODO 选择币种逻辑
    }
    // </editor-fold>

    // <editor-fold defaultState="collapsed" desc="输入框相关逻辑">
    private val fromInputTextWatcher = object : TextWatcherSimple() {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (!etFromInputBox.isFocused) return

            val inputText = s?.toString() ?: ""
            val amountStr = correctInputText(inputText)
            if (inputText != amountStr) {
                setupInputText(amountStr, etFromInputBox, this)
            }

            etToInputBox.setText(amountStr)
        }
    }

    private val correctInputText: (String) -> String = { inputText ->
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

    private val setupInputText: (String, EditText, TextWatcher) -> Unit =
        { amountStr, inputBox, textWatcher ->
            inputBox.removeTextChangedListener(textWatcher)

            inputBox.setText(amountStr)
            inputBox.setSelection(amountStr.length)

            inputBox.addTextChangedListener(textWatcher)
        }

    private fun clearInputBoxFocusAndHideSoftInput() {
        if (etFromInputBox.isFocused) {
            etFromInputBox.clearFocus()
            btnMapping.requestFocus()
            hideSoftInput(etFromInputBox)
        }
    }

    private fun adjustInputBoxPaddingEnd() {
        etFromInputBox.post {
            val paddingRight = if (llFromSelectGroup.visibility != View.VISIBLE) {
                DensityUtility.dp2px(this, 16)
            } else {
                llFromSelectGroup.width + DensityUtility.dp2px(this, 23)
            }
            etFromInputBox.setPadding(
                etFromInputBox.paddingLeft,
                etFromInputBox.paddingTop,
                paddingRight,
                etFromInputBox.paddingBottom
            )
        }
        etToInputBox.post {
            val paddingRight = if (tvToCoinName.visibility != View.VISIBLE) {
                DensityUtility.dp2px(this, 16)
            } else {
                tvToCoinName.width + DensityUtility.dp2px(this, 16) * 2
            }
            etToInputBox.setPadding(
                etToInputBox.paddingLeft,
                etToInputBox.paddingTop,
                paddingRight,
                etToInputBox.paddingBottom
            )
        }
    }
    // </editor-fold>

    // <editor-fold defaultState="collapsed" desc="映射相关逻辑">
    private fun mappingPreconditionsInvalid(): Boolean {
        // 未选择币种判断
        val coinPair = mappingViewModel.getCurrMappingCoinPairLiveData().value
        if (coinPair == null) {
            showToast(R.string.tips_mapping_coin_not_selected)
            return true
        }

        // 未输入判断
        val inputAmountStr = etFromInputBox.text.toString().trim()
        if (inputAmountStr.isEmpty()) {
            showToast(R.string.tips_mapping_amount_not_input)
            return true
        }

        // 输入为0判断, 当作未输入判断
        val inputAmount =
            convertAmountToDisplayAmount(inputAmountStr, str2CoinType(coinPair.fromCoin.chainName))
        if (inputAmount <= BigDecimal.ZERO) {
            showToast(R.string.tips_mapping_amount_not_input)
            return true
        }

        // 余额不足判断
        if (inputAmount > coinBalance) {
            showToast(R.string.tips_mapping_insufficient_balance)
            return true
        }

        return false
    }

    private fun authenticateAccountAndMapping() {
        launch {
            val accountDO = withContext(Dispatchers.IO) {
                mappingViewModel.getAccount()
            } ?: return@launch

            authenticateAccount(accountDO, mappingViewModel.accountManager) {
                mappingViewModel.execute(
                    it,
                    etFromInputBox.text.toString().trim(),
                    action = ACTION_MAPPING
                ) {
                    CommandActuator.postDelay(RefreshAssetsAllListCommand(), 2000)
                    showToast(R.string.tips_mapping_success)
                }
            }
        }
    }
    // </editor-fold>

    // <editor-fold defaultState="collapsed" desc="其它逻辑">
    private fun reset() {
        tvBalance.setText(R.string.value_null)
        tvExchangeRate.setText(R.string.value_null)
        tvMinerFees.setText(R.string.value_null)
        tvFromSelectText.setText(R.string.select_mapping_coin)
        tvToCoinName.text = ""
        etFromInputBox.setText("")
        etToInputBox.setText("0")
        coinBalance = BigDecimal.ZERO
    }

    private val coinBalanceSubscriber =
        object : BalanceSubscriber(null) {
            override fun onNotice(assets: AssetsVo?) {
                launch {
                    val coinPair =
                        mappingViewModel.getCurrMappingCoinPairLiveData().value ?: return@launch

                    val coinType = str2CoinType(coinPair.fromCoin.chainName)
                    coinBalance = convertDisplayAmountToAmount(
                        assets?.amountWithUnit?.amount ?: "0",
                        coinType
                    )

                    tvBalance.text =
                        "${convertAmountToDisplayAmountStr(
                            coinBalance, coinType
                        )} ${coinPair.fromCoin.assets.displayName}"
                }
            }
        }
    // </editor-fold>
}