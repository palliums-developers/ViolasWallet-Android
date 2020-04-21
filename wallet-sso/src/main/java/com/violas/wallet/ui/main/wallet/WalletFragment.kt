package com.violas.wallet.ui.main.wallet

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.annotation.WorkerThread
import androidx.recyclerview.widget.RecyclerView
import com.palliums.base.BaseFragment
import com.palliums.net.RequestException
import com.palliums.net.getErrorTipsMsg
import com.palliums.utils.isFastMultiClick
import com.quincysx.crypto.CoinTypes
import com.takusemba.spotlight.Spotlight
import com.takusemba.spotlight.Target
import com.takusemba.spotlight.shape.RoundedRectangle
import com.violas.wallet.R
import com.violas.wallet.biz.*
import com.violas.wallet.biz.bean.AssertToken
import com.violas.wallet.event.BackupMnemonicEvent
import com.violas.wallet.event.RefreshBalanceEvent
import com.violas.wallet.event.SwitchAccountEvent
import com.violas.wallet.event.TokenBalanceUpdateEvent
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.ui.account.walletmanager.WalletManagerActivity
import com.violas.wallet.ui.backup.BackupMnemonicFrom
import com.violas.wallet.ui.backup.BackupPromptActivity
import com.violas.wallet.ui.collection.CollectionActivity
import com.violas.wallet.ui.desktopManagement.LoginDesktopActivity
import com.violas.wallet.ui.identity.IdentityActivity
import com.violas.wallet.ui.record.TransactionRecordActivity
import com.violas.wallet.ui.scan.ScanActivity
import com.violas.wallet.ui.scan.ScanResultActivity
import com.violas.wallet.ui.tokenInfo.TokenInfoActivity
import com.violas.wallet.ui.transfer.TransferActivity
import com.violas.wallet.utils.ClipboardUtils
import com.violas.wallet.utils.convertAmountToDisplayUnit
import com.violas.wallet.widget.dialog.FastIntoWalletDialog
import com.violas.wallet.widget.dialog.PasswordInputDialog
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.android.synthetic.main.item_wallet_assert.view.*
import kotlinx.android.synthetic.main.view_backup_now_wallet.*
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class WalletFragment : BaseFragment() {
    companion object {
        private const val REQUEST_ADD_ASSERT = 0
        private const val REQUEST_SCAN_QR_CODE = 1
        private const val REQUEST_TOKEN_INFO = 2
    }

    private var refreshAssertJob: Job? = null

    private val mAccountManager by lazy {
        AccountManager()
    }

    private val mTokenManger by lazy {
        TokenManager()
    }

    private val mEnableTokens = mutableListOf<AssertToken>()
    private val mAssertAdapter by lazy {
        AssertAdapter(mEnableTokens) {
            TokenInfoActivity.start(this@WalletFragment, it.id, REQUEST_TOKEN_INFO)
        }
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_wallet
    }

    override fun onSupportVisible() {
        super.onSupportVisible()
        setStatusBarMode(false)
    }

    override fun onLazyInitView(savedInstanceState: Bundle?) {
        super.onLazyInitView(savedInstanceState)

        EventBus.getDefault().register(this)

        recyclerAssert.adapter = mAssertAdapter
        // 初始化钱包当作是切换钱包逻辑
        refreshAssert(true)

        ivCopy.setOnClickListener(this)
        ivScan.setOnClickListener(this)
        ivWalletInfo.setOnClickListener(this)
        btnCollection.setOnClickListener(this)
        btnTransfer.setOnClickListener(this)
        vTransactionRecordLayout.setOnClickListener(this)
        layoutWalletType.setOnClickListener(this)

        if (mAccountManager.isFastIntoWallet()) {
            FastIntoWalletDialog()
                .setConfirmCallback {
                    handleBackupMnemonicTips()
                }
                .show(requireActivity().supportFragmentManager, "fast")
        } else {
            handleBackupMnemonicTips()
        }

        launch(Dispatchers.IO) {
            val currentAccount = mAccountManager.currentAccount()
            val walletType = WalletType.parse(currentAccount.walletType)

            if (mAccountManager.isFastIntoCurrentTypeWallet(walletType)) {
                withContext(Dispatchers.Main) {
                    showHintSpotlight(walletType)
                }
            }
        }

        swipeRefreshLayout.setOnRefreshListener {
            refreshAssert(false)
        }
    }

    private fun showHintSpotlight(walletType: WalletType) {

        context?.let {
            val frameLayout = FrameLayout(it)

            val targetView = if (walletType == WalletType.SSO) {
                layoutInflater.inflate(
                    R.layout.view_help_change_governor,
                    frameLayout
                )
            } else {
                layoutInflater.inflate(
                    R.layout.view_help_change_sso,
                    frameLayout
                )
            }
            val target = Target.Builder()
                .setAnchor(layoutWalletType)
                .setShape(RoundedRectangle(100f, 400f, 10f, 50))
                .setOverlay(targetView)
                .build()

            val spotlight =
                Spotlight.Builder(requireActivity())
                    .setTargets(target)
                    .setBackgroundColor(R.color.spotlightBackground)
                    .setDuration(1000L)
                    .setAnimation(DecelerateInterpolator(2f))
                    .build()

            spotlight.start()
            val closeSpotlight = View.OnClickListener { spotlight.finish() }
            targetView.findViewById<View>(R.id.viewClose).setOnClickListener(closeSpotlight)
        }
    }

    private fun handleBackupMnemonicTips() {
        refreshAssertJob = launch(Dispatchers.IO) {
            val currentAccount = mAccountManager.currentAccount()
            val walletType = WalletType.parse(currentAccount.walletType)
            if (mAccountManager.isMnemonicBackup(walletType)) return@launch

            withContext(Dispatchers.Main) {
                layoutBackupNow.visibility = View.VISIBLE
                btnConfirm.setOnClickListener(this@WalletFragment)
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        cancelRefreshAssertJob()
    }

    private fun cancelRefreshAssertJob() {
        try {
            refreshAssertJob?.cancel()
        } catch (ignore: Exception) {
        }
    }

    override fun onViewClick(view: View) {
        when (view.id) {
            R.id.ivCopy -> {
                launch(Dispatchers.IO) {
                    val currentAccount = mAccountManager.currentAccount()
                    withContext(Dispatchers.Main) {
                        activity?.let { it1 -> ClipboardUtils.copy(it1, currentAccount.address) }
                    }
                }
            }

            R.id.ivScan -> {
                activity?.let { it1 ->
                    ScanActivity.start(this, REQUEST_SCAN_QR_CODE)
                }
            }

            R.id.ivWalletInfo -> {
                launch(Dispatchers.IO) {
                    val currentAccount = mAccountManager.currentAccount()
                    WalletManagerActivity.start(this@WalletFragment, currentAccount.id)
                }
            }

            R.id.btnCollection -> {
                launch(Dispatchers.IO) {
                    val currentAccount = mAccountManager.currentAccount()
                    activity?.let { it1 -> CollectionActivity.start(it1, currentAccount.id) }
                }
            }

            R.id.btnTransfer -> {
                launch(Dispatchers.IO) {
                    activity?.let { it1 ->
                        TransferActivity.start(
                            it1,
                            mAccountManager.currentAccount().id
                        )
                    }
                }
            }

            R.id.vTransactionRecordLayout -> {
                launch(Dispatchers.IO) {
                    val currentAccount = mAccountManager.currentAccount()
                    activity?.let { TransactionRecordActivity.start(it, currentAccount.id) }
                }
            }

            R.id.btnConfirm -> {
                layoutBackupNow.visibility = View.GONE
                PasswordInputDialog()
                    .setConfirmListener { bytes, dialogFragment ->
                        launch(Dispatchers.IO) {
                            activity?.applicationContext?.let {
                                try {
                                    val currentAccount = mAccountManager.currentAccount()
                                    val mnemonic =
                                        mAccountManager.getAccountMnemonic(
                                            it, bytes, currentAccount
                                        )
                                    val backupMnemonicFrom =
                                        if (currentAccount.walletType == WalletType.Governor.type)
                                            BackupMnemonicFrom.BACKUP_GOVERNOR_WALLET
                                        else
                                            BackupMnemonicFrom.BACKUP_SSO_WALLET
                                    withContext(Dispatchers.Main) {
                                        if (!mnemonic.isNullOrEmpty()) {
                                            dialogFragment.dismiss()
                                            BackupPromptActivity.start(
                                                requireActivity(),
                                                mnemonic,
                                                backupMnemonicFrom
                                            )
                                        } else {
                                            showToast(R.string.hint_password_error)
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                    .show(requireActivity().supportFragmentManager)
            }
            R.id.layoutWalletType -> {
                showProgress()
                launch(Dispatchers.IO) {
                    val currentAccount = mAccountManager.currentAccount()
                    when (currentAccount.walletType) {
                        WalletType.SSO.type -> {
                            handlerChangeWallet(WalletType.Governor)
                        }
                        WalletType.Governor.type -> {
                            handlerChangeWallet(WalletType.SSO)
                        }
                    }
                    dismissProgress()
                }
            }
        }
    }

    @WorkerThread
    private fun handlerChangeWallet(walletType: WalletType) {
        val account =
            mAccountManager.getIdentityByWalletType(walletType.type)
        if (account == null) {
            activity?.let {
                IdentityActivity.start(it, walletType)
                finishActivity()
            }
        } else {
            mAccountManager.switchCurrentAccount(account.id)
            EventBus.getDefault().post(SwitchAccountEvent())
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRefreshBalanceEvent(event: RefreshBalanceEvent) {
        launch(Dispatchers.IO) {
            if (event.delay >= 1) {
                delay(event.delay * 1000L)
            }

            withContext(Dispatchers.Main) {
                refreshAssert(false)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSwitchAccountEvent(event: SwitchAccountEvent) {
        refreshAssert(true)
        handleBackupMnemonicTips()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBackupMnemonicEvent(event: BackupMnemonicEvent) {
        layoutBackupNow.visibility = View.GONE
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTokenBalanceUpdateEvent(event: TokenBalanceUpdateEvent) {
        launch(Dispatchers.IO) {
            val currentAccount = mAccountManager.currentAccount()
            if (currentAccount.address != event.accountAddress) {
                return@launch
            }

            mEnableTokens.forEachIndexed { index, assertToken ->
                if (assertToken.tokenIdx == event.tokenIdx) {
                    withContext(Dispatchers.Main) {
                        assertToken.amount = event.tokenBalance
                        mAssertAdapter.notifyItemChanged(index)
                    }
                    return@launch
                }
            }
        }
    }

    private fun refreshAssert(switchWallet: Boolean) {
        cancelRefreshAssertJob()

        refreshAssertJob = launch(Dispatchers.IO) {
            val currentAccount = mAccountManager.currentAccount()
            val enableTokens = mTokenManger.loadEnableToken(currentAccount)

            withContext(Dispatchers.Main) {
                when (currentAccount.walletType) {
                    WalletType.SSO.type -> {
                        tvWalletType.setText(R.string.title_sso_wallet)
                    }
                    WalletType.Governor.type -> {
                        tvWalletType.setText(R.string.title_governor_wallet)
                    }
                }

                // 刷新当前钱包的信息和当前平台的资产
                if (switchWallet) {
                    mEnableTokens.clear()
                    mEnableTokens.addAll(enableTokens)
                    mAssertAdapter.notifyDataSetChanged()
                    updateWalletInfo(currentAccount)
                }
            }

            if (currentAccount.coinNumber == CoinTypes.Violas.coinType()) {
                refreshViolasAssert(currentAccount, enableTokens)
            } else {
                val result = try {
                    mAccountManager.refreshAccount(currentAccount)
                    true
                } catch (e: Exception) {
                    if (!RequestException.isActiveCancellation(e)) {
                        showToast(e.getErrorTipsMsg())
                    }
                    false
                }

                withContext(Dispatchers.Main) {
                    if (swipeRefreshLayout.isRefreshing) {
                        swipeRefreshLayout.isRefreshing = false
                    }

                    if (!result) return@withContext

                    // 刷新当前钱包的信息
                    updateWalletInfo(currentAccount)

                    // 刷新当前平台的资产
                    if (mEnableTokens.size >= 1) {
                        mEnableTokens[0].amount = currentAccount.amount
                        mAssertAdapter.notifyItemChanged(0)
                    }
                }
            }
        }
    }

    private suspend fun refreshViolasAssert(
        accountDO: AccountDO? = null,
        tokens: List<AssertToken>? = null
    ) {
        val currentAccount = accountDO ?: mAccountManager.currentAccount()
        val enableTokens = tokens ?: mTokenManger.loadEnableToken(currentAccount)
        val balanceInfos =
            try {
                mTokenManger.refreshBalance(currentAccount.address, enableTokens)
            } catch (e: Exception) {
                if (!RequestException.isActiveCancellation(e)) {
                    showToast(e.getErrorTipsMsg())
                }
                null
            }

        // 更新钱包余额
        balanceInfos?.first?.let {
            currentAccount.amount = it
            mAccountManager.updateAccount(currentAccount)
        }

        withContext(Dispatchers.Main) {
            if (swipeRefreshLayout.isRefreshing) {
                swipeRefreshLayout.isRefreshing = false
            }

            if (balanceInfos == null) return@withContext

            // 刷新当前钱包的信息
            updateWalletInfo(currentAccount)

            // 刷新当前平台的资产
            mEnableTokens.clear()
            mEnableTokens.addAll(balanceInfos.second)
            mAssertAdapter.notifyDataSetChanged()
        }
    }

    private fun updateWalletInfo(currentAccount: AccountDO) {
        tvAddress.text = currentAccount.address

        val coinType = CoinTypes.parseCoinType(currentAccount.coinNumber)
        tvUnit.text = coinType.coinUnit()

        val parseCoinType = CoinTypes.parseCoinType(currentAccount.coinNumber)
        val convertAmountToDisplayUnit =
            convertAmountToDisplayUnit(currentAccount.amount, parseCoinType)
        tvAmount.text = convertAmountToDisplayUnit.first
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_ADD_ASSERT -> {
                refreshAssertJob = launch(Dispatchers.IO) {
                    refreshViolasAssert()
                }
            }
            REQUEST_SCAN_QR_CODE -> {
                data?.getStringExtra(ScanActivity.RESULT_QR_CODE_DATA)?.let { msg ->
                    decodeScanQRCode(msg) { scanType, scanBean ->
                        when (scanType) {
                            ScanCodeType.Address -> {
                                scanBean as ScanTranBean
                                activity?.let {
                                    TransferActivity.start(
                                        it,
                                        scanBean.coinType,
                                        scanBean.address,
                                        scanBean.amount,
                                        scanBean.tokenName
                                    )
                                }
                            }
                            ScanCodeType.Text -> {
                                activity?.let {
                                    ScanResultActivity.start(it, scanBean.msg)
                                }
                            }
                            ScanCodeType.LoginDesktop -> {
                                activity?.let {
                                    LoginDesktopActivity.start(it, scanBean.msg)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        cancel()
        super.onDestroy()
    }
}

class AssertAdapter(
    val data: List<AssertToken>,
    val call: (AssertToken) -> Unit
) :
    RecyclerView.Adapter<AssertAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_wallet_assert,
                parent,
                false
            )
        )
    }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemData = data[position]
        holder.itemView.name.text = itemData.name

        val parseCoinType = CoinTypes.parseCoinType(itemData.coinType)
        val convertAmountToDisplayUnit =
            convertAmountToDisplayUnit(itemData.amount, parseCoinType)
        holder.itemView.amount.text = convertAmountToDisplayUnit.first
        holder.itemView.setOnClickListener {
            if (!isFastMultiClick(it)) {
                if (itemData.isToken) {
                    call.invoke(itemData)
                }
            }
        }
    }

    class ViewHolder(item: View) : RecyclerView.ViewHolder(item)
}