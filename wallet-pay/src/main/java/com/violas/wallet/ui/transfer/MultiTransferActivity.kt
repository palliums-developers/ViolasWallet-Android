package com.violas.wallet.ui.transfer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.SeekBar
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.palliums.extensions.expandTouchArea
import com.palliums.extensions.logError
import com.palliums.extensions.show
import com.palliums.utils.start
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.ScanCodeType
import com.violas.wallet.biz.ScanTranBean
import com.violas.wallet.biz.command.CommandActuator
import com.violas.wallet.biz.command.RefreshAssetsAllListCommand
import com.violas.wallet.biz.decodeScanQRCode
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.subscribeHub.BalanceSubscribeHub
import com.violas.wallet.repository.subscribeHub.BalanceSubscriber
import com.violas.wallet.ui.addressBook.AddressBookActivity
import com.violas.wallet.ui.main.market.bean.IAssetsMark
import com.violas.wallet.ui.scan.ScanActivity
import com.violas.wallet.utils.authenticateAccount
import com.violas.wallet.utils.convertAmountToDisplayUnit
import com.violas.wallet.utils.convertDisplayUnitToAmount
import com.violas.wallet.viewModel.WalletAppViewModel
import com.violas.wallet.viewModel.bean.AssetsCoinVo
import com.violas.wallet.viewModel.bean.AssetsTokenVo
import com.violas.wallet.viewModel.bean.AssetsVo
import com.violas.wallet.widget.dialog.AssetsVoTokenSelectTokenDialog
import kotlinx.android.synthetic.main.activity_multi_transfer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class MultiTransferActivity : BaseAppActivity(),
    AssetsVoTokenSelectTokenDialog.AssetsDataResourcesBridge {
    companion object {
        private const val REQUEST_SELECTOR_ADDRESS = 1
        private const val REQUEST_SCAN_QR_CODE = 2

        private const val EXT_ADDRESS = "1"
        private const val EXT_AMOUNT = "2"
        private const val EXT_ASSETS_NAME = "3"
        private const val EXT_COIN_NUMBER = "4"

        fun start(
            context: Context,
            assetsVo: AssetsVo? = null,
            toAddress: String? = null,
            amount: Long? = null
        ) {
            val assetsName = if (assetsVo is AssetsCoinVo) {
                null
            } else {
                assetsVo?.getAssetsName()
            }
            val coinNumber = assetsVo?.getCoinNumber() ?: (if (Vm.TestNet) {
                CoinTypes.BitcoinTest.coinType()
            } else {
                CoinTypes.Bitcoin.coinType()
            })
            Intent(context, MultiTransferActivity::class.java).apply {
                putExtra(EXT_ADDRESS, toAddress)
                putExtra(EXT_AMOUNT, amount)
                putExtra(EXT_ASSETS_NAME, assetsName)
                putExtra(EXT_COIN_NUMBER, coinNumber)
            }.start(context)
        }
    }

    private var initTag = false
    private var assetsName: String? = ""
    private var coinNumber: Int = CoinTypes.Violas.coinType()
    private var transferAmount = 0L
    private var toAddress: String? = ""
    private val mAccountManager by lazy { AccountManager() }
    private var mCurrAssetsAmount = BigDecimal("0")

    private val mWalletAppViewModel by lazy {
        WalletAppViewModel.getViewModelInstance(this@MultiTransferActivity)
    }
    private val mMultiTransferViewModel by lazy {
        ViewModelProvider(this).get(MultiTransferViewModel::class.java)
    }

    private val mCurrAssertsAmountSubscriber = object : BalanceSubscriber(null) {
        override fun onNotice(assets: AssetsVo?) {
            launch {
                tvCoinAmount.text = String.format(
                    getString(R.string.hint_transfer_amount),
                    assets?.amountWithUnit?.amount ?: "- -",
                    assets?.amountWithUnit?.unit ?: ""
                )
                withContext(Dispatchers.IO) {
                    mCurrAssetsAmount = BigDecimal(assets?.amountWithUnit?.amount ?: "0")
                }
            }
        }
    }

    override fun getLayoutResId() = R.layout.activity_multi_transfer

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        assetsName?.let { outState.putString(EXT_ASSETS_NAME, it) }
        outState.putInt(EXT_COIN_NUMBER, coinNumber)
        outState.putLong(
            EXT_AMOUNT,
            convertDisplayUnitToAmount(
                editAmountInput.text.toString().trim(),
                CoinTypes.parseCoinType(coinNumber)
            )
        )
        outState.putString(EXT_ADDRESS, editAddressInput.text.toString().trim())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.title_transfer)

        mWalletAppViewModel.mAssetsListLiveData.observe(this, Observer {
            if (!initTag) {
                initTag = true
                init(savedInstanceState)
            }
        })
    }

    private fun init(savedInstanceState: Bundle?) {
        initData(savedInstanceState)
        initView()

        sbQuota.progress = mMultiTransferViewModel.mFeeProgressAmount.value
            ?: MultiTransferViewModel.DEF_FEE_PROGRESS
        // 监听进度条变化通知 ViewModel
        sbQuota.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                mMultiTransferViewModel.mFeeProgressAmount.value = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
        // 监听转账金额变化通知 ViewModel
        editAmountInput.addTextChangedListener {
            mMultiTransferViewModel.mTransferAmount.value = it.toString()
        }
        // 监听转账地址变化通知 ViewModel
        editAddressInput.addTextChangedListener {
            mMultiTransferViewModel.mTransferPayeeAddress.value = it.toString()
        }

        // 订阅当前币种的金额变化
        BalanceSubscribeHub.observe(this, mCurrAssertsAmountSubscriber)
        // 选择币种的点击事件
        llToSelectGroup.setOnClickListener {
            showSelectTokenDialog()
        }
        llToSelectGroup.expandTouchArea(12)
        // 转账按钮的点击事件
        btnConfirm.setOnClickListener {
            transfer()
        }
        // 跳转地址簿的点击事件
        ivAddressBook.setOnClickListener {
            AddressBookActivity.start(
                this@MultiTransferActivity,
                coinNumber,
                isSelector = true,
                requestCode = REQUEST_SELECTOR_ADDRESS
            )
        }
        ivAddressBook.expandTouchArea(8)
        // 扫码点击事件
        ivScan.setOnClickListener {
            ScanActivity.start(this, REQUEST_SCAN_QR_CODE)
        }

        // 订阅当前需要转账的币种变化
        mMultiTransferViewModel.mCurrAssets.observe(this, Observer {
            launch {
                assetsName = it.getAssetsName()
                coinNumber = it.getCoinNumber()

                tvToSelectText.text = it.getAssetsName()
                withContext(Dispatchers.IO) {
                    mCurrAssertsAmountSubscriber.changeSubscriber(IAssetsMark.convert(it))
                }
            }
        })
        // 订阅手续费
        mMultiTransferViewModel.mFeeAmount.observe(this, Observer {
            launch {
                tvFee.setText("${it.first} ${it.second}")
            }
        })
    }

    private fun initData(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            assetsName = savedInstanceState.getString(EXT_ASSETS_NAME)
            coinNumber = savedInstanceState.getInt(EXT_COIN_NUMBER, CoinTypes.Violas.coinType())
            transferAmount = savedInstanceState.getLong(EXT_AMOUNT, 0)
            toAddress = savedInstanceState.getString(EXT_ADDRESS)
        } else if (intent != null) {
            assetsName = intent.getStringExtra(EXT_ASSETS_NAME)
            coinNumber = intent.getIntExtra(EXT_COIN_NUMBER, CoinTypes.Violas.coinType())
            transferAmount = intent.getLongExtra(EXT_AMOUNT, 0)
            toAddress = intent.getStringExtra(EXT_ADDRESS)
        }
    }

    private fun initView() {
        if (transferAmount > 0) {
            val displayAmount =
                convertAmountToDisplayUnit(transferAmount, CoinTypes.parseCoinType(coinNumber))
            editAmountInput.setText(displayAmount.first)
        }
        editAddressInput.setText(toAddress)

        launch(Dispatchers.IO) {
            changeCurrAssets(coinNumber, assetsName)
        }
    }

    private fun transfer() {
        val assets = mMultiTransferViewModel.mCurrAssets.value
        if (assets == null) {
            showToast(R.string.hint_please_select_currency_transfer)
            return
        }
        launch(Dispatchers.IO) {
            try {
                showProgress()
                val account = mAccountManager.getIdentityByCoinType(assets.getCoinNumber())
                if (account == null) {
                    showToast(getString(R.string.hint_unsupported_tokens))
                    return@launch
                }
                mMultiTransferViewModel.checkConditions(account, mCurrAssetsAmount)
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
        authenticateAccount(account, mAccountManager, passwordCallback = {
            launch(Dispatchers.IO) {
                try {
                    showProgress()
                    mMultiTransferViewModel.transfer(account, it.toByteArray())
                    dismissProgress()
                    withContext(Dispatchers.Main) {
                        showToast(getString(R.string.hint_transfer_broadcast_success))
                        CommandActuator.postDelay(RefreshAssetsAllListCommand(), 2000)
                        finish()
                    }
                } catch (e: Exception) {
                    e.message?.let { showToast(it) }
                    dismissProgress()
                }
            }
        })
    }

    private fun showSelectTokenDialog() {
        AssetsVoTokenSelectTokenDialog()
            .setCallback { assetsVo ->
                changeCurrAssets(assetsVo)
            }
            .show(supportFragmentManager)
    }

    override suspend fun getSupportAssetsTokens(): LiveData<List<AssetsVo>?> {
        return mWalletAppViewModel.mAssetsListLiveData
    }

    override fun getCurrCoin(): AssetsVo? {
        return mMultiTransferViewModel.mCurrAssets.value
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_SELECTOR_ADDRESS -> {
                data?.apply {
                    val address = getStringExtra(AddressBookActivity.RESULT_SELECT_ADDRESS) ?: ""
                    launch {
                        editAddressInput.setText(address)
                    }
                }
            }
            REQUEST_SCAN_QR_CODE -> {
                data?.getStringExtra(ScanActivity.RESULT_QR_CODE_DATA)?.let { msg ->
                    decodeScanQRCode(msg) { scanType, scanBean ->
                        if (scanType == ScanCodeType.Address) {
                            scanBean as ScanTranBean
                            changeCurrAssets(scanBean.coinType, scanBean.tokenName)
                            onScanAddressQr(scanBean.address)
                        } else if (scanType == ScanCodeType.Text) {
                            onScanAddressQr(scanBean.msg)
                        }
                    }
                }
            }
        }
    }

    private fun changeCurrAssets(assetsVo: AssetsVo) {
        launch {
            if (mMultiTransferViewModel.mCurrAssets.value != assetsVo) {
                mMultiTransferViewModel.mCurrAssets.value = assetsVo
            }
        }
    }

    private fun changeCurrAssets(coinType: Int, tokenModule: String?) {
        val assetsList = mWalletAppViewModel.mAssetsListLiveData.value
        logError { "assetsList size = ${assetsList?.size ?: 0}" }
        assetsList?.forEach { assets ->
            if (coinType == CoinTypes.BitcoinTest.coinType()
                || coinType == CoinTypes.Bitcoin.coinType()
            ) {
                if (coinType == assets.getCoinNumber()) {
                    changeCurrAssets(assets)
                    return@forEach
                }
            } else {
                if (tokenModule == null) {
                    return@forEach
                }
                if (assets is AssetsTokenVo
                    && coinType == assets.getCoinNumber()
                    && assets.module.equals(tokenModule, true)
                ) {
                    changeCurrAssets(assets)
                    return@forEach
                }
            }
        }
    }

    private fun onScanAddressQr(address: String) {
        launch {
            editAddressInput.setText(address)
        }
    }
}