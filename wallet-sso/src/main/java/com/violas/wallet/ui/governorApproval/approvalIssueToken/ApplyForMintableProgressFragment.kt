package com.violas.wallet.ui.governorApproval.approvalIssueToken

import android.app.Activity
import android.content.Intent
import android.view.View
import androidx.lifecycle.Observer
import com.palliums.utils.getColor
import com.violas.wallet.R
import com.violas.wallet.biz.SSOApplicationState
import com.violas.wallet.repository.http.governor.SSOApplicationDetailsDTO
import com.violas.wallet.ui.governorApproval.ApprovalFragmentViewModel.Companion.ACTION_TRANSFER_COIN_TO_ISSUER
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

        tvStep1Desc.text =
            getString(R.string.apply_for_mintable_step_1, details.idName, details.tokenName)
        if (details.applicationStatus == SSOApplicationState.GOVERNOR_APPROVED) {
            vStep2Line.setBackgroundResource(R.color.color_9D9BA3)
            tvStep2Desc.setTextColor(getColor(R.color.def_text_title))
            return
        }

        vStep2Line.setBackgroundResource(R.color.colorAccent)
        tvStep2Desc.setTextColor(getColor(R.color.color_82808A))
        ivStep3Icon.visibility = View.VISIBLE
        tvStep3Desc.visibility = View.VISIBLE
        if (details.applicationStatus == SSOApplicationState.CHAIRMAN_UNAPPROVED) {
            vStep3Line.visibility = View.VISIBLE
            tvStep3Reason.visibility = View.VISIBLE
            ivStep3Icon.setBackgroundResource(R.drawable.ic_application_failed)
            tvStep3Desc.setText(R.string.apply_for_mintable_step_3_2)
            tvStep3Reason.text =
                getString(
                    R.string.apply_for_mintable_step_3_3,
                    if (details.unapprovedRemarks.isNullOrEmpty())
                        details.unapprovedReason
                    else
                        details.unapprovedRemarks
                )
            return
        }

        ivStep3Icon.setBackgroundResource(R.drawable.ic_application_completed)
        tvStep3Desc.text = getString(R.string.apply_for_mintable_step_3_1)
        vTransferAndNotifyLine.visibility = View.VISIBLE
        tvTransferAndNotifyDesc.visibility = View.VISIBLE
        tvTransferAndNotifyBtn.visibility = View.VISIBLE
        vApplyingUnpassedBlank.visibility = View.GONE
        if (details.applicationStatus == SSOApplicationState.GOVERNOR_TRANSFERRED) {
            refreshTransferAndNotifyBtn(3)
            return
        }

        mViewModel.mIsTransferredCoinToIssuerLD.observe(this, Observer {
            refreshTransferAndNotifyBtn(if (it) 2 else 1)
        })
    }

    // 1: no transfer and notify; 2: transferred; 3: transferred and notified
    private fun refreshTransferAndNotifyBtn(status: Int) {
        when (status) {
            1 -> {
                tvTransferAndNotifyBtn.setText(R.string.apply_for_mintable_action_1)
                tvTransferAndNotifyBtn.setTextColor(getColor(R.color.white))
                tvTransferAndNotifyBtn.isEnabled = true
            }
            2 -> {
                tvTransferAndNotifyBtn.setText(R.string.apply_for_mintable_action_2)
                tvTransferAndNotifyBtn.setTextColor(getColor(R.color.white))
                tvTransferAndNotifyBtn.isEnabled = true
            }
            else -> {
                tvTransferAndNotifyBtn.setText(R.string.apply_for_mintable_action_3)
                tvTransferAndNotifyBtn.setTextColor(getColor(R.color.colorAccent))
                tvTransferAndNotifyBtn.isEnabled = false
            }
        }
    }

    override fun initEvent() {
        super.initEvent()

        tvTransferAndNotifyBtn.setOnClickListener {
            if (mViewModel.mIsTransferredCoinToIssuerLD.value == true) {
                notifyIssuerCanApplyForMint()
            } else {
                mViewModel.transferCoinToSSO(this, TRANSFER_REQUEST_CODE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            TRANSFER_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    notifyIssuerCanApplyForMint()
                }
            }
        }
    }

    private fun notifyIssuerCanApplyForMint() {
        mViewModel.execute(
            action = ACTION_TRANSFER_COIN_TO_ISSUER,
            failureCallback = {
                if (mViewModel.mIsTransferredCoinToIssuerLD.value != true) {
                    mViewModel.mIsTransferredCoinToIssuerLD.postValue(true)
                }
            }
        ) {
            mSSOApplicationDetails.applicationStatus =
                SSOApplicationState.GOVERNOR_TRANSFERRED
            refreshTransferAndNotifyBtn(3)
        }
    }
}