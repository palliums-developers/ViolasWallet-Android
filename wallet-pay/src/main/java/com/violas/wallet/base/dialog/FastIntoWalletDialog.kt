package com.violas.wallet.base.dialog

import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.violas.wallet.R
import kotlinx.android.synthetic.main.dialog_fast_into_wallet.view.*

class FastIntoWalletDialog : DialogFragment() {
    private lateinit var mRootView: View

    private var message: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.TOP or Gravity.CENTER)

            val params = attributes;
            val windowManager = windowManager
            val display = windowManager.defaultDisplay
            val point = Point()
            display.getSize(point)
            params?.y = (point.y * 0.18).toInt()
            dialog?.window?.attributes = params
        }

        mRootView = inflater.inflate(R.layout.dialog_fast_into_wallet, container)
        isCancelable = false
        mRootView.btnConfirm.setOnClickListener {
            dismiss()
        }
        return mRootView
    }
}