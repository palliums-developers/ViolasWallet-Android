package com.violas.wallet.ui.main.applyFor

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.palliums.base.BaseFragment
import com.palliums.utils.start
import com.violas.wallet.R
import com.violas.wallet.ui.mintSuccess.MintSuccessActivity
import kotlinx.android.synthetic.main.fragment_apply_status.*

class ApplyStatusFragment : BaseFragment() {
    override fun getLayoutResId(): Int {
        return R.layout.fragment_apply_status
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        layoutItem.setOnClickListener {
            activity?.let { it1 ->
                Intent(
                    this.activity,
                    MintSuccessActivity::class.java
//                    ApplyForMintActivity::class.java
                ).start(it1)
            }
        }
    }
}
