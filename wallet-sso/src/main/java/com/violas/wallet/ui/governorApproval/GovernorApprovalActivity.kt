package com.violas.wallet.ui.governorApproval

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
import com.violas.wallet.common.EXTRA_KEY_SSO_MSG
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.repository.http.governor.SSOApplicationDetailsDTO
import com.violas.wallet.ui.governorApproval.approvalIssueToken.ApplyForMintableProgressFragment
import com.violas.wallet.ui.governorApproval.approvalIssueToken.AuditExceptionFragment
import com.violas.wallet.ui.governorApproval.approvalIssueToken.AuditIssueTokenFragment
import com.violas.wallet.ui.governorApproval.approvalMintToken.MintTokenSuccessFragment
import com.violas.wallet.ui.governorApproval.approvalMintToken.MintTokenToSSOFragment
import com.violas.wallet.ui.main.message.SSOApplicationMsgVO
import kotlinx.android.synthetic.main.activity_governor_approval.*

/**
 * Created by elephant on 2020/3/4 14:50.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 州长审批页面，如果申请状态不为未审批，则只是展示SSO申请信息
 */
class GovernorApprovalActivity : BaseAppActivity() {

    companion object {

        fun start(context: Context, msgVO: SSOApplicationMsgVO) {
            Intent(context, GovernorApprovalActivity::class.java)
                .apply { putExtra(EXTRA_KEY_SSO_MSG, msgVO) }
                .start(context)
        }

        fun start(context: Context, details: SSOApplicationDetailsDTO) {
            start(
                context,
                SSOApplicationMsgVO(
                    applicationId = details.applicationId,
                    applicationStatus = details.applicationStatus,
                    applicationDate = details.applicationDate,
                    expirationDate = details.expirationDate,
                    applicantIdName = details.idName,
                    msgRead = true
                )
            )
        }
    }

    private lateinit var mSSOApplicationMsgVO: SSOApplicationMsgVO

    private val mViewModel by lazy {
        ViewModelProvider(this, ApprovalActivityViewModelFactory(mSSOApplicationMsgVO))
            .get(ApprovalActivityViewModel::class.java)
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_governor_approval
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
        outState.putParcelable(EXTRA_KEY_SSO_MSG, mSSOApplicationMsgVO)
    }

    private fun initData(savedInstanceState: Bundle?): Boolean {
        var msgVO: SSOApplicationMsgVO? = null
        if (savedInstanceState != null) {
            msgVO = savedInstanceState.getParcelable(EXTRA_KEY_SSO_MSG)
        } else if (intent != null) {
            msgVO = intent.getParcelableExtra(EXTRA_KEY_SSO_MSG)
        }

        if (msgVO == null) {
            return false
        }

        mSSOApplicationMsgVO = msgVO
        return true
    }

    private fun initView() {
        /*title =
            getString(R.string.title_sso_msg_issuing_token, mSSOApplicationMsgVO.applicantIdName)*/

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
            if (it == null) {
                showToast(R.string.tips_not_found_sso_application_details)
                close()
                return@Observer
            }

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

    private fun loadFragment(details: SSOApplicationDetailsDTO) {
        val fragment = when (details.applicationStatus) {
            SSOApplicationState.APPLYING_ISSUE_TOKEN -> {
                AuditIssueTokenFragment()
            }

            SSOApplicationState.APPLYING_MINTABLE,
            SSOApplicationState.GIVEN_MINTABLE,
            SSOApplicationState.TRANSFERRED_AND_NOTIFIED,
            SSOApplicationState.CHAIRMAN_UNAPPROVED -> {
                ApplyForMintableProgressFragment()
            }

            SSOApplicationState.APPLYING_MINT_TOKEN -> {
                MintTokenToSSOFragment()
            }

            SSOApplicationState.MINTED_TOKEN -> {
                MintTokenSuccessFragment()
            }

            else -> {
                AuditExceptionFragment()
            }
        }.apply {
            arguments = Bundle().apply { putParcelable(KEY_ONE, details) }
        }

        loadRootFragment(R.id.flFragmentContainer, fragment)
    }
}