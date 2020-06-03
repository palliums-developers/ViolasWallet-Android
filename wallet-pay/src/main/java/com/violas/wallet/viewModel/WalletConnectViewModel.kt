package com.violas.wallet.viewModel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.palliums.content.App
import com.palliums.content.ContextProvider
import com.palliums.utils.CustomMainScope
import com.violas.wallet.walletconnect.WalletConnect
import com.violas.wallet.walletconnect.WalletConnectListener
import com.violas.wallet.walletconnect.WalletConnectStatus
import kotlinx.coroutines.CoroutineScope

class WalletConnectViewModel : ViewModel(), CoroutineScope by CustomMainScope() {
    companion object {
        fun getViewModelInstance(context: Context): WalletConnectViewModel {
            return ViewModelProvider(context.applicationContext as App).get(WalletConnectViewModel::class.java)
        }
    }

    val mWalletConnectStatusLiveData = MutableLiveData(WalletConnectStatus.None)

    private val mWalletConnect = WalletConnect.getInstance(ContextProvider.getContext())

    init {
        mWalletConnect.restore()
        mWalletConnect.mWalletConnectListener = object : WalletConnectListener {
            override fun onConnect() {
                mWalletConnectStatusLiveData.postValue(WalletConnectStatus.Connected)
            }

            override fun onLogin() {
                mWalletConnectStatusLiveData.postValue(WalletConnectStatus.Login)
            }

            override fun onDisconnect() {
                mWalletConnectStatusLiveData.postValue(WalletConnectStatus.None)
            }

        }
    }

}