package com.violas.wallet.ui.governorApproval.approvalIssueToken

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseActivity
import com.palliums.base.BaseFragment
import com.palliums.net.LoadState
import com.palliums.utils.formatDate
import com.violas.wallet.R
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.image.viewImage
import com.violas.wallet.repository.http.governor.SSOApplicationDetailsDTO
import com.violas.wallet.ui.governorApproval.ApprovalFragmentViewModel
import com.violas.wallet.ui.governorApproval.ApprovalFragmentViewModelFactory
import com.violas.wallet.ui.governorApproval.GovernorApprovalActivity
import com.violas.wallet.utils.convertViolasTokenUnit
import kotlinx.android.synthetic.main.layout_approval_issue_token_info.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Created by elephant on 2020/4/27 17:54.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 州长审批SSO发币申请的 base fragment
 */
abstract class BaseApprovalIssueTokenFragment : BaseFragment() {

    protected lateinit var mSSOApplicationDetails: SSOApplicationDetailsDTO

    protected val mViewModel by lazy {
        ViewModelProvider(
            this,
            ApprovalFragmentViewModelFactory(mSSOApplicationDetails)
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
        (activity as? BaseActivity)?.title =
            getString(R.string.title_sso_msg_issuing_token, mSSOApplicationDetails.idName)
        setApplicationInfo(mSSOApplicationDetails)
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

        uivReservePhoto.setOnClickListener {
            uivReservePhoto.getContentImageView()?.let {
                it.viewImage(mSSOApplicationDetails.reservePhotoUrl)
            }
        }
        uivBankChequePhotoPositive.setOnClickListener {
            uivBankChequePhotoPositive.getContentImageView()?.let {
                it.viewImage(mSSOApplicationDetails.bankChequePhotoPositiveUrl)
            }
        }
        uivBankChequePhotoBack.setOnClickListener {
            uivBankChequePhotoBack.getContentImageView()?.let {
                it.viewImage(mSSOApplicationDetails.bankChequePhotoBackUrl)
            }
        }
    }

    protected open fun setApplicationInfo(details: SSOApplicationDetailsDTO) {
        asivFiatCurrencyType.setContent(details.fiatCurrencyType)
        asivTokenAmount.setContent(convertViolasTokenUnit(details.tokenAmount))
        asivTokenValue.setContent("${details.tokenValue}${details.fiatCurrencyType}")
        asivTokenName.setContent(details.tokenName)
        asivSSOWalletAddress.setContent(details.issuerWalletAddress)
        asivApplicationDate.setContent(
            formatDate(details.applicationDate, pattern = "yyyy.MM.dd HH:mm")
        )
        asivApplicationPeriod.setContent(getString(R.string.format_days, details.applicationPeriod))
        asivExpirationDate.setContent(
            formatDate(details.expirationDate, pattern = "yyyy.MM.dd HH:mm")
        )

        asivIDName.setContent(details.idName)
        asivNationality.setContent(details.countryName)
        // TODO 证件类型没有区分
        asivIDType.setContent(getString(R.string.id_card))
        asivIDNumber.setContent(details.idNumber)

        /*val phoneAreaCode = if (details.phoneAreaCode.startsWith("+"))
            details.phoneAreaCode
        else
            "+${details.phoneAreaCode}"*/
        val phoneEndIndex = if (details.phoneNumber.length > 7)
            7
        else
            details.phoneNumber.length
        val phoneStartIndex = if (phoneEndIndex >= 3) 3 else phoneEndIndex
        var phoneReplacement = ""
        for (index in phoneStartIndex until phoneEndIndex) {
            phoneReplacement += "*"
        }
        val phoneNumber = details.phoneNumber.replaceRange(
            phoneStartIndex, phoneEndIndex, phoneReplacement
        )
        //asivPhoneNumber.setContent("$phoneAreaCode $phoneNumber")
        asivPhoneNumber.setContent(phoneNumber)

        val emailEndIndex = details.emailAddress.indexOf("@")
        val emailStartIndex = if (emailEndIndex >= 3) 3 else emailEndIndex
        var emailReplacement = ""
        for (index in emailStartIndex until emailEndIndex) {
            emailReplacement += "*"
        }
        val emailAddress = details.emailAddress.replaceRange(
            emailStartIndex, emailEndIndex, emailReplacement
        )
        asivEmail.setContent(emailAddress)

        uivReservePhoto.setContentImage(details.reservePhotoUrl, true)
        uivBankChequePhotoPositive.setContentImage(details.bankChequePhotoPositiveUrl, true)
        uivBankChequePhotoBack.setContentImage(details.bankChequePhotoBackUrl, true)
    }

    protected fun startNewApprovalActivity() {
        context?.let {
            GovernorApprovalActivity.start(it, mSSOApplicationDetails)
            launch(Dispatchers.IO) {
                delay(500)
                close()
            }
        }
    }
}