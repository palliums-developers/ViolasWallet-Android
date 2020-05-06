package com.violas.wallet.ui.main.message

import android.os.Parcelable
import com.violas.wallet.biz.SSOApplicationState
import kotlinx.android.parcel.Parcelize

/**
 * Created by elephant on 2020/3/2 19:24.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: SSO发币申请消息
 */
@Parcelize
data class SSOApplicationMsgVO(
    val applicationId: String,          // 申请ID
    @SSOApplicationState
    var applicationStatus: Int,         // 申请状态
    val applicationDate: Long,          // 申请日期
    val expirationDate: Long,           // 申请失效日期
    val applicantIdName: String,        // 申请人身份姓名
    var msgRead: Boolean                // 消息已读，true: 已读; false: 未读
) : Parcelable