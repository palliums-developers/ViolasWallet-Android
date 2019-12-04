package com.violas.wallet.ui.main.applyFor

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import com.palliums.base.BaseFragment
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.ApplyManager
import com.violas.wallet.event.RefreshPageEvent
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.sso.ApplyForStatusDTO
import com.violas.wallet.ui.main.provideUserViewModel
import kotlinx.android.synthetic.main.fragment_apply_for_sso.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class ApplyForSSOFragment : BaseFragment() {
    private var mApplyStatus = -1
    private var mAccount: AccountDO? = null
    private val mAccountManager by lazy {
        AccountManager()
    }
    private val mUserViewModel by lazy {
        requireActivity().provideUserViewModel()
    }

    private val mApplyManager by lazy {
        ApplyManager()
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_apply_for_sso
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        EventBus.getDefault().register(this)
        vTitleMiddleText.text = getString(R.string.title_apply_issue_sso)
        launch(Dispatchers.IO) {
            mAccount = mAccountManager.currentAccount()
            mAccount?.let {
                val applyStatus = mApplyManager.getApplyStatus(it.address)
                refreshFragment(applyStatus?.data)
            }
        }
        mUserViewModel.init()

        activity?.let {
            mUserViewModel.getAllReadyLiveData().observe(it, Observer { ready ->
                if (ready) {
                    childFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainerView, ApplySubmitFragment())
                        .commit()
                } else {
                    childFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainerView, CheckVerifyFragment())
                        .commit()
                }
            })
        }
    }

    @Subscribe
    fun onRefreshPage(event: RefreshPageEvent? = null) {
        launch(Dispatchers.IO) {
            mAccount?.let {
                val applyStatus = mApplyManager.getApplyStatus(it.address)
                refreshFragment(applyStatus?.data)
            }
        }
    }

    private suspend fun refreshFragment(status: ApplyForStatusDTO?) {
        mAccount?.let {
            status?.apply {
                if (mApplyStatus == status.approval_status) {
                    return
                }
                mApplyStatus = status.approval_status
                withContext(Dispatchers.Main) {
                    val fragment = when (status.approval_status) {
                        0, 1, 2, 3, 4 -> {
                            ApplyStatusFragment.getInstance(status.approval_status)
                        }
                        else -> {
                            CheckVerifyFragment()
                        }
                    }
                    childFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainerView, fragment)
                        .commit()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        onRefreshPage()
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }
}
