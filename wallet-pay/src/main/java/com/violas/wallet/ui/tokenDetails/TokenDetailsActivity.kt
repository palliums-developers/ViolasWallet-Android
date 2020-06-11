package com.violas.wallet.ui.tokenDetails

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.tabs.TabLayout
import com.palliums.base.ViewController
import com.palliums.extensions.close
import com.palliums.extensions.setTitleToCenter
import com.palliums.extensions.show
import com.palliums.utils.CustomMainScope
import com.palliums.utils.StatusBarUtil
import com.palliums.utils.start
import com.palliums.widget.loading.LoadingDialog
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.common.KEY_TWO
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.ui.changeLanguage.MultiLanguageUtility
import com.violas.wallet.ui.collection.CollectionActivity
import com.violas.wallet.ui.transactionRecord.TransactionRecordFragment
import com.violas.wallet.ui.transactionRecord.TransactionType
import com.violas.wallet.ui.transfer.TransferActivity
import com.violas.wallet.utils.ClipboardUtils
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
                        putExtra(KEY_TWO, assetsVo.getAssetsName())
                    }
                }
                .start(context)
        }
    }

    private val mWalletAppViewModel by lazy {
        WalletAppViewModel.getViewModelInstance(this)
    }

    private var mCoinNumber = Int.MIN_VALUE
    private var mTokenName: String? = null

    private lateinit var mAssetsVo: AssetsVo
    private lateinit var mAccountDO: AccountDO

    private var mLoadingDialog: LoadingDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_token_details)
        initView()
        initEvent()
        initData(savedInstanceState)
    }

    private fun initData(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            mCoinNumber = savedInstanceState.getInt(KEY_ONE, mCoinNumber)
            mTokenName = savedInstanceState.getString(KEY_TWO)
        } else if (intent != null) {
            mCoinNumber = intent.getIntExtra(KEY_ONE, mCoinNumber)
            mTokenName = intent.getStringExtra(KEY_TWO)
        }
        if (mCoinNumber == Int.MIN_VALUE) {
            close()
            return
        }

        mWalletAppViewModel.mAssetsListLiveData.observe(this, Observer {
            var exists = false
            for (item in it) {
                if (item.getCoinNumber() == mCoinNumber
                    && ((item is AssetsCoinVo && mTokenName.isNullOrBlank())
                            || (item is AssetsTokenVo && item.getAssetsName() == mTokenName))
                ) {
                    mAssetsVo = item
                    exists = true
                    break
                }
            }

            if (!exists) {
                close()
                return@Observer
            }

            if (viewPager.adapter != null) {
                initTokenInfoView()
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
                    initTokenInfoView()
                    initTransactionRecordsView()
                }
            }
        })
    }

    private fun initTokenInfoView() {
        tvTitle.text = mAssetsVo.getAssetsName()

        Glide.with(this)
            .load(mAssetsVo.getLogoUrl())
            .error(R.drawable.ic_token_info_logo_default)
            .placeholder(R.drawable.ic_token_info_logo_default)
            .into(ivTokenLogo)

        tvTokenName.text = mAssetsVo.getAssetsName()
        tvTokenAmount.text = mAssetsVo.amountWithUnit.amount
        tvFiatAmount.text =
            "≈${mAssetsVo.fiatAmountWithUnit.symbol}${mAssetsVo.fiatAmountWithUnit.amount}"
        tvTokenAddress.text = mAccountDO.address
    }

    private fun initTransactionRecordsView(){
        val tokenAddress =
            if (mAssetsVo is AssetsTokenVo) (mAssetsVo as AssetsTokenVo).address else null
        val fragments = mutableListOf(
            Pair(
                getString(R.string.label_all),
                TransactionRecordFragment.newInstance(
                    walletAddress = mAccountDO.address,
                    coinNumber = mCoinNumber,
                    transactionType = TransactionType.ALL,
                    tokenAddress = tokenAddress,
                    tokenName = mTokenName
                )
            )
        )
        if (mCoinNumber == CoinTypes.Libra.coinType()
            || mCoinNumber == CoinTypes.Violas.coinType()
        ) {
            fragments.add(
                Pair(
                    getString(R.string.label_transfer_in),
                    TransactionRecordFragment.newInstance(
                        walletAddress = mAccountDO.address,
                        coinNumber = mCoinNumber,
                        transactionType = TransactionType.TRANSFER,
                        tokenAddress = tokenAddress,
                        tokenName = mTokenName
                    )
                )
            )
            fragments.add(
                Pair(
                    getString(R.string.label_transfer_out),
                    TransactionRecordFragment.newInstance(
                        walletAddress = mAccountDO.address,
                        coinNumber = mCoinNumber,
                        transactionType = TransactionType.COLLECTION,
                        tokenAddress = tokenAddress,
                        tokenName = mTokenName
                    )
                )
            )
        }

        viewPager.offscreenPageLimit = 2
        viewPager.adapter = TransactionRecordFragmentAdapter(supportFragmentManager, fragments)

        tabLayout.setupWithViewPager(viewPager)
        tabLayout.getTabAt(0)?.select()
    }

    private fun initView() {
        StatusBarUtil.setLightStatusBarMode(this.window, true)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val rectangle = Rect()
                val window = window
                window.decorView.getWindowVisibleDisplayFrame(rectangle)
                val statusBarHeight = rectangle.top

                val toolbarLayoutParams =
                    toolbar.layoutParams as CollapsingToolbarLayout.LayoutParams
                toolbarLayoutParams.height = toolbarLayoutParams.height + statusBarHeight
                toolbar.layoutParams = toolbarLayoutParams
                toolbar.setPadding(0, statusBarHeight, 0, 0)

                val tokenInfoLayoutParams =
                    clTokenInfo.layoutParams as CollapsingToolbarLayout.LayoutParams
                tokenInfoLayoutParams.topMargin = toolbar.height + statusBarHeight
                clTokenInfo.layoutParams = tokenInfoLayoutParams

                toolbar.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
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

    private fun initEvent() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                getTextView(tab)?.let {
                    it.setTypeface(Typeface.DEFAULT, Typeface.NORMAL)
                }
            }

            override fun onTabSelected(tab: TabLayout.Tab) {
                viewPager.setCurrentItem(tab.position, true)

                getTextView(tab)?.let {
                    it.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
                }
            }

            private fun getTextView(tab: TabLayout.Tab): TextView? {
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
        })

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

    inner class TransactionRecordFragmentAdapter(
        fragmentManager: FragmentManager,
        private val fragments: List<Pair<String, TransactionRecordFragment>>
    ) : FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getItem(position: Int): Fragment {
            return fragments[position].second
        }

        override fun getCount(): Int {
            return fragments.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return fragments[position].first
        }
    }
}