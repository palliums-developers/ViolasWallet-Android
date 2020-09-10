package com.violas.wallet.ui.tokenDetails

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.google.android.material.tabs.TabLayout
import com.palliums.base.ViewController
import com.palliums.extensions.close
import com.palliums.extensions.setTitleToCenter
import com.palliums.extensions.show
import com.palliums.utils.CustomMainScope
import com.palliums.utils.getColorByAttrId
import com.palliums.utils.getResourceId
import com.palliums.utils.start
import com.palliums.widget.adapter.FragmentPagerAdapterSupport
import com.palliums.widget.loading.LoadingDialog
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.common.KEY_FOUR
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.common.KEY_THREE
import com.violas.wallet.common.KEY_TWO
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.ui.changeLanguage.MultiLanguageUtility
import com.violas.wallet.ui.collection.CollectionActivity
import com.violas.wallet.ui.transactionRecord.TransactionRecordFragment
import com.violas.wallet.ui.transactionRecord.TransactionType
import com.violas.wallet.ui.transfer.TransferActivity
import com.violas.wallet.utils.ClipboardUtils
import com.violas.wallet.utils.loadCircleImage
import com.violas.wallet.viewModel.WalletAppViewModel
import com.violas.wallet.viewModel.bean.AssetsCoinVo
import com.violas.wallet.viewModel.bean.AssetsTokenVo
import com.violas.wallet.viewModel.bean.AssetsVo
import kotlinx.android.synthetic.main.activity_token_details.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.yokeyword.fragmentation.SupportActivity

/**
 * Created by elephant on 2020/6/3 15:27.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 币种详情页面（包括当前币的币种信息和交易记录）
 */
class TokenDetailsActivity : SupportActivity(), ViewController,
    CoroutineScope by CustomMainScope() {

    companion object {
        fun start(context: Context, assetsVo: AssetsVo) {
            Intent(context, TokenDetailsActivity::class.java)
                .apply {
                    putExtra(KEY_ONE, assetsVo.getCoinNumber())
                    if (assetsVo is AssetsTokenVo) {
                        putExtra(KEY_TWO, assetsVo.address)
                        putExtra(KEY_THREE, assetsVo.module)
                        putExtra(KEY_FOUR, assetsVo.name)
                    }
                }
                .start(context)
        }
    }

    private val mWalletAppViewModel by lazy {
        WalletAppViewModel.getViewModelInstance(this)
    }

    private var mCoinNumber = Int.MIN_VALUE
    private var mTokenAddress: String? = null
    private var mTokenModule: String? = null
    private var mTokenName: String? = null

    private lateinit var mAssetsVo: AssetsVo
    private lateinit var mAccountDO: AccountDO

    private var mLoadingDialog: LoadingDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_token_details)
        initTopView()
        init(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mCoinNumber != Int.MIN_VALUE) {
            outState.putInt(KEY_ONE, mCoinNumber)
        }
        mTokenAddress?.let { outState.putString(KEY_TWO, it) }
        mTokenModule?.let { outState.putString(KEY_THREE, it) }
        mTokenName?.let { outState.putString(KEY_FOUR, it) }
    }

    private fun init(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            mCoinNumber = savedInstanceState.getInt(KEY_ONE, mCoinNumber)
            mTokenAddress = savedInstanceState.getString(KEY_TWO)
            mTokenModule = savedInstanceState.getString(KEY_THREE)
            mTokenName = savedInstanceState.getString(KEY_FOUR)
        } else if (intent != null) {
            mCoinNumber = intent.getIntExtra(KEY_ONE, mCoinNumber)
            mTokenAddress = intent.getStringExtra(KEY_TWO)
            mTokenModule = intent.getStringExtra(KEY_THREE)
            mTokenName = intent.getStringExtra(KEY_FOUR)
        }
        if (mCoinNumber == Int.MIN_VALUE) {
            close()
            return
        }

        mWalletAppViewModel.mAssetsListLiveData.observe(this, Observer {
            var exists = false
            for (item in it) {
                if (item.getCoinNumber() == mCoinNumber
                    && ((item is AssetsCoinVo && mTokenModule.isNullOrBlank())
                            || (item is AssetsTokenVo
                            && item.address == mTokenAddress
                            && item.module == mTokenModule
                            && item.name == mTokenName)
                            )
                ) {
                    mAssetsVo = item
                    exists = true
                    break
                }
            }

            if (exists && this::mAccountDO.isInitialized) {
                updateTokenInfoView()
                return@Observer
            } else if (!exists) {
                if (!this::mAccountDO.isInitialized) {
                    close()
                }
                return@Observer
            }

            launch {
                val accountDO = withContext(Dispatchers.IO) {
                    try {
                        mWalletAppViewModel.mAccountManager
                            .getAccountById(mAssetsVo.getAccountId())
                    } catch (e: Exception) {
                        null
                    }
                }

                if (accountDO == null) {
                    close()
                } else {
                    mAccountDO = accountDO
                    updateTokenInfoView()
                    initFragmentPager()
                    initEvent()
                }
            }
        })
    }

    private fun initTopView() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener {
            onBackPressedSupport()
        }

        collapsingToolbarLayout.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View?,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int
            ) {
                if (oldBottom == bottom) {
                    collapsingToolbarLayout.removeOnLayoutChangeListener(this)
                    toolbar.setTitleToCenter(tvTitle)
                }
            }
        })
    }

    private fun updateTokenInfoView() {
        tvTitle.text = mAssetsVo.getAssetsName()

        ivTokenLogo.loadCircleImage(
            mAssetsVo.getLogoUrl(),
            getResourceId(R.attr.iconCoinDefLogo, this)
        )

        tvTokenName.text = mAssetsVo.getAssetsName()
        tvTokenAmount.text = mAssetsVo.amountWithUnit.amount
        tvFiatAmount.text =
            "≈${mAssetsVo.fiatAmountWithUnit.symbol}${mAssetsVo.fiatAmountWithUnit.amount}"
        tvTokenAddress.text = mAccountDO.address
    }

    private fun initFragmentPager() {
        val tokenId = (mAssetsVo as? AssetsTokenVo)?.module
        val tokenDisplayName = mAssetsVo.getAssetsName()

        val fragments = mutableListOf<Fragment>()
        supportFragmentManager.fragments.forEach {
            if (it is TransactionRecordFragment) {
                fragments.add(it)
            }
        }
        if (fragments.isEmpty()) {
            fragments.add(
                TransactionRecordFragment.newInstance(
                    walletAddress = mAccountDO.address,
                    coinNumber = mCoinNumber,
                    transactionType = TransactionType.ALL,
                    tokenId = tokenId,
                    tokenDisplayName = tokenDisplayName
                )
            )
            if (mCoinNumber == CoinTypes.Libra.coinType()
                || mCoinNumber == CoinTypes.Violas.coinType()
            ) {
                fragments.add(
                    TransactionRecordFragment.newInstance(
                        walletAddress = mAccountDO.address,
                        coinNumber = mCoinNumber,
                        transactionType = TransactionType.COLLECTION,
                        tokenId = tokenId,
                        tokenDisplayName = tokenDisplayName
                    )
                )
                fragments.add(
                    TransactionRecordFragment.newInstance(
                        walletAddress = mAccountDO.address,
                        coinNumber = mCoinNumber,
                        transactionType = TransactionType.TRANSFER,
                        tokenId = tokenId,
                        tokenDisplayName = tokenDisplayName
                    )
                )
            }
        }

        val titles = mutableListOf(getString(R.string.label_all))
        if (mCoinNumber == CoinTypes.Libra.coinType()
            || mCoinNumber == CoinTypes.Violas.coinType()
        ) {
            titles.add(getString(R.string.label_transfer_in))
            titles.add(getString(R.string.label_transfer_out))
        }

        viewPager.offscreenPageLimit = 2
        viewPager.adapter = FragmentPagerAdapterSupport(supportFragmentManager).apply {
            setFragments(fragments)
            setTitles(titles)
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                updateTab(tab, false)
            }

            override fun onTabSelected(tab: TabLayout.Tab) {
                updateTab(tab, true)
            }
        })
        tabLayout.setupWithViewPager(viewPager)
        tabLayout.post {
            val count = viewPager.adapter!!.count
            for (i in 0 until count) {
                tabLayout.getTabAt(i)?.let { tab ->
                    updateTab(tab, i == viewPager.currentItem)?.let {
                        it.text = tab.text
                    }
                }
            }
        }
    }

    private fun updateTab(tab: TabLayout.Tab, select: Boolean): TextView? {
        return getTabTextView(tab)?.also {
            it.setTypeface(Typeface.DEFAULT, if (select) Typeface.BOLD else Typeface.NORMAL)
            it.setTextColor(
                getColorByAttrId(
                    if (select) R.attr.colorPrimary else R.attr.tokenDetailsTabNormalTextColor,
                    this
                )
            )
        }
    }

    private fun getTabTextView(tab: TabLayout.Tab): TextView? {
        return try {
            val textViewField = tab.view.javaClass.getDeclaredField("textView")
            textViewField.isAccessible = true
            val textView = textViewField.get(tab.view) as TextView
            textViewField.isAccessible = false
            textView
        } catch (e: Exception) {
            null
        }
    }

    private fun initEvent() {
        tvTokenAddress.setOnClickListener {
            ClipboardUtils.copy(this, mAccountDO.address)
        }

        btnTransfer.setOnClickListener {
            TransferActivity.start(this, mAssetsVo)
        }

        btnCollection.setOnClickListener {
            if (mAssetsVo is AssetsTokenVo) {
                CollectionActivity.start(
                    context = this,
                    accountId = mAssetsVo.getAccountId(),
                    isToken = true,
                    tokenId = mAssetsVo.getId()
                )
            } else {
                CollectionActivity.start(
                    context = this,
                    accountId = mAssetsVo.getAccountId(),
                    isToken = false
                )
            }
        }

        flExchange.setOnClickListener {
            // TODO 跳转到闪兑页面
            Log.d("dddddddddd", "click")
            showToast(R.string.hint_quotes_not_open)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(MultiLanguageUtility.attachBaseContext(newBase))
    }

    override fun showProgress(@StringRes resId: Int) {
        showProgress(getString(resId))
    }

    override fun showProgress(msg: String?) {
        launch {
            if (mLoadingDialog == null) {
                mLoadingDialog = LoadingDialog()
                    .setMessage(msg)
                mLoadingDialog!!.show(supportFragmentManager)
            } else {
                mLoadingDialog!!.setMessage(msg)
            }
        }
    }

    override fun dismissProgress() {
        launch {
            mLoadingDialog?.close()
            mLoadingDialog = null
        }
    }

    override fun showToast(@StringRes msgId: Int, duration: Int) {
        showToast(getString(msgId), duration)
    }

    override fun showToast(msg: String, duration: Int) {
        launch {
            Toast.makeText(this@TokenDetailsActivity, msg, duration).show()
        }
    }
}