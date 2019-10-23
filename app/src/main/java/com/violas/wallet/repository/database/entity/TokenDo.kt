package com.violas.wallet.repository.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = TokenDo.TABLE_NAME,
    indices = [
        Index(unique = true, value = ["account_id", "name"])
    ]
)
data class TokenDo(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long? = null,
    @ColumnInfo(name = "account_id")
    var account_id: Long = 0,
    @ColumnInfo(name = "name")
    var name: String = "Libra",
    @ColumnInfo(name = "enable")
    var enable: Boolean = false,
    @ColumnInfo(name = "amount")
    var amount: Long = 0
) {
    companion object {
        const val TABLE_NAME = "token"
    }
}