package com.violas.wallet.repository.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.violas.wallet.repository.database.entity.AccountDO

@Dao
interface AccountDao : BaseDao<AccountDO> {

    /**
     * 加载一个指定钱包类型的钱包
     * @param walletType 0：身份钱包；1：非身份钱包
     */
    @Query("SELECT * FROM account WHERE wallet_type LIKE :walletType LIMIT 1")
    fun loadByWalletType(walletType: Int): AccountDO?

    /**
     * 根据 ID 查找账户
     * @param id 账户 ID
     */
    @Query("SELECT * FROM account WHERE id = :id LIMIT 1")
    fun findById(id: Long): AccountDO?
}