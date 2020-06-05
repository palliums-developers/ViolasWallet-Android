package com.violas.wallet.ui.tokenInfo

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.TokenManager
import com.violas.wallet.event.RefreshBalanceEvent
import com.violas.wallet.event.TokenBalanceUpdateEvent
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.database.entity.TokenDo
import com.violas.wallet.ui.collection.CollectionActivity
import com.violas.wallet.ui.transactionRecord.TransactionRecordFragment
import com.violas.wallet.ui.transactionRecord.TransactionType
import com.violas.wallet.ui.transfer.TransferActivity
import com.violas.wallet.utils.ClipboardUtils
import com.violas.wallet.utils.convertAmountToDisplayUnit
import kotlinx.android.synthetic.main.activity_token_info.*
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class TokenInfoActivity : BaseAppActivity() {
    companion object {
        private const val EXT_TOKEN_ID = "1"
        fun start(fragment: Fragment, tokenId: Long = 0, responseCode: Int) {
            val intent = Intent(fragment.activity, TokenInfoActivity::class.java)
                .apply {
                    putExtra(EXT_TOKEN_ID, tokenId)
                }
            fragment.startActivityForResult(intent, responseCode)
        }
    }

    private var refreshBalanceJob: Job? = null

    override fun getLayoutResId(): Int {
        return R.layout.activity_token_info
    }

    override fun getTitleStyle(): Int {
        return TITLE_STYLE_DARK_TITLE_PLIGHT_CONTENT
    }

    private var mTokenId: Long = -100
    private lateinit var mTokenDo: TokenDo
    private lateinit var mAccountDO: AccountDO

    private val mTokenManager by lazy {
        TokenManager()
    }

    private val mAccountManager by lazy {
        AccountManager()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        findFragment(TransactionRecordFragment::class.java)?.pop()

        launch(Dispatchers.IO) {
            val result = initData(savedInstanceState)
            withContext(Dispatchers.Main) {
                if (result) {
                    initView()
                } else {
                    showToast(getString(R.string.hint_unknown_error))
                    finish()
                }
            }
        }
    }

    override fun onDestroy() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        super.onDestroy()
    }

    private fun initData(savedInstanceState: Bundle?): Boolean {
        if (savedInstanceState != null) {
            mTokenId = savedInstanceState.getLong(EXT_TOKEN_ID, mTokenId)
        } else if (intent != null) {
            mTokenId = intent.getLongExtra(EXT_TOKEN_ID, mTokenId)
        }

        if (mTokenId == -100L) {
            return false
        }

        try {
            mTokenDo = mTokenManager.findTokenById(mTokenId) ?: return false
            mAccountDO = mAccountManager.getAccountById(mTokenDo.account_id)

            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun initView() {
        EventBus.getDefault().register(this)

        title = mTokenDo.name
        tvUnit.text = mTokenDo.name
        tvAddress.text = mAccountDO.address
        setAmount(mTokenDo.amount)

        ivCopy.setOnClickListener(this)
        btnTransfer.setOnClickListener(this)
        btnCollection.setOnClickListener(this)

        loadRootFragment(
            R.id.flFragmentContainer,
            TransactionRecordFragment.newInstance(
                mAccountDO.address,
                CoinTypes.Violas,
                TransactionType.ALL,
                mTokenDo.address
            )
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mTokenId != -100L) {
            outState.putLong(EXT_TOKEN_ID, mTokenId)
        }
    }

    override fun onViewClick(view: View) {
        when (view.id) {
            R.id.ivCopy -> {
                ClipboardUtils.copy(applicationContext, mAccountDO.address)
            }

            R.id.btnTransfer -> {
                launch(Dispatchers.IO) {
                    TransferActivity.start(
                        this@TokenInfoActivity,
                        mTokenDo.account_id,
                        "",
                        0,
                        true,
                        mTokenDo.id!!
                    )
                }
            }

            R.id.btnCollection -> {
                CollectionActivity.start(
                    this,
                    mAccountDO.id,
                    true,
                    mTokenDo.id!!
                )
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 100)
    fun onRefreshBalanceEvent(event: RefreshBalanceEvent) {
        try {
            // 币种信息页面和钱包首页都注册了该事件，只需要一处请求服务器刷新，在通知另一处更新UI
            EventBus.getDefault().cancelEventDelivery(event)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        launch(Dispatchers.IO) {
            if (event.delay >= 1) {
                delay(event.delay * 1000L)
            }

            withContext(Dispatchers.Main) {
                refreshTokenBalance()
            }
        }
    }

    private fun refreshTokenBalance() {
        refreshBalanceJob?.cancel()

        refreshBalanceJob = launch(Dispatchers.IO) {
            val tokenBalance = mTokenManager.getTokenBalance(
                mAccountDO.address,
                mTokenDo
            )
            if (tokenBalance != 0L) {
                withContext(Dispatchers.Main) {
                    setAmount(tokenBalance)

                    // 通知钱包首页更新当前币种余额UI
                    EventBus.getDefault().post(
                        TokenBalanceUpdateEvent(
                            mAccountDO.address,
//                            mTokenDo.tokenIdx,
                            0,
                            tokenBalance
                        )
                    )
                }
            }
        }
    }

    private fun setAmount(currentAccount: Long) {
        val convertAmountToDisplayUnit =
            convertAmountToDisplayUnit(currentAccount, CoinTypes.Violas)
        tvAmount.text = convertAmountToDisplayUnit.first
    }
}
