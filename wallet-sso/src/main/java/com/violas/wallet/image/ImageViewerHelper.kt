package com.violas.wallet.image

import android.widget.ImageView
import com.lxj.xpopup.XPopup

/**
 * Created by elephant on 2020/5/9 11:37.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

fun ImageView.viewImage(uri: Any) {
    XPopup.Builder(context)
        .asImageViewer(
            this,
            uri,
            false,
            -1,
            -1,
            -1,
            false,
            ImageLoader()
        )
        .show()
}