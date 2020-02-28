package com.violas.wallet.ui.applyForLicence

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseViewModel
import com.palliums.content.App
import com.violas.wallet.R
import com.violas.wallet.base.BaseViewModelActivity
import com.violas.wallet.ui.main.MainActivity
import kotlinx.android.synthetic.main.activity_apply_for_licence.*

/**
 * Created by elephant on 2020/2/26 14:53.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 申请州长牌照页面
 */
class ApplyForLicenceActivity : BaseViewModelActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, ApplyForLicenceActivity::class.java))
        }
    }

    private val mViewModel by lazy {
        ViewModelProvider(this).get(ApplyForLicenceViewModel::class.java)
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_apply_for_licence
    }

    override fun getPageStyle(): Int {
        return PAGE_STYLE_PLIGHT_TITLE_SLIGHT_CONTENT
    }

    override fun getViewModel(): BaseViewModel {
        return mViewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.title_apply_for_licence)
        btnOkToSend.setOnClickListener(this)
        mViewModel.mAccountLD.observe(this, Observer {
            tvGovernorAddress.text = it.address
        })
    }

    override fun onTitleLeftViewClick() {
        handleBackAndComplete()
    }

    override fun onBackPressedSupport() {
        // 只有主页面存在时，点击手机返回键才返回
        if (App.existsActivity(MainActivity::class.java)) {
            super.onBackPressedSupport()
        }
    }

    override fun onViewClick(view: View) {
        when (view.id) {
            R.id.btnOkToSend -> {
                mViewModel.execute(etTxid.text.toString().trim()) {
                    handleBackAndComplete()
                }
            }
        }
    }

    private fun handleBackAndComplete() {
        if (App.existsActivity(MainActivity::class.java)) {
            close()
        } else {
            MainActivity.start(this)
            close()
        }
    }
}