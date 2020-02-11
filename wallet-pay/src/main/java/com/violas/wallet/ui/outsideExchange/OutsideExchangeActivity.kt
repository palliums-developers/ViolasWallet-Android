package com.violas.wallet.ui.outsideExchange

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.palliums.utils.start
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity

class OutsideExchangeActivity : BaseAppActivity() {

    companion object {
        private const val EXT_ACCOUNT_ID = "account_id"
        fun start(context: Context, accountId: Long) {
            Intent(context, OutsideExchangeActivity::class.java)
                .putExtra(EXT_ACCOUNT_ID, accountId)
                .start(context)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitleStyle(TITLE_STYLE_NOT_TITLE)
        val accountId = intent.getLongExtra(EXT_ACCOUNT_ID, -1L)

        if (accountId == -1L) {
            return
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, OutsideExchangeFragment.newInstance(accountId))
            .commit()

    }

    override fun getLayoutResId() = R.layout.activity_outside_exchange
}
