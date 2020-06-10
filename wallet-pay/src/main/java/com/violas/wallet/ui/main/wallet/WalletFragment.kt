package com.violas.wallet.ui.main.wallet

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.TouchDelegate
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.biometric.BiometricManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.palliums.base.BaseFragment
import com.palliums.biometric.BiometricCompat
import com.palliums.extensions.show
import com.violas.wallet.R
import com.violas.wallet.biz.*
import com.violas.wallet.biz.bean.AssertToken
import com.violas.wallet.event.BackupIdentityMnemonicEvent
import com.violas.wallet.repository.database.entity.AccountType
import com.violas.wallet.ui.account.walletmanager.WalletManagerActivity
import com.violas.wallet.ui.backup.BackupMnemonicFrom
import com.violas.wallet.ui.backup.BackupPromptActivity
import com.violas.wallet.ui.biometric.OpenBiometricsPromptDialog
import com.violas.wallet.ui.identity.createIdentity.CreateIdentityActivity
import com.violas.wallet.ui.identity.importIdentity.ImportIdentityActivity
import com.violas.wallet.ui.scan.ScanActivity
import com.violas.wallet.ui.scan.ScanResultActivity
import com.violas.wallet.ui.tokenInfo.TokenInfoActivity
import com.violas.wallet.ui.transfer.TransferActivity
import com.violas.wallet.ui.walletconnect.WalletConnectAuthorizationActivity
import com.violas.wallet.ui.walletconnect.WalletConnectManagerActivity
import com.violas.wallet.ui.webManagement.LoginWebActivity
import com.violas.wallet.utils.authenticateAccount
import com.violas.wallet.viewModel.WalletAppViewModel
import com.violas.wallet.viewModel.WalletConnectViewModel
import com.violas.wallet.viewModel.bean.AssetsCoinVo
import com.violas.wallet.viewModel.bean.AssetsVo
import com.violas.wallet.walletconnect.WalletConnectStatus
import com.violas.wallet.widget.dialog.FastIntoWalletDialog
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.android.synthetic.main.item_wallet_assert.view.*
import kotlinx.android.synthetic.main.view_backup_now_wallet.*
import kotlinx.coroutines.*
import me.jessyan.autosize.utils.AutoSizeUtils
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
    private val mWalletConnectViewModel by lazy {
        context?.let { WalletConnectViewModel.getViewModelInstance(it) }
    }
    private val mWalletViewModel by lazy {
        ViewModelProvider(this).get(WalletViewModel::class.java)
    }

    private var refreshAssertJob: Job? = null

    private val mAccountManager by lazy {
        AccountManager()
    }


    private val mEnableTokens = mutableListOf<AssertToken>()
    private val mAssertAdapter by lazy {
        AssertAdapter {
            activity?.let { it1 ->
                TokenInfoActivity.start(it1, it)
            }
        }
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_wallet
    }

    override fun onLazyInitView(savedInstanceState: Bundle?) {
        super.onLazyInitView(savedInstanceState)

        EventBus.getDefault().register(this)

        recyclerAssert.adapter = mAssertAdapter

        mWalletAppViewModel?.mDataRefreshingLiveData?.observe(this, Observer {
            swipeRefreshLayout.isRefreshing = it
        })
        mWalletAppViewModel?.mAssetsListLiveData?.observe(this, Observer {
            val filter =
                it.filter {
                    if (it !is AssetsCoinVo) {
                        true
                    } else {
                        it.accountType != AccountType.NoDollars
                    }
                }
            mAssertAdapter.submitList(filter)
            mWalletViewModel.calculateFiat(filter)
        })
        mWalletAppViewModel?.mExistsAccountLiveData?.observe(this, Observer {
            if (it) {
                viewAssetsGroup.visibility = View.VISIBLE
                viewAddAccount.visibility = View.GONE
            } else {
                viewAssetsGroup.visibility = View.GONE
                viewAddAccount.visibility = View.VISIBLE
            }
            checkBackup()
        })
        mWalletViewModel.mTotalFiatBalanceStrLiveData.observe(this, Observer {
            tvAmount.text = it
        })
        mWalletViewModel.mHiddenTotalFiatBalanceLiveData.observe(this, Observer {
            if (it) {
                ivTotalHidden.setImageResource(R.drawable.ic_total_balance_hidden)
            } else {
                ivTotalHidden.setImageResource(R.drawable.ic_total_balance_show)
            }
        })
        tvTotalAssetsTitle.setOnClickListener {
            mWalletViewModel.taggerTotalDisplay()
        }

        setTouchDelegate(tvTotalAssetsTitle, 100)

        mWalletConnectViewModel?.mWalletConnectStatusLiveData?.observe(this, Observer { status ->
            when (status) {
                WalletConnectStatus.None -> {
                    viewWalletConnect.visibility = View.GONE
                }
                WalletConnectStatus.Login -> {
                    tvWalletConnectStatus.text = getString(R.string.wallet_connect_have_landed)
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

        swipeRefreshLayout.setOnRefreshListener {
            mWalletAppViewModel?.refreshAssetsList()
        }

        checkBackup()
    }

    private fun checkBackup() {
        if (mAccountManager.isFastIntoWallet()) {
            FastIntoWalletDialog()
                .show(requireActivity().supportFragmentManager, "fast")
        } else if (!mAccountManager.isIdentityMnemonicBackup()) {
            launch(Dispatchers.IO) {
                delay(1000)
                if (!mAccountManager.isIdentityMnemonicBackup() && mWalletAppViewModel?.isExistsAccount() == true) {
                    withContext(Dispatchers.Main) {
                        layoutBackupNow.visibility = View.VISIBLE
                        btnConfirm.setOnClickListener(this@WalletFragment)

                        handleOpenBiometricsPrompt()
                    }
                }
            }
        }
    }

    private fun setTouchDelegate(view: View, expandTouchWidth: Int) {
        val parentView = view.parent as View
        parentView.post {
            val rect = Rect()
            view.getHitRect(rect)
            rect.top -= expandTouchWidth
            rect.bottom += expandTouchWidth
            rect.left -= expandTouchWidth
            rect.right += expandTouchWidth
            val touchDelegate = TouchDelegate(rect, view)
            parentView.touchDelegate = touchDelegate
        }
    }

    override fun onViewClick(view: View) {
        when (view.id) {
            R.id.ivAddAssert -> {
                launch(Dispatchers.IO) {
                    //                    val currentAccount = mAccountManager.currentAccount()
//                    withContext(Dispatchers.Main) {
//                        activity?.let { activity ->
//                            ManagerAssertActivity.start(
//                                this@WalletFragment,
//                                currentAccount.id,
//                                REQUEST_ADD_ASSERT
//                            )
//                        }
//                    }
                }
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
            R.id.viewCreateAccount -> {
                activity?.let { CreateIdentityActivity.start(it) }
            }
            R.id.viewImportAccount -> {
                activity?.let { ImportIdentityActivity.start(it) }
            }
//            R.id.ivWalletInfo -> {
//                launch(Dispatchers.IO) {
//                    val currentAccount = mAccountManager.currentAccount()
//                    WalletManagerActivity.start(this@WalletFragment, currentAccount.id)
//                }
//            }

//            R.id.btnCollection -> {
//                launch(Dispatchers.IO) {
//                    val currentAccount = mAccountManager.currentAccount()
//                    activity?.let { it1 -> CollectionActivity.start(it1, currentAccount.id) }
//                }
//            }

            R.id.btnTransfer -> {
                launch(Dispatchers.IO) {
                    activity?.let { it1 ->
                        //                        TransferActivity.start(
//                            it1,
//                            mAccountManager.currentAccount().id
//                        )
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
                    WalletManagerActivity.start(this@WalletFragment)
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
            BackupMnemonicFrom.BACKUP_IDENTITY_WALLET -> {
                layoutBackupNow.visibility = View.GONE
            }
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
                                    WalletConnectAuthorizationActivity.startActivity(it, msg)
//                                    WalletConnect.getInstance(it.applicationContext).connect(
//                                        msg
//                                    )
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

    private fun loadTransform(
        context: Context,
        @DrawableRes placeholderId: Int,
        radius: Int
    ): RequestBuilder<Drawable?>? {
        return Glide.with(context)
            .load(placeholderId)
            .apply(RequestOptions.bitmapTransform(RoundedCorners(radius)))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemData = getItem(position)
        Glide.with(holder.itemView.context)
            .load(itemData.getLogoUrl())
            .error(R.drawable.assets_default)
            .placeholder(R.drawable.assets_default)
            .thumbnail(
                loadTransform(
                    holder.itemView.context,
                    R.drawable.assets_default,
                    AutoSizeUtils.dp2px(holder.itemView.context, 14F)
                )
            )
            .apply(
                RequestOptions.bitmapTransform(
                    RoundedCorners(
                        AutoSizeUtils.dp2px(
                            holder.itemView.context,
                            14F
                        )
                    )
                )
            )
            .into(holder.itemView.ivLogo)
        holder.itemView.tvName.text = itemData.getAssetsName()

        holder.itemView.tvAmount.text = itemData.amountWithUnit.amount
        holder.itemView.tvFiatAmount.text =
            "≈${itemData.fiatAmountWithUnit.symbol}${itemData.fiatAmountWithUnit.amount}"
    }

    class ViewHolder(item: View) : RecyclerView.ViewHolder(item)
}