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
import com.violas.wallet.event.ChangeAccountEvent
import com.violas.wallet.repository.database.entity.AccountDO
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.coroutines.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class WalletFragment : Fragment(), CoroutineScope by MainScope() {
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
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun refreshAccountData(event: ChangeAccountEvent? = null) {
        launch(Dispatchers.IO) {
            var currentAccount = mAccountManager.currentAccount()
            withContext(Dispatchers.Main) {
                setViewData(currentAccount)
            }
            currentAccount = mAccountManager.refreshAccountAmount(currentAccount)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setViewData(currentAccount: AccountDO) {
        tvAddress.text = currentAccount.address
        val coinType = CoinTypes.parseCoinType(currentAccount.coinNumber)
        tvWalletType.text = "${coinType.coinName()} Wallet"
        tvAmount.text = "${currentAccount.amount}"
        tvUnit.text = coinType.coinUnit()
    }

    override fun onDestroy() {
        cancel()
        super.onDestroy()
    }
}