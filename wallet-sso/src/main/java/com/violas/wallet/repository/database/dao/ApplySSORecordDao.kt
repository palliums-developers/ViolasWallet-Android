package com.violas.wallet.repository.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.violas.wallet.repository.database.entity.ApplySSORecordDo

@Dao
interface ApplySSORecordDao : BaseDao<ApplySSORecordDo> {
    @Query("SELECT * FROM apply_sso_record WHERE account_id = :accountId AND status <= 3")
    fun findUnDoneRecord(accountId: Long): ApplySSORecordDo?
}