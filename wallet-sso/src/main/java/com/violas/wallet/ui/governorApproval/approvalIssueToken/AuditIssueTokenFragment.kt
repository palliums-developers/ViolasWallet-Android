package com.violas.wallet.ui.governorApproval.approvalIssueToken

import android.view.View
import androidx.lifecycle.Observer
import com.violas.wallet.R
import com.violas.wallet.repository.http.governor.SSOApplicationDetailsDTO
import com.violas.wallet.repository.http.governor.UnapproveReasonDTO
import com.violas.wallet.ui.governorApproval.ApprovalFragmentViewModel.Companion.ACTION_APPLY_FOR_MINT_POWER
import com.violas.wallet.ui.governorApproval.ApprovalFragmentViewModel.Companion.ACTION_LOAD_UNAPPROVE_REASONS
import com.violas.wallet.ui.governorApproval.ApprovalFragmentViewModel.Companion.ACTION_UNAPPROVE_APPLICATION
import com.violas.wallet.utils.showPwdInputDialog
import kotlinx.android.synthetic.main.fragment_audit_issue_token.*

/**
 * Created by elephant on 2020/4/27 18:47.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 州长审核发币申请视图
 */
class AuditIssueTokenFragment : BaseApprovalIssueTokenFragment() {

    override fun getLayoutResId(): Int {
        return R.layout.fragment_audit_issue_token
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
                    mViewModel.execute(action = ACTION_APPLY_FOR_MINT_POWER) {
                        // TODO 跳转处理
                    }
                })
        }

        tvUnapprove.setOnClickListener {
            val unapproveReasons = mViewModel.mUnapproveReasonsLD.value
            if (unapproveReasons.isNullOrEmpty()) {
                mViewModel.execute(action = ACTION_LOAD_UNAPPROVE_REASONS)
            } else {
                showSelectUnapproveReasonDialog(unapproveReasons)
            }
        }

        mViewModel.mUnapproveReasonsLD.observe(this, Observer {
            showSelectUnapproveReasonDialog(it)
        })
    }

    private fun showSelectUnapproveReasonDialog(unapproveReasons: List<UnapproveReasonDTO>) {
        SelectUnapproveReasonDialog.newInstance(unapproveReasons)
            .setOnConfirmCallback { reasonType, remark ->
                mViewModel.execute(
                    reasonType, remark,
                    action = ACTION_UNAPPROVE_APPLICATION
                ) {
                    // TODO 跳转处理
                }
            }
            .show(childFragmentManager, SelectUnapproveReasonDialog::javaClass.name)
    }
}