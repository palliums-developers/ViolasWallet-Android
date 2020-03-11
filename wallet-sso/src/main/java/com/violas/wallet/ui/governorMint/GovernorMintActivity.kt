package com.violas.wallet.ui.governorMint

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.palliums.net.RequestException
import com.palliums.utils.start
import com.palliums.widget.status.IStatusLayout
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.common.EXTRA_KEY_SSO_MSG
import com.violas.wallet.repository.http.governor.SSOApplicationDetailsDTO
import com.violas.wallet.ui.governorMint.GovernorMintViewModel.Companion.ACTION_LOAD_APPLICATION_DETAILS
import com.violas.wallet.ui.governorMint.GovernorMintViewModel.Companion.ACTION_MINT_TOKEN_TO_SSO_ACCOUNT
import com.violas.wallet.ui.main.message.SSOApplicationMsgVO
import com.violas.wallet.utils.convertViolasTokenUnit
import com.violas.wallet.utils.showPwdInputDialog
import kotlinx.android.synthetic.main.activity_governor_mint.*
import org.palliums.violascore.wallet.Account

/**
 * Created by elephant on 2020/3/5 12:46.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 州长铸币页面
 */
class GovernorMintActivity : BaseAppActivity() {

    companion object {

        fun start(context: Context, msgVO: SSOApplicationMsgVO) {
            Intent(context, GovernorMintActivity::class.java)
                .apply { putExtra(EXTRA_KEY_SSO_MSG, msgVO) }
                .start(context)
        }
    }

    private lateinit var mSSOApplicationMsgVO: SSOApplicationMsgVO

    private val mViewModel by lazy {
        ViewModelProvider(this, GovernorMintViewModelFactory(mSSOApplicationMsgVO))
            .get(GovernorMintViewModel::class.java)
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_governor_mint
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
        title = getString(R.string.title_sso_msg_mint_token, mSSOApplicationMsgVO.applicantIdName)

        dslStatusLayout.showStatus(IStatusLayout.Status.STATUS_LOADING)
        srlRefreshLayout.isEnabled = false
        srlRefreshLayout.setOnRefreshListener {
            loadApplicationDetails()
        }

        btnMint.setOnClickListener {
            showPwdInputDialog(
                mViewModel.mAccountLD.value!!,
                accountMnemonicCallback = { account, mnemonics ->
                    mintTokenToSSOAccount(account, mnemonics)
                })
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

            fillApplicationInfo(it)
        })

        mViewModel.mAccountLD.observe(this, Observer {
            loadApplicationDetails()
        })
    }

    private fun loadApplicationDetails() {
        mViewModel.execute(
            action = ACTION_LOAD_APPLICATION_DETAILS,
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
            },
            successCallback = {
                if (srlRefreshLayout.isRefreshing) {
                    srlRefreshLayout.isRefreshing = false
                }
                srlRefreshLayout.isEnabled = false

                llContentLayout.visibility = View.VISIBLE
                dslStatusLayout.showStatus(IStatusLayout.Status.STATUS_NONE)
            })
    }

    private fun mintTokenToSSOAccount(
        account: Account,
        mnemonics: List<String>
    ) {
        mViewModel.execute(
            account, mnemonics,
            action = ACTION_MINT_TOKEN_TO_SSO_ACCOUNT,
            failureCallback = {
                dismissProgress()
            },
            successCallback = {
                dismissProgress()
                showToast(
                    getString(
                        R.string.tips_governor_mint_token_success,
                        mSSOApplicationMsgVO.applicantIdName
                    )
                )
                close()
            }
        )
    }

    private fun fillApplicationInfo(details: SSOApplicationDetailsDTO) {
        asivSSOWalletAddress.setContent(details.ssoWalletAddress)
        asivTokenName.setContent(details.tokenName)
        asivTokenAmount.setContent(convertViolasTokenUnit(details.tokenAmount))
    }
}