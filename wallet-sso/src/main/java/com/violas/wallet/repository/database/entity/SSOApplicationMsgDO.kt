package com.violas.wallet.repository.database.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

/**
 * Created by elephant on 2020/3/3 19:50.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
@Entity(
    tableName = SSOApplicationMsgDO.TABLE_NAME,
    indices = [Index(unique = true, value = ["account_id", "application_id"])]
)
@Parcelize
data class SSOApplicationMsgDO(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long? = null,                       // 自增id
    @ColumnInfo(name = "account_id")
    var accountId: Long = 0,                    // 州长钱包账户id
    @ColumnInfo(name = "application_id")
    var applicationId: String = "",             // 申请id
    @ColumnInfo(name = "application_date")
    var applicationDate: Long = 0,              // 申请时间
    @ColumnInfo(name = "application_status")
    var applicationStatus: Int = 0,             // 申请状态 0: not approved; 1: pass; 2: not pass; 3: published; 4: minted
    @ColumnInfo(name = "applicant_id_name")
    var applicantIdName: String,                // 申请人身份姓名
    @ColumnInfo(name = "issuing_unread")
    var issuingUnread: Boolean = true,          // 发币请求未读
    @ColumnInfo(name = "mint_unread")
    var mintUnread: Boolean = true              // 铸币请求未读
) : Parcelable {
    companion object {
        const val TABLE_NAME = "sso_application_msg"
    }
}