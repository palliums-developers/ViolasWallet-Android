package com.violas.wallet.ui.main.applyFor

import android.os.Bundle
import android.view.View
import com.palliums.base.BaseFragment
import com.violas.wallet.R
import com.violas.wallet.event.ApplyPageRefreshEvent
import kotlinx.android.synthetic.main.fragment_network_status.*
import org.greenrobot.eventbus.EventBus

class NetworkStatusFragment : BaseFragment() {
    override fun getLayoutResId(): Int {
        return R.layout.fragment_network_status
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btnRetry.setOnClickListener {
            EventBus.getDefault().post(ApplyPageRefreshEvent())
        }
    }
}
