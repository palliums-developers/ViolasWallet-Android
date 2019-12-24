package com.violas.wallet.ui.main.applyFor

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.palliums.base.BaseFragment
import com.palliums.utils.start
import com.violas.wallet.R
import kotlinx.android.synthetic.main.fragment_check_verify.*

class VerifySuccessFragment : BaseFragment() {
    override fun getLayoutResId(): Int {
        return R.layout.fragment_verify_success
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btnGo.setOnClickListener {
            activity?.let { it1 ->
                Intent(
                    activity,
                    ApplyFotSsoSubmitActivity::class.java
                ).start(it1)
            }
        }
    }
}
