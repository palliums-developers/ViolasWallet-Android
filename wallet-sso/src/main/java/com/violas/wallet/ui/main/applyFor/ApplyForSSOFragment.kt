package com.violas.wallet.ui.main.applyFor

import android.os.Bundle
import android.view.View
import com.palliums.base.BaseFragment
import com.violas.wallet.R
import kotlinx.android.synthetic.main.fragment_apply_for_sso.*

class ApplyForSSOFragment : BaseFragment() {
    override fun getLayoutResId(): Int {
        return R.layout.fragment_apply_for_sso
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vTitleMiddleText.text = getString(R.string.title_apply_issue_sso)
        childFragmentManager.beginTransaction()
//            .replace(R.id.fragmentContainerView,ApplySubmitFragment())
            .replace(R.id.fragmentContainerView,ApplyStatusFragment())
            .commit()
    }
}
