package com.violas.wallet.ui.main.wallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.event.SwitchAccountEvent
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.ui.managerAssert.ManagerAssertActivity
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.coroutines.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class WalletFragment : Fragment(), CoroutineScope by MainScope() {
    private val REQUEST_ADD_ASSERT = 0

    private val mAccountManager by lazy {
        AccountManager()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_wallet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        refreshAccountData()
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
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun refreshAccountData(event: SwitchAccountEvent? = null) {
        launch(Dispatchers.IO) {
            val currentAccount = mAccountManager.currentAccount()
            withContext(Dispatchers.Main) {
                setViewData(currentAccount)
            }
            mAccountManager.refreshAccountAmount(currentAccount) {
                launch(Dispatchers.Main) {
                    setAmount(currentAccount)
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
        tvAmount.text = "${currentAccount.amount}"
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == REQUEST_ADD_ASSERT && resultCode == Activity.RESULT_OK) {
////            refreshAssert()
//        }
//    }

    override fun onDestroy() {
        cancel()
        super.onDestroy()
    }
}