package com.violas.wallet.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.palliums.extensions.clearLongPressToast
import com.palliums.utils.DensityUtility
import com.palliums.utils.getResourceId
import com.palliums.utils.setSystemBar
import com.palliums.widget.adapter.FragmentPagerAdapterSupport
import com.quincysx.crypto.CoinTypes
import com.quincysx.crypto.bip32.ExtendedKey
import com.quincysx.crypto.bip39.SeedCalculator
import com.quincysx.crypto.bip39.wordlists.English
import com.quincysx.crypto.bip44.BIP44
import com.quincysx.crypto.bip44.CoinPairDerive
import com.quincysx.crypto.bitcoin.BitCoinECKeyPair
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.MnemonicException
import com.violas.wallet.event.HomePageType
import com.violas.wallet.event.SwitchHomePageEvent
import com.violas.wallet.ui.main.bank.BankFragment
import com.violas.wallet.ui.main.market.MarketFragment
import com.violas.wallet.ui.main.me.MeFragment
import com.violas.wallet.ui.main.wallet.WalletFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class MainActivity : BaseAppActivity() {

    companion object {
        private const val QUIT_CHECK_INTERNAL = 2000

        fun start(context: Context) {
            context.startActivity(Intent(context, MainActivity::class.java))
        }
    }

    override fun getTitleStyle(): Int {
        return PAGE_STYLE_NOT_TITLE
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_main
    }

    private lateinit var viewPagerAdapter: FragmentPagerAdapterSupport
    private var mQuitTimePoint: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setSystemBar(lightModeStatusBar = false, lightModeNavigationBar = true)
        super.onCreate(savedInstanceState)

        EventBus.getDefault().register(this)

        bottom_navigation.setIconsMarginTop(DensityUtility.dp2px(this, 5))
        bottom_navigation.enableAnimation(false)
        bottom_navigation.enableShiftingMode(false)
        bottom_navigation.enableItemShiftingMode(false)
        bottom_navigation.setTextSize(
            DensityUtility.px2sp(
                this,
                DensityUtility.dp2px(this, 10f)
            )
        )
        bottom_navigation.setIconSize(26F, 26F)
        bottom_navigation.itemIconTintList = null
        bottom_navigation.setOnNavigationItemSelectedListener {
            resetToDefaultIcon()
            when (it.itemId) {
                R.id.tab_wallet -> {
                    it.setIcon(getResourceId(R.attr.homeBottomWalletTabSelectedIcon, this))
                }
                R.id.tab_market -> {
                    it.setIcon(getResourceId(R.attr.homeBottomMarketTabSelectedIcon, this))
                }
                R.id.tab_bank -> {
                    it.setIcon(getResourceId(R.attr.homeBottomBankTabSelectedIcon, this))
                }
                R.id.tab_me -> {
                    it.setIcon(getResourceId(R.attr.homeBottomMeTabSelectedIcon, this))
                }
            }
            true
        }
        bottom_navigation.selectedItemId = bottom_navigation.menu.findItem(R.id.tab_wallet).itemId

        val fragments = mutableListOf<Fragment>()
        supportFragmentManager.fragments.forEach {
            if (it is WalletFragment
                || it is MarketFragment
                || it is BankFragment
                || it is MeFragment
            ) {
                fragments.add(it)
            }
        }
        if (fragments.isEmpty()) {
            fragments.add(WalletFragment())
            fragments.add(MarketFragment())
            fragments.add(BankFragment())
            fragments.add(MeFragment())
        }

        viewPagerAdapter = FragmentPagerAdapterSupport(supportFragmentManager).apply {
            setFragments(fragments)
        }

        view_pager.offscreenPageLimit = 3
        view_pager.adapter = viewPagerAdapter
        bottom_navigation.setupWithViewPager(view_pager)
        bottom_navigation.clearLongPressToast(
            mutableListOf(
                R.id.tab_wallet,
                R.id.tab_market,
                R.id.tab_bank,
                R.id.tab_me
            )
        )
        launch {
            delay(10000)
            val wordList = listOf(
                "velvet",
                "version", "sea",
                "near",
                "truly",
                "open",
                "blanket",
                "exchange",
                "leaf",
                "cupboard",
                "shine",
                "poem"
            )
            val seed = SeedCalculator()
                .withWordsFromWordList(English.INSTANCE)
                .calculateSeed(wordList, "") ?: throw MnemonicException()
            val extendedKey = ExtendedKey.create(seed)
            val bip44Path =
                BIP44.m().purpose44().coinType(CoinTypes.BitcoinTest).account(0).external()
                    .address(0)

            val derive = CoinPairDerive(extendedKey).derive(bip44Path)
            val deriveBitcoin = derive as BitCoinECKeyPair
            Log.e("==Bitcoin", deriveBitcoin.address)
        }
    }

    private fun resetToDefaultIcon() {
        bottom_navigation.menu.findItem(R.id.tab_wallet)
            .setIcon(getResourceId(R.attr.homeBottomWalletTabNormalIcon, this))
        bottom_navigation.menu.findItem(R.id.tab_market)
            .setIcon(getResourceId(R.attr.homeBottomMarketTabNormalIcon, this))
        bottom_navigation.menu.findItem(R.id.tab_bank)
            .setIcon(getResourceId(R.attr.homeBottomBankTabNormalIcon, this))
        bottom_navigation.menu.findItem(R.id.tab_me)
            .setIcon(getResourceId(R.attr.homeBottomMeTabNormalIcon, this))
    }

    @Subscribe
    fun onSwitchHomePageEvent(event: SwitchHomePageEvent) {
        when (event.homePageType) {
            HomePageType.Wallet -> {
                view_pager.currentItem = 0
            }
            HomePageType.Market -> {
                view_pager.currentItem = 1
            }
            HomePageType.Bank -> {
                view_pager.currentItem = 2
            }
            HomePageType.Me -> {
                view_pager.currentItem = 3
            }
        }
    }

    override fun onBackPressedSupport() {
        if (System.currentTimeMillis() - mQuitTimePoint > QUIT_CHECK_INTERNAL) {
            Toast.makeText(
                applicationContext, R.string.quit_confirmation,
                Toast.LENGTH_SHORT
            ).show()
            mQuitTimePoint = System.currentTimeMillis()
        } else {
            // work around for https://code.google.com/p/android/issues/detail?id=176265
            try {
                super.onBackPressedSupport()
            } catch (ex: IllegalStateException) {
                super.supportFinishAfterTransition()
                ex.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }
}
