package com.violas.wallet.ui.ssoApplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.palliums.net.RequestException
import com.palliums.utils.isExpired
import com.palliums.utils.start
import com.palliums.widget.status.IStatusLayout
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.SSOApplicationState
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.repository.http.governor.SSOApplicationDetailsDTO
import com.violas.wallet.repository.http.sso.ApplyForStatusDTO
import com.violas.wallet.ui.ssoApplication.issueToken.SSOApplyForIssueTokenFragment
import com.violas.wallet.ui.ssoApplication.issueToken.SSOIssueTokenAuditingFragment
import com.violas.wallet.ui.ssoApplication.issueToken.SSOReapplyForIssueTokenFragment
import com.violas.wallet.ui.ssoApplication.mintToken.SSOApplyForMintTokenFragment
import com.violas.wallet.ui.ssoApplication.mintToken.SSOMintTokenProgressFragment
import kotlinx.android.synthetic.main.activity_sso_application.*

/**
 * Created by elephant on 2020/5/7 16:31.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 发行商申请发币业务页面
 */
class SSOApplicationActivity : BaseAppActivity() {

    companion object {

        fun start(context: Context, applicationMsg: ApplyForStatusDTO) {
            Intent(context, SSOApplicationActivity::class.java)
                .apply { putExtra(KEY_ONE, applicationMsg) }
                .start(context)
        }

        fun start(context: Context, applicationDetails: SSOApplicationDetailsDTO) {
            start(context, ApplyForStatusDTO.newInstance(applicationDetails))
        }

        fun start(context: Context) {
            start(context, ApplyForStatusDTO(Int.MIN_VALUE))
        }
    }

    private lateinit var mSSOApplicationMsg: ApplyForStatusDTO

    private val mViewModel by lazy {
        ViewModelProvider(this, SSOApplicationParentViewModelFactory(mSSOApplicationMsg))
            .get(SSOApplicationParentViewModel::class.java)
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_sso_application
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (initData(savedInstanceState)) {
            initView()
        } else {
            close()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_ONE, mSSOApplicationMsg)
    }

    private fun initData(savedInstanceState: Bundle?): Boolean {
        var applicationMsg: ApplyForStatusDTO? = null
        if (savedInstanceState != null) {
            applicationMsg = savedInstanceState.getParcelable(KEY_ONE)
        } else if (intent != null) {
            applicationMsg = intent.getParcelableExtra(KEY_ONE)
        }

        if (applicationMsg == null) {
            return false
        }

        mSSOApplicationMsg = applicationMsg
        return true
    }

    private fun initView() {
        dslStatusLayout.showStatus(IStatusLayout.Status.STATUS_LOADING)
        srlRefreshLayout.isEnabled = false
        srlRefreshLayout.setOnRefreshListener {
            loadApplicationDetails()
        }

        mViewModel.tipsMessage.observe(this, Observer {
            it.getDataIfNotHandled()?.let { msg ->
                if (msg.isNotEmpty()) {
                    showToast(msg)
                }
            }
        })

        mViewModel.mSSOApplicationDetailsLD.observe(this, Observer {
            loadFragment(it)
        })

        mViewModel.mAccountLD.observe(this, Observer {
            loadApplicationDetails()
        })
    }

    private fun loadApplicationDetails() {
        mViewModel.execute(
            failureCallback = {
                if (srlRefreshLayout.isRefreshing) {
                    srlRefreshLayout.isRefreshing = false
                }
                srlRefreshLayout.isEnabled = true

                dslStatusLayout.showStatus(
                    if (RequestException.isNoNetwork(it))
                        IStatusLayout.Status.STATUS_NO_NETWORK
                    else
                        IStatusLayout.Status.STATUS_FAILURE
                )
            }) {
            if (srlRefreshLayout.isRefreshing) {
                srlRefreshLayout.isRefreshing = false
            }
            srlRefreshLayout.isEnabled = false

            dslStatusLayout.showStatus(IStatusLayout.Status.STATUS_NONE)
        }
    }

    private fun loadFragment(details: SSOApplicationDetailsDTO?) {
        if (details == null) {
            loadRootFragment(R.id.flFragmentContainer, SSOApplyForIssueTokenFragment())
            return
        }

        val fragment = when (details.applicationStatus) {
            SSOApplicationState.APPLYING_ISSUE_TOKEN -> {
                if (isExpired(details.expirationDate)) {
                    SSOReapplyForIssueTokenFragment()
                } else {
                    SSOIssueTokenAuditingFragment()
                }
            }

            SSOApplicationState.APPLYING_MINTABLE,
            SSOApplicationState.GIVEN_MINTABLE -> {
                SSOIssueTokenAuditingFragment()
            }

            SSOApplicationState.TRANSFERRED_AND_NOTIFIED -> {
                SSOApplyForMintTokenFragment()
            }

            SSOApplicationState.APPLYING_MINT_TOKEN,
            SSOApplicationState.MINTED_TOKEN -> {
                SSOMintTokenProgressFragment()
            }

            else -> {
                SSOReapplyForIssueTokenFragment()
            }
        }.apply {
            arguments = Bundle().apply { putParcelable(KEY_ONE, details) }
        }

        loadRootFragment(R.id.flFragmentContainer, fragment)
    }
}