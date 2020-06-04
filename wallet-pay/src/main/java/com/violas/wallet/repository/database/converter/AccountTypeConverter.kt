package com.violas.wallet.repository.database.converter

import androidx.room.TypeConverter
import com.violas.wallet.repository.database.entity.AccountType

class AccountTypeConverter {
    @TypeConverter
    fun toAccountType(value: Int): AccountType {
        return AccountType.convert(value)
    }

    @TypeConverter
    fun toAccountTypeValue(accountType: AccountType): Int {
        return accountType.value
    }
}