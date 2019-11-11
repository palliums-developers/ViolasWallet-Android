package com.violas.wallet.repository.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = AddressBookDo.TABLE_NAME
)
data class AddressBookDo(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0,
    @ColumnInfo(name = "note")
    var note: String = "",
    @ColumnInfo(name = "address")
    var address: String = "",
    @ColumnInfo(name = "coin_number")
    var coin_number: Int = 0
) {
    companion object {
        const val TABLE_NAME = "address_book"
    }
}