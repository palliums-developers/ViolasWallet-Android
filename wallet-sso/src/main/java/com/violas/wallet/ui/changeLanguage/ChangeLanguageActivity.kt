package com.violas.wallet.ui.changeLanguage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.palliums.utils.start
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import kotlinx.android.synthetic.main.activity_change_language.*

class ChangeLanguageActivity : BaseAppActivity() {
    companion object {
        fun start(context: Context) {
            Intent(context, ChangeLanguageActivity::class.java).start(context)
        }
    }

    val viewModel by lazy {
        ViewModelProvider(this).get(ChangeLanguageViewModel::class.java)
    }

    override fun getLayoutResId() = R.layout.activity_change_language

    override fun getPageStyle(): Int {
        return PAGE_STYLE_PLIGHT_TITLE_SLIGHT_CONTENT
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.setting_multi_language)

        val adapter = ChangeLanguageAdapter(viewModel)
        recyclerView.adapter = adapter

        subscribeUi(adapter)
    }

    private fun subscribeUi(adapter: ChangeLanguageAdapter) {
        viewModel.mLanguageList.observe(this, Observer {
            adapter.submitList(it)
        })
    }

    override fun onBackPressedSupport() {
        viewModel.finish()
        super.onBackPressedSupport()
    }
}
