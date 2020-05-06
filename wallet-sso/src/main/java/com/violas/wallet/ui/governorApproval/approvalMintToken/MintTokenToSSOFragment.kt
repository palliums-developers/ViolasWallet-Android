package com.violas.wallet.ui.governorApproval.approvalMintToken

import com.palliums.utils.getColor
import com.violas.wallet.R
import com.violas.wallet.repository.http.governor.SSOApplicationDetailsDTO
import com.violas.wallet.ui.governorApproval.ApprovalFragmentViewModel.Companion.ACTION_MINT_TOKEN_TO_SSO
import com.violas.wallet.ui.governorApproval.GovernorApprovalActivity
import com.violas.wallet.utils.showPwdInputDialog
import kotlinx.android.synthetic.main.fragment_mint_token_to_sso.*
import kotlinx.android.synthetic.main.layout_token_application_status.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Created by elephant on 2020/4/29 22:17.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 州长给发行商铸币视图
 */
class MintTokenToSSOFragment : BaseApprovalMintTokenFragment() {

    override fun getLayoutResId(): Int {
        return R.layout.fragment_mint_token_to_sso
    }

    override fun setApplicationInfo(details: SSOApplicationDetailsDTO) {
        super.setApplicationInfo(details)

        ivIcon.setBackgroundResource(R.drawable.ic_application_passed)
        tvStatusDesc.setText(R.string.token_application_status_desc_approved)
        tvStatusDesc.setTextColor(getColor(R.color.color_00D1AF))
    }

    override fun initEvent() {
        super.initEvent()

        btnMint.setOnClickListener {
            showPwdInputDialog(
                mViewModel.mAccountLD.value!!,
                accountCallback = {
                    mViewModel.execute(it, action = ACTION_MINT_TOKEN_TO_SSO) {
                        startNewApprovalActivity()
                    }
                })
        }
    }
}