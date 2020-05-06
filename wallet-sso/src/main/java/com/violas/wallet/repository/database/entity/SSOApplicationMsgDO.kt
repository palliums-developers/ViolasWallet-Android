package com.violas.wallet.repository.database.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.violas.wallet.biz.SSOApplicationState
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
    var accountId: Long,                        // 州长钱包账户id
    @ColumnInfo(name = "application_id")
    var applicationId: String,                  // 申请id
    @SSOApplicationState
    @ColumnInfo(name = "application_status")
    var applicationStatus: Int,                 // 申请状态
    @ColumnInfo(name = "application_date")
    var applicationDate: Long,                  // 申请时间
    @ColumnInfo(name = "expiration_date")
    val expirationDate: Long,                   // 申请失效日期
    @ColumnInfo(name = "applicant_id_name")
    var applicantIdName: String,                // 申请人身份姓名
    @ColumnInfo(name = "issue_read")
    var issueRead: Boolean,                     // 发币请求已读
    @ColumnInfo(name = "mint_read")
    var mintRead: Boolean                       // 铸币请求已读
) : Parcelable {
    companion object {
        const val TABLE_NAME = "sso_application_msg"
    }
}