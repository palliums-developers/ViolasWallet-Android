package com.violas.wallet.ui.ssoApplication.mintToken

import com.violas.wallet.R
import com.violas.wallet.ui.ssoApplication.SSOApplicationChildViewModel.Companion.ACTION_APPLY_FOR_MINT_TOKEN
import com.violas.wallet.utils.showPwdInputDialog
import kotlinx.android.synthetic.main.fragment_sso_apply_for_mint_token.*

/**
 * Created by elephant on 2020/5/7 19:14.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 发行商申请铸币视图
 */
class SSOApplyForMintTokenFragment : BaseSSOMintTokenFragment() {

    override fun getLayoutResId(): Int {
        return R.layout.fragment_sso_apply_for_mint_token
    }

    override fun initEvent() {
        super.initEvent()

        btnMint.setOnClickListener {
            val accountDO = mViewModel.mAccountLD.value ?: return@setOnClickListener
            showPwdInputDialog(
                accountDO,
                accountCallback = {
                    mViewModel.execute(it, action = ACTION_APPLY_FOR_MINT_TOKEN) {
                        startNewApplicationActivity()
                    }
                }
            )
        }
    }
}