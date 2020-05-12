package com.violas.wallet.ui.issuerApplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.palliums.net.RequestException
import com.palliums.utils.start
import com.palliums.widget.status.IStatusLayout
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.SSOApplicationState
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.repository.http.issuer.ApplyForSSODetailsDTO
import com.violas.wallet.repository.http.issuer.ApplyForSSOSummaryDTO
import com.violas.wallet.ui.issuerApplication.issueToken.IssuerApplyForIssueTokenFragment
import com.violas.wallet.ui.issuerApplication.issueToken.IssuerIssueTokenAuditingFragment
import com.violas.wallet.ui.issuerApplication.issueToken.IssuerReapplyForIssueTokenFragment
import com.violas.wallet.ui.issuerApplication.mintToken.IssuerApplyForMintTokenFragment
import com.violas.wallet.ui.issuerApplication.mintToken.IssuerMintTokenProgressFragment
import kotlinx.android.synthetic.main.activity_issuer_application.*

/**
 * Created by elephant on 2020/5/7 16:31.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 发行商申请发行SSO业务页面
 */
class IssuerApplicationActivity : BaseAppActivity() {

    companion object {

        fun start(context: Context, summary: ApplyForSSOSummaryDTO) {
            Intent(context, IssuerApplicationActivity::class.java)
                .apply { putExtra(KEY_ONE, summary) }
                .start(context)
        }

        fun start(context: Context, details: ApplyForSSODetailsDTO) {
            start(context, ApplyForSSOSummaryDTO.newInstance(details))
        }

        fun start(context: Context) {
            start(context, ApplyForSSOSummaryDTO(Int.MIN_VALUE))
        }
    }

    private lateinit var mApplyForSSOSummary: ApplyForSSOSummaryDTO

    private val mViewModel by lazy {
        ViewModelProvider(this, IssuerApplicationParentViewModelFactory(mApplyForSSOSummary))
            .get(IssuerApplicationParentViewModel::class.java)
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_issuer_application
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
        outState.putParcelable(KEY_ONE, mApplyForSSOSummary)
    }

    private fun initData(savedInstanceState: Bundle?): Boolean {
        var summary: ApplyForSSOSummaryDTO? = null
        if (savedInstanceState != null) {
            summary = savedInstanceState.getParcelable(KEY_ONE)
        } else if (intent != null) {
            summary = intent.getParcelableExtra(KEY_ONE)
        }

        if (summary == null) {
            return false
        }

        mApplyForSSOSummary = summary
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

        mViewModel.mApplyForSSODetailsLiveData.observe(this, Observer {
            loadFragment(it)
        })

        mViewModel.mAccountDOLiveData.observe(this, Observer {
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

    private fun loadFragment(details: ApplyForSSODetailsDTO?) {
        if (details == null) {
            loadRootFragment(R.id.flFragmentContainer, IssuerApplyForIssueTokenFragment())
            return
        }

        val fragment = when (details.applicationStatus) {
            SSOApplicationState.ISSUER_APPLYING -> {
                    IssuerIssueTokenAuditingFragment()
            }

            SSOApplicationState.GOVERNOR_APPROVED,
            SSOApplicationState.CHAIRMAN_APPROVED -> {
                IssuerIssueTokenAuditingFragment()
            }

            SSOApplicationState.GOVERNOR_TRANSFERRED -> {
                IssuerApplyForMintTokenFragment()
            }

            SSOApplicationState.ISSUER_PUBLISHED,
            SSOApplicationState.GOVERNOR_MINTED -> {
                IssuerMintTokenProgressFragment()
            }

            else -> {
                IssuerReapplyForIssueTokenFragment()
            }
        }.apply {
            arguments = Bundle().apply { putParcelable(KEY_ONE, details) }
        }

        loadRootFragment(R.id.flFragmentContainer, fragment)
    }
}