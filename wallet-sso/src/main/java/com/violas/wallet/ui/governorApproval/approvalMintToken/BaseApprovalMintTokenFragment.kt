package com.violas.wallet.ui.governorApproval.approvalMintToken

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseActivity
import com.palliums.base.BaseFragment
import com.palliums.net.LoadState
import com.violas.wallet.R
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.repository.http.governor.SSOApplicationDetailsDTO
import com.violas.wallet.ui.governorApproval.ApprovalFragmentViewModel
import com.violas.wallet.ui.governorApproval.ApprovalFragmentViewModelFactory
import com.violas.wallet.ui.governorApproval.GovernorApprovalActivity
import com.violas.wallet.utils.convertViolasTokenUnit
import kotlinx.android.synthetic.main.layout_approval_mint_token_info.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Created by elephant on 2020/4/27 17:54.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 州长审批SSO铸币申请的 base fragment
 */
abstract class BaseApprovalMintTokenFragment : BaseFragment() {

    protected lateinit var mSSOApplicationDetailsDTO: SSOApplicationDetailsDTO

    protected val mViewModel by lazy {
        ViewModelProvider(
            this,
            ApprovalFragmentViewModelFactory(mSSOApplicationDetailsDTO)
        ).get(ApprovalFragmentViewModel::class.java)
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
        outState.putParcelable(KEY_ONE, mSSOApplicationDetailsDTO)
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

        mSSOApplicationDetailsDTO = detailsDTO
        return true
    }

    protected open fun initView() {
        (activity as? BaseActivity)?.title =
            getString(R.string.title_sso_msg_issuing_token, mSSOApplicationDetailsDTO.idName)
        setApplicationInfo(mSSOApplicationDetailsDTO)
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

    protected open fun setApplicationInfo(details: SSOApplicationDetailsDTO) {
        asivSSOWalletAddress.setContent(details.issuerWalletAddress)
        asivTokenName.setContent(details.tokenName)
        asivTokenAmount.setContent(convertViolasTokenUnit(details.tokenAmount))
    }

    protected fun startNewApprovalActivity() {
        context?.let {
            GovernorApprovalActivity.start(it, mSSOApplicationDetailsDTO)
            launch(Dispatchers.IO) {
                delay(500)
                close()
            }
        }
    }
}