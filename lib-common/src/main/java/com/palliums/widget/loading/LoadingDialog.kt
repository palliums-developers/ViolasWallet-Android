package com.palliums.widget.loading

import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.palliums.R
import kotlinx.android.synthetic.main.widget_dialog_loading.*
import kotlinx.android.synthetic.main.widget_dialog_loading.view.*

class LoadingDialog : DialogFragment() {
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

            val params = attributes
            val windowManager = windowManager
            val display = windowManager.defaultDisplay
            val point = Point()
            display.getSize(point)
            params?.y = (point.y * 0.36).toInt()
            dialog?.window?.attributes = params
        }

        mRootView = inflater.inflate(R.layout.widget_dialog_loading, container)
        isCancelable = false
        changeContent(mRootView.tvContent, message)
        return mRootView
    }

    fun setMessage(msg: String?): LoadingDialog {
        message = msg
        if (dialog != null
            && dialog!!.isShowing
        ) {
            changeContent(tvContent, message)
        }
        return this
    }

    private fun changeContent(view: TextView, msg: String?) {
        if (msg != null && msg.isNotEmpty()) {
            view.text = msg
            view.visibility = View.VISIBLE
        } else {
            view.visibility = View.GONE
        }
    }
}