package com.violas.wallet.repository.database.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.violas.wallet.biz.governorApproval.GovernorApprovalStatus
import kotlinx.android.parcel.Parcelize

@Entity(
    tableName = SSOApplicationRecordDo.TABLE_NAME,
    indices = [Index(unique = true, value = ["wallet_address", "application_id"])]
)
@Parcelize
data class SSOApplicationRecordDo(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long? = null,
    @ColumnInfo(name = "wallet_address")
    var walletAddress: String = "",
    @ColumnInfo(name = "application_id")
    var applicationId: String = "",
    @GovernorApprovalStatus
    @ColumnInfo(name = "status")
    var status: Int = 0
) : Parcelable {

    companion object {
        const val TABLE_NAME = "sso_application_record"
    }
}