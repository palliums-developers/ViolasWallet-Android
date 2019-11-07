package com.violas.wallet.widget.refresh

import android.content.Context
import android.util.AttributeSet
import com.scwang.smartrefresh.layout.header.ClassicsHeader
import com.violas.wallet.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by elephant on 2019-08-19 14:47.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: RefreshClassicsHeader
 */
class RefreshClassicsHeader : ClassicsHeader {

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        mArrowDrawable.setColor(context.resources.getColor(R.color.colorAccent))
        mProgressDrawable.setColor(context.resources.getColor(R.color.colorAccent))

        mTextPulling = context.getString(R.string.refresh_header_pulling)
        mTextRefreshing = context.getString(R.string.refresh_header_refreshing)
        mTextLoading = context.getString(R.string.refresh_header_loading)
        mTextRelease = context.getString(R.string.refresh_header_release)
        mTextFinish = context.getString(R.string.refresh_header_finish)
        mTextFailed = context.getString(R.string.refresh_header_failed)
        mTextUpdate = context.getString(R.string.refresh_header_update)

        mLastUpdateFormat = SimpleDateFormat(mTextUpdate, Locale.getDefault())
        setLastUpdateTime(Date(mShared.getLong(KEY_LAST_UPDATE_TIME, System.currentTimeMillis())))
    }
}