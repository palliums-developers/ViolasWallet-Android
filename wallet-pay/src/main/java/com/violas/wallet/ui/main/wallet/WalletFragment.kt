package com.violas.wallet.ui.main.wallet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.biometric.BiometricManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.palliums.base.BaseFragment
import com.palliums.biometric.BiometricCompat
import com.palliums.extensions.expandTouchArea
import com.palliums.extensions.show
import com.palliums.utils.DensityUtility
import com.palliums.utils.StatusBarUtil
import com.palliums.utils.getResourceId
import com.violas.wallet.R
import com.violas.wallet.biz.*
import com.violas.wallet.biz.command.CommandActuator
import com.violas.wallet.biz.command.RefreshAssetsAllListCommand
import com.violas.wallet.event.BackupIdentityMnemonicEvent
import com.violas.wallet.repository.database.entity.AccountType
import com.violas.wallet.ui.account.walletmanager.WalletManagerActivity
import com.violas.wallet.ui.backup.BackupMnemonicFrom
import com.violas.wallet.ui.backup.BackupPromptActivity
import com.violas.wallet.ui.biometric.OpenBiometricsPromptDialog
import com.violas.wallet.ui.collection.MultiCollectionActivity
import com.violas.wallet.ui.identity.createIdentity.CreateIdentityActivity
import com.violas.wallet.ui.identity.importIdentity.ImportIdentityActivity
import com.violas.wallet.ui.managerAssert.ManagerAssertActivity
import com.violas.wallet.ui.mapping.MappingActivity
import com.violas.wallet.ui.scan.ScanActivity
import com.violas.wallet.ui.scan.ScanResultActivity
import com.violas.wallet.ui.tokenDetails.TokenDetailsActivity
import com.violas.wallet.ui.transfer.MultiTransferActivity
import com.violas.wallet.ui.transfer.TransferActivity
import com.violas.wallet.ui.walletconnect.WalletConnectAuthorizationActivity
import com.violas.wallet.ui.walletconnect.WalletConnectManagerActivity
import com.violas.wallet.ui.webManagement.LoginWebActivity
import com.violas.wallet.utils.authenticateAccount
import com.violas.wallet.utils.loadCircleImage
import com.violas.wallet.viewModel.WalletAppViewModel
import com.violas.wallet.viewModel.WalletConnectViewModel
import com.violas.wallet.viewModel.bean.AssetsCoinVo
import com.violas.wallet.viewModel.bean.AssetsVo
import com.violas.wallet.viewModel.bean.HiddenTokenVo
import com.violas.wallet.walletconnect.WalletConnectStatus
import com.violas.wallet.widget.dialog.FastIntoWalletDialog
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.android.synthetic.main.item_wallet_assert.view.*
import kotlinx.android.synthetic.main.view_backup_now_wallet.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * 钱包首页
 */
class WalletFragment : BaseFragment() {
    companion object {
        private const val REQUEST_ADD_ASSERT = 0
        private const val REQUEST_SCAN_QR_CODE = 1
        private const val REQUEST_TOKEN_INFO = 2
    }

    private val mWalletAppViewModel by lazy {
        context?.let { WalletAppViewModel.getViewModelInstance(it) }
    }
    private val mWalletConnectViewModel by lazy {
        context?.let { WalletConnectViewModel.getViewModelInstance(it) }
    }
    private val mWalletViewModel by lazy {
        ViewModelProvider(this).get(WalletViewModel::class.java)
    }

    private val mAccountManager by lazy {
        AccountManager()
    }


    private val mAssertAdapter by lazy {
        AssertAdapter {
            activity?.let { it1 ->
                TokenDetailsActivity.start(it1, it)
            }
        }
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_wallet
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        EventBus.getDefault().register(this)
        actionBar.post { adapterViewHeight() }
        recyclerAssert.adapter = mAssertAdapter

        mWalletAppViewModel?.mDataRefreshingLiveData?.observe(viewLifecycleOwner, Observer {
            if (!it) {
                swipeRefreshLayout.finishRefresh()
            }
        })
        Log.d("==assets==", "WalletFragment Subscribe to AssetsList")
        mWalletAppViewModel?.mAssetsListLiveData?.observe(viewLifecycleOwner, Observer {
            Log.d("==assets==", "WalletFragment AssetsList Observer Update")
            val filter =
                it.filter { asset ->
                    when (asset) {
                        is HiddenTokenVo -> {
                            false
                        }
                        !is AssetsCoinVo -> {
                            true
                        }
                        else -> {
                            asset.accountType != AccountType.NoDollars
                        }
                    }
                }
            mAssertAdapter.submitList(filter)
            mWalletViewModel.calculateFiat(filter)
        })
        mWalletAppViewModel?.mExistsAccountLiveData?.observe(viewLifecycleOwner, Observer {
            if (it) {
                viewAssetsGroup.visibility = View.VISIBLE
                viewAddAccount.visibility = View.GONE
            } else {
                viewAssetsGroup.visibility = View.GONE
                viewAddAccount.visibility = View.VISIBLE
            }
            handleBackupMnemonicWarn(it)
            handleDialogShow(it)
        })
        mWalletViewModel.mTotalFiatBalanceStrLiveData.observe(viewLifecycleOwner, Observer {
            tvAmount.text = it
        })
        mWalletViewModel.mHiddenTotalFiatBalanceLiveData.observe(viewLifecycleOwner, Observer {
            mAssertAdapter.assetsHidden(it)
            if (it) {
                ivTotalHidden.setImageResource(
                    getResourceId(
                        R.attr.iconHidePrimary,
                        requireContext()
                    )
                )
            } else {
                ivTotalHidden.setImageResource(
                    getResourceId(
                        R.attr.iconShowPrimary,
                        requireContext()
                    )
                )
            }
        })

        ivTotalHidden.setOnClickListener {
            mWalletViewModel.taggerTotalDisplay()
        }
        ivTotalHidden.expandTouchArea(28)

        mWalletConnectViewModel?.mWalletConnectStatusLiveData?.observe(
            viewLifecycleOwner,
            Observer { status ->
                when (status) {
                    WalletConnectStatus.None -> {
                        viewWalletConnect.visibility = View.INVISIBLE
                    }
                    WalletConnectStatus.Login -> {
                        viewWalletConnect.visibility = View.VISIBLE
                    }
                }
            })
        viewWalletConnect.setOnClickListener {
            activity?.let { it1 -> WalletConnectManagerActivity.startActivity(it1) }
        }

        ivAddAssert.setOnClickListener(this)
        ivScan.setOnClickListener(this)
        viewCreateAccount.setOnClickListener(this)
        viewImportAccount.setOnClickListener(this)
        llTransferGroup.setOnClickListener(this)
        llCollectionGroup.setOnClickListener(this)
        llMappingGroup.setOnClickListener(this)

        swipeRefreshLayout.setEnableOverScrollDrag(true)
        swipeRefreshLayout.setOnRefreshListener {
            CommandActuator.post(RefreshAssetsAllListCommand())
        }
        swipeRefreshLayout.autoRefresh()
    }

    private fun adapterViewHeight() {
        val statusBarHeight = StatusBarUtil.getStatusBarHeight()
        val topViewHeight = clTopGroup.measuredHeight
        val bottomViewTopMargin =
            topViewHeight + statusBarHeight - DensityUtility.dp2px(requireContext(), 16)

        clTopGroup.setPadding(
            clTopGroup.paddingLeft,
            statusBarHeight,
            clTopGroup.paddingRight,
            clTopGroup.paddingBottom
        )
        viewAssetsGroup.layoutParams =
            (viewAssetsGroup.layoutParams as ConstraintLayout.LayoutParams).apply {
                topMargin = bottomViewTopMargin
            }
        viewAddAccount.layoutParams =
            (viewAddAccount.layoutParams as ConstraintLayout.LayoutParams).apply {
                topMargin = bottomViewTopMargin
            }
    }

    private fun handleBackupMnemonicWarn(existsAccount: Boolean) {
        if (!existsAccount) {
            layoutBackupNow.visibility = View.GONE
            return
        }

        if (!mAccountManager.isIdentityMnemonicBackup()) {
            btnConfirm.setOnClickListener(this)
            layoutBackupNow.visibility = View.VISIBLE
        }
    }

    private fun handleDialogShow(existsAccount: Boolean) {
        if (mAccountManager.isFastIntoWallet()) {
            FastIntoWalletDialog()
                .setConfirmCallback {
                    handleOpenBiometricsPrompt(existsAccount)
                }
                .show(childFragmentManager)
        } else {
            handleOpenBiometricsPrompt(existsAccount)
        }
    }

    private fun handleOpenBiometricsPrompt(existsAccount: Boolean) {
        if (!existsAccount || mAccountManager.isOpenBiometricsPrompted()) return

        val biometricCompat = BiometricCompat.Builder(requireContext()).build()
        if (biometricCompat.canAuthenticate() != BiometricManager.BIOMETRIC_SUCCESS) return

        mAccountManager.setOpenBiometricsPrompted()
        OpenBiometricsPromptDialog()
            .setCallback {
                WalletManagerActivity.start(requireContext())
            }
            .show(childFragmentManager)
    }

    override fun onViewClick(view: View) {
        when (view.id) {
            R.id.ivAddAssert -> {
                ManagerAssertActivity.start(
                    this@WalletFragment,
                    REQUEST_ADD_ASSERT
                )
            }
            R.id.ivScan -> {
                activity?.let { it1 ->
                    if (mWalletAppViewModel?.isExistsAccount() == true) {
                        ScanActivity.start(this, REQUEST_SCAN_QR_CODE)
                    } else {
                        showToast(R.string.tips_create_or_import_wallet)
                    }
                }
            }

            R.id.llTransferGroup -> {
                activity?.let {
                    if (mWalletAppViewModel?.isExistsAccount() == true) {
                        context?.let { it1 -> MultiTransferActivity.start(it1) }
                    } else {
                        showToast(R.string.tips_create_or_import_wallet)
                    }
                }
            }

            R.id.llCollectionGroup -> {
                activity?.let {
                    if (mWalletAppViewModel?.isExistsAccount() == true) {
                        context?.let { it1 -> MultiCollectionActivity.start(it1) }
                    } else {
                        showToast(R.string.tips_create_or_import_wallet)
                    }
                }
            }

            R.id.llMappingGroup -> {
                activity?.let {
                    if (mWalletAppViewModel?.isExistsAccount() == true) {
                        MappingActivity.start(it)
                    } else {
                        showToast(R.string.tips_create_or_import_wallet)
                    }
                }
            }

            R.id.viewCreateAccount -> {
                activity?.let { CreateIdentityActivity.start(it) }
            }

            R.id.viewImportAccount -> {
                activity?.let { ImportIdentityActivity.start(it) }
            }
            R.id.btnConfirm -> {
                backupWallet()
            }
        }
    }

    private fun backupWallet() {
        launch {
            try {
                val accountDO = mAccountManager.getDefaultAccount()
                authenticateAccount(
                    accountDO,
                    mAccountManager,
                    dismissLoadingWhenDecryptEnd = true,
                    mnemonicCallback = {
                        BackupPromptActivity.start(
                            requireContext(),
                            it,
                            BackupMnemonicFrom.BACKUP_IDENTITY_WALLET
                        )
                    }
                )
            } catch (e: Exception) {
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBackupIdentityMnemonicEvent(event: BackupIdentityMnemonicEvent) {
        layoutBackupNow.visibility = View.GONE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_ADD_ASSERT -> {
                if (resultCode == Activity.RESULT_OK) {
                    mWalletAppViewModel?.refreshAssetsList(true)
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
                                    WalletConnectAuthorizationActivity.startActivity(it, msg)
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

    override fun onResume() {
        StatusBarUtil.setLightStatusBarMode(requireActivity().window, false)
        super.onResume()
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
        val isChange = oldItem.getId() == newItem.getId() &&
                oldItem.amountWithUnit.amount == newItem.amountWithUnit.amount &&
                oldItem.amountWithUnit.unit == newItem.amountWithUnit.unit &&
                oldItem.fiatAmountWithUnit.unit == newItem.fiatAmountWithUnit.unit &&
                oldItem.fiatAmountWithUnit.symbol == newItem.fiatAmountWithUnit.symbol &&
                oldItem.fiatAmountWithUnit.amount == newItem.fiatAmountWithUnit.amount &&
                oldItem.getAmount() == newItem.getAmount() &&
                oldItem.getAssetsName() == newItem.getAssetsName() &&
                oldItem.getAccountId() == newItem.getAccountId() &&
                oldItem.getLogoUrl() == newItem.getLogoUrl()
        Log.e("areContentsTheSame", "${oldItem.getAssetsName()}   $isChange")
        /**
         * 此处接收到数据刷新做全列表刷新，列表因为列表加载逻辑分三步。
         * 1、记载本地列表
         * 2、网络查询填充币种阅
         * 3、网络查询填充法币价值
         * 为了考虑未来币种数量变多，所以不是每执行一步都创建一个对象数组，三步全程操作同一组对象，就会导致该对比方法失效。
         *
         * 先忽略此操作不做对比，全列表刷新。
         */
        return false
    }
}) {
    private var mAssetsHidden: Boolean = false
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_wallet_assert,
                parent,
                false
            )
        ).also { viewHolder ->
            viewHolder.itemView.setOnClickListener {
                call.invoke(getItem(viewHolder.adapterPosition))
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemData = getItem(position)

        holder.itemView.ivLogo.loadCircleImage(
            itemData.getLogoUrl(),
            getResourceId(R.attr.iconCoinDefLogo, holder.itemView.context)
        )

        holder.itemView.tvName.text = itemData.getAssetsName()

        if (mAssetsHidden) {
            holder.itemView.tvAmount.text = "****"
            holder.itemView.tvFiatAmount.text = "****"
        } else {
            holder.itemView.tvAmount.text = itemData.amountWithUnit.amount
            holder.itemView.tvFiatAmount.text =
                "≈${itemData.fiatAmountWithUnit.symbol}${itemData.fiatAmountWithUnit.amount}"
        }

    }

    fun assetsHidden(it: Boolean) {
        mAssetsHidden = it
        notifyDataSetChanged()
    }

    class ViewHolder(item: View) : RecyclerView.ViewHolder(item)
}