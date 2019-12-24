package com.violas.wallet.ui.main.me

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.lifecycle.Observer
import com.palliums.base.BaseFragment
import com.palliums.net.LoadState
import com.palliums.utils.getColor
import com.palliums.utils.isNetworkConnected
import com.palliums.widget.MenuItemView
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

        if (!mViewModel.tipsMessage.hasObservers()) {
            mViewModel.tipsMessage.observe(this, Observer {
                if (it.isNotEmpty()) {
                    showToast(it)
                }
            })
        }

        /*
         * 因为申请发行首页与我的首页共用UserViewModel，当先进入申请发行首页时，用户信息会开始同步，
         * 当再切换进入我的首页时，若用户信息已同步结束，此时先添加对UserViewModel的LiveData观察
         * 时，会立即返回相应结果，若用户信息同步失败会立即更新我的页面。此时应该判断UserViewModel
         * 是否已初始化，若已初始化则判断是否重新同步用户信息
         */
        if (!mViewModel.init()) {
            val loadState = mViewModel.loadState.value
            if (loadState != null
                && loadState.status == LoadState.Status.FAILURE
                && isNetworkConnected()
            ) {
                mViewModel.execute()
            }
        }

        mViewModel.getIdInfoLiveData().observe(this, Observer {
            when (it.first.idAuthenticationStatus) {
                IDAuthenticationStatus.UNKNOWN -> {
                    mivIDAuthentication.showEndArrow(false)
                    mivIDAuthentication.setEndDescText("")

                    handLoadState(pbIDAuthenticationLoading, it.second)
                }

                IDAuthenticationStatus.AUTHENTICATED -> {
                    mivIDAuthentication.showEndArrow(true)
                    mivIDAuthentication.setEndDescText(R.string.desc_authenticated)
                    mivIDAuthentication.setEndDescTextColor(getColor(R.color.def_text_title))

                    handLoadState(pbIDAuthenticationLoading, it.second)
                }

                else -> {
                    mivIDAuthentication.showEndArrow(true)
                    mivIDAuthentication.setEndDescText(R.string.desc_unauthorized)
                    mivIDAuthentication.setEndDescTextColor(getColor(R.color.def_text_warn))

                    handLoadState(pbIDAuthenticationLoading, it.second, mivIDAuthentication)
                }
            }
        })

        mViewModel.getPhoneInfoLiveData().observe(this, Observer {
            when (it.first.accountBindingStatus) {
                AccountBindingStatus.UNKNOWN -> {
                    mivPhoneVerification.showEndArrow(false)
                    mivPhoneVerification.setEndDescText("")

                    handLoadState(pbPhoneVerificationLoading, it.second)
                }

                AccountBindingStatus.BOUND -> {
                    mivPhoneVerification.showEndArrow(false)
                    mivPhoneVerification.setEndDescText(it.first.phoneNumber)
                    mivPhoneVerification.setEndDescTextColor(getColor(R.color.def_text_title))
                    mivPhoneVerification.setOnClickListener(null)
                    mivPhoneVerification.setBackgroundColor(getColor(R.color.white))

                    handLoadState(pbPhoneVerificationLoading, it.second)
                }

                else -> {
                    mivPhoneVerification.showEndArrow(true)
                    mivPhoneVerification.setEndDescText(R.string.desc_unbound)
                    mivPhoneVerification.setEndDescTextColor(getColor(R.color.def_text_warn))

                    handLoadState(pbPhoneVerificationLoading, it.second, mivPhoneVerification)
                }
            }
        })

        mViewModel.getEmailInfoLiveData().observe(this, Observer {
            when (it.first.accountBindingStatus) {
                AccountBindingStatus.UNKNOWN -> {
                    mivEmailVerification.showEndArrow(false)
                    mivEmailVerification.setEndDescText("")

                    handLoadState(pbEmailVerificationLoading, it.second)
                }

                AccountBindingStatus.BOUND -> {
                    mivEmailVerification.showEndArrow(false)
                    mivEmailVerification.setEndDescText(it.first.emailAddress)
                    mivEmailVerification.setEndDescTextColor(getColor(R.color.def_text_title))
                    mivEmailVerification.setOnClickListener(null)
                    mivEmailVerification.setBackgroundColor(getColor(R.color.white))

                    handLoadState(pbEmailVerificationLoading, it.second)
                }

                else -> {
                    mivEmailVerification.showEndArrow(true)
                    mivEmailVerification.setEndDescText(R.string.desc_unbound)
                    mivEmailVerification.setEndDescTextColor(getColor(R.color.def_text_warn))

                    handLoadState(pbEmailVerificationLoading, it.second, mivEmailVerification)
                }
            }
        })
    }

    private fun handLoadState(
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

    override fun onViewClick(view: View) {
        when (view.id) {
            R.id.mivIDAuthentication -> {
                val idInfo = mViewModel.getIdInfo() ?: return
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
                val phoneInfo = mViewModel.getPhoneInfo() ?: return
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
                val emailInfo = mViewModel.getEmailInfo() ?: return
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