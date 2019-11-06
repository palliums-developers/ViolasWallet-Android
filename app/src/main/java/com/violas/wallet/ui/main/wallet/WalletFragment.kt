package com.violas.wallet.ui.main.wallet

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.dialog.FastIntoWalletDialog
import com.violas.wallet.base.dialog.PasswordInputDialog
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.TokenManager
import com.violas.wallet.biz.bean.AssertToken
import com.violas.wallet.biz.decodeScanQRCode
import com.violas.wallet.event.SwitchAccountEvent
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.ui.account.selection.AccountSelectionActivity
import com.violas.wallet.ui.account.walletmanager.WalletManagerActivity
import com.violas.wallet.ui.backup.BackupMnemonicFrom
import com.violas.wallet.ui.backup.BackupPromptActivity
import com.violas.wallet.ui.collection.CollectionActivity
import com.violas.wallet.ui.managerAssert.ManagerAssertActivity
import com.violas.wallet.ui.scan.ScanActivity
import com.violas.wallet.ui.transfer.TransferActivity
import com.violas.wallet.utils.ClipboardUtils
import com.violas.wallet.utils.convertAmountToDisplayUnit
import com.violas.wallet.utils.start
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
    }

    private val mAccountManager by lazy {
        AccountManager()
    }

    private val mTokenManger by lazy {
        TokenManager()
    }

    private val mEnableTokens = mutableListOf<AssertToken>()
    private val mAssertAdapter by lazy {
        AssertAdapter(mEnableTokens)
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

        ivAddAssert.setOnClickListener {
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

        layoutWalletType.setOnClickListener {
            activity?.let { it1 ->
                Intent(
                    activity,
                    AccountSelectionActivity::class.java
                ).start(it1)
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

        if (mAccountManager.isFastIntoWallet()) {
            fragmentManager?.let {
                FastIntoWalletDialog()
                    .show(it, "fast")
            }
        } else {
            if (!mAccountManager.isIdentityMnemonicBackup()) {
                layoutBackupNow.visibility = View.VISIBLE
                btnConfirm.setOnClickListener {
                    layoutBackupNow?.visibility = View.GONE

                    fragmentManager?.let {
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
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSwitchAccountEvent(event: SwitchAccountEvent) {
        refreshAccountData()
        refreshAssert()
    }

    private fun refreshAccountData() {
        launch(Dispatchers.IO) {
            val currentAccount = mAccountManager.currentAccount()
            withContext(Dispatchers.Main) {
                setViewData(currentAccount)
            }
            mAccountManager.refreshAccountAmount(currentAccount) {
                setAmount(currentAccount)
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
        if (coinType == CoinTypes.VToken) {
            ivAddAssert.visibility = View.VISIBLE
        } else {
            ivAddAssert.visibility = View.GONE
        }
    }

    private fun setAmount(currentAccount: AccountDO) {
        activity?.runOnUiThread {
            val parseCoinType = CoinTypes.parseCoinType(currentAccount.coinNumber)
            val convertAmountToDisplayUnit =
                convertAmountToDisplayUnit(currentAccount.amount, parseCoinType)
            tvAmount.text = "${convertAmountToDisplayUnit.first}"
        }
    }

    private fun refreshAssert() {
        launch(Dispatchers.IO) {
            mEnableTokens.clear()
            val currentAccount = mAccountManager.currentAccount()
            mEnableTokens.addAll(mTokenManger.loadEnableToken(currentAccount))
            withContext(Dispatchers.Main) {
                mAssertAdapter.notifyDataSetChanged()
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
                    decodeScanQRCode(msg) { coinType, address, amount ->
                        val currentAccount = mAccountManager.currentAccount()
                        if (coinType == currentAccount.coinNumber) {
                            activity?.let { TransferActivity.start(it, currentAccount.id, address) }
                        } else {
                            val account = mAccountManager.getIdentityByCoinType(coinType)
                                ?: return@decodeScanQRCode
                            activity?.let { TransferActivity.start(it, account.id, address) }
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
    val data: List<AssertToken>
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
        holder.itemView.amount.text = "${itemData.amount}"
    }

    class ViewHolder(item: View) : RecyclerView.ViewHolder(item)
}