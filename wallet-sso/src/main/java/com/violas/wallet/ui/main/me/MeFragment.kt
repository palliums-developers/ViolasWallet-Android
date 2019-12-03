package com.violas.wallet.ui.main.me

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import com.palliums.base.BaseFragment
import com.palliums.net.LoadState
import com.palliums.utils.getColor
import com.violas.wallet.R
import com.violas.wallet.repository.local.user.AccountBindingStatus
import com.violas.wallet.repository.local.user.IDAuthenticationStatus
import com.violas.wallet.ui.addressBook.AddressBookActivity
import com.violas.wallet.ui.authentication.IDAuthenticationActivity
import com.violas.wallet.ui.authentication.IDInformationActivity
import com.violas.wallet.ui.main.provideUserViewModel
import com.violas.wallet.ui.setting.SettingActivity
import com.violas.wallet.ui.verification.EmailVerificationActivity
import com.violas.wallet.ui.verification.PhoneVerificationActivity
import kotlinx.android.synthetic.main.fragment_me.*

/**
 * 我的页面
 */
class MeFragment : BaseFragment() {

    private val mViewModel by lazy {
        requireActivity().provideUserViewModel()
    }

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

        mViewModel.tipsMessage.observe(this, Observer {
            if (it.isNotEmpty()) {
                showToast(it)
            }
        })

        mViewModel.loadState.observe(this, Observer {
            when (it.status) {
                LoadState.Status.RUNNING -> {
                    val idInfo = mViewModel.getIdInfo().value
                    if (idInfo != null && !idInfo.isAuthenticatedID()) {
                        pbIDAuthenticationLoading.visibility = View.VISIBLE
                    }

                    val phoneInfo = mViewModel.getPhoneInfo().value
                    if (phoneInfo != null && !phoneInfo.isBoundPhone()) {
                        pbPhoneVerificationLoading.visibility = View.VISIBLE
                    }

                    val emailInfo = mViewModel.getEmailInfo().value
                    if (emailInfo != null && !emailInfo.isBoundEmail()) {
                        pbEmailVerificationLoading.visibility = View.VISIBLE
                    }
                }

                else -> {
                    pbIDAuthenticationLoading.visibility = View.GONE
                    pbPhoneVerificationLoading.visibility = View.GONE
                    pbEmailVerificationLoading.visibility = View.GONE
                }
            }
        })

        mViewModel.getIdInfo().observe(this, Observer {
            when (it.idAuthenticationStatus) {
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
        })

        mViewModel.getPhoneInfo().observe(this, Observer {
            when (it.accountBindingStatus) {
                AccountBindingStatus.UNKNOWN -> {
                    mivPhoneVerification.showEndArrow(false)
                    mivPhoneVerification.setEndDescText("")
                }

                AccountBindingStatus.BOUND -> {
                    mivPhoneVerification.showEndArrow(false)
                    mivPhoneVerification.setEndDescText(it.phoneNumber)
                    mivPhoneVerification.setEndDescTextColor(getColor(R.color.def_text_title))
                    mivPhoneVerification.setOnClickListener(null)
                    mivPhoneVerification.setBackgroundColor(getColor(R.color.white))
                }

                else -> {
                    mivPhoneVerification.showEndArrow(true)
                    mivPhoneVerification.setEndDescText(R.string.desc_unbound)
                    mivPhoneVerification.setEndDescTextColor(getColor(R.color.def_text_warn))
                }
            }
        })

        mViewModel.getEmailInfo().observe(this, Observer {
            when (it.accountBindingStatus) {
                AccountBindingStatus.UNKNOWN -> {
                    mivEmailVerification.showEndArrow(false)
                    mivEmailVerification.setEndDescText("")
                }

                AccountBindingStatus.BOUND -> {
                    mivEmailVerification.showEndArrow(false)
                    mivEmailVerification.setEndDescText(it.emailAddress)
                    mivEmailVerification.setEndDescTextColor(getColor(R.color.def_text_title))
                    mivEmailVerification.setOnClickListener(null)
                    mivEmailVerification.setBackgroundColor(getColor(R.color.white))
                }

                else -> {
                    mivEmailVerification.showEndArrow(true)
                    mivEmailVerification.setEndDescText(R.string.desc_unbound)
                    mivEmailVerification.setEndDescTextColor(getColor(R.color.def_text_warn))
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