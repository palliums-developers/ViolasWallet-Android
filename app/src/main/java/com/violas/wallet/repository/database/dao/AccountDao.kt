package com.violas.wallet.repository.database.dao

import androidx.room.Dao
import com.violas.wallet.repository.database.entity.AccountDO

@Dao
interface AccountDao : BaseDao<AccountDO> {
}