package com.violas.wallet.ui.issuerApplication.issueToken

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseActivity
import com.palliums.base.BaseFragment
import com.palliums.net.LoadState
import com.violas.wallet.R
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.repository.http.governor.SSOApplicationDetailsDTO
import com.violas.wallet.repository.http.issuer.ApplyForSSODetailsDTO
import com.violas.wallet.ui.issuerApplication.IssuerApplicationActivity
import com.violas.wallet.ui.issuerApplication.IssuerApplicationChildViewModel
import com.violas.wallet.ui.issuerApplication.IssuerApplicationChildViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Created by elephant on 2020/5/7 18:16.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 发行商申请发币流程 base fragment
 */
abstract class BaseIssuerIssueTokenFragment : BaseFragment() {

    protected var mApplyForSSODetails: ApplyForSSODetailsDTO? = null

    protected val mViewModel by lazy {
        ViewModelProvider(
            this,
            IssuerApplicationChildViewModelFactory(mApplyForSSODetails)
        ).get(IssuerApplicationChildViewModel::class.java)
    }

    override fun onLazyInitView(savedInstanceState: Bundle?) {
        super.onLazyInitView(savedInstanceState)

        if (initData(savedInstanceState)) {
            initView()
            initEvent()
        } else {
            finishActivity()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mApplyForSSODetails?.let { outState.putParcelable(KEY_ONE, it) }
    }

    protected open fun initData(savedInstanceState: Bundle?): Boolean {
        var details: ApplyForSSODetailsDTO? = null
        if (savedInstanceState != null) {
            details = savedInstanceState.getParcelable(KEY_ONE)
        } else if (arguments != null) {
            details = arguments!!.getParcelable(KEY_ONE)
        }

        if (details == null) {
            return false
        }

        mApplyForSSODetails = details
        return true
    }

    protected open fun initView() {
        (activity as? BaseActivity)?.setTitle(R.string.title_apply_issue_sso)
    }

    protected open fun initEvent() {
        mViewModel.tipsMessage.observe(this, Observer {
            it.getDataIfNotHandled()?.let { msg ->
                if (msg.isNotEmpty()) {
                    showToast(msg)
                }
            }
        })

        mViewModel.loadState.observe(this, Observer {
            when (it.peekData().status) {
                LoadState.Status.RUNNING -> {
                    showProgress()
                }

                else -> {
                    dismissProgress()
                }
            }
        })
    }

    protected fun startNewApplicationActivity() {
        context?.let {
            if (mApplyForSSODetails == null) {
                IssuerApplicationActivity.start(it)
            } else {
                IssuerApplicationActivity.start(it, mApplyForSSODetails!!)
            }

            launch(Dispatchers.IO) {
                delay(500)
                close()
            }
        }
    }
}