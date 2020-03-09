package com.violas.wallet.repository.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.violas.wallet.repository.database.entity.SSOApplicationMsgDO

/**
 * Created by elephant on 2020/3/3 21:33.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
@Dao
interface SSOApplicationMsgDao : BaseDao<SSOApplicationMsgDO> {

    @Query("SELECT * FROM ${SSOApplicationMsgDO.TABLE_NAME} WHERE account_id = :accountId AND application_id IN (:applicationIds)")
    fun loadMsgsFromApplicationIds(
        accountId: Long,
        vararg applicationIds: String
    ): List<SSOApplicationMsgDO>

    @Query("SELECT * FROM ${SSOApplicationMsgDO.TABLE_NAME} WHERE account_id = :accountId AND application_id = :applicationId")
    fun loadMsgFromApplicationId(
        accountId: Long,
        applicationId: String
    ): SSOApplicationMsgDO?

    @Query("SELECT * FROM ${SSOApplicationMsgDO.TABLE_NAME} WHERE account_id = :accountId AND application_id = :applicationId")
    fun loadMsgLiveDataFromApplicationId(
        accountId: Long,
        applicationId: String
    ): LiveData<SSOApplicationMsgDO?>
}