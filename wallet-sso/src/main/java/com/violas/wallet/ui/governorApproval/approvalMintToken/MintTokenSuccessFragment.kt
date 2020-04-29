package com.violas.wallet.ui.governorApproval.approvalMintToken

import com.violas.wallet.R
import com.violas.wallet.repository.http.governor.SSOApplicationDetailsDTO
import kotlinx.android.synthetic.main.layout_token_application_status.*

/**
 * Created by elephant on 2020/4/29 22:48.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 州长铸币成功视图
 */
class MintTokenSuccessFragment : BaseApprovalMintTokenFragment() {

    override fun getLayoutResId(): Int {
        return R.layout.fragment_mint_token_success
    }

    override fun setApplicationInfo(details: SSOApplicationDetailsDTO) {
        super.setApplicationInfo(details)

        ivIcon.setBackgroundResource(R.drawable.ic_application_passed)
        tvStatusDesc.setText(R.string.token_application_status_desc_minted)
    }
}