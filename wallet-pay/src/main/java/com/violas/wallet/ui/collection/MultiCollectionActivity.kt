package com.violas.wallet.ui.collection

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.LiveData
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
import com.violas.wallet.ui.transfer.TransferAssetsDataResourcesBridge
import com.violas.wallet.ui.transfer.TransferSelectTokenDialog
import com.violas.wallet.utils.ClipboardUtils
import com.violas.wallet.viewModel.WalletAppViewModel
import com.violas.wallet.viewModel.bean.AssetsCoinVo
import com.violas.wallet.viewModel.bean.AssetsTokenVo
import com.violas.wallet.viewModel.bean.AssetsVo
import kotlinx.android.synthetic.main.activity_collection.*
import kotlinx.android.synthetic.main.activity_multi_transfer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class MultiCollectionActivity : BaseAppActivity(), TransferAssetsDataResourcesBridge {
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
        return PAGE_STYLE_SECONDARY
    }

    private fun initViewData() = launch(Dispatchers.IO) {
        val assetsName = intent.getStringExtra(EXT_ASSETS_NAME)
        val coinNumber = intent.getIntExtra(EXT_COIN_NUMBER, CoinTypes.Violas.coinType())

        changeCurrAssets(coinNumber, assetsName)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.title_colletction)

        initViewData()

        // 选择币种的点击事件
        llToSelectGroup.setOnClickListener {
            showSelectTokenDialog()
        }
        llToSelectGroup.expandTouchArea(12)

        // 订阅当前需要转账的币种变化
        mMultiCollectionViewModel.mCurrAssets.observe(this, androidx.lifecycle.Observer { assets ->
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

                val prefix = if (assets is AssetsTokenVo) {
                    val tokenDo = mTokenManager.findTokenById(assets.getId())
                    if (tokenDo != null) {
                        withContext(Dispatchers.Main) {
//                        tvWalletName.text = "${currentAccount.walletNickname}-${tokenDo.name}"
                        }
                        "-${tokenDo.name.toLowerCase(Locale.CHINA)}"
                    } else {
                        ""
                    }
                } else {
                    ""
                }

                val collectionAddress =
                    "${CoinTypes.parseCoinType(currentAccount.coinNumber).fullName()
                        .toLowerCase(Locale.CHINA)}${prefix}:${currentAccount.address}"
                val createQRCodeBitmap = QRUtils.createQRCodeBitmap(
                    collectionAddress,
                    DensityUtility.dp2px(this@MultiCollectionActivity, 164),
                    null
                )
                withContext(Dispatchers.Main) {
                    ivQRCode.setImageBitmap(createQRCodeBitmap)
                }
            }
        })
    }

    private fun showSelectTokenDialog() {
        TransferSelectTokenDialog
            .newInstance()
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