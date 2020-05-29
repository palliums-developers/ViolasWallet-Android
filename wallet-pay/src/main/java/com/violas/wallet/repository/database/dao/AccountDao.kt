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
    @Query("SELECT * FROM account WHERE wallet_type = :walletType LIMIT 1")
    fun loadByWalletType(walletType: Int): AccountDO?

    /**
     * 加载所有指定钱包类型的钱包，并按创建时间升序排序
     */
    @Query("SELECT * FROM account WHERE wallet_type = :walletType ORDER BY modify_date ASC")
    fun loadAllByWalletType(walletType: Int): List<AccountDO>

    /**
     * 加载所有指定币种编号的钱包，并按创建时间升序排序
     */
    @Query("SELECT * FROM account WHERE coin_number = :coinNumber ORDER BY modify_date ASC")
    fun loadAllByCoinType(coinNumber: Int): List<AccountDO>

    /**
     * 加载所有钱包账号，并按创建时间升序排序
     */
    @Query("SELECT * FROM account ORDER BY modify_date ASC")
    fun loadAll(): List<AccountDO>

    /**
     * 查找所有指定钱包类型和币种编号的钱包，并按创建时间升序排序
     */
    @Query("SELECT * FROM account WHERE  wallet_type = :walletType AND coin_number = :coinNumber ORDER BY modify_date ASC")
    fun findAllByWalletTypeAndCoinType(walletType: Int, coinNumber: Int): List<AccountDO>?

    /**
     * 根据 ID 查找账户
     * @param id 账户 ID
     */
    @Query("SELECT * FROM account WHERE id = :id LIMIT 1")
    fun findById(id: Long): AccountDO?

    @Query("SELECT * FROM account WHERE coin_number = :coinType AND wallet_type = 0 LIMIT 1")
    fun findByCoinTypeByIdentity(coinType: Int): AccountDO?

    @Query("SELECT * FROM account WHERE coin_number = :coinType AND address = :address LIMIT 1")
    fun findByCoinTypeAndCoinAddress(coinType: Int, address: String): AccountDO?

    @Query("UPDATE ${AccountDO.TABLE_NAME} set encrypted_password = :encryptedPassword WHERE id = :id")
    fun updateEncryptedPassword(
        id: Long,
        encryptedPassword: ByteArray
    )
}