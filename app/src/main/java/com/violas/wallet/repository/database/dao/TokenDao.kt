package com.violas.wallet.repository.database.dao

import androidx.room.Dao
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.database.entity.TokenDo

@Dao
interface TokenDao : BaseDao<TokenDo> {
}