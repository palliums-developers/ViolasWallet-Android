package com.violas.wallet.ui.main.applyFor

import android.os.Bundle
import android.view.View
import com.palliums.base.BaseFragment
import com.violas.wallet.R
import com.violas.wallet.event.HomePageModifyEvent
import com.violas.wallet.event.HomePageType
import kotlinx.android.synthetic.main.fragment_check_verify.*
import org.greenrobot.eventbus.EventBus

class CheckVerifyFragment : BaseFragment() {
    override fun getLayoutResId(): Int {
        return R.layout.fragment_check_verify
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btnGo.setOnClickListener {
            EventBus.getDefault().post(HomePageModifyEvent(HomePageType.Me))
        }
    }
}
