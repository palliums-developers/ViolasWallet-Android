package com.violas.wallet.ui.tokenDetails

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.palliums.base.ViewController
import com.palliums.extensions.close
import com.palliums.extensions.setTitleToCenter
import com.palliums.extensions.show
import com.palliums.utils.*
import com.palliums.widget.adapter.FragmentPagerAdapterSupport
import com.palliums.widget.loading.LoadingDialog
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.bean.DiemCurrency
import com.violas.wallet.common.*
import com.violas.wallet.event.HomePageType
import com.violas.wallet.event.MarketPageType
import com.violas.wallet.event.SwitchHomePageEvent
import com.violas.wallet.event.SwitchMarketPageEvent
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.subscribeHub.BalanceSubscribeHub
import com.violas.wallet.repository.subscribeHub.BalanceSubscriber
import com.violas.wallet.ui.changeLanguage.MultiLanguageUtility
import com.violas.wallet.ui.collection.CollectionActivity
import com.violas.wallet.ui.main.market.bean.IAssetMark
import com.violas.wallet.ui.transactionRecord.TransactionRecordFragment
import com.violas.wallet.ui.transactionRecord.TransactionType
import com.violas.wallet.ui.transfer.TransferActivity
import com.violas.wallet.utils.ClipboardUtils
import com.violas.wallet.utils.loadCircleImage
import com.violas.wallet.viewModel.bean.AssetVo
import com.violas.wallet.viewModel.bean.DiemCurrencyAssetVo
import kotlinx.android.synthetic.main.activity_token_details.*
import kotlinx.android.synthetic.main.fragment_market_pool.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.yokeyword.fragmentation.SupportActivity
import org.greenrobot.eventbus.EventBus

/**
 * Created by elephant on 2020/6/3 15:27.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 币种详情页面（包括当前币的币种信息和交易记录）
 */
class TokenDetailsActivity : SupportActivity(), ViewController,
    CoroutineScope by CustomMainScope() {

    companion object {
        fun start(context: Context, asset: AssetVo) {
            Intent(context, TokenDetailsActivity::class.java)
                .apply {
                    putExtra(KEY_ONE, asset.getCoinNumber())
                    if (asset is DiemCurrencyAssetVo) {
                        putExtra(KEY_TWO, asset.currency)
                    }
                }
                .start(context)
        }
    }

    private var mCoinNumber = Int.MIN_VALUE
    private var mCurrency: DiemCurrency? = null

    private lateinit var mAsset: AssetVo
    private lateinit var mAccount: AccountDO

    private var mLoadingDialog: LoadingDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setSystemBar(
            layoutToStatusBar = false,
            lightModeStatusBar = true,
            lightModeNavigationBar = true
        )
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
        mCurrency?.let { outState.putParcelable(KEY_TWO, it) }
    }

    private fun init(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            mCoinNumber = savedInstanceState.getInt(KEY_ONE, mCoinNumber)
            mCurrency = savedInstanceState.getParcelable(KEY_TWO)
        } else if (intent != null) {
            mCoinNumber = intent.getIntExtra(KEY_ONE, mCoinNumber)
            mCurrency = intent.getParcelableExtra(KEY_TWO)
        }
        if (mCoinNumber == Int.MIN_VALUE) {
            close()
            return
        }

        val assetMark = IAssetMark.convert(mCoinNumber, mCurrency)
        val balanceSubscriber = object : BalanceSubscriber(assetMark) {
            override fun onNotice(asset: AssetVo?) {
                launch {
                    if (this@TokenDetailsActivity::mAccount.isInitialized) {
                        if (asset == null) return@launch
                        mAsset = asset
                        updateTokenInfoView()
                    } else {
                        if (asset == null) {
                            close()
                            return@launch
                        }

                        mAsset = asset
                        val account = withContext(Dispatchers.IO) {
                            try {
                                AccountManager.getAccountById(mAsset.getAccountId())
                            } catch (e: Exception) {
                                null
                            }
                        }

                        if (account == null) {
                            close()
                        } else {
                            mAccount = account
                            updateTokenInfoView()
                            initFragmentPager()
                            initEvent()
                        }
                    }
                }
            }
        }
        BalanceSubscribeHub.observe(this, balanceSubscriber)
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
                if (oldBottom != 0 && oldBottom <= bottom) {
                    collapsingToolbarLayout.removeOnLayoutChangeListener(this)
                    toolbar.setTitleToCenter(tvTitle)
                }
            }
        })

        tvTokenAddress.post {
            tvTokenAddress?.maxWidth =
                clTokenInfo.width - DensityUtility.dp2px(this, 28)
        }
    }

    private fun updateTokenInfoView() {
        tvTitle.text = mAsset.getAssetsName()

        ivTokenLogo.loadCircleImage(
            mAsset.getLogoUrl(),
            getResourceId(R.attr.iconCoinDefLogo, this)
        )

        tvTokenName.text = mAsset.getAssetsName()
        tvTokenAmount.text = mAsset.amountWithUnit.amount
        tvFiatAmount.text =
            "≈${mAsset.fiatAmountWithUnit.symbol}${mAsset.fiatAmountWithUnit.amount}"
        tvTokenAddress.text = mAccount.address
    }

    private fun initFragmentPager() {
        val tokenId = (mAsset as? DiemCurrencyAssetVo)?.currency?.module
        val tokenDisplayName = mAsset.getAssetsName()

        val fragments = mutableListOf<Fragment>()
        supportFragmentManager.fragments.forEach {
            if (it is TransactionRecordFragment) {
                fragments.add(it)
            }
        }
        if (fragments.isEmpty()) {
            fragments.add(
                TransactionRecordFragment.newInstance(
                    walletAddress = mAccount.address,
                    coinNumber = mCoinNumber,
                    transactionType = TransactionType.ALL,
                    tokenId = tokenId,
                    tokenDisplayName = tokenDisplayName
                )
            )
            if (mCoinNumber == getDiemCoinType().coinNumber()
                || mCoinNumber == getViolasCoinType().coinNumber()
            ) {
                fragments.add(
                    TransactionRecordFragment.newInstance(
                        walletAddress = mAccount.address,
                        coinNumber = mCoinNumber,
                        transactionType = TransactionType.COLLECTION,
                        tokenId = tokenId,
                        tokenDisplayName = tokenDisplayName
                    )
                )
                fragments.add(
                    TransactionRecordFragment.newInstance(
                        walletAddress = mAccount.address,
                        coinNumber = mCoinNumber,
                        transactionType = TransactionType.TRANSFER,
                        tokenId = tokenId,
                        tokenDisplayName = tokenDisplayName
                    )
                )
            }
        }

        val titles = mutableListOf(getString(R.string.currency_details_tab_1))
        if (mCoinNumber == getDiemCoinType().coinNumber()
            || mCoinNumber == getViolasCoinType().coinNumber()
        ) {
            titles.add(getString(R.string.currency_details_tab_2))
            titles.add(getString(R.string.currency_details_tab_3))
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
            it.typeface = Typeface.create(
                getString(if (select) R.string.font_family_title else R.string.font_family_normal),
                Typeface.NORMAL
            )
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
            ClipboardUtils.copy(this, mAccount.address)
        }

        btnTransfer.setOnClickListener {
            TransferActivity.start(this, mAsset)
        }

        btnCollection.setOnClickListener {
            if (mAsset is DiemCurrencyAssetVo) {
                CollectionActivity.start(
                    context = this,
                    accountId = mAsset.getAccountId(),
                    isToken = true,
                    tokenId = mAsset.getId()
                )
            } else {
                CollectionActivity.start(
                    context = this,
                    accountId = mAsset.getAccountId(),
                    isToken = false
                )
            }
        }

        flExchange.setOnClickListener {
            EventBus.getDefault().post(SwitchHomePageEvent(HomePageType.Market))
            EventBus.getDefault().post(SwitchMarketPageEvent(MarketPageType.Swap))
            finish()
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