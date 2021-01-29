package com.violas.wallet.repository.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.violas.wallet.repository.database.entity.AddressBookDo

@Dao
interface AddressBookDao : BaseDao<AddressBookDo> {
    @Query("SELECT * FROM address_book")
    fun findAll(): List<AddressBookDo>

    @Query("SELECT * FROM address_book WHERE coin_number = :coinType")
    fun findByCoinType(coinType: Int): List<AddressBookDo>

    @Query("SELECT * FROM address_book WHERE coin_number = :coinNumber AND address = :address LIMIT 1")
    fun query(coinNumber: Int, address: String): AddressBookDo?
}