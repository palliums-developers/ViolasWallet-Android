package com.violas.wallet.repository.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = TokenDo.TABLE_NAME,
    indices = [
        Index(unique = true, value = ["address", "module", "name", "account_id"])
    ]
)
data class TokenDo(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long? = null,
    @ColumnInfo(name = "account_id")
    var accountId: Long = 0,
    @ColumnInfo(name = "assets_name")
    var assetsName: String = "",
    @ColumnInfo(name = "address")
    var address: String = "",
    @ColumnInfo(name = "module")
    var module: String = "",
    @ColumnInfo(name = "name")
    var name: String = "",
    @ColumnInfo(name = "enable")
    var enable: Boolean = false,
    @ColumnInfo(name = "amount")
    var amount: Long = 0,
    @ColumnInfo(name = "logo")
    var logo: String = ""
) {
    companion object {
        const val TABLE_NAME = "token"
    }
}