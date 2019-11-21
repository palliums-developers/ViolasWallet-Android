package com.violas.wallet.repository.database.entity

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = TokenDo.TABLE_NAME,
    indices = [
        Index(unique = true, value = ["account_id", "tokenAddress"])
    ]
)
data class TokenDo(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long? = null,
    @ColumnInfo(name = "account_id")
    var account_id: Long = 0,
    @ColumnInfo(name = "tokenAddress")
    var tokenAddress: String = "",
    @ColumnInfo(name = "name")
    var name: String = "Libra",
    @ColumnInfo(name = "enable")
    var enable: Boolean = false,
    @ColumnInfo(name = "amount")
    var amount: Long = 0
) : Parcelable {

    constructor(source: Parcel) : this(
        source.readValue(Long::class.java.classLoader) as Long?,
        source.readLong(),
        source.readString()!!,
        source.readString()!!,
        1 == source.readInt(),
        source.readLong()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeValue(id)
        writeLong(account_id)
        writeString(tokenAddress)
        writeString(name)
        writeInt((if (enable) 1 else 0))
        writeLong(amount)
    }

    companion object {
        const val TABLE_NAME = "token"

        @JvmField
        val CREATOR: Parcelable.Creator<TokenDo> = object : Parcelable.Creator<TokenDo> {
            override fun createFromParcel(source: Parcel): TokenDo = TokenDo(source)
            override fun newArray(size: Int): Array<TokenDo?> = arrayOfNulls(size)
        }
    }
}