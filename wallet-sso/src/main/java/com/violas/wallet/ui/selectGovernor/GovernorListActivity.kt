package com.violas.wallet.ui.selectGovernor

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.palliums.utils.hideSoftInput
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.ui.selectCurrency.view.QuickIndexBar
import com.violas.wallet.ui.selectCurrency.view.StickyHeaderDecoration
import kotlinx.android.synthetic.main.activity_select_currency.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class GovernorListActivity : BaseAppActivity() {
    companion object {
        const val EXT_GOVERNOR_ITEM = "123"
        fun start(activity: Fragment, requestCode: Int) {
            val intent = Intent(activity.activity, GovernorListActivity::class.java)
            activity.startActivityForResult(intent, requestCode)
        }
    }

    private val mIssuerRepository by lazy {
        DataRepository.getIssuerService()
    }
    override fun getLayoutResId(): Int {
        return R.layout.activity_select_currency
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = getString(R.string.title_select_governor)

        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager

        launch(Dispatchers.IO) {
            val currency = (mIssuerRepository.getGovernorList().data?:ArrayList()).sortedBy { it.name }

            val currencyAdapter =
                GovernorAdapter(this@GovernorListActivity, ArrayList(currency)) {
                    val apply = Intent().apply {
                        putExtra(EXT_GOVERNOR_ITEM, it)
                    }
                    setResult(Activity.RESULT_OK, apply)
                    finish()
                }

            withContext(Dispatchers.Main) {
                recyclerView.addItemDecoration(StickyHeaderDecoration(currencyAdapter))
                recyclerView.adapter = currencyAdapter

                quickIndexBar.onLetterChangeListener =
                    object : QuickIndexBar.OnLetterChangeListener {
                        override fun onLetterChange(letter: String) {
                            // 隐藏软键盘
                            hideSoftInput(window.decorView)

                            for (i in currency.indices) {
                                if (letter == currency.get(i).getNameFirst() + "") {
                                    val position = currencyAdapter.getPositionForSection(
                                        currency.get(i).getNameFirst()
                                    )
                                    if (position != -1) {
                                        //滑动到指定位置
                                        layoutManager.scrollToPositionWithOffset(position, 0)
                                    }
                                    break
                                }
                            }
                        }

                        override fun onReset() {
                        }
                    }
            }
        }
    }
}
