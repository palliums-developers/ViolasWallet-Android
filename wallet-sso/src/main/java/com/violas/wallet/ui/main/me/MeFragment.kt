package com.violas.wallet.ui.main.me

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import com.palliums.base.BaseFragment
import com.palliums.utils.getColor
import com.violas.wallet.R
import com.violas.wallet.repository.local.user.AccountBindingStatus
import com.violas.wallet.repository.local.user.IDAuthenticationStatus
import com.violas.wallet.ui.addressBook.AddressBookActivity
import com.violas.wallet.ui.authentication.IDAuthenticationActivity
import com.violas.wallet.ui.authentication.IDInformationActivity
import com.violas.wallet.ui.main.UserViewModel
import com.violas.wallet.ui.main.provideUserViewModel
import com.violas.wallet.ui.setting.SettingActivity
import com.violas.wallet.ui.verification.EmailVerificationActivity
import com.violas.wallet.ui.verification.PhoneVerificationActivity
import kotlinx.android.synthetic.main.fragment_me.*

/**
 * 我的页面
 */
class MeFragment : BaseFragment() {

    private lateinit var mViewModel: UserViewModel

    override fun getLayoutResId(): Int {
        return R.layout.fragment_me
    }

    override fun onLazyInitView(savedInstanceState: Bundle?) {
        super.onLazyInitView(savedInstanceState)

        mivIDAuthentication.setOnClickListener(this)
        mivPhoneVerification.setOnClickListener(this)
        mivEmailVerification.setOnClickListener(this)
        mivAddressBook.setOnClickListener(this)
        mivSettings.setOnClickListener(this)

        mViewModel = requireActivity().provideUserViewModel()

        mViewModel.tipsMessage.observe(this, Observer {
            if (it.isNotEmpty()) {
                showToast(it)
            }
        })

        mViewModel.getIdInfo().observe(this, Observer {
            when (it.idAuthenticationStatus) {
                IDAuthenticationStatus.UNKNOWN -> {
                    mivIDAuthentication.showEndArrow(false)
                    mivIDAuthentication.setEndDescText("")

                    pbIDAuthenticationLoading.visibility = View.VISIBLE
                }

                IDAuthenticationStatus.AUTHENTICATED -> {
                    mivIDAuthentication.showEndArrow(true)
                    mivIDAuthentication.setEndDescText(R.string.desc_authenticated)
                    mivIDAuthentication.setEndDescTextColor(getColor(R.color.def_text_title))

                    pbIDAuthenticationLoading.visibility = View.GONE
                }

                else -> {
                    mivIDAuthentication.showEndArrow(true)
                    mivIDAuthentication.setEndDescText(R.string.desc_unauthorized)
                    mivIDAuthentication.setEndDescTextColor(getColor(R.color.def_text_warn))

                    pbIDAuthenticationLoading.visibility = View.GONE
                }
            }
        })

        mViewModel.getPhoneInfo().observe(this, Observer {
            when (it.accountBindingStatus) {
                AccountBindingStatus.UNKNOWN -> {
                    mivPhoneVerification.showEndArrow(false)
                    mivPhoneVerification.setEndDescText("")

                    pbPhoneVerificationLoading.visibility = View.VISIBLE
                }

                AccountBindingStatus.BOUND -> {
                    mivPhoneVerification.showEndArrow(false)
                    mivPhoneVerification.setEndDescText(it.phoneNumber)
                    mivPhoneVerification.setEndDescTextColor(getColor(R.color.def_text_title))
                    mivPhoneVerification.setOnClickListener(null)
                    mivPhoneVerification.setBackgroundColor(getColor(R.color.white))

                    pbPhoneVerificationLoading.visibility = View.GONE
                }

                else -> {
                    mivPhoneVerification.showEndArrow(true)
                    mivPhoneVerification.setEndDescText(R.string.desc_unbound)
                    mivPhoneVerification.setEndDescTextColor(getColor(R.color.def_text_warn))

                    pbPhoneVerificationLoading.visibility = View.GONE
                }
            }
        })

        mViewModel.getEmailInfo().observe(this, Observer {
            when (it.accountBindingStatus) {
                AccountBindingStatus.UNKNOWN -> {
                    mivEmailVerification.showEndArrow(false)
                    mivEmailVerification.setEndDescText("")

                    pbEmailVerificationLoading.visibility = View.VISIBLE
                }

                AccountBindingStatus.BOUND -> {
                    mivEmailVerification.showEndArrow(false)
                    mivEmailVerification.setEndDescText(it.emailAddress)
                    mivEmailVerification.setEndDescTextColor(getColor(R.color.def_text_title))
                    mivEmailVerification.setOnClickListener(null)
                    mivEmailVerification.setBackgroundColor(getColor(R.color.white))

                    pbEmailVerificationLoading.visibility = View.GONE
                }

                else -> {
                    mivEmailVerification.showEndArrow(true)
                    mivEmailVerification.setEndDescText(R.string.desc_unbound)
                    mivEmailVerification.setEndDescTextColor(getColor(R.color.def_text_warn))

                    pbEmailVerificationLoading.visibility = View.GONE
                }
            }
        })

        mViewModel.init()
    }

    override fun onViewClick(view: View) {
        when (view.id) {
            R.id.mivIDAuthentication -> {
                val idInfo = mViewModel.getIdInfo().value ?: return
                when (idInfo.idAuthenticationStatus) {
                    IDAuthenticationStatus.UNKNOWN -> {
                        mViewModel.execute()
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
                        startActivity(
                            Intent(
                                requireActivity(),
                                IDAuthenticationActivity::class.java
                            )
                        )
                    }
                }
            }

            R.id.mivPhoneVerification -> {
                val phoneInfo = mViewModel.getPhoneInfo().value ?: return
                when (phoneInfo.accountBindingStatus) {
                    AccountBindingStatus.UNKNOWN -> {
                        mViewModel.execute()
                    }

                    AccountBindingStatus.UNBOUND -> {
                        startActivity(
                            Intent(
                                requireActivity(),
                                PhoneVerificationActivity::class.java
                            )
                        )
                    }
                }
            }

            R.id.mivEmailVerification -> {
                val emailInfo = mViewModel.getEmailInfo().value ?: return
                when (emailInfo.accountBindingStatus) {
                    AccountBindingStatus.UNKNOWN -> {
                        mViewModel.execute()
                    }

                    AccountBindingStatus.UNBOUND -> {
                        startActivity(
                            Intent(
                                requireActivity(),
                                EmailVerificationActivity::class.java
                            )
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
}