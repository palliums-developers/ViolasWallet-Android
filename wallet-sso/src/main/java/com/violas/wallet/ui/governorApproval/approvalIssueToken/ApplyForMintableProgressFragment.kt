package com.violas.wallet.ui.governorApproval.approvalIssueToken

import android.app.Activity
import android.content.Intent
import android.view.View
import com.palliums.utils.getColor
import com.violas.wallet.R
import com.violas.wallet.repository.http.governor.SSOApplicationDetailsDTO
import com.violas.wallet.ui.governorApproval.GovernorApprovalViewModel.Companion.ACTION_APPROVE_APPLICATION
import kotlinx.android.synthetic.main.fragment_apply_for_mintable_progress.*

/**
 * Created by elephant on 2020/4/28 22:09.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 州长申请铸币权进度视图
 */
class ApplyForMintableProgressFragment : BaseApprovalIssueTokenFragment() {

    companion object {
        private const val TRANSFER_REQUEST_CODE = 100
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_apply_for_mintable_progress
    }

    override fun setApplicationInfo(details: SSOApplicationDetailsDTO) {
        super.setApplicationInfo(details)

        // 申请发币状态（负数为失败情况）：
        // 1：州长已审核通过，并申请铸币权；
        // 2：董事长已发布新Token，并指定铸币权给州长；
        // 3：州长已给发行商转VToken，并通知；
        // -2：董事长审核未通过。
        tvStep1.text =
            getString(R.string.apply_for_mintable_step_1, details.idName, details.tokenName)
        if (details.applicationStatus == 1) {
            vStep2.setBackgroundResource(R.color.color_9D9BA3)
            return
        }

        vStep2.setBackgroundResource(R.color.colorAccent)
        ivStep3.visibility = View.VISIBLE
        tvStep3.visibility = View.VISIBLE
        if (details.applicationStatus == -2) {
            vStep3.visibility = View.VISIBLE
            tvStep3Reason.visibility = View.VISIBLE
            ivStep3.setBackgroundResource(R.drawable.ic_application_unpassed)
            tvStep3.setText(R.string.apply_for_mintable_step_3_2)
            tvStep3Reason.text =
                getString(R.string.apply_for_mintable_step_3_3, details.unapproveReason)
            return
        }

        ivStep3.setBackgroundResource(R.drawable.ic_application_passed)
        tvStep3.text = getString(R.string.apply_for_mintable_step_3_1)
        vLine.visibility = View.VISIBLE
        tvDesc.visibility = View.VISIBLE
        tvTransferAndNotify.visibility = View.VISIBLE
        vBlank.visibility = View.GONE
        if (details.applicationStatus == 2) {
            tvTransferAndNotify.setText(R.string.apply_for_mintable_action_1)
            tvTransferAndNotify.setTextColor(getColor(R.color.white))
            tvTransferAndNotify.isEnabled = true
        } else {
            disableTransferAndNotifyBtn()
        }
    }

    private fun disableTransferAndNotifyBtn() {
        tvTransferAndNotify.setText(R.string.apply_for_mintable_action_2)
        tvTransferAndNotify.setTextColor(getColor(R.color.colorAccent))
        tvTransferAndNotify.isEnabled = false
    }

    override fun initEvent() {
        super.initEvent()

        tvTransferAndNotify.setOnClickListener {
            mViewModel.transferVTokenToSSO(context!!, TRANSFER_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            TRANSFER_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    mViewModel.execute(action = ACTION_APPROVE_APPLICATION) {
                        mSSOApplicationDetailsDTO.applicationStatus = 3
                        disableTransferAndNotifyBtn()
                    }
                }
            }
        }
    }
}