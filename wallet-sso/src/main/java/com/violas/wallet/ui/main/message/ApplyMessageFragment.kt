package com.violas.wallet.ui.main.message

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseFragment
import com.palliums.net.LoadState
import com.palliums.widget.status.IStatusLayout
import com.violas.wallet.R
import com.violas.wallet.event.RefreshGovernorApplicationProgressEvent
import com.violas.wallet.ui.main.message.governorApplication.GovernorApplicationProgressFragment
import com.violas.wallet.ui.main.message.ssoApplication.SSOApplicationListFragment
import kotlinx.android.synthetic.main.fragment_apply_message.*
import me.yokeyword.fragmentation.SupportFragment
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class ApplyMessageFragment : BaseFragment() {

    private val mViewModel by lazy {
        ViewModelProvider(this).get(ApplyMessageViewModel::class.java)
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_apply_message
    }

    override fun onLazyInitView(savedInstanceState: Bundle?) {
        super.onLazyInitView(savedInstanceState)
        val fragment =
            findChildFragment(GovernorApplicationProgressFragment::class.java)
                ?: findChildFragment(SSOApplicationListFragment::class.java)
        fragment?.let {
            it.popChild()
        }

        handleReload()
        handleApplicationStatus()
        handleLoadStatus()
        handleLoadTips()
    }

    override fun onSupportVisible() {
        super.onSupportVisible()
        setStatusBarMode(true)
    }

    override fun onDetach() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }

        super.onDetach()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRefreshGovernorApplicationProgressEvent(event: RefreshGovernorApplicationProgressEvent) {
        mViewModel.execute(checkNetworkBeforeExecute = false)
    }

    private fun handleReload() {
        srlRefreshLayout.isEnabled = false
        srlRefreshLayout.setOnRefreshListener {
            mViewModel.execute(checkNetworkBeforeExecute = false)
        }
    }

    private fun handleApplicationStatus() {
        mViewModel.mGovernorInfoLD.observe(this, Observer {
            when (it.applicationStatus) {
                -1, 0, 1, 2, 3 -> { // -1: no application
                    srlRefreshLayout.isEnabled = true

                    if (!EventBus.getDefault().isRegistered(this)) {
                        EventBus.getDefault().register(this)
                    }

                    val fragment =
                        findChildFragment(GovernorApplicationProgressFragment::class.java)
                    if (fragment == null) {
                        loadRootFragment(
                            R.id.flFragmentContainer,
                            GovernorApplicationProgressFragment.newInstance(it.applicationStatus)
                        )
                    } else {
                        fragment.putNewBundle(
                            GovernorApplicationProgressFragment.newBundle(it.applicationStatus)
                        )
                        (topChildFragment as SupportFragment).start(
                            fragment,
                            SupportFragment.SINGLETASK
                        )
                    }
                }

                else -> {
                    srlRefreshLayout.isEnabled = false

                    val fragment =
                        findChildFragment(GovernorApplicationProgressFragment::class.java)
                    if (fragment == null) {
                        loadRootFragment(
                            R.id.flFragmentContainer,
                            SSOApplicationListFragment()
                        )
                    } else {
                        replaceFragment(SSOApplicationListFragment(), true)
                    }
                }
            }
        })
    }

    private fun handleLoadStatus() {
        mViewModel.loadState.observe(this, Observer {
            when (it.peekData().status) {
                LoadState.Status.RUNNING -> {
                    srlRefreshLayout.isRefreshing = true

                    /*val fragment =
                        findChildFragment(GovernorApplicationProgressFragment::class.java)
                    if (fragment == null) {
                        slStatusLayout.showStatus(IStatusLayout.Status.STATUS_LOADING)
                    }*/
                }

                LoadState.Status.SUCCESS -> {
                    srlRefreshLayout.isRefreshing = false

                    slStatusLayout.showStatus(IStatusLayout.Status.STATUS_NONE)
                }

                else -> {
                    srlRefreshLayout.isRefreshing = false

                    val fragment =
                        findChildFragment(GovernorApplicationProgressFragment::class.java)
                    if (fragment == null) {
                        srlRefreshLayout.isEnabled = true
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
}
