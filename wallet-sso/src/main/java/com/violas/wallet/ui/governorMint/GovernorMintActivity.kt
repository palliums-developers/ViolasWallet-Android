package com.violas.wallet.ui.governorMint

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.palliums.net.RequestException
import com.palliums.utils.start
import com.palliums.widget.status.IStatusLayout
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.common.EXTRA_KEY_SSO_MSG
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.repository.http.governor.SSOApplicationDetailsDTO
import com.violas.wallet.ui.governorMint.GovernorMintViewModel.Companion.ACTION_LOAD_APPLICATION_DETAILS
import com.violas.wallet.ui.governorMint.GovernorMintViewModel.Companion.ACTION_MINT_TOKEN
import com.violas.wallet.ui.main.message.SSOApplicationMsgVO
import com.violas.wallet.utils.convertViolasTokenUnit
import com.violas.wallet.widget.dialog.PasswordInputDialog
import kotlinx.android.synthetic.main.activity_governor_mint.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.palliums.libracore.wallet.KeyPair
import org.palliums.violascore.wallet.Account

/**
 * Created by elephant on 2020/3/5 12:46.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 州长铸币页面
 */
class GovernorMintActivity : BaseAppActivity() {

    companion object {

        fun start(context: Context, msg: SSOApplicationMsgVO) {
            Intent(context, GovernorMintActivity::class.java)
                .apply { putExtra(EXTRA_KEY_SSO_MSG, msg) }
                .start(context)
        }
    }

    private lateinit var mSSOApplicationMsg: SSOApplicationMsgVO

    private val mViewModel by lazy {
        ViewModelProvider(this, GovernorMintViewModelFactory(mSSOApplicationMsg))
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
        title = getString(R.string.title_sso_msg_mint_token, mSSOApplicationMsg.applicantIdName)

        dslStatusLayout.showStatus(IStatusLayout.Status.STATUS_LOADING)
        srlRefreshLayout.isEnabled = false
        srlRefreshLayout.setOnRefreshListener {
            loadApplicationDetails()
        }

        btnMint.setOnClickListener {
            showPasswordInputDialog()
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

            if (it.applicationStatus != mSSOApplicationMsg.applicationStatus) {
                // 如果发现状态不一样，需要更新消息首页的消息状态
                mSSOApplicationMsg.applicationStatus = it.applicationStatus
                EventBus.getDefault().post(mSSOApplicationMsg)
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

    private fun showPasswordInputDialog() {
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

                    mintToken(account!!, mnemonics!!)
                }
            }
            .show(supportFragmentManager)
    }

    private fun mintToken(
        account: Account,
        mnemonics: List<String>
    ) {
        mViewModel.execute(
            account, mnemonics,
            action = ACTION_MINT_TOKEN,
            failureCallback = {
                dismissProgress()
            },
            successCallback = {
                dismissProgress()
                showToast(
                    getString(
                        R.string.tips_governor_mint_token_success,
                        mSSOApplicationMsg.applicantIdName
                    ),
                    Toast.LENGTH_LONG
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