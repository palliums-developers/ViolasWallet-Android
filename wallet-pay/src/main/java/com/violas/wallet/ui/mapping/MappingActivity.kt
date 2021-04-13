package com.violas.wallet.ui.mapping

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.AmountInputFilter
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.lifecycle.EnhancedMutableLiveData
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.palliums.extensions.getShowErrorMessage
import com.palliums.net.LoadState
import com.palliums.utils.*
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.LackOfBalanceException
import com.violas.wallet.biz.ReceiverAccountCurrencyNotAddException
import com.violas.wallet.biz.ReceiverAccountNotActivationException
import com.violas.wallet.biz.bean.DiemAppToken
import com.violas.wallet.biz.command.CommandActuator
import com.violas.wallet.biz.command.RefreshAssetsCommand
import com.violas.wallet.biz.mapping.UnsupportedMappingCoinPairException
import com.violas.wallet.common.getBitcoinCoinType
import com.violas.wallet.common.getEthereumCoinType
import com.violas.wallet.common.getViolasDappUrl
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.subscribeHub.BalanceSubscribeHub
import com.violas.wallet.repository.subscribeHub.BalanceSubscriber
import com.violas.wallet.ui.main.market.bean.CoinAssetMark
import com.violas.wallet.ui.main.market.bean.DiemCurrencyAssetMark
import com.violas.wallet.ui.main.market.bean.ITokenVo
import com.violas.wallet.ui.main.market.selectToken.CoinsBridge
import com.violas.wallet.ui.main.market.selectToken.SelectTokenDialog
import com.violas.wallet.ui.main.market.selectToken.SelectTokenDialog.Companion.ACTION_MAPPING_SELECT
import com.violas.wallet.utils.*
import com.violas.wallet.viewModel.bean.AssetVo
import com.violas.wallet.widget.dialog.PublishTokenDialog
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
class MappingActivity : BaseAppActivity(), CoinsBridge {

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
        Intent(this, MappingRecordActivity::class.java).start(this)
    }

    // <editor-fold defaultState="collapsed" desc="初始化View、Event、Observer">
    override fun onCreate(savedInstanceState: Bundle?) {
        window.setSystemBar(lightModeStatusBar = false, lightModeNavigationBar = true)
        super.onCreate(savedInstanceState)

        // 设置页面
        setTitleLeftImageResource(getResourceId(R.attr.iconBackSecondary, this))
        setTitleRightTextColor(getResourceId(R.attr.colorOnPrimary, this))
        titleColor = getResourceId(R.attr.colorOnPrimary, this)
        setTopBackgroundResource(getResourceId(R.attr.homeFragmentTopBg, this))
        setTitleRightImageResource(getResourceId(R.attr.iconRecordSecondary, this))
        setTitle(R.string.mapping_title)

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
            onClickMapping()
        }

        tvDappUrl.text = getViolasDappUrl()
        tvDappUrl.setOnClickListener {
            ClipboardUtils.copy(this, tvDappUrl.text.toString())
        }

        // 数据观察
        mappingViewModel.getCurrMappingCoinPairLiveData().observe(this) {
            if (it == null) {
                reset()
                coinBalanceSubscriber.changeSubscriber(null)
            } else {
                tvExchangeRate.text =
                    "1 ${it.fromCoin.assets.displayName} = 1 ${it.toCoin.assets.displayName}"
                tvFromSelectText.text = it.fromCoin.assets.displayName
                tvToCoinName.text = it.toCoin.assets.displayName
                etAddress.setText("")
                etAddress.visibility =
                    if (str2CoinType(it.toCoin.chainName) == getEthereumCoinType())
                        View.VISIBLE
                    else
                        View.GONE

                val coinType = str2CoinType(it.fromCoin.chainName)
                val assetsMark =
                    if (coinType == getBitcoinCoinType()) {
                        CoinAssetMark(coinType)
                    } else {
                        DiemCurrencyAssetMark(
                            coinType,
                            it.fromCoin.assets.module,
                            it.fromCoin.assets.address,
                            it.fromCoin.assets.name
                        )
                    }
                coinBalanceSubscriber.changeSubscriber(assetsMark)
            }

            adjustInputBoxPaddingEnd()
        }

        mappingViewModel.loadState.observe(this, Observer {
            when (it.peekData().status) {
                LoadState.Status.RUNNING -> {
                    showProgress()
                }

                else -> {
                    dismissProgress()
                }
            }
        })
        mappingViewModel.tipsMessage.observe(this, Observer {
            it.getDataIfNotHandled()?.let { msg ->
                if (msg.isNotEmpty()) {
                    showToast(msg)
                }
            }
        })

        BalanceSubscribeHub.observe(this, coinBalanceSubscriber)
    }
    // </editor-fold>

    // <editor-fold defaultState="collapsed" desc="选择币种相关逻辑">
    private fun showSelectCoinDialog() {
        SelectTokenDialog.newInstance(ACTION_MAPPING_SELECT)
            .show(supportFragmentManager, SelectTokenDialog::javaClass.name)
    }

    override fun onSelectCoin(action: Int, coin: ITokenVo) {
        mappingViewModel.selectCoin(coin)
    }

    override fun getMarketSupportCoins(failureCallback: (error: Throwable) -> Unit) {
        mappingViewModel.getMappingCoinParis(failureCallback)
    }

    override fun getMarketSupportCoinsLiveData(): LiveData<List<ITokenVo>> {
        return mappingViewModel.getCoinsLiveData()
    }

    override fun getTipsMessageLiveData(): EnhancedMutableLiveData<String> {
        return mappingViewModel.coinsTipsMessage
    }

    override fun getCurrCoin(action: Int): ITokenVo? {
        val coinPair =
            mappingViewModel.getCurrMappingCoinPairLiveData().value ?: return null
        return mappingViewModel.coinPair2Coin(coinPair)
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
    private fun onClickMapping() {
        clearInputBoxFocusAndHideSoftInput()

        launch {
            // 未选择币种判断
            val coinPair = mappingViewModel.getCurrMappingCoinPairLiveData().value
            if (coinPair == null) {
                showToast(R.string.mapping_tips_token_empty)
                return@launch
            }

            // 未输入判断
            val inputAmountStr = etFromInputBox.text.toString().trim()
            if (inputAmountStr.isEmpty()) {
                showToast(R.string.mapping_tips_mapping_amount_empty)
                return@launch
            }

            // 输入为0判断, 当作未输入判断
            val inputAmount = convertDisplayAmountToAmount(
                inputAmountStr,
                str2CoinType(coinPair.fromCoin.chainName)
            )
            if (inputAmount <= BigDecimal.ZERO) {
                showToast(R.string.mapping_tips_mapping_amount_empty)
                return@launch
            }

            // 余额不足判断
            if (inputAmount > coinBalance) {
                showToast(R.string.common_tips_insufficient_available_balance)
                return@launch
            }

            // 付款账户判断
            val payerAccountDO = withContext(Dispatchers.IO) {
                mappingViewModel.getAccount(true)
            }
            if (payerAccountDO == null) {
                showToast(R.string.common_tips_account_error)
                return@launch
            }

            // 收款账户判断
            var payeeAddress: String? = null
            var payeeAccountDO: AccountDO? = null
            if (str2CoinType(coinPair.toCoin.chainName) == getEthereumCoinType()) {
                payeeAddress = etAddress.text.toString().trim()
                if (payeeAddress.isNullOrBlank()) {
                    showToast(R.string.mapping_tips_mapping_ethereum_address_empty)
                    return@launch
                }

                if (payeeAddress.length != 42) {
                    showToast(R.string.mapping_tips_mapping_ethereum_address_error)
                    return@launch
                }
            } else {
                payeeAccountDO = withContext(Dispatchers.IO) {
                    mappingViewModel.getAccount(false)
                }
                if (payeeAccountDO == null) {
                    showToast(R.string.common_tips_account_error)
                    return@launch
                }
            }

            authenticateAccount(
                payerAccountDO,
                passwordCallback = {
                    mapping(
                        true,
                        payeeAddress,
                        payeeAccountDO,
                        payerAccountDO,
                        it.toByteArray()
                    )
                }
            )
        }
    }

    private fun mapping(
        checkPayeeAccount: Boolean,
        payeeAddress: String?,
        payeeAccountDO: AccountDO?,
        payerAccountDO: AccountDO,
        password: ByteArray
    ) {
        showProgress()
        launch(Dispatchers.IO) {
            try {
                mappingViewModel.mapping(
                    checkPayeeAccount,
                    payeeAddress,
                    payeeAccountDO,
                    payerAccountDO,
                    password,
                    etFromInputBox.text.toString().trim()
                )

                CommandActuator.postDelay(RefreshAssetsCommand(), 2000)
                clearInputBox()
                dismissProgress()
                showToast(R.string.mapping_tips_mapping_success)
            } catch (e: Exception) {
                dismissProgress()

                when (e) {
                    is UnsupportedMappingCoinPairException -> {
                        showToast(R.string.mapping_tips_unsupported_token)
                    }

                    is ReceiverAccountNotActivationException -> {
                        showToast(R.string.chain_tips_payee_account_not_active)
                    }

                    is LackOfBalanceException ->{
                        showToast(R.string.transfer_tips_insufficient_balance_or_assets_unconfirmed)
                    }

                    is ReceiverAccountCurrencyNotAddException -> {
                        showPublishTokenDialog(
                            payeeAddress,
                            payeeAccountDO,
                            payerAccountDO,
                            password,
                            e.appToken
                        )
                    }

                    else -> {
                        e.printStackTrace()
                        showToast(e.getShowErrorMessage(R.string.mapping_tips_mapping_failure))
                    }
                }
            }
        }
    }

    private fun showPublishTokenDialog(
        payeeAddress: String?,
        payeeAccountDO: AccountDO?,
        payerAccountDO: AccountDO,
        password: ByteArray,
        appToken: DiemAppToken
    ) {
        PublishTokenDialog()
            .setAddCurrencyPage(false)
            .setCurrencyName(appToken.name)
            .setConfirmListener {
                it.dismiss()

                launch(Dispatchers.IO) {
                    showProgress()
                    try {
                        if (mappingViewModel.publishToken(
                                password,
                                payeeAccountDO!!,
                                appToken
                            )
                        ) {
                            mapping(
                                false,
                                payeeAddress,
                                payeeAccountDO,
                                payerAccountDO,
                                password
                            )
                        } else {
                            showToast(R.string.txn_details_state_add_currency_failure)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()

                        dismissProgress()
                        showToast(R.string.txn_details_state_add_currency_failure)
                    }
                }
            }.show(supportFragmentManager)
    }

    private fun clearInputBox() {
        launch {
            etFromInputBox.setText("")
            etToInputBox.setText("0")
        }
    }
    // </editor-fold>

    // <editor-fold defaultState="collapsed" desc="其它逻辑">
    private fun reset() {
        tvBalance.setText(R.string.common_desc_value_null)
        tvExchangeRate.setText(R.string.common_desc_value_null)
        tvMinerFees.setText(R.string.common_desc_value_null)
        tvFromSelectText.setText(R.string.common_action_select_token)
        tvToCoinName.text = ""
        etFromInputBox.setText("")
        etToInputBox.setText("0")
        etAddress.setText("")
        etAddress.visibility = View.GONE
        coinBalance = BigDecimal.ZERO
    }

    private val coinBalanceSubscriber =
        object : BalanceSubscriber(null) {
            override fun onNotice(asset: AssetVo?) {
                launch {
                    val coinPair =
                        mappingViewModel.getCurrMappingCoinPairLiveData().value ?: return@launch

                    val coinType = str2CoinType(coinPair.fromCoin.chainName)
                    coinBalance = convertDisplayAmountToAmount(
                        asset?.amountWithUnit?.amount ?: "0",
                        coinType
                    )

                    tvBalance.text =
                        "${
                            convertAmountToDisplayAmountStr(
                                coinBalance, coinType
                            )
                        } ${coinPair.fromCoin.assets.displayName}"
                }
            }
        }
    // </editor-fold>
}