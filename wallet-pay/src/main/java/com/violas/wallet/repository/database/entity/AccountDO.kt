package com.violas.wallet.repository.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(
    tableName = AccountDO.TABLE_NAME
)
data class AccountDO(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0,
    @ColumnInfo(name = "private_key", typeAffinity = ColumnInfo.BLOB)
    var privateKey: ByteArray = ByteArray(0),
    @ColumnInfo(name = "public_key")
    var publicKey: String = "",
    @ColumnInfo(name = "auth_key_prefix")
    var authKeyPrefix: String = "",
    @ColumnInfo(name = "mnemonic", typeAffinity = ColumnInfo.BLOB)
    var mnemonic: ByteArray = ByteArray(0),
    @ColumnInfo(name = "wallet_nickname")
    var walletNickname: String = "wallet",
    @ColumnInfo(name = "wallet_type")
    var walletType: Int = 0,
    @ColumnInfo(name = "coin_number")
    var coinNumber: Int = 0,
    @ColumnInfo(name = "address")
    var address: String = "",
    @ColumnInfo(name = "amount")
    var amount: Long = 0,
    @ColumnInfo(name = "modify_date")
    var modifyDate: Date = Date(System.currentTimeMillis())
) {
    companion object {
        const val TABLE_NAME = "account"
    }

    fun getEncryptedPasswordStr(): String? {
        return null
    }

    fun isOpenedBiometricPayment(): Boolean {
        return false
    }

    fun getBiometricKey(): String {
        return "account_${id}_$address"
    }
}