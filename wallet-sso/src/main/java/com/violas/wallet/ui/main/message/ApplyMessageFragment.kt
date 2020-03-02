package com.violas.wallet.ui.main.message

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseFragment
import com.palliums.net.LoadState
import com.palliums.widget.status.IStatusLayout
import com.violas.wallet.R
import com.violas.wallet.ui.main.message.governorApplication.GovernorApplicationFragment
import com.violas.wallet.ui.main.message.ssoApplication.SSOApplicationFragment
import kotlinx.android.synthetic.main.fragment_apply_message.*
import me.yokeyword.fragmentation.SupportFragment

class ApplyMessageFragment : BaseFragment() {

    private val mViewModel by lazy {
        ViewModelProvider(this).get(ApplyMessageViewModel::class.java)
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_apply_message
    }

    override fun onSupportVisible() {
        super.onSupportVisible()
        setStatusBarMode(true)
    }

    override fun onLazyInitView(savedInstanceState: Bundle?) {
        super.onLazyInitView(savedInstanceState)
        val fragment = findChildFragment(BaseFragment::class.java)
        fragment?.let {
            it.popChild()
        }

        handleApplicationStatus()
        handleLoadStatus()
        handleLoadTips()
        handleReload()
    }

    private fun handleApplicationStatus() {
        mViewModel.mGovernorInfoLD.observe(this, Observer {
            when (it.applicationStatus) {
                -1, 0, 1, 2, 3 -> { // -1: no application
                    val baseFragment = findChildFragment(BaseFragment::class.java)
                    if (baseFragment == null) {
                        loadRootFragment(
                            R.id.flFragmentContainer,
                            GovernorApplicationFragment.newInstance(it.applicationStatus)
                        )
                        return@Observer
                    }

                    val fragment =
                        findChildFragment(GovernorApplicationFragment::class.java)
                    if (fragment == null) {
                        replaceFragment(
                            GovernorApplicationFragment.newInstance(it.applicationStatus)
                            , true
                        )
                    } else {
                        fragment.putNewBundle(
                            GovernorApplicationFragment.newBundle(it.applicationStatus)
                        )
                        (topChildFragment as SupportFragment).start(
                            fragment,
                            SupportFragment.SINGLETASK
                        )
                    }
                }

                else -> {
                    val baseFragment = findChildFragment(BaseFragment::class.java)
                    if (baseFragment == null) {
                        loadRootFragment(
                            R.id.flFragmentContainer,
                            SSOApplicationFragment()
                        )
                    } else {
                        replaceFragment(SSOApplicationFragment(), true)
                    }
                }
            }
        })
    }

    private fun handleLoadStatus() {
        mViewModel.loadState.observe(this, Observer {
            when (it.peekData().status) {
                LoadState.Status.RUNNING -> {
                    val fragment = findChildFragment(BaseFragment::class.java)
                    if (fragment == null) {
                        slStatusLayout.showStatus(IStatusLayout.Status.STATUS_LOADING)
                    }
                }

                LoadState.Status.SUCCESS -> {
                    slStatusLayout.showStatus(IStatusLayout.Status.STATUS_NONE)
                }

                else -> {
                    val fragment = findChildFragment(BaseFragment::class.java)
                    if (fragment == null) {
                        slStatusLayout.isClickable = true
                        slStatusLayout.showStatus(
                            if (it.peekData().isNoNetwork())
                                IStatusLayout.Status.STATUS_NO_NETWORK
                            else
                                IStatusLayout.Status.STATUS_FAILURE
                        )
                    }
                }
            }
        })
    }

    private fun handleLoadTips() {
        mViewModel.tipsMessage.observe(this, Observer {
            it.getDataIfNotHandled()?.let { msg ->
                if (msg.isNotEmpty()) {
                    showToast(msg)
                }
            }
        })
    }

    private fun handleReload() {
        slStatusLayout.setOnClickListener(this)
        slStatusLayout.isClickable = false
    }

    override fun onViewClick(view: View) {
        when (view) {
            slStatusLayout -> {
                slStatusLayout.isClickable = false
                mViewModel.execute(checkNetworkBeforeExecute = false)
            }
        }
    }
}
