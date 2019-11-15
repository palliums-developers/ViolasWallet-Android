package com.violas.wallet.ui.changeLanguage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.violas.wallet.R
import com.violas.wallet.base.BaseActivity
import com.violas.wallet.utils.start
import kotlinx.android.synthetic.main.activity_change_language.*

class ChangeLanguageActivity : BaseActivity() {
    companion object {
        fun start(context: Context) {
            Intent(context, ChangeLanguageActivity::class.java).start(context)
        }
    }

    val viewModel by lazy {
        ViewModelProvider(this).get(ChangeLanguageViewModel::class.java)
    }

    override fun getLayoutResId() = R.layout.activity_change_language

    override fun getTitleStyle(): Int {
        return TITLE_STYLE_GREY_BACKGROUND
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.title_change_language)

        val adapter = ChangeLanguageAdapter(viewModel)
        recyclerView.adapter = adapter

//        val dividerItemDecoration = DividerItemDecoration(
//            this,
//            DividerItemDecoration.VERTICAL
//        )
//        dividerItemDecoration.setDrawable(
//            ResourcesCompat.getDrawable(
//                resources,
//                R.drawable.divider_unit_bitcoin_manager,
//                null
//            )!!
//        )
//        recyclerView.addItemDecoration(dividerItemDecoration)
        subscribeUi(adapter)
    }

    override fun onTitleLeftViewClick() {
        onBackPressed()
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
