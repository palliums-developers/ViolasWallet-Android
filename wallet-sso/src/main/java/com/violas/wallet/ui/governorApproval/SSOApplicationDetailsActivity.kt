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
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.image.GlideApp
import com.violas.wallet.repository.http.governor.SSOApplicationDetailsDTO
import com.violas.wallet.ui.governorApproval.SSOApplicationDetailsViewModel.Companion.ACTION_APPROVAL_APPLICATION
import com.violas.wallet.ui.governorApproval.SSOApplicationDetailsViewModel.Companion.ACTION_LOAD_APPLICATION_DETAILS
import com.violas.wallet.ui.main.message.SSOApplicationMsgVO
import com.violas.wallet.utils.convertViolasTokenUnit
import com.violas.wallet.widget.dialog.PasswordInputDialog
import kotlinx.android.synthetic.main.activity_sso_application_details.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.palliums.libracore.wallet.KeyPair
import org.palliums.violascore.wallet.Account

/**
 * Created by elephant on 2020/3/4 14:50.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: SSO发币申请详情页面
 */
class SSOApplicationDetailsActivity : BaseAppActivity() {

    companion object {
        private const val EXTRA_KEY_SSO_MSG = "EXTRA_KEY_SSO_MSG"

        fun start(context: Context, msg: SSOApplicationMsgVO) {
            Intent(context, SSOApplicationDetailsActivity::class.java)
                .apply { putExtra(EXTRA_KEY_SSO_MSG, msg) }
                .start(context)
        }
    }

    private lateinit var mSSOApplicationMsg: SSOApplicationMsgVO

    private val mViewModel by lazy {
        ViewModelProvider(this, SSOApplicationDetailsViewModelFactory(mSSOApplicationMsg))
            .get(SSOApplicationDetailsViewModel::class.java)
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_sso_application_details
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
        outState.putParcelable(EXTRA_KEY_SSO_MSG, mSSOApplicationMsg)
    }

    private fun initData(savedInstanceState: Bundle?): Boolean {
        var msg: SSOApplicationMsgVO? = null
        if (savedInstanceState != null) {
            msg = savedInstanceState.getParcelable(EXTRA_KEY_SSO_MSG)
        } else if (intent != null) {
            msg = intent.getParcelableExtra(EXTRA_KEY_SSO_MSG)
        }

        if (msg == null) {
            return false
        }

        mSSOApplicationMsg = msg
        return true
    }

    private fun initView() {
        title = getString(R.string.title_sso_msg_issuing_token, mSSOApplicationMsg.applicantIdName)

        dslStatusLayout.showStatus(IStatusLayout.Status.STATUS_LOADING)
        srlRefreshLayout.isEnabled = false
        srlRefreshLayout.setOnRefreshListener {
            loadApplicationDetails()
        }

        btnApprovalPass.setOnClickListener {
            showPasswordInputDialog(true)
        }
        tvApprovalNotPass.setOnClickListener {
            showPasswordInputDialog(false)
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

    private fun showPasswordInputDialog(pass: Boolean) {
        PasswordInputDialog()
            .setConfirmListener { password, dialogFragment ->
                dialogFragment.dismiss()
                showProgress()

                launch(Dispatchers.Main) {
                    var account: Account? = null
                    var mnemonics: List<String>? = null
                    withContext(Dispatchers.IO) {
                        val simpleSecurity = SimpleSecurity.instance(applicationContext)
                        val privateKey = simpleSecurity.decrypt(
                            password, mViewModel.mAccountLD.value!!.privateKey
                        )
                        if (privateKey != null) {
                            account = Account(KeyPair.fromSecretKey(privateKey))
                        }

                        val mnemonicByteArray = simpleSecurity.decrypt(
                            password, mViewModel.mAccountLD.value!!.mnemonic
                        )
                        if (mnemonicByteArray != null) {
                            val mnemonicStr = String(mnemonicByteArray)
                            mnemonics = mnemonicStr.substring(1, mnemonicStr.length - 1)
                                .split(",")
                                .map { it.trim() }
                        }
                    }

                    if (account == null || mnemonics == null) {
                        dismissProgress()
                        showToast(getString(R.string.hint_password_error))
                        return@launch
                    }

                    approvalApplication(pass, account!!, mnemonics!!)
                }
            }
            .show(supportFragmentManager)
    }

    private fun approvalApplication(
        pass: Boolean,
        account: Account,
        mnemonics: List<String>
    ) {
        mViewModel.execute(
            pass, account, mnemonics,
            action = ACTION_APPROVAL_APPLICATION,
            failureCallback = {
                dismissProgress()
            },
            successCallback = {
                dismissProgress()
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
        val phoneAreaCode = if (details.phoneAreaCode.startsWith("+"))
            details.phoneAreaCode
        else
            "+${details.phoneAreaCode}"
        asivPhoneNumber.setContent("$phoneAreaCode ${details.phoneNumber}")
        asivEmail.setContent(details.emailAddress)

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