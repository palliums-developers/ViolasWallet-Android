package com.violas.wallet.ui.main.wallet

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.TokenManager
import com.violas.wallet.biz.bean.AssertToken
import com.violas.wallet.biz.decodeScanQRCode
import com.violas.wallet.event.SwitchAccountEvent
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.ui.account.walletmanager.WalletManagerActivity
import com.violas.wallet.ui.backup.BackupMnemonicFrom
import com.violas.wallet.ui.backup.BackupPromptActivity
import com.violas.wallet.ui.collection.CollectionActivity
import com.violas.wallet.ui.record.TransactionRecordActivity
import com.violas.wallet.ui.scan.ScanActivity
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


class WalletFragment : Fragment(), CoroutineScope by MainScope() {
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

    private lateinit var mView: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_wallet, container, false)
        return mView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        EventBus.getDefault().register(this)
        refreshAccountData()

        recyclerAssert.adapter = mAssertAdapter
        refreshAssert()

        ivCopy.setOnClickListener {
            launch(Dispatchers.IO) {
                val currentAccount = mAccountManager.currentAccount()
                withContext(Dispatchers.Main) {
                    activity?.let { it1 -> ClipboardUtils.copy(it1, currentAccount.address) }
                }
            }
        }
        ivScan.setOnClickListener {
            this@WalletFragment.activity?.let { it1 ->
                ScanActivity.start(this, REQUEST_SCAN_QR_CODE)
            }
        }
        ivWalletInfo.setOnClickListener {
            launch(Dispatchers.IO) {
                val currentAccount = mAccountManager.currentAccount()
                WalletManagerActivity.start(this@WalletFragment, currentAccount.id)
            }
        }

        btnCollection.setOnClickListener {
            launch(Dispatchers.IO) {
                val currentAccount = mAccountManager.currentAccount()
                activity?.let { it1 -> CollectionActivity.start(it1, currentAccount.id) }
            }
        }
        btnTransfer.setOnClickListener {
            launch(Dispatchers.IO) {
                activity?.let { it1 ->
                    TransferActivity.start(
                        it1,
                        mAccountManager.currentAccount().id
                    )
                }
            }
        }
        vTransactionRecordLayout.setOnClickListener {
            launch(Dispatchers.IO) {
                val currentAccount = mAccountManager.currentAccount()
                activity?.let { TransactionRecordActivity.start(it, currentAccount.id) }
            }
        }

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
            onSwitchAccountEvent(SwitchAccountEvent())
            launch(Dispatchers.IO) {
                delay(1500)
                withContext(Dispatchers.Main) {
                    swipeRefreshLayout.isRefreshing = false
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSwitchAccountEvent(event: SwitchAccountEvent) {
//        mCompositeDisposable.dispose()
        refreshAccountData()
        refreshAssert()
    }

    private fun refreshAccountData() {
        launch(Dispatchers.IO) {
            val currentAccount = mAccountManager.currentAccount()
            withContext(Dispatchers.Main) {
                setViewData(currentAccount)
                mAccountManager.refreshAccountAmount(currentAccount) {
                    try {
                        setAmount(currentAccount)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setViewData(currentAccount: AccountDO) {
        tvAddress.text = currentAccount.address
        val coinType = CoinTypes.parseCoinType(currentAccount.coinNumber)
        tvWalletType.text = "${coinType.coinName()} Wallet"
        tvUnit.text = coinType.coinUnit()
        setAmount(currentAccount)
    }

    private fun setAmount(currentAccount: AccountDO) {
        activity?.runOnUiThread {
            val parseCoinType = CoinTypes.parseCoinType(currentAccount.coinNumber)
            val convertAmountToDisplayUnit =
                convertAmountToDisplayUnit(currentAccount.amount, parseCoinType)
            tvAmount.text = "${convertAmountToDisplayUnit.first}"

            if (mEnableTokens.size >= 1) {
                mEnableTokens[0].amount = currentAccount.amount
                recyclerAssert.post {
                    mAssertAdapter.notifyItemChanged(0)
                }
            }
        }
    }

    private fun refreshAssert() {
        launch(Dispatchers.IO) {
            mEnableTokens.clear()
            recyclerAssert.post {
                mAssertAdapter.notifyItemRangeRemoved(0, mAssertAdapter.itemCount)
            }
            val currentAccount = mAccountManager.currentAccount()
            val loadEnableToken = mTokenManger.loadEnableToken(currentAccount)
            if (currentAccount.coinNumber == CoinTypes.VToken.coinType()) {
                mTokenManger.refreshBalance(
                    currentAccount.address,
                    loadEnableToken
                ) {
                    mEnableTokens.addAll(it)
                    recyclerAssert.post {
                        mAssertAdapter.notifyItemRangeRemoved(0, mAssertAdapter.itemCount)
                    }
                }
            } else {
                mEnableTokens.addAll(loadEnableToken)
                recyclerAssert.post {
                    mAssertAdapter.notifyItemRangeRemoved(0, mAssertAdapter.itemCount)
                }
            }

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_ADD_ASSERT -> {
                refreshAssert()
            }
            REQUEST_SCAN_QR_CODE -> {
                data?.getStringExtra(ScanActivity.RESULT_QR_CODE_DATA)?.let { msg ->
                    decodeScanQRCode(msg) { coinType, address, amount, tokenName ->
                        Log.e(
                            "====",
                            "coinType:$coinType address:$address amount:$amount tokenName:$tokenName"
                        )
                        activity?.let {
                            TransferActivity.start(
                                it,
                                coinType,
                                address,
                                amount,
                                tokenName
                            )
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
            if (itemData.isToken) {
                call.invoke(itemData)
            }
        }
    }

    class ViewHolder(item: View) : RecyclerView.ViewHolder(item)
}