package com.violas.wallet.ui.main.wallet

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.palliums.base.BaseFragment
import com.palliums.utils.isFastMultiClick
import com.palliums.utils.isMainThread
import com.palliums.utils.start
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.biz.*
import com.violas.wallet.biz.bean.AssertToken
import com.violas.wallet.event.RefreshBalanceEvent
import com.violas.wallet.event.SwitchAccountEvent
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.ui.account.selection.AccountSelectionActivity
import com.violas.wallet.ui.account.walletmanager.WalletManagerActivity
import com.violas.wallet.ui.backup.BackupMnemonicFrom
import com.violas.wallet.ui.backup.BackupPromptActivity
import com.violas.wallet.ui.collection.CollectionActivity
import com.violas.wallet.ui.managerAssert.ManagerAssertActivity
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

    override fun onLazyInitView(savedInstanceState: Bundle?) {
        super.onLazyInitView(savedInstanceState)

        EventBus.getDefault().register(this)

        recyclerAssert.adapter = mAssertAdapter
        refreshAssert(activeRefresh = false, switchWallet = false)

        ivAddAssert.setOnClickListener(this)
        ivCopy.setOnClickListener(this)
        ivScan.setOnClickListener(this)
        ivWalletInfo.setOnClickListener(this)
        layoutWalletType.setOnClickListener(this)
        btnCollection.setOnClickListener(this)
        btnTransfer.setOnClickListener(this)
        vTransactionRecordLayout.setOnClickListener(this)

        if (mAccountManager.isFastIntoWallet()) {
            activity?.supportFragmentManager?.let {
                FastIntoWalletDialog()
                    .show(it, "fast")
            }
        } else {
            if (!mAccountManager.isIdentityMnemonicBackup()) {
                layoutBackupNow.visibility = View.VISIBLE
                btnConfirm.setOnClickListener {
                    layoutBackupNow?.visibility = View.GONE

                    activity?.supportFragmentManager?.let {
                        PasswordInputDialog()
                            .setConfirmListener { bytes, dialogFragment ->
                                launch(Dispatchers.IO) {
                                    activity?.applicationContext?.let { it1 ->
                                        try {
                                            val currentAccount =
                                                mAccountManager.getIdentityWalletMnemonic(
                                                    it1,
                                                    bytes
                                                )
                                                    ?.toMutableList()
                                            if (currentAccount != null) {
                                                dialogFragment.dismiss()
                                                BackupPromptActivity.start(
                                                    activity!!,
                                                    currentAccount as ArrayList<String>,
                                                    BackupMnemonicFrom.IDENTITY_WALLET
                                                )
                                            } else {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(
                                                        it1,
                                                        R.string.hint_password_error,
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            }
                            .show(it)
                    }
                }
            }
        }

        swipeRefreshLayout.setOnRefreshListener {
            refreshAssert(activeRefresh = true, switchWallet = false)
        }
    }

    override fun onViewClick(view: View) {
        when (view.id) {
            R.id.ivAddAssert -> {
                launch(Dispatchers.IO) {
                    val currentAccount = mAccountManager.currentAccount()
                    withContext(Dispatchers.Main) {
                        activity?.let { activity ->
                            ManagerAssertActivity.start(
                                this@WalletFragment,
                                currentAccount.id,
                                REQUEST_ADD_ASSERT
                            )
                        }
                    }
                }
            }

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

            R.id.layoutWalletType -> {
                activity?.let { it1 ->
                    Intent(
                        activity,
                        AccountSelectionActivity::class.java
                    ).start(it1)
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
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRefreshBalanceEvent(event: RefreshBalanceEvent) {
        launch(Dispatchers.IO) {
            delay(event.delay * 1000L)
            withContext(Dispatchers.Main) {
                refreshAssert(true, false)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSwitchAccountEvent(event: SwitchAccountEvent) {
        refreshAssert(activeRefresh = false, switchWallet = true)
    }

    private fun refreshAssert(activeRefresh: Boolean, switchWallet: Boolean) {
        launch(Dispatchers.IO) {
            val currentAccount = mAccountManager.currentAccount()
            val enableTokens = mTokenManger.loadEnableToken(currentAccount)

            // 刷新当前钱包的信息和当前平台的资产
            if (switchWallet) {
                mEnableTokens.clear()
                mEnableTokens.addAll(enableTokens)
                recyclerAssert.post {
                    mAssertAdapter.notifyDataSetChanged()
                    updateWalletInfo(currentAccount)
                }
            }

            if (currentAccount.coinNumber == CoinTypes.VToken.coinType()) {
                refreshViolasAssert(activeRefresh, currentAccount, enableTokens)
            } else {

                mAccountManager.refreshAccountAmount(currentAccount) {
                    if (activeRefresh) {
                        swipeRefreshLayout.isRefreshing = false
                    }

                    // 刷新当前钱包的信息
                    updateWalletInfo(it)

                    // 刷新当前平台的资产
                    if (mEnableTokens.size >= 1) {
                        mEnableTokens[0].amount = it.amount
                        mAssertAdapter.notifyItemChanged(0)
                    }
                }
            }
        }
    }

    private fun refreshViolasAssert(
        activeRefresh: Boolean,
        accountDO: AccountDO? = null,
        tokens: List<AssertToken>? = null
    ) {
        if (isMainThread()) {
            launch(Dispatchers.IO) {
                refreshViolasAssert(activeRefresh, accountDO, tokens)
            }
            return
        }

        val currentAccount = accountDO ?: mAccountManager.currentAccount()
        val enableTokens = tokens ?: mTokenManger.loadEnableToken(currentAccount)
        mTokenManger.refreshBalance(
            currentAccount.address,
            enableTokens
        ) { accountAmount, assertTokens ->
            if (activeRefresh) {
                swipeRefreshLayout.isRefreshing = false
            }

            // 刷新当前钱包的信息
            currentAccount.amount = accountAmount
            mAccountManager.updateAccount(currentAccount)
            updateWalletInfo(currentAccount)

            // 刷新当前平台的资产
            mEnableTokens.clear()
            mEnableTokens.addAll(assertTokens)
            recyclerAssert.post {
                mAssertAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun updateWalletInfo(currentAccount: AccountDO) {
        tvAddress.text = currentAccount.address

        val coinType = CoinTypes.parseCoinType(currentAccount.coinNumber)
        tvWalletType.text = "${coinType.coinName()} Wallet"
        tvUnit.text = coinType.coinUnit()

        val parseCoinType = CoinTypes.parseCoinType(currentAccount.coinNumber)
        val convertAmountToDisplayUnit =
            convertAmountToDisplayUnit(currentAccount.amount, parseCoinType)
        tvAmount.text = convertAmountToDisplayUnit.first

        if (coinType == CoinTypes.VToken) {
            ivAddAssert.visibility = View.VISIBLE
        } else {
            ivAddAssert.visibility = View.GONE
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_ADD_ASSERT -> {
                refreshViolasAssert(false)
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