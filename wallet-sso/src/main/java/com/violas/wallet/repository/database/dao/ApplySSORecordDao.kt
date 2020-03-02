package com.violas.wallet.repository.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.violas.wallet.biz.applysso.SSOApplyTokenStatus
import com.violas.wallet.repository.database.entity.ApplySSORecordDo

@Dao
interface ApplySSORecordDao : BaseDao<ApplySSORecordDo> {
    @Query("SELECT * FROM apply_sso_record WHERE wallet_address = :walletAddress AND status <= 3")
    fun findUnDoneRecord(walletAddress: String): ApplySSORecordDo?

    @Query("UPDATE apply_sso_record set status = :status WHERE wallet_address = :walletAddress AND child_number = :childNumber")
    fun updateRecordStatus(
        walletAddress: String,
        childNumber: Long, @SSOApplyTokenStatus status: Int
    )

    @Query("UPDATE apply_sso_record set status = :status and token_address = :tokenAddress WHERE wallet_address = :walletAddress AND child_number = :childNumber")
    fun updateRecordStatusAndTokenAddress(
        walletAddress: String,
        childNumber: Long,
        tokenAddress: String,
        @SSOApplyTokenStatus status: Int
    )

    @Query("SELECT * FROM apply_sso_record WHERE status >= 5 AND token_address = :mintTokenAddress")
    fun findUnMintRecord(mintTokenAddress: String): ApplySSORecordDo?

    @Query("DELETE from apply_sso_record WHERE child_number = :layerWallet AND wallet_address = :accountAddress")
    fun remove(accountAddress: String, layerWallet: Long)
}