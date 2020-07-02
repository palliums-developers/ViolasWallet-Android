package com.violas.wallet.ui.walletconnect

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.walletconnect.WalletConnect
import kotlinx.android.synthetic.main.activity_wallet_connect_manager.*

class WalletConnectManagerActivity : BaseAppActivity() {
    companion object {
        fun startActivity(context: Context) {
            context.startActivity(
                Intent(
                    context,
                    WalletConnectManagerActivity::class.java
                )
            )
        }
    }

    override fun getTitleStyle() = PAGE_STYLE_NOT_TITLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        btnLogout.setOnClickListener {
            if (WalletConnect.getInstance(this).disconnect()) {
                finish()
            }else{
                showToast(String.format(getString(R.string.common_http_request_fail), ""))
            }
        }
        ivClose.setOnClickListener {
            finish()
        }
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_wallet_connect_manager
    }
}