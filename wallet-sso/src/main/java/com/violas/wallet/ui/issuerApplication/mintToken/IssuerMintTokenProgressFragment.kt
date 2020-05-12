package com.violas.wallet.ui.issuerApplication.mintToken

import android.view.View
import com.palliums.utils.getColor
import com.violas.wallet.R
import com.violas.wallet.biz.SSOApplicationState
import com.violas.wallet.repository.http.issuer.ApplyForSSODetailsDTO
import kotlinx.android.synthetic.main.layout_token_application_status.*

/**
 * Created by elephant on 2020/5/7 18:43.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:  发行商申请铸币进度视图（包括铸币审核中和铸币成功）
 */
class IssuerMintTokenProgressFragment : BaseIssuerMintTokenFragment() {

    override fun getLayoutResId(): Int {
        return R.layout.fragment_issuer_mint_token_progress
    }

    override fun setApplicationInfo(details: ApplyForSSODetailsDTO) {
        super.setApplicationInfo(details)

        if (details.applicationStatus == SSOApplicationState.GOVERNOR_MINTED) {
            ivIcon.setBackgroundResource(R.drawable.ic_application_completed)
            tvStatusDesc.setText(R.string.sso_application_details_status_3_1)
            tvStatusDesc.setTextColor(getColor(R.color.color_00D1AF))
            llSubDesc.visibility = View.VISIBLE
            tvSubDescLabel.visibility = View.GONE
            tvSubDesc.setText(R.string.sso_application_details_status_3_2)
        } else {
            ivIcon.setBackgroundResource(R.drawable.ic_application_processing)
            tvStatusDesc.setText(R.string.sso_application_details_status_2)
            tvStatusDesc.setTextColor(getColor(R.color.color_FAA030))
        }
    }
}