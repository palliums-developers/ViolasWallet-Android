package com.violas.wallet.ui.main.message

import android.os.Parcelable
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
    val applicationDate: Long,          // 申请日期
    val applicationStatus: Int,         // 申请状态 0: not approved; 1: pass; 2: not pass; 3: published; 4: minted
    val applicantIdName: String,        // 申请人身份姓名
    var msgUnread: Boolean              // 消息未读已读，true: 未读; false: 已读
) : Parcelable