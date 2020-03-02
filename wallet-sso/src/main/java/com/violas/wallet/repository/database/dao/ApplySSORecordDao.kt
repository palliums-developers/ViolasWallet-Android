package com.violas.wallet.repository.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.violas.wallet.biz.applysso.SSOApplyTokenStatus
import com.violas.wallet.repository.database.entity.ApplySSORecordDo

@Dao
interface ApplySSORecordDao : BaseDao<ApplySSORecordDo> {
    @Query("SELECT * FROM apply_sso_record WHERE account_id = :accountId AND status <= 3")
    fun findUnDoneRecord(accountId: Long): ApplySSORecordDo?

    @Query("UPDATE apply_sso_record set status = :status WHERE account_id = :accountId AND wallet_address = :walletAddress AND child_number = :childNumber")
    fun updateRecordStatus(
        accountId: Long,
        walletAddress: String,
        childNumber: Long, @SSOApplyTokenStatus status: Int
    )

    @Query("UPDATE apply_sso_record set status = :status and token_address = :tokenAddress WHERE account_id = :accountId AND wallet_address = :walletAddress AND child_number = :childNumber")
    fun updateRecordStatusAndTokenAddress(
        accountId: Long,
        walletAddress: String,
        childNumber: Long,
        tokenAddress: String,
        @SSOApplyTokenStatus status: Int
    )

    @Query("SELECT * FROM apply_sso_record WHERE account_id = :accountId AND status >= 5 AND token_address = :mintTokenAddress")
    fun findUnMintRecord(accountId: Long, mintTokenAddress: String): ApplySSORecordDo?

    @Query("DELETE from apply_sso_record WHERE account_id = :accountId AND child_number = :layerWallet AND wallet_address = :accountAddress")
    fun remove(accountId: Long, accountAddress: String, layerWallet: Long)
}