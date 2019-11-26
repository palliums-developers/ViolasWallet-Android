package com.violas.wallet.ui.main.applyFor

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.palliums.base.BaseFragment
import com.violas.wallet.R
import com.violas.wallet.ui.selectCurrency.SelectCurrencyActivity
import com.violas.wallet.ui.selectCurrency.bean.CurrencyBean
import kotlinx.android.synthetic.main.fragment_apply_submit.*

class ApplySubmitFragment : BaseFragment() {
    companion object {
        private const val REQUEST_CURRENCY_CODE = 0
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_apply_submit
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvContent.setOnClickListener {
            SelectCurrencyActivity.start(this@ApplySubmitFragment, REQUEST_CURRENCY_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CURRENCY_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    val parcelable =
                        data?.getParcelableExtra<CurrencyBean>(SelectCurrencyActivity.EXT_CURRENCY_ITEM)
                    tvContent.text = parcelable?.currency
                    tvStableCurrencyValue.setContent("${parcelable?.exchange}")
                }
            }
        }
    }
}
