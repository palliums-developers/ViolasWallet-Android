package com.violas.wallet.widget.dialog

import android.content.Context
import com.lxj.xpopup.core.BottomPopupView
import com.lxj.xpopup.util.XPopupUtils
import com.violas.wallet.R
import kotlinx.android.synthetic.main.custom_bottom_take_photo_popup.view.*


class TakePhotoPopup(
    context: Context,
    private val photo: () -> Unit,
    private val album: () -> Unit
) :
    BottomPopupView(context) {


    override fun getImplLayoutId(): Int {
        return R.layout.custom_bottom_take_photo_popup
    }

    override fun onCreate() {
        super.onCreate()
        btnAlbum.setOnClickListener {
            album.invoke()
            dismiss()
        }

        btnTakePhoto.setOnClickListener {
            photo.invoke()
            dismiss()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }
    }

    // 最大高度为Window的0.85
    override fun getMaxHeight(): Int {
        return XPopupUtils.dp2px(context, 185F)
    }
}