package com.violas.wallet.ui.governorApproval

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.palliums.net.RequestException
import com.palliums.utils.formatDate
import com.palliums.utils.start
import com.palliums.widget.status.IStatusLayout
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.common.EXTRA_KEY_SSO_MSG
import com.violas.wallet.image.GlideApp
import com.violas.wallet.repository.http.governor.SSOApplicationDetailsDTO
import com.violas.wallet.ui.governorApproval.GovernorApprovalViewModel.Companion.ACTION_APPROVAL_APPLICATION
import com.violas.wallet.ui.governorApproval.GovernorApprovalViewModel.Companion.ACTION_LOAD_APPLICATION_DETAILS
import com.violas.wallet.ui.main.message.SSOApplicationMsgVO
import com.violas.wallet.utils.convertViolasTokenUnit
import com.violas.wallet.utils.showPwdInputDialog
import kotlinx.android.synthetic.main.activity_governor_approval.*
import org.palliums.violascore.wallet.Account

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
    }

    private lateinit var mSSOApplicationMsgVO: SSOApplicationMsgVO

    private val mViewModel by lazy {
        ViewModelProvider(this, GovernorApprovalViewModelFactory(mSSOApplicationMsgVO))
            .get(GovernorApprovalViewModel::class.java)
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
        title =
            getString(R.string.title_sso_msg_issuing_token, mSSOApplicationMsgVO.applicantIdName)

        dslStatusLayout.showStatus(IStatusLayout.Status.STATUS_LOADING)
        srlRefreshLayout.isEnabled = false
        srlRefreshLayout.setOnRefreshListener {
            loadApplicationDetails()
        }

        btnApprovalPass.setOnClickListener {
            showPwdInputDialog(
                mViewModel.mAccountLD.value!!,
                accountCallback = {
                    approvalApplication(true, it)
                })
        }
        tvApprovalNotPass.setOnClickListener {
            showPwdInputDialog(
                mViewModel.mAccountLD.value!!,
                accountCallback = {
                    approvalApplication(false)
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

                nsvContentLayout.visibility = View.VISIBLE
                dslStatusLayout.showStatus(IStatusLayout.Status.STATUS_NONE)
            })
    }

    private fun approvalApplication(
        pass: Boolean,
        account: Account? = null
    ) {
        val params = if (pass) {
            arrayOf(pass, account!!)
        } else {
            arrayOf(pass)
        }

        mViewModel.execute(
            params = *params,
            action = ACTION_APPROVAL_APPLICATION,
            failureCallback = {
                dismissProgress()
            },
            successCallback = {
                dismissProgress()
                showToast(
                    getString(
                        if (pass)
                            R.string.tips_governor_approval_pass_success
                        else
                            R.string.tips_governor_approval_not_pass_success
                        ,
                        mSSOApplicationMsgVO.applicantIdName
                    )
                )
                close()
            }
        )
    }

    private fun fillApplicationInfo(details: SSOApplicationDetailsDTO) {
        tvApplicationTitle.text =
            getString(R.string.format_sso_application_title, details.idName, details.tokenName)
        when (details.applicationStatus) {
            0 -> {
                tvApplicationStatus.visibility = View.GONE
                llApprovalLayout.visibility = View.VISIBLE
            }

            1 -> {
                llApprovalLayout.visibility = View.GONE
                tvApplicationStatus.visibility = View.VISIBLE
                tvApplicationStatus.setText(R.string.state_passed)
            }

            2 -> {
                llApprovalLayout.visibility = View.GONE
                tvApplicationStatus.visibility = View.VISIBLE
                tvApplicationStatus.setText(R.string.state_rejected)
            }

            3 -> {
                tvApplicationStatus.visibility = View.GONE
                llApprovalLayout.visibility = View.GONE
            }

            4 -> {
                llApprovalLayout.visibility = View.GONE
                tvApplicationStatus.visibility = View.VISIBLE
                tvApplicationStatus.setText(R.string.state_minted)
            }

            else -> {
                llApprovalLayout.visibility = View.GONE
                tvApplicationStatus.visibility = View.GONE
            }
        }

        asivFiatCurrencyType.setContent(details.fiatCurrencyType)
        asivTokenAmount.setContent(convertViolasTokenUnit(details.tokenAmount))
        asivTokenValue.setContent("${details.tokenValue}${details.fiatCurrencyType}")
        asivTokenName.setContent(details.tokenName)
        asivSSOWalletAddress.setContent(details.ssoWalletAddress)
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

        GlideApp.with(this)
            .load(details.reservePhotoUrl)
            .centerCrop()
            .transition(DrawableTransitionOptions.withCrossFade())
            .placeholder(R.drawable.bg_id_card_front)
            .error(R.drawable.bg_id_card_front)
            .into(ivReservePhoto)
        GlideApp.with(this)
            .load(details.bankChequePhotoPositiveUrl)
            .centerCrop()
            .transition(DrawableTransitionOptions.withCrossFade())
            .placeholder(R.drawable.bg_id_card_front)
            .error(R.drawable.bg_id_card_front)
            .into(ivBankChequePhotoPositive)
        GlideApp.with(this)
            .load(details.bankChequePhotoBackUrl)
            .centerCrop()
            .transition(DrawableTransitionOptions.withCrossFade())
            .placeholder(R.drawable.bg_id_card_front)
            .error(R.drawable.bg_id_card_front)
            .into(ivBankChequePhotoBack)
    }
}