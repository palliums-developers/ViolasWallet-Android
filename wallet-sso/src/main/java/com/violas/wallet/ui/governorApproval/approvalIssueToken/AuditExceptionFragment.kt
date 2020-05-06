package com.violas.wallet.ui.governorApproval.approvalIssueToken

import android.view.View
import com.palliums.utils.formatDate
import com.palliums.utils.getColor
import com.violas.wallet.R
import com.violas.wallet.repository.http.governor.SSOApplicationDetailsDTO
import kotlinx.android.synthetic.main.layout_token_application_status.*

/**
 * Created by elephant on 2020/4/29 21:34.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 州长审核未通过和审核已超时视图
 */
class AuditExceptionFragment : BaseApprovalIssueTokenFragment() {

    override fun getLayoutResId(): Int {
        return R.layout.fragment_audit_exception
    }

    override fun setApplicationInfo(details: SSOApplicationDetailsDTO) {
        super.setApplicationInfo(details)

        // 申请发币状态（负数为失败情况）：
        // -1：州长审核未通过；
        // -2：董事长审核未通过；
        // 其它为审核超时
        tvStatusDesc.setTextColor(getColor(R.color.color_F55753))
        llSubDesc.visibility = View.VISIBLE
        if (details.applicationStatus == -1) {
            ivIcon.setBackgroundResource(R.drawable.ic_application_unpassed)
            tvStatusDesc.setText(R.string.token_application_status_desc_unapproved)
            tvSubDescLabel.setText(R.string.token_application_status_label_reason)
            tvSubDesc.text = details.unapprovedReason
        } else {
            // TODO 1.超时计算的规则，是州长未审核前才计算超时，还是只要未成功铸币都计算超时; 2.替换超时图标
            ivIcon.setBackgroundResource(R.drawable.ic_application_unpassed)
            tvStatusDesc.setText(R.string.token_application_status_desc_timeout)
            tvSubDescLabel.setText(R.string.token_application_status_label_time)
            tvSubDesc.text = formatDate(details.expirationDate, pattern = "yyyy/MM/dd HH:mm")
        }
    }
}