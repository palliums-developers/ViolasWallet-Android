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
    /**
     * 钱包类型
     * 0: SSO 钱包
     * 1： 州长钱包
     * @see com.violas.wallet.biz.WalletType
     */
    @ColumnInfo(name = "wallet_type")
    var walletType: Int = 0,
    @ColumnInfo(name = "coin_number")
    var coinNumber: Int = 0,
    @ColumnInfo(name = "address")
    var address: String = "",
    @ColumnInfo(name = "amount")
    var amount: Long = 0,
    @ColumnInfo(name = "avoid_password")
    var avoidPassword: Boolean = false,
    @ColumnInfo(name = "modify_date")
    var modifyDate: Date = Date(System.currentTimeMillis())
) {
    companion object {
        const val TABLE_NAME = "account"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AccountDO

        if (id != other.id) return false
        if (!privateKey.contentEquals(other.privateKey)) return false
        if (publicKey != other.publicKey) return false
        if (authKeyPrefix != other.authKeyPrefix) return false
        if (!mnemonic.contentEquals(other.mnemonic)) return false
        if (walletNickname != other.walletNickname) return false
        if (walletType != other.walletType) return false
        if (coinNumber != other.coinNumber) return false
        if (address != other.address) return false
        if (amount != other.amount) return false
        if (avoidPassword != other.avoidPassword) return false
        if (modifyDate != other.modifyDate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + privateKey.contentHashCode()
        result = 31 * result + publicKey.hashCode()
        result = 31 * result + authKeyPrefix.hashCode()
        result = 31 * result + mnemonic.contentHashCode()
        result = 31 * result + walletNickname.hashCode()
        result = 31 * result + walletType
        result = 31 * result + coinNumber
        result = 31 * result + address.hashCode()
        result = 31 * result + amount.hashCode()
        result = 31 * result + avoidPassword.hashCode()
        result = 31 * result + modifyDate.hashCode()
        return result
    }
}