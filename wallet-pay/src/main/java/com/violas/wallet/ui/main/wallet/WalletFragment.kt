package com.violas.wallet.ui.main.wallet

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.biometric.BiometricManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.palliums.base.BaseFragment
import com.palliums.biometric.BiometricCompat
import com.palliums.extensions.getShowErrorMessage
import com.palliums.extensions.isActiveCancellation
import com.palliums.extensions.show
import com.palliums.utils.isFastMultiClick
import com.palliums.utils.start
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.biz.*
import com.violas.wallet.biz.bean.AssertToken
import com.violas.wallet.event.BackupIdentityMnemonicEvent
import com.violas.wallet.event.RefreshBalanceEvent
import com.violas.wallet.event.SwitchAccountEvent
import com.violas.wallet.event.TokenBalanceUpdateEvent
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.ui.account.selection.AccountSelectionActivity
import com.violas.wallet.ui.account.walletmanager.WalletManagerActivity
import com.violas.wallet.ui.backup.BackupMnemonicFrom
import com.violas.wallet.ui.backup.BackupPromptActivity
import com.violas.wallet.ui.biometric.OpenBiometricsPromptDialog
import com.violas.wallet.ui.collection.CollectionActivity
import com.violas.wallet.ui.managerAssert.ManagerAssertActivity
import com.violas.wallet.ui.outsideExchange.OutsideExchangeActivity
import com.violas.wallet.ui.record.TransactionRecordActivity
import com.violas.wallet.ui.scan.ScanActivity
import com.violas.wallet.ui.scan.ScanResultActivity
import com.violas.wallet.ui.tokenInfo.TokenInfoActivity
import com.violas.wallet.ui.transfer.TransferActivity
import com.violas.wallet.ui.webManagement.LoginWebActivity
import com.violas.wallet.utils.ClipboardUtils
import com.violas.wallet.utils.authenticateAccount
import com.violas.wallet.utils.convertAmountToDisplayUnit
import com.violas.wallet.viewModel.WalletAppViewModel
import com.violas.wallet.viewModel.bean.AssetsCoinVo
import com.violas.wallet.viewModel.bean.AssetsVo
import com.violas.wallet.walletconnect.WalletConnect
import com.violas.wallet.widget.dialog.FastIntoWalletDialog
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

    private val mWalletAppViewModel by lazy {
        context?.let { WalletAppViewModel.getViewModelInstance(it) }
    }

    private var refreshAssertJob: Job? = null

    private val mAccountManager by lazy {
        AccountManager()
    }


    private val mEnableTokens = mutableListOf<AssertToken>()
    private val mAssertAdapter by lazy {
        AssertAdapter {
//            TokenInfoActivity.start(this@WalletFragment, it.id, REQUEST_TOKEN_INFO)
        }
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_wallet
    }

    override fun onLazyInitView(savedInstanceState: Bundle?) {
        super.onLazyInitView(savedInstanceState)

        EventBus.getDefault().register(this)

        recyclerAssert.adapter = mAssertAdapter

        mWalletAppViewModel?.mAssetsListLiveData?.observe(this, Observer {
            mAssertAdapter.submitList(it)
        })

        // 初始化钱包当作是切换钱包逻辑
//        refreshAssert(true)

        ivAddAssert.setOnClickListener(this)
//        ivCopy.setOnClickListener(this)
        ivScan.setOnClickListener(this)
//        ivWalletInfo.setOnClickListener(this)
//        layoutWalletType.setOnClickListener(this)
//        btnCollection.setOnClickListener(this)
//        btnTransfer.setOnClickListener(this)
//        vCrossChainExchangeLayout.setOnClickListener(this)
//        vTransactionRecordLayout.setOnClickListener(this)

        if (mAccountManager.isFastIntoWallet()) {
            FastIntoWalletDialog()
                .setConfirmCallback {
                    if (!mAccountManager.isIdentityMnemonicBackup()) {
                        layoutBackupNow.visibility = View.VISIBLE
                        btnConfirm.setOnClickListener(this)

                        handleOpenBiometricsPrompt()
                    }
                }
                .show(requireActivity().supportFragmentManager, "fast")
        } else if (!mAccountManager.isIdentityMnemonicBackup()) {
            layoutBackupNow.visibility = View.VISIBLE
            btnConfirm.setOnClickListener(this)

            handleOpenBiometricsPrompt()
        }

//        swipeRefreshLayout.setOnRefreshListener {
//            refreshAssert(false)
//        }
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

//            R.id.ivWalletInfo -> {
//                launch(Dispatchers.IO) {
//                    val currentAccount = mAccountManager.currentAccount()
//                    WalletManagerActivity.start(this@WalletFragment, currentAccount.id)
//                }
//            }

//            R.id.layoutWalletType -> {
//                activity?.let { it1 ->
//                    Intent(
//                        activity,
//                        AccountSelectionActivity::class.java
//                    ).start(it1)
//                }
//            }

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

//            R.id.vCrossChainExchangeLayout -> {
//                launch(Dispatchers.IO) {
//                    val currentAccount = mAccountManager.currentAccount()
//                    activity?.let { OutsideExchangeActivity.start(it, currentAccount.id) }
//                }
//            }
//
//            R.id.vTransactionRecordLayout -> {
//                launch(Dispatchers.IO) {
//                    val currentAccount = mAccountManager.currentAccount()
//                    activity?.let { TransactionRecordActivity.start(it, currentAccount.id) }
//                }
//            }

            R.id.btnConfirm -> {
                layoutBackupNow.visibility = View.GONE
                launch(Dispatchers.Main) {
                    val identityAccount = withContext(Dispatchers.IO) {
                        mAccountManager.getIdentityAccount()
                    }

                    authenticateAccount(
                        identityAccount,
                        mAccountManager,
                        dismissLoadingWhenDecryptEnd = true,
                        mnemonicCallback = {
                            BackupPromptActivity.start(
                                requireActivity(),
                                it,
                                BackupMnemonicFrom.BACKUP_IDENTITY_WALLET
                            )
                        }
                    )
                }
            }
        }
    }

    private fun handleOpenBiometricsPrompt() {
        if (mAccountManager.isOpenBiometricsPrompted()) {
            return
        }

        val biometricCompat = BiometricCompat.Builder(requireContext()).build()
        if (biometricCompat.canAuthenticate() != BiometricManager.BIOMETRIC_SUCCESS) {
            return
        }

        mAccountManager.setOpenBiometricsPrompted()
        OpenBiometricsPromptDialog()
            .setCallback {
                launch(Dispatchers.IO) {
                    val currentAccount = mAccountManager.currentAccount()
                    WalletManagerActivity.start(this@WalletFragment, currentAccount.id)
                }
            }
            .show(childFragmentManager)
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBackupIdentityMnemonicEvent(event: BackupIdentityMnemonicEvent) {
        layoutBackupNow.visibility = View.GONE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_ADD_ASSERT -> {
                refreshAssertJob = launch(Dispatchers.IO) {
//                    refreshViolasAssert()
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

                            ScanCodeType.WalletConnectSocket -> {
                                context?.let {
                                    WalletConnect.getInstance(it.applicationContext).connect(
                                        msg
                                    )
                                }
                            }

                            ScanCodeType.Login -> {
                                scanBean as ScanLoginBean
                                activity?.let {
                                    LoginWebActivity.start(it, scanBean)
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
    val call: (AssetsVo) -> Unit
) : ListAdapter<AssetsVo, AssertAdapter.ViewHolder>(object : DiffUtil.ItemCallback<AssetsVo>() {
    override fun areItemsTheSame(oldItem: AssetsVo, newItem: AssetsVo): Boolean {
        return if (oldItem is AssetsCoinVo && newItem is AssetsCoinVo) {
            oldItem.getId() == newItem.getId()
        } else {
            oldItem.getAccountId() == newItem.getAccountId() && oldItem.getId() == newItem.getId()
        }
    }

    override fun areContentsTheSame(oldItem: AssetsVo, newItem: AssetsVo): Boolean {
        return if (oldItem is AssetsCoinVo && newItem is AssetsCoinVo) {
            oldItem.getId() == newItem.getId()
        } else {
            oldItem.getAccountId() == newItem.getAccountId() && oldItem.getId() == newItem.getId()
        }
    }
}) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_wallet_assert,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemData = getItem(position)
        holder.itemView.tvName.text = itemData.getAssetsName()

//        val parseCoinType = CoinTypes.parseCoinType(itemData.coinType)
//        val convertAmountToDisplayUnit =
//            convertAmountToDisplayUnit(itemData.amount, parseCoinType)
        holder.itemView.tvAmount.text = itemData.getAmountWithUnit().amount
    }

    class ViewHolder(item: View) : RecyclerView.ViewHolder(item)
}