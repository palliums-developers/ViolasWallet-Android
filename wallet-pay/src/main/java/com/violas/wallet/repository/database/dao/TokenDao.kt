package com.violas.wallet.repository.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.violas.wallet.repository.database.entity.TokenDo

@Dao
interface TokenDao : BaseDao<TokenDo> {
    @Query("SELECT * FROM token WHERE id = :id LIMIT 1")
    fun findById(id: Long): TokenDo?

    @Query("SELECT * FROM token WHERE account_id = :accountId")
    fun findByAccountId(accountId: Long): List<TokenDo>

    @Query("SELECT * FROM token WHERE account_id = :accountId AND enable = 1")
    fun findEnableTokenByAccountId(accountId: Long): List<TokenDo>
}