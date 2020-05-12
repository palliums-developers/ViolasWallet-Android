package com.violas.wallet.ui.main.applyFor

import android.os.Bundle
import android.view.View
import com.palliums.base.BaseFragment
import com.palliums.utils.getColor
import com.violas.wallet.R
import com.violas.wallet.biz.SSOApplicationState
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.repository.http.issuer.ApplyForSSOSummaryDTO
import com.violas.wallet.ui.issuerApplication.IssuerApplicationActivity
import kotlinx.android.synthetic.main.fragment_issuer_apply_for_sso_status.*

/**
 * 发行商申请发行SSO状态视图
 */
class IssuerApplyForSSOStatusFragment : BaseFragment() {

    companion object {
        fun getInstance(summary: ApplyForSSOSummaryDTO): BaseFragment {
            val fragment = IssuerApplyForSSOStatusFragment()
            fragment.arguments = Bundle().apply {
                putParcelable(KEY_ONE, summary)
            }
            return fragment
        }
    }

    private lateinit var mApplyForSSOSummary: ApplyForSSOSummaryDTO

    override fun getLayoutResId(): Int {
        return R.layout.fragment_issuer_apply_for_sso_status
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (initData(savedInstanceState)) {
            initView()
            initEvent()
        }
    }

    private fun initData(savedInstanceState: Bundle?): Boolean {
        var summary: ApplyForSSOSummaryDTO? = null
        if (savedInstanceState != null) {
            summary = savedInstanceState.getParcelable(KEY_ONE)
        } else if (arguments != null) {
            summary = arguments!!.getParcelable(KEY_ONE)
        }

        if (summary == null) {
            return false
        }

        mApplyForSSOSummary = summary
        return true
    }

    private fun initView() {
        tvTitle.text = getString(R.string.hint_mint_condition)
        when (mApplyForSSOSummary.applicationStatus) {
            SSOApplicationState.ISSUER_APPLYING,
            SSOApplicationState.GOVERNOR_APPROVED,
            SSOApplicationState.CHAIRMAN_APPROVED -> {
                tvContent.text = getString(R.string.sso_application_msg_status_1)
                tvContent.setTextColor(getColor(R.color.color_FAA030))
            }

            SSOApplicationState.GOVERNOR_TRANSFERRED -> {
                tvContent.text = getString(R.string.sso_application_msg_status_6)
                tvContent.setTextColor(getColor(R.color.color_00D1AF))
            }

            SSOApplicationState.ISSUER_PUBLISHED -> {
                tvContent.text = getString(R.string.sso_application_msg_status_2)
                tvContent.setTextColor(getColor(R.color.color_FAA030))
            }

            SSOApplicationState.GOVERNOR_MINTED -> {
                tvContent.text = getString(R.string.sso_application_msg_status_3)
                tvContent.setTextColor(getColor(R.color.color_00D1AF))
            }

            SSOApplicationState.AUDIT_TIMEOUT -> {
                tvContent.text = getString(R.string.sso_application_msg_status_5)
                tvContent.setTextColor(getColor(R.color.color_F55753))
            }

            else -> {
                tvContent.text = getString(R.string.sso_application_msg_status_4)
                tvContent.setTextColor(getColor(R.color.color_F55753))
            }
        }
    }

    private fun initEvent() {
        layoutItem.setOnClickListener {
            activity?.let { it1 ->
                IssuerApplicationActivity.start(it1, mApplyForSSOSummary)
            }
        }
    }
}
