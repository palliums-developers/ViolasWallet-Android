package com.violas.wallet.ui.main.applyFor

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import com.palliums.base.BaseFragment
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.ApplyManager
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.sso.ApplyForStatusDTO
import com.violas.wallet.ui.main.provideUserViewModel
import kotlinx.android.synthetic.main.fragment_apply_for_sso.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ApplyForSSOFragment : BaseFragment() {
    private var mApplyStatus = 0
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
        vTitleMiddleText.text = getString(R.string.title_apply_issue_sso)
        launch(Dispatchers.IO) {
            mAccount = mAccountManager.currentAccount()
            mAccount?.let {
                val applyStatus = mApplyManager.getApplyStatus(it.address)
                refreshFragment(applyStatus?.data)
            }
        }

        childFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView, ApplySubmitFragment())
            // .replace(R.id.fragmentContainerView,ApplyStatusFragment())
            .commit()
        activity?.let {
            mUserViewModel.getAllReady().observe(it, Observer { ready ->
                if (ready) {
                    childFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainerView, ApplySubmitFragment())
                        // .replace(R.id.fragmentContainerView,ApplyStatusFragment())
                        .commit()
                }
            })
        }
    }

    private suspend fun refreshFragment(status: ApplyForStatusDTO?) {
        mAccount?.let {
            status?.apply {
                // todo
                mApplyStatus = 0
                withContext(Dispatchers.Main) {
                    childFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainerView, ApplySubmitFragment())
                        // .replace(R.id.fragmentContainerView,ApplyStatusFragment())
                        .commit()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        launch(Dispatchers.IO) {
            mAccount?.let {
                // todo
                // if(mApplyStatus == it.xxx){
                val applyStatus = mApplyManager.getApplyStatus(it.address)
                refreshFragment(applyStatus?.data)
                // }
            }
        }
    }
}
