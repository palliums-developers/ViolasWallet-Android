package com.violas.wallet.ui.transfer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.SeekBar
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import com.palliums.extensions.expandTouchArea
import com.palliums.extensions.show
import com.palliums.utils.start
import com.quincysx.crypto.CoinType
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.*
import com.violas.wallet.biz.bean.DiemCurrency
import com.violas.wallet.biz.command.CommandActuator
import com.violas.wallet.biz.command.RefreshAssetsCommand
import com.violas.wallet.common.getBitcoinCoinType
import com.violas.wallet.common.getDiemCoinType
import com.violas.wallet.common.getViolasCoinType
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.subscribeHub.BalanceSubscribeHub
import com.violas.wallet.repository.subscribeHub.BalanceSubscriber
import com.violas.wallet.ui.addressBook.AddressBookActivity
import com.violas.wallet.ui.main.market.bean.IAssetMark
import com.violas.wallet.ui.scan.ScanActivity
import com.violas.wallet.utils.authenticateAccount
import com.violas.wallet.utils.convertAmountToDisplayUnit
import com.violas.wallet.utils.convertDisplayUnitToAmount
import com.violas.wallet.viewModel.WalletAppViewModel
import com.violas.wallet.viewModel.bean.AssetVo
import com.violas.wallet.viewModel.bean.DiemCurrencyAssetVo
import com.violas.wallet.widget.dialog.AssetsVoTokenSelectTokenDialog
import kotlinx.android.synthetic.main.activity_multi_transfer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 转账通用页面
 */
class MultiTransferActivity : BaseAppActivity(),
    AssetsVoTokenSelectTokenDialog.AssetsDataResourcesBridge {

    companion object {
        private const val REQUEST_SELECTOR_ADDRESS = 1
        private const val REQUEST_SCAN_QR_CODE = 2

        private const val EXT_COIN_NUMBER = "1"
        private const val EXT_CURRENCY = "2"
        private const val EXT_PAYEE_ADDRESS = "3"
        private const val EXT_PAYEE_SUB_ADDRESS = "4"
        private const val EXT_AMOUNT = "5"

        fun start(
            context: Context,
            asset: AssetVo? = null,
            payeeAddress: String? = null,
            payeeSubAddress: String? = null,
            amount: Long? = null
        ) {
            start(
                context,
                asset?.getCoinNumber() ?: getBitcoinCoinType().coinNumber(),
                if (asset is DiemCurrencyAssetVo)
                    asset.currency
                else
                    null,
                payeeAddress,
                payeeSubAddress,
                amount
            )
        }

        fun start(
            context: Context,
            coinNumber: Int = getBitcoinCoinType().coinNumber(),
            currency: DiemCurrency? = null,
            payeeAddress: String? = null,
            payeeSubAddress: String? = null,
            amount: Long? = null
        ) {
            Intent(context, MultiTransferActivity::class.java).apply {
                putExtra(EXT_COIN_NUMBER, coinNumber)
                putExtra(EXT_CURRENCY, currency)
                putExtra(EXT_PAYEE_ADDRESS, payeeAddress)
                putExtra(EXT_PAYEE_SUB_ADDRESS, payeeSubAddress)
                putExtra(EXT_AMOUNT, amount)
            }.start(context)
        }
    }

    private var mCoinNumber: Int = Int.MIN_VALUE
    private var mCurrency: DiemCurrency? = null
    private var mPayeeAddress: String? = null
    private var mPayeeSubAddress: String? = null
    private var mAmount = 0L

    private val mViewModel by lazy {
        ViewModelProvider(this).get(MultiTransferViewModel::class.java)
    }

    private lateinit var mBalanceSubscriber: BalanceSubscriber

    override fun getLayoutResId() = R.layout.activity_multi_transfer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.transfer_title)
        init(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mCoinNumber != Int.MIN_VALUE)
            outState.putInt(EXT_COIN_NUMBER, mCoinNumber)
        mCurrency?.let { outState.putParcelable(EXT_CURRENCY, it) }
        val payeeAddress = editAddressInput.text.toString().trim()
        if (payeeAddress.isNotBlank())
            outState.putString(EXT_PAYEE_ADDRESS, payeeAddress)
        if (!mPayeeSubAddress.isNullOrBlank())
            outState.putString(EXT_PAYEE_SUB_ADDRESS, mPayeeSubAddress)
        outState.putLong(
            EXT_AMOUNT,
            convertDisplayUnitToAmount(
                editAmountInput.text.toString().trim(),
                CoinType.parseCoinNumber(mCoinNumber)
            )
        )
    }

    private fun init(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            mCoinNumber = savedInstanceState.getInt(EXT_COIN_NUMBER, Int.MIN_VALUE)
            mCurrency = savedInstanceState.getParcelable(EXT_CURRENCY)
            mPayeeAddress = savedInstanceState.getString(EXT_PAYEE_ADDRESS)
            mPayeeSubAddress = savedInstanceState.getString(EXT_PAYEE_SUB_ADDRESS)
            mAmount = savedInstanceState.getLong(EXT_AMOUNT, 0)
        } else if (intent != null) {
            mCoinNumber = intent.getIntExtra(EXT_COIN_NUMBER, Int.MIN_VALUE)
            mCurrency = intent.getParcelableExtra(EXT_CURRENCY)
            mPayeeAddress = intent.getStringExtra(EXT_PAYEE_ADDRESS)
            mPayeeSubAddress = intent.getStringExtra(EXT_PAYEE_SUB_ADDRESS)
            mAmount = intent.getLongExtra(EXT_AMOUNT, 0)
        }

        if (mCoinNumber != getViolasCoinType().coinNumber() &&
            mCoinNumber != getDiemCoinType().coinNumber() &&
            mCoinNumber != getBitcoinCoinType().coinNumber()
        ) {
            close()
            return
        }

        val assetMark = IAssetMark.convert(mCoinNumber, mCurrency)
        mBalanceSubscriber = object : BalanceSubscriber(assetMark) {
            override fun onNotice(asset: AssetVo?) {
                launch {
                    // 已初始化，资产更新
                    val currAsset = mViewModel.mAssetLiveData.value
                    if (currAsset != null) {
                        if (asset == null) return@launch
                        mViewModel.mAssetLiveData.value = asset
                        return@launch
                    }

                    // 未找到对应资产
                    if (asset == null) {
                        showToast(getString(R.string.transfer_tips_unopened_or_unsupported_token))
                        close()
                        return@launch
                    }

                    // 找到资产，初始化
                    mViewModel.mAssetLiveData.value = asset
                    initView()
                    initEvent()
                    CommandActuator.post(RefreshAssetsCommand())
                }
            }
        }
        BalanceSubscribeHub.observe(this, mBalanceSubscriber)
    }

    private fun initView() {
        if (!mPayeeAddress.isNullOrBlank()) {
            editAddressInput.setText(mPayeeAddress)
            mViewModel.mPayeeAddressLiveData.value = mPayeeAddress
        }
        if (mAmount > 0) {
            val displayAmount =
                convertAmountToDisplayUnit(mAmount, CoinType.parseCoinNumber(mCoinNumber))
            editAmountInput.setText(displayAmount.first)
            mViewModel.mAmountLiveData.value = displayAmount.first
        }
    }

    private fun initEvent() {
        // 监听进度条变化通知 ViewModel
        sbQuota.progress = mViewModel.mFeeProgressLiveData.value
            ?: MultiTransferViewModel.DEF_FEE_PROGRESS
        sbQuota.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                mViewModel.mFeeProgressLiveData.value = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        // 监听转账金额变化通知 ViewModel
        editAmountInput.addTextChangedListener {
            mViewModel.mAmountLiveData.value = it.toString()
        }

        // 监听转账地址变化通知 ViewModel
        editAddressInput.addTextChangedListener {
            mViewModel.mPayeeAddressLiveData.value = it.toString()
        }

        // 选择币种的点击事件
        llToSelectGroup.setOnClickListener {
            showSelectTokenDialog()
        }

        // 转账按钮的点击事件
        llToSelectGroup.expandTouchArea(12)
        btnConfirm.setOnClickListener {
            transfer()
        }

        // 跳转地址簿的点击事件
        ivAddressBook.setOnClickListener {
            AddressBookActivity.start(
                this@MultiTransferActivity,
                mCoinNumber,
                isSelector = true,
                requestCode = REQUEST_SELECTOR_ADDRESS
            )
        }

        // 扫码点击事件
        ivAddressBook.expandTouchArea(8)
        ivScan.setOnClickListener {
            ScanActivity.start(this, REQUEST_SCAN_QR_CODE)
        }

        // 订阅当前需要转账的币种变化
        mViewModel.mAssetLiveData.observe(this) {
            launch {
                mCoinNumber = it.getCoinNumber()
                mCurrency = if (it is DiemCurrencyAssetVo) it.currency else null

                tvToSelectText.text = it.getAssetsName()
                tvCoinAmount.text = getString(
                    R.string.common_label_balance_with_unit_format,
                    it.amountWithUnit.amount,
                    it.amountWithUnit.unit
                )
            }
        }

        // 订阅手续费
        mViewModel.mFeeAmount.observe(this) {
            launch {
                tvFee.text = "${it.first} ${it.second}"
            }
        }
    }

    private fun transfer() {
        val assets = mViewModel.mAssetLiveData.value
        if (assets == null) {
            showToast(R.string.transfer_tips_token_empty)
            return
        }

        launch(Dispatchers.IO) {
            try {
                showProgress()
                val account = AccountManager.getAccountByCoinNumber(assets.getCoinNumber())
                if (account == null) {
                    showToast(getString(R.string.transfer_tips_unopened_or_unsupported_token))
                    return@launch
                }

                mViewModel.checkConditions(account)
                dismissProgress()
                showPasswordAndSend(account)
            } catch (e: Exception) {
                dismissProgress()
                e.message?.let { it1 -> showToast(it1) }
                e.printStackTrace()
            }
        }
    }

    private fun showPasswordAndSend(account: AccountDO) {
        authenticateAccount(account, passwordCallback = {
            launch(Dispatchers.IO) {
                try {
                    showProgress()
                    mViewModel.transfer(account, it.toByteArray(), mPayeeSubAddress)
                    dismissProgress()
                    withContext(Dispatchers.Main) {
                        showToast(getString(R.string.transfer_tips_transfer_success))
                        CommandActuator.postDelay(RefreshAssetsCommand(), 2000)
                        finish()
                    }
                } catch (e: Exception) {
                    dismissProgress()
                    showToast(e.message ?: getString(R.string.transfer_tips_transfer_failure))
                }
            }
        })
    }

    private fun showSelectTokenDialog() {
        AssetsVoTokenSelectTokenDialog()
            .setCallback {
                changeBalanceSubscriber(
                    it.getCoinNumber(),
                    if (it is DiemCurrencyAssetVo) it.currency else null
                )
            }
            .show(supportFragmentManager)
    }

    override suspend fun getSupportAssetsTokens(): LiveData<List<AssetVo>?> {
        return WalletAppViewModel.getInstance().mAssetsLiveData
    }

    override fun getCurrCoin(): AssetVo? {
        return mViewModel.mAssetLiveData.value
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_SELECTOR_ADDRESS -> {
                data?.getStringExtra(AddressBookActivity.RESULT_SELECT_ADDRESS)?.let {
                    if (it.isBlank()) return@let
                    launch {
                        editAddressInput.setText(it)
                    }
                }
            }

            REQUEST_SCAN_QR_CODE -> {
                data?.getParcelableExtra<QRCode>(ScanActivity.RESULT_QR_CODE_DATA)?.let { qrCode ->
                    when (qrCode) {
                        is TransferQRCode -> {
                            changeBalanceSubscriber(
                                qrCode.coinNumber,
                                if (!qrCode.tokenName.isNullOrBlank()) DiemCurrency(qrCode.tokenName) else null
                            )

                            mPayeeSubAddress = qrCode.subAddress
                            editAddressInput.setText(qrCode.address)
                        }

                        is CommonQRCode -> {
                            mPayeeSubAddress = null
                            editAddressInput.setText(qrCode.content)
                        }
                    }
                }
            }
        }
    }

    private fun changeBalanceSubscriber(coinNumber: Int, currency: DiemCurrency?) {
        launch {
            if (coinNumber != mCoinNumber || mCurrency != currency) {
                mCoinNumber = coinNumber
                mCurrency = currency
                mBalanceSubscriber.changeSubscriber(
                    IAssetMark.convert(
                        coinNumber,
                        currency
                    )
                )
            }
        }
    }
}