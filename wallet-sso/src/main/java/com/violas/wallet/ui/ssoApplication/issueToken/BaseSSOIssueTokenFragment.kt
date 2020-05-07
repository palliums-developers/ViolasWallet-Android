package com.violas.wallet.ui.ssoApplication.issueToken

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseActivity
import com.palliums.base.BaseFragment
import com.palliums.net.LoadState
import com.violas.wallet.R
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.repository.http.governor.SSOApplicationDetailsDTO
import com.violas.wallet.ui.ssoApplication.SSOApplicationActivity
import com.violas.wallet.ui.ssoApplication.SSOApplicationChildViewModel
import com.violas.wallet.ui.ssoApplication.SSOApplicationChildViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Created by elephant on 2020/5/7 18:16.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
abstract class BaseSSOIssueTokenFragment : BaseFragment() {

    protected lateinit var mSSOApplicationDetails: SSOApplicationDetailsDTO

    protected val mViewModel by lazy {
        ViewModelProvider(
            this,
            SSOApplicationChildViewModelFactory(mSSOApplicationDetails)
        ).get(SSOApplicationChildViewModel::class.java)
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
        outState.putParcelable(KEY_ONE, mSSOApplicationDetails)
    }

    private fun initData(savedInstanceState: Bundle?): Boolean {
        var detailsDTO: SSOApplicationDetailsDTO? = null
        if (savedInstanceState != null) {
            detailsDTO = savedInstanceState.getParcelable(KEY_ONE)
        } else if (arguments != null) {
            detailsDTO = arguments!!.getParcelable(KEY_ONE)
        }

        if (detailsDTO == null) {
            return false
        }

        mSSOApplicationDetails = detailsDTO
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
            SSOApplicationActivity.start(it, mSSOApplicationDetails)
            launch(Dispatchers.IO) {
                delay(500)
                close()
            }
        }
    }
}