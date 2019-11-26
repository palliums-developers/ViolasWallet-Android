package com.violas.wallet.ui.selectCurrency

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.ui.selectCurrency.CurrencyFactory.parseCurrency
import com.violas.wallet.ui.selectCurrency.view.QuickIndexBar
import com.violas.wallet.ui.selectCurrency.view.StickyHeaderDecoration
import kotlinx.android.synthetic.main.activity_select_currency.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class SelectCurrencyActivity : BaseAppActivity() {
    companion object {
        const val EXT_CURRENCY_ITEM = "123"
        fun start(activity: Fragment, requestCode: Int) {
            val intent = Intent(activity.activity, SelectCurrencyActivity::class.java)
            activity.startActivityForResult(intent, requestCode)
        }
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_select_currency
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = getString(R.string.title_select_currency)

        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager

        launch(Dispatchers.IO) {
            val currency = parseCurrency(assets.open("currency.json")).sortedBy {
                it.countryOrTerritory
            }

            val currencyAdapter =
                CurrencyAdapter(this@SelectCurrencyActivity, ArrayList(currency)) {
                    val apply = Intent().apply {
                        putExtra(EXT_CURRENCY_ITEM, it)
                    }
                    setResult(Activity.RESULT_OK, apply)
                    finish()
                }
            recyclerView.addItemDecoration(StickyHeaderDecoration(currencyAdapter))
            recyclerView.adapter = currencyAdapter

            withContext(Dispatchers.Main) {
                quickIndexBar.onLetterChangeListener =
                    object : QuickIndexBar.OnLetterChangeListener {
                        override fun onLetterChange(letter: String) {
                            val imm =
                                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            // 隐藏软键盘
                            imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
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
