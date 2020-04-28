package com.violas.wallet.ui.governorApproval.approvalIssueToken

import android.view.View
import androidx.lifecycle.Observer
import com.violas.wallet.R
import com.violas.wallet.repository.http.governor.SSOApplicationDetailsDTO
import com.violas.wallet.repository.http.governor.UnapproveReasonDTO
import com.violas.wallet.ui.governorApproval.GovernorApprovalViewModel.Companion.ACTION_APPLY_FOR_MINT_POWER
import com.violas.wallet.ui.governorApproval.GovernorApprovalViewModel.Companion.ACTION_LOAD_UNAPPROVE_REASONS
import com.violas.wallet.ui.governorApproval.GovernorApprovalViewModel.Companion.ACTION_UNAPPROVE_APPLICATION
import com.violas.wallet.utils.showPwdInputDialog
import kotlinx.android.synthetic.main.fragment_governor_audit.*

/**
 * Created by elephant on 2020/4/27 18:47.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 州长审核发币申请视图
 */
class GovernorAuditFragment : BaseApprovalIssueTokenFragment() {

    override fun getLayoutResId(): Int {
        return R.layout.fragment_governor_audit
    }

    override fun setApplicationInfo(details: SSOApplicationDetailsDTO) {
        super.setApplicationInfo(details)

        tvApplicationTitle.text =
            getString(R.string.format_sso_application_title, details.idName, details.tokenName)
        tvApplicationStatus.visibility = View.GONE
    }

    override fun initEvent() {
        super.initEvent()

        btnApproveAndApplyForMintPower.setOnClickListener {
            showPwdInputDialog(
                mViewModel.mAccountLD.value!!,
                accountCallback = {
                    mViewModel.execute(action = ACTION_APPLY_FOR_MINT_POWER)
                })
        }

        tvUnapprove.setOnClickListener {
            val unapproveReasons = mViewModel.mUnapproveReasons.value
            if (unapproveReasons.isNullOrEmpty()) {
                mViewModel.execute(action = ACTION_LOAD_UNAPPROVE_REASONS)
            } else {
                showSelectUnapproveReasonDialog(unapproveReasons)
            }
        }

        mViewModel.mUnapproveReasons.observe(this, Observer {
            showSelectUnapproveReasonDialog(it)
        })
    }

    private fun showSelectUnapproveReasonDialog(unapproveReasons: List<UnapproveReasonDTO>) {
        SelectUnapproveReasonDialog.newInstance(
            unapproveReasons
        )
            .setOnConfirmCallback { reasonType, remark ->
                mViewModel.execute(
                    reasonType, remark,
                    action = ACTION_UNAPPROVE_APPLICATION
                )
            }
            .show(childFragmentManager, SelectUnapproveReasonDialog::javaClass.name)
    }
}