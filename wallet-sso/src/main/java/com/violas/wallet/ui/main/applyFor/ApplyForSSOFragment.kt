package com.violas.wallet.ui.main.applyFor

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseFragment
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vTitleMiddleText.text = getString(R.string.title_apply_issue_sso)
        EventBus.getDefault().register(this)

        mApplyForSSOViewModel.getApplyStatusLiveData().observe(viewLifecycleOwner, Observer {
            Log.e("====", "====监听状态==${it}")
            val fragment = when (it) {
                CODE_NETWORK_LOADING -> {
                    NetworkLoadingFragment()
                }
                CODE_NETWORK_ERROR -> {
                    NetworkStatusFragment()
                }
                CODE_VERIFICATION_SUCCESS -> {
                    VerifySuccessFragment()
                }
                0, 1, 2, 3, 4 -> {
                    ApplyStatusFragment.getInstance(it)
                }
                CODE_VERIFICATION_ACCOUNT -> {
                    CheckVerifyFragment()
                }
                else -> {
                    CheckVerifyFragment()
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
