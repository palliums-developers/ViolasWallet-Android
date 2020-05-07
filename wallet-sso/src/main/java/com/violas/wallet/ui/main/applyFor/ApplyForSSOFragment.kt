package com.violas.wallet.ui.main.applyFor

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseFragment
import com.palliums.net.LoadState
import com.palliums.utils.isNetworkConnected
import com.violas.wallet.R
import com.violas.wallet.event.ApplyPageRefreshEvent
import com.violas.wallet.ui.main.applyFor.ApplyForSSOViewModel.Companion.CODE_NETWORK_ERROR
import com.violas.wallet.ui.main.applyFor.ApplyForSSOViewModel.Companion.CODE_NETWORK_LOADING
import com.violas.wallet.ui.main.applyFor.ApplyForSSOViewModel.Companion.CODE_VERIFICATION_ACCOUNT
import com.violas.wallet.ui.main.applyFor.ApplyForSSOViewModel.Companion.CODE_VERIFICATION_SUCCESS
import com.violas.wallet.ui.main.provideUserViewModel
import kotlinx.android.synthetic.main.fragment_apply_for_sso.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class ApplyForSSOFragment : BaseFragment() {
    private val mUserViewModel by lazy {
        requireActivity().provideUserViewModel()
    }
    private val mApplyForSSOViewModel by lazy {
        ViewModelProvider(this, ApplyForSSOViewModelFactory(mUserViewModel)).get(
            ApplyForSSOViewModel::class.java
        )
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_apply_for_sso
    }

    override fun onSupportVisible() {
        super.onSupportVisible()
        setStatusBarMode(false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vTitleMiddleText.text = getString(R.string.title_apply_issue_sso)
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }

        mUserViewModel.mCurrentAccountLD.observe(viewLifecycleOwner, Observer {
            /*
             * 因为申请发行首页与我的首页共用UserViewModel，当先进入我的首页时，用户信息会开始同步，当
             * 再切换进入申请发行首页时，若用户信息已同步结束，此时先添加对UserViewModel的LiveData观
             * 察时，会立即返回相应结果，若用户信息同步失败会立即更新申请页面。此时应该判断UserViewModel
             * 是否已初始化，若已初始化则判断是否重新同步用户信息
             */
            if (!mUserViewModel.init()) {
                val loadState = mUserViewModel.loadState.value?.peekData()
                if (loadState != null
                    && loadState.status == LoadState.Status.FAILURE
                    && isNetworkConnected()
                ) {
                    mUserViewModel.execute(checkNetworkBeforeExecute = false)
                }
            }
        })

        mUserViewModel.tipsMessage.observe(viewLifecycleOwner, Observer {
            it.getDataIfNotHandled()?.let { msg ->
                if (msg.isNotEmpty()) {
                    showToast(msg)
                }
            }
        })

        mApplyForSSOViewModel.getApplyStatusLiveData().observe(viewLifecycleOwner, Observer {
            Log.e("====", "====监听状态==${it}")
            val fragment = when (it.approvalStatus) {
                CODE_NETWORK_LOADING -> {
                    NetworkLoadingFragment()
                }
                CODE_NETWORK_ERROR -> {
                    NetworkStatusFragment()
                }
                CODE_VERIFICATION_SUCCESS -> {
                    SSOVerifySuccessFragment()
                }
                CODE_VERIFICATION_ACCOUNT -> {
                    SSOVerifyUserInfoFragment()
                }
                else -> {
                    SSOApplicationStatusFragment.getInstance(it)
                }
            }

            childFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, fragment)
                .commit()
        })
    }

    @Subscribe
    fun onApplyPageRefreshEvent(event: ApplyPageRefreshEvent) {
        mApplyForSSOViewModel.refreshApplyStatus()
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }
}
