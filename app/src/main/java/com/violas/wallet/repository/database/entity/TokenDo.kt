package com.violas.wallet.repository.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = TokenDo.TABLE_NAME
)
data class TokenDo(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0,
    @ColumnInfo(name = "account_id")
    var account_id: Long = 0,
    @ColumnInfo(name = "name")
    var name: String = "Libra",
    @ColumnInfo(name = "switch")
    var switch: Boolean = false,
    @ColumnInfo(name = "amount")
    var amount: Long = 0
) {
    companion object {
        const val TABLE_NAME = "token"
    }
}