package com.palliums.widget.popup

import android.content.Context
import com.lxj.xpopup.impl.AttachListPopupView

/**
 * Created by elephant on 2019-08-12 13:12.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: [AttachListPopupView] 加强版，支持在显示前和消失前回调
 */
open class EnhancedAttachListPopupView(
    context: Context,
    bindLayoutId: Int = 0,
    bindItemLayoutId: Int = 0
) : AttachListPopupView(context, bindLayoutId, bindItemLayoutId) {

    override fun doAfterShow() {
        popupInfo?.xPopupCallback?.run {
            if (this is EnhancedPopupCallback) {
                onShowBefore(this@EnhancedAttachListPopupView)
            }
        }

        super.doAfterShow()
    }

    override fun doAfterDismiss() {
        popupInfo?.xPopupCallback?.run {
            if (this is EnhancedPopupCallback) {
                onDismissBefore(this@EnhancedAttachListPopupView)
            }
        }

        super.doAfterDismiss()
    }
}