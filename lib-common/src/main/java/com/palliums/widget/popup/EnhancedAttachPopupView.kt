package com.palliums.widget.popup

import android.content.Context
import com.lxj.xpopup.core.AttachPopupView

/**
 * Created by elephant on 2020/6/30 09:43.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: [AttachPopupView] 加强版，支持在显示前和消失前回调
 */
open class EnhancedAttachPopupView(context: Context) : AttachPopupView(context) {

    override fun doAfterShow() {
        popupInfo?.xPopupCallback?.run {
            if (this is EnhancedPopupCallback) {
                onShowBefore()
            }
        }

        super.doAfterShow()
    }

    override fun doAfterDismiss() {
        popupInfo?.xPopupCallback?.run {
            if (this is EnhancedPopupCallback) {
                onDismissBefore()
            }
        }

        super.doAfterDismiss()
    }
}