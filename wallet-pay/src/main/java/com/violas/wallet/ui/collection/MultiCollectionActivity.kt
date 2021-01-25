package com.violas.wallet.ui.collection

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import cn.bertsir.zbar.utils.QRUtils
import com.palliums.extensions.expandTouchArea
import com.palliums.extensions.show
import com.palliums.utils.DensityUtility
import com.palliums.utils.start
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.TokenManager
import com.violas.wallet.common.Vm
import com.violas.wallet.utils.ClipboardUtils
import com.violas.wallet.viewModel.WalletAppViewModel
import com.violas.wallet.viewModel.bean.AssetsCoinVo
import com.violas.wallet.viewModel.bean.AssetsTokenVo
import com.violas.wallet.viewModel.bean.AssetsVo
import com.violas.wallet.widget.dialog.AssetsVoTokenSelectTokenDialog
import kotlinx.android.synthetic.main.activity_collection.*
import kotlinx.android.synthetic.main.activity_multi_transfer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.palliums.libracore.transaction.AccountAddress
import org.palliums.libracore.wallet.AccountIdentifier
import org.palliums.libracore.wallet.IntentIdentifier
import org.palliums.violascore.serialization.hexToBytes
import java.util.*

/**
 *  通用的收款页面
 */
class MultiCollectionActivity : BaseAppActivity(),
    AssetsVoTokenSelectTokenDialog.AssetsDataResourcesBridge {

    companion object {
        const val EXT_ASSETS_NAME = "1"
        const val EXT_COIN_NUMBER = "2"

        fun start(context: Context, assetsVo: AssetsVo? = null) {
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
            Intent(context, MultiCollectionActivity::class.java).apply {
                putExtra(EXT_ASSETS_NAME, assetsName)
                putExtra(EXT_COIN_NUMBER, coinNumber)
            }.start(context)
        }
    }

    private var initTag = false
    private var assetsName: String? = ""
    private var coinNumber: Int = CoinTypes.Violas.coinType()

    private val mWalletAppViewModel by lazy {
        WalletAppViewModel.getViewModelInstance(this@MultiCollectionActivity)
    }
    private val mMultiCollectionViewModel by lazy {
        ViewModelProvider(this).get(MultiCollectionViewModel::class.java)
    }

    private val mTokenManager by lazy {
        TokenManager()
    }
    private val mAccountManager by lazy {
        AccountManager()
    }
    private var mJob: Job? = null

    override fun getLayoutResId() = R.layout.activity_multi_collection

    override fun getTitleStyle(): Int {
        return PAGE_STYLE_LIGHT_MODE_PRIMARY_TOP_BAR
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        assetsName?.let { outState.putString(EXT_ASSETS_NAME, it) }
        outState.putInt(EXT_COIN_NUMBER, coinNumber)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.receive_title)

        mWalletAppViewModel.mAssetsListLiveData.observe(this, Observer {
            if (!initTag) {
                initTag = true
                init(savedInstanceState)
            }
        })
    }

    private fun init(savedInstanceState: Bundle?) {
        initData(savedInstanceState)
        changeCurrAssets(coinNumber, assetsName)

        // 选择币种的点击事件
        llToSelectGroup.setOnClickListener {
            showSelectTokenDialog()
        }
        llToSelectGroup.expandTouchArea(12)

        // 订阅当前需要转账的币种变化
        mMultiCollectionViewModel.mCurrAssets.observe(this, Observer { assets ->
            assetsName = assets.getAssetsName()
            coinNumber = assets.getCoinNumber()

            tvToSelectText.text = assets.getAssetsName()
            mJob?.cancel()
            mJob = launch(Dispatchers.IO) {
                val currentAccount = mAccountManager.getAccountById(assets.getAccountId())
                withContext(Dispatchers.Main) {
                    tvAddress.text = currentAccount.address
//                tvWalletName.text = currentAccount.walletNickname
                    btnCopy.setOnClickListener {
                        ClipboardUtils.copy(this@MultiCollectionActivity, currentAccount.address)
                    }
                    btnCopy.expandTouchArea(20)
                    tvAddress.setOnClickListener {
                        ClipboardUtils.copy(this@MultiCollectionActivity, currentAccount.address)
                    }
                }

                val collectionAddress = when (currentAccount.coinNumber) {
                    CoinTypes.Bitcoin.coinType(), CoinTypes.BitcoinTest.coinType() -> {
                        "${
                            CoinTypes.parseCoinType(currentAccount.coinNumber).fullName()
                                .toLowerCase(Locale.CHINA)
                        }:${currentAccount.address}"
                    }
                    CoinTypes.Libra.coinType() -> {
                        val tokenDo = mTokenManager.findTokenById(assets.getId())
                        val network = if (Vm.TestNet) {
                            AccountIdentifier.NetworkPrefix.TestnetPrefix
                        } else {
                            AccountIdentifier.NetworkPrefix.MainnetPrefix
                        }
                        IntentIdentifier(
                            AccountIdentifier(
                                network,
                                AccountAddress(currentAccount.address.hexToBytes())
                            ), currency = tokenDo?.name
                        ).encode()
                    }
                    CoinTypes.Violas.coinType() -> {
                        val tokenDo = mTokenManager.findTokenById(assets.getId())
                        val network = if (Vm.TestNet) {
                            org.palliums.violascore.wallet.AccountIdentifier.NetworkPrefix.TestnetPrefix
                        } else {
                            org.palliums.violascore.wallet.AccountIdentifier.NetworkPrefix.MainnetPrefix
                        }
                        org.palliums.violascore.wallet.IntentIdentifier(
                            org.palliums.violascore.wallet.AccountIdentifier(
                                network,
                                org.palliums.violascore.transaction.AccountAddress(currentAccount.address.hexToBytes())
                            ), currency = tokenDo?.name
                        ).encode()
                    }
                    else -> null
                }
                collectionAddress?.let {
                    val createQRCodeBitmap = QRUtils.createQRCodeBitmap(
                        collectionAddress,
                        DensityUtility.dp2px(this@MultiCollectionActivity, 164),
                        null
                    )
                    withContext(Dispatchers.Main) {
                        ivQRCode.setImageBitmap(createQRCodeBitmap)
                    }
                }
            }
        })
    }

    private fun initData(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            assetsName = savedInstanceState.getString(EXT_ASSETS_NAME)
            coinNumber = savedInstanceState.getInt(EXT_COIN_NUMBER, CoinTypes.Violas.coinType())
        } else if (intent != null) {
            assetsName = intent.getStringExtra(EXT_ASSETS_NAME)
            coinNumber = intent.getIntExtra(EXT_COIN_NUMBER, CoinTypes.Violas.coinType())
        }
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
        return mMultiCollectionViewModel.mCurrAssets.value
    }


    private fun changeCurrAssets(assetsVo: AssetsVo) {
        launch {
            if (mMultiCollectionViewModel.mCurrAssets.value != assetsVo) {
                mMultiCollectionViewModel.mCurrAssets.value = assetsVo
            }
        }
    }

    private fun changeCurrAssets(coinType: Int, tokenModule: String?) {
        mWalletAppViewModel.mAssetsListLiveData.value?.forEach { assets ->
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

}