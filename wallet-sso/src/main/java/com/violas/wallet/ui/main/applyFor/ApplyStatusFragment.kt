package com.violas.wallet.ui.main.applyFor

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.palliums.base.BaseFragment
import com.palliums.utils.start
import com.violas.wallet.R
import com.violas.wallet.ui.applyForMint.ApplyForMintActivity
import com.violas.wallet.ui.mintSuccess.MintSuccessActivity
import kotlinx.android.synthetic.main.fragment_apply_status.*

class ApplyStatusFragment : BaseFragment() {
    companion object {
        private val EXT_STATUS = "EXT_STATUS"
        fun getInstance(status: Int): Fragment {
            val fragment = ApplyStatusFragment()
            fragment.arguments = Bundle().apply {
                putInt(EXT_STATUS, status)
            }
            return fragment
        }
    }

    private val status by lazy {
        arguments?.getInt(EXT_STATUS) ?: 1
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_apply_status
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTitle.text = getString(R.string.hint_mint_condition)
        when (status) {
            0 -> {
                tvContent.text = getString(R.string.hint_mint_wait)
                tvContent.setTextColor(Color.parseColor("#3C3848"))
            }
            1 -> {
                tvContent.text = getString(R.string.hint_mint_through)
                tvContent.setTextColor(Color.parseColor("#00D1AF"))
            }
            2 -> {
                tvContent.text = getString(R.string.hint_mint_error)
                tvContent.setTextColor(Color.parseColor("#F74E4E"))
            }
            3 -> {
                tvContent.text = getString(R.string.hint_mint_wait_publish)
                tvContent.setTextColor(Color.parseColor("#00D1AF"))
            }
            4 -> {
                tvContent.text = getString(R.string.hint_mint_success)
                tvContent.setTextColor(Color.parseColor("#00D1AF"))
            }
        }
        layoutItem.setOnClickListener {
            activity?.let { it1 ->
                when (status) {
                    1 -> {
                        Intent(
                            this.activity,
                            ApplyForMintActivity::class.java
                        ).start(it1)
                    }
                    4 -> {
                        Intent(
                            this.activity,
                            MintSuccessActivity::class.java
                        ).start(it1)
                    }
                }

            }
        }
    }
}
