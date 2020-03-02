package com.violas.wallet.ui.main.message.governorApplication

import android.os.Bundle
import android.view.View
import com.palliums.base.BaseFragment
import com.palliums.utils.getColor
import com.violas.wallet.R
import com.violas.wallet.ui.applyForLicence.ApplyForLicenceActivity
import kotlinx.android.synthetic.main.fragment_governor_application.*

/**
 * Created by elephant on 2020/2/28 18:25.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 州长牌照申请进度页面
 */
class GovernorApplicationFragment : BaseFragment() {

    companion object {
        private const val EXTRA_KEY_APPLICATION_STATUS = "EXTRA_KEY_APPLICATION_STATUS"

        fun newInstance(applicationStatus: Int): GovernorApplicationFragment {
            return GovernorApplicationFragment().apply {
                arguments = newBundle(applicationStatus)
            }
        }

        fun newBundle(applicationStatus: Int): Bundle {
            return Bundle().apply {
                putInt(EXTRA_KEY_APPLICATION_STATUS, applicationStatus)
            }
        }
    }

    private var mApplicationStatus = -1

    override fun getLayoutResId(): Int {
        return R.layout.fragment_governor_application
    }

    override fun onLazyInitView(savedInstanceState: Bundle?) {
        super.onLazyInitView(savedInstanceState)

        arguments?.let {
            mApplicationStatus = it.getInt(EXTRA_KEY_APPLICATION_STATUS, mApplicationStatus)
        }
        btnGoto.setOnClickListener(this)

        refreshView()
    }

    override fun onNewBundle(args: Bundle?) {
        super.onNewBundle(args)

        args?.let {
            mApplicationStatus = it.getInt(EXTRA_KEY_APPLICATION_STATUS, mApplicationStatus)
            refreshView()
        }
    }

    private fun refreshView() {
        // -1: no application; 2: not pass
        if (mApplicationStatus == -1 || mApplicationStatus == 2) {
            mivStatus.setEndDescText(R.string.desc_authentication_failed)
            mivStatus.setEndDescTextColor(getColor(R.color.def_text_warn))
            btnGoto.visibility = View.VISIBLE
        } else {
            mivStatus.setEndDescText(R.string.desc_authenticating)
            mivStatus.setEndDescTextColor(getColor(R.color.color_00D1AF))
            btnGoto.visibility = View.GONE
        }
    }

    override fun onViewClick(view: View) {
        when (view.id) {
            R.id.btnGoto -> {
                activity?.let {
                    ApplyForLicenceActivity.start(it)
                }
            }
        }
    }
}