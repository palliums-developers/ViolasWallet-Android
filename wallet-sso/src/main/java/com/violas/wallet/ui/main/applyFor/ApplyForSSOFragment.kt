package com.violas.wallet.ui.main.applyFor

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseFragment
import com.palliums.net.RequestException
import com.palliums.widget.status.IStatusLayout
import com.violas.wallet.R
import com.violas.wallet.repository.http.sso.ApplyForStatusDTO
import com.violas.wallet.ui.main.applyFor.ApplyForSSOViewModel.Companion.CODE_AUTHENTICATION_ACCOUNT
import com.violas.wallet.ui.main.applyFor.ApplyForSSOViewModel.Companion.CODE_AUTHENTICATION_COMPLETE
import kotlinx.android.synthetic.main.fragment_apply_for_sso.*

/**
 * 发行商申请发行SSO首页
 */
class ApplyForSSOFragment : BaseFragment() {

    private val mViewModel by lazy {
        ViewModelProvider(this, ViewModelProvider.NewInstanceFactory())
            .get(ApplyForSSOViewModel::class.java)
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_apply_for_sso
    }

    override fun onSupportVisible() {
        super.onSupportVisible()
        setStatusBarMode(false)
    }

    override fun onLazyInitView(savedInstanceState: Bundle?) {
        super.onLazyInitView(savedInstanceState)
        initView()
        initEvent()
    }

    private fun initView() {
        vTitleMiddleText.text = getString(R.string.title_apply_issue_sso)
        dslStatusLayout.showStatus(IStatusLayout.Status.STATUS_LOADING)
        srlRefreshLayout.isEnabled = false
    }

    private fun initEvent() {
        srlRefreshLayout.setOnRefreshListener {
            loadIssueSSOStatus()
        }

        mViewModel.tipsMessage.observe(this, Observer {
            it.getDataIfNotHandled()?.let { msg ->
                if (msg.isNotEmpty()) {
                    showToast(msg)
                }
            }
        })

        mViewModel.mIssueSSOStatusLiveData.observe(this, Observer {
            it.getDataIfNotHandled()?.let { status ->
                loadFragment(status)
            }
        })

        mViewModel.mAccountDOLiveData.observe(this, Observer {
            loadIssueSSOStatus()
        })
    }

    private fun loadIssueSSOStatus() {
        mViewModel.execute(
            failureCallback = {
                if (srlRefreshLayout.isRefreshing) {
                    srlRefreshLayout.isRefreshing = false
                } else {
                    srlRefreshLayout.isEnabled = true
                }

                dslStatusLayout.showStatus(
                    if (RequestException.isNoNetwork(it))
                        IStatusLayout.Status.STATUS_NO_NETWORK
                    else
                        IStatusLayout.Status.STATUS_FAILURE
                )
            }
        ) {
            if (srlRefreshLayout.isRefreshing) {
                srlRefreshLayout.isRefreshing = false
            } else {
                srlRefreshLayout.isEnabled = true
            }

            dslStatusLayout.showStatus(IStatusLayout.Status.STATUS_NONE)
        }
    }

    private fun loadFragment(issueSSOStatus: ApplyForStatusDTO) {
        val fragment = when (issueSSOStatus.approvalStatus) {
            CODE_AUTHENTICATION_ACCOUNT -> {
                SSOVerifyUserInfoFragment()
            }

            CODE_AUTHENTICATION_COMPLETE -> {
                SSOVerifySuccessFragment()
            }

            else -> {
                SSOApplicationStatusFragment.getInstance(issueSSOStatus)
            }
        }

        loadRootFragment(R.id.fragmentContainerView, fragment)
    }
}
