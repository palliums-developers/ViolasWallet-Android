package com.violas.wallet.ui.main.me

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.lifecycle.Observer
import com.palliums.base.BaseFragment
import com.palliums.net.LoadState
import com.palliums.utils.getColor
import com.palliums.widget.MenuItemView
import com.violas.wallet.R
import com.violas.wallet.biz.WalletType
import com.violas.wallet.event.RefreshGovernorApplicationProgressEvent
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.governor.GovernorInfoDTO
import com.violas.wallet.repository.local.user.AccountBindingStatus
import com.violas.wallet.repository.local.user.IDAuthenticationStatus
import com.violas.wallet.ui.addressBook.AddressBookActivity
import com.violas.wallet.ui.authentication.IDAuthenticationActivity
import com.violas.wallet.ui.authentication.IDInformationActivity
import com.violas.wallet.ui.setting.SettingActivity
import com.violas.wallet.ui.verification.EmailVerificationActivity
import com.violas.wallet.ui.verification.PhoneVerificationActivity
import com.violas.wallet.utils.showPwdInputDialog
import kotlinx.android.synthetic.main.fragment_me.*
import org.greenrobot.eventbus.EventBus
import org.palliums.violascore.wallet.Account

/**
 * 我的页面
 */
class MeFragment : BaseFragment() {

    companion object {
        private const val REQUEST_CODE_BIND_PHONE = 0x11
        private const val REQUEST_CODE_BIND_EMAIL = 0x22
        private const val REQUEST_CODE_AUTHENTICATION_ID = 0x33
    }

    private var mShowedApplyForLicenceDialogFlag = false

    private val mViewModel by lazy {
        requireActivity().provideUserViewModel()
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_me
    }

    override fun onSupportVisible() {
        super.onSupportVisible()
        setStatusBarMode(false)
        showApplyForLicenceDialog(
            mViewModel.mCurrentAccountLD.value,
            mViewModel.mGovernorInfoLD.value
        )
    }

    override fun onLazyInitView(savedInstanceState: Bundle?) {
        super.onLazyInitView(savedInstanceState)

        mivIDAuthentication.setOnClickListener(this)
        mivPhoneVerification.setOnClickListener(this)
        mivEmailVerification.setOnClickListener(this)
        mivAddressBook.setOnClickListener(this)
        mivSettings.setOnClickListener(this)

        mViewModel.mCurrentAccountLD.observe(viewLifecycleOwner, Observer {
            if (it.walletType == WalletType.Governor.type) {
                tvWalletTypeName.setText(R.string.title_governor_wallet)
                showGovernorHeaderView(it, mViewModel.mGovernorInfoLD.value)
                showApplyForLicenceDialog(it, mViewModel.mGovernorInfoLD.value)
            } else {
                tvWalletTypeName.setText(R.string.title_sso_wallet)
                showSSOHeaderView()
            }

            mViewModel.init()
        })

        mViewModel.mGovernorInfoLD.observe(viewLifecycleOwner, Observer {
            showGovernorHeaderView(mViewModel.mCurrentAccountLD.value, it)
            showApplyForLicenceDialog(mViewModel.mCurrentAccountLD.value, it)
        })

        mViewModel.tipsMessage.observe(viewLifecycleOwner, Observer {
            it.getDataIfNotHandled()?.let { msg ->
                if (msg.isNotEmpty()) {
                    showToast(msg)
                }
            }
        })

        mViewModel.getIdInfoLiveData().observe(viewLifecycleOwner, Observer {
            when (it.first.idAuthenticationStatus) {
                IDAuthenticationStatus.UNKNOWN -> {
                    mivIDAuthentication.showEndArrow(false)
                    mivIDAuthentication.setEndDescText("")
                }

                IDAuthenticationStatus.AUTHENTICATED -> {
                    mivIDAuthentication.showEndArrow(true)
                    mivIDAuthentication.setEndDescText(R.string.desc_authenticated)
                    mivIDAuthentication.setEndDescTextColor(getColor(R.color.def_text_title))
                }

                else -> {
                    mivIDAuthentication.showEndArrow(true)
                    mivIDAuthentication.setEndDescText(R.string.desc_unauthorized)
                    mivIDAuthentication.setEndDescTextColor(getColor(R.color.def_text_warn))
                }
            }

            handleLoadState(pbIDAuthenticationLoading, it.second, mivIDAuthentication)
        })

        mViewModel.getPhoneInfoLiveData().observe(viewLifecycleOwner, Observer {
            when (it.first.accountBindingStatus) {
                AccountBindingStatus.UNKNOWN -> {
                    mivPhoneVerification.showEndArrow(false)
                    mivPhoneVerification.setEndDescText("")
                }

                AccountBindingStatus.BOUND -> {
                    mivPhoneVerification.showEndArrow(false)
                    mivPhoneVerification.setEndDescText(it.first.phoneNumber)
                    mivPhoneVerification.setEndDescTextColor(getColor(R.color.def_text_title))
                }

                else -> {
                    mivPhoneVerification.showEndArrow(true)
                    mivPhoneVerification.setEndDescText(R.string.desc_unbound)
                    mivPhoneVerification.setEndDescTextColor(getColor(R.color.def_text_warn))
                }
            }

            handleLoadState(pbPhoneVerificationLoading, it.second, mivPhoneVerification)
        })

        mViewModel.getEmailInfoLiveData().observe(viewLifecycleOwner, Observer {
            when (it.first.accountBindingStatus) {
                AccountBindingStatus.UNKNOWN -> {
                    mivEmailVerification.showEndArrow(false)
                    mivEmailVerification.setEndDescText("")
                }

                AccountBindingStatus.BOUND -> {
                    mivEmailVerification.showEndArrow(false)
                    mivEmailVerification.setEndDescText(it.first.emailAddress)
                    mivEmailVerification.setEndDescTextColor(getColor(R.color.def_text_title))
                }

                else -> {
                    mivEmailVerification.showEndArrow(true)
                    mivEmailVerification.setEndDescText(R.string.desc_unbound)
                    mivEmailVerification.setEndDescTextColor(getColor(R.color.def_text_warn))
                }
            }

            handleLoadState(pbEmailVerificationLoading, it.second, mivEmailVerification)
        })
    }

    private fun handleLoadState(
        progressBar: ProgressBar,
        loadState: LoadState,
        menuItemView: MenuItemView? = null
    ) {
        when (loadState.status) {
            LoadState.Status.IDLE -> {
                // ignore
            }

            LoadState.Status.RUNNING -> {
                progressBar.visibility = View.VISIBLE
                menuItemView?.let {
                    it.showEndArrow(false)
                    it.setEndDescText("")
                }
            }

            else -> {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun showGovernorHeaderView(account: AccountDO?, governorInfo: GovernorInfoDTO?) {
        if (account == null
            || governorInfo == null
            || account.address != governorInfo.walletAddress
        ) {
            return
        }

        if (governorInfo.applicationStatus == 4) {
            // 4: minted
            tvNickname.text = account.walletNickname
            tvNickname.setTextColor(getColor(R.color.white))
            tvNickname.visibility = View.VISIBLE
            nivAvatar.setImageResource(R.drawable.ic_avatar_governor_default)
            nivAvatar.visibility = View.VISIBLE
            return
        }

        tvNickname.setTextColor(getColor(R.color.def_text_warn))
        tvNickname.visibility = View.VISIBLE
        nivAvatar.visibility = View.GONE
        tvNickname.setText(
            when (governorInfo.applicationStatus) {
                0 -> {  // 0: not approved
                    R.string.state_approving
                }

                1 -> {  // 1: pass
                    R.string.state_approving
                }

                2 -> {  // 2: not pass
                    R.string.state_not_pass
                }

                3 -> {  // 3: published
                    R.string.desc_governor_application_published
                }

                else -> {  // -1: no application
                    R.string.state_not_pass
                }
            }
        )
    }

    private fun showSSOHeaderView() {
        tvNickname.text = "SSO"
        tvNickname.setTextColor(getColor(R.color.white))
        tvNickname.visibility = View.VISIBLE
        nivAvatar.setImageResource(R.drawable.ic_avatar_sso_default)
        nivAvatar.visibility = View.VISIBLE
    }

    private fun showApplyForLicenceDialog(account: AccountDO?, governorInfo: GovernorInfoDTO?) {
        if (account == null
            || governorInfo == null
            || governorInfo.applicationStatus != 1
            || account.address != governorInfo.walletAddress
            || mShowedApplyForLicenceDialogFlag
            || !isSupportVisible
        ) {
            return
        }

        mShowedApplyForLicenceDialogFlag = true
        ApplyForLicenceDialog()
            .setApplyForCallback { applyForLicenceDialog ->
                applyForLicenceDialog.close()

                showPwdInputDialog(
                    mViewModel.mCurrentAccountLD.value!!,
                    accountCallback = {
                        publishContract(it)
                    })
            }
            .show()
    }

    private fun publishContract(account: Account) {
        mViewModel.execute(
            account,
            action = UserViewModel.ACTION_PUBLISH_CONTRACT,
            failureCallback = {
                dismissProgress()
            }
        ) {
            EventBus.getDefault().post(RefreshGovernorApplicationProgressEvent())
            dismissProgress()
            showToast(R.string.tips_apply_for_licence_success)
        }
    }

    override fun onViewClick(view: View) {
        when (view.id) {
            R.id.mivIDAuthentication -> {
                val idInfo = mViewModel.getIdInfo() ?: return
                when (idInfo.idAuthenticationStatus) {
                    IDAuthenticationStatus.UNKNOWN -> {
                        mViewModel.execute(checkNetworkBeforeExecute = false)
                    }

                    IDAuthenticationStatus.AUTHENTICATED -> {
                        startActivity(
                            Intent(
                                requireActivity(),
                                IDInformationActivity::class.java
                            )
                        )
                    }

                    else -> {
                        startActivityForResult(
                            Intent(
                                requireActivity(),
                                IDAuthenticationActivity::class.java
                            ),
                            REQUEST_CODE_AUTHENTICATION_ID
                        )
                    }
                }
            }

            R.id.mivPhoneVerification -> {
                val phoneInfo = mViewModel.getPhoneInfo() ?: return
                when (phoneInfo.accountBindingStatus) {
                    AccountBindingStatus.UNKNOWN -> {
                        mViewModel.execute(checkNetworkBeforeExecute = false)
                    }

                    AccountBindingStatus.UNBOUND -> {
                        startActivityForResult(
                            Intent(
                                requireActivity(),
                                PhoneVerificationActivity::class.java
                            ),
                            REQUEST_CODE_BIND_PHONE
                        )
                    }
                }
            }

            R.id.mivEmailVerification -> {
                val emailInfo = mViewModel.getEmailInfo() ?: return
                when (emailInfo.accountBindingStatus) {
                    AccountBindingStatus.UNKNOWN -> {
                        mViewModel.execute(checkNetworkBeforeExecute = false)
                    }

                    AccountBindingStatus.UNBOUND -> {
                        startActivityForResult(
                            Intent(
                                requireActivity(),
                                EmailVerificationActivity::class.java
                            ),
                            REQUEST_CODE_BIND_EMAIL
                        )
                    }
                }
            }

            R.id.mivAddressBook -> {
                AddressBookActivity.start(requireActivity())
            }

            R.id.mivSettings -> {
                startActivity(Intent(requireActivity(), SettingActivity::class.java))
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_BIND_PHONE -> {
                if (resultCode == Activity.RESULT_OK) {
                    showToast(R.string.hint_phone_number_bind_success)
                }
            }

            REQUEST_CODE_BIND_EMAIL -> {
                if (resultCode == Activity.RESULT_OK) {
                    showToast(R.string.hint_email_bind_success)
                }
            }

            REQUEST_CODE_AUTHENTICATION_ID -> {
                if (resultCode == Activity.RESULT_OK) {
                    showToast(R.string.hint_id_authentication_success)
                }
            }
        }
    }
}