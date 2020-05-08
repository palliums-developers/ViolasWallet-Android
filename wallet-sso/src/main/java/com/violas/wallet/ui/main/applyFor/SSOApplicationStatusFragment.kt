package com.violas.wallet.ui.main.applyFor

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.palliums.base.BaseFragment
import com.palliums.utils.getColor
import com.violas.wallet.R
import com.violas.wallet.biz.SSOApplicationState
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.event.RefreshPageEvent
import com.violas.wallet.repository.http.sso.ApplyForStatusDTO
import com.violas.wallet.ui.ssoApplication.SSOApplicationActivity
import kotlinx.android.synthetic.main.fragment_sso_application_status.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

/**
 * 发行商发行申请状态视图
 */
class SSOApplicationStatusFragment : BaseFragment() {

    companion object {
        fun getInstance(applicationMsg: ApplyForStatusDTO): Fragment {
            val fragment = SSOApplicationStatusFragment()
            fragment.arguments = Bundle().apply {
                putParcelable(KEY_ONE, applicationMsg)
            }
            return fragment
        }
    }

    private lateinit var mSSOApplicationMsg: ApplyForStatusDTO

    override fun getLayoutResId(): Int {
        return R.layout.fragment_sso_application_status
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (initData(savedInstanceState)) {
            initView()
            initEvent()
        }
    }

    private fun initData(savedInstanceState: Bundle?): Boolean {
        var applicationMsg: ApplyForStatusDTO? = null
        if (savedInstanceState != null) {
            applicationMsg = savedInstanceState.getParcelable(KEY_ONE)
        } else if (arguments != null) {
            applicationMsg = arguments!!.getParcelable(KEY_ONE)
        }

        if (applicationMsg == null) {
            return false
        }

        mSSOApplicationMsg = applicationMsg
        return true
    }

    private fun initView() {
        tvTitle.text = getString(R.string.hint_mint_condition)
        when (mSSOApplicationMsg.approvalStatus) {
            SSOApplicationState.APPLYING_ISSUE_TOKEN,
            SSOApplicationState.APPLYING_MINTABLE,
            SSOApplicationState.GIVEN_MINTABLE -> {
                tvContent.text = getString(R.string.sso_application_msg_status_1)
                tvContent.setTextColor(getColor(R.color.color_FAA030))
            }

            SSOApplicationState.TRANSFERRED_AND_NOTIFIED -> {
                tvContent.text = getString(R.string.sso_application_msg_status_6)
                tvContent.setTextColor(getColor(R.color.color_00D1AF))
            }

            SSOApplicationState.APPLYING_MINT_TOKEN -> {
                tvContent.text = getString(R.string.sso_application_msg_status_2)
                tvContent.setTextColor(getColor(R.color.color_FAA030))
            }

            SSOApplicationState.MINTED_TOKEN -> {
                tvContent.text = getString(R.string.sso_application_msg_status_3)
                tvContent.setTextColor(getColor(R.color.color_00D1AF))
            }

            SSOApplicationState.APPROVAL_TIMEOUT -> {
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
        swipeRefresh.setOnRefreshListener {
            launch(Dispatchers.IO) {
                EventBus.getDefault().post(RefreshPageEvent())
                delay(800)
                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                }
            }
        }

        layoutItem.setOnClickListener {
            activity?.let { it1 ->
                SSOApplicationActivity.start(it1, mSSOApplicationMsg)
            }
        }
    }
}
