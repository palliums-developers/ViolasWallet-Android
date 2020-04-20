package com.violas.wallet.repository.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.violas.wallet.biz.applysso.SSOApplyTokenStatus
import com.violas.wallet.repository.database.entity.ApplySSORecordDo

@Dao
interface ApplySSORecordDao : BaseDao<ApplySSORecordDo> {

    @Query("SELECT * FROM apply_sso_record WHERE wallet_address = :walletAddress AND status <= 3 LIMIT 1")
    fun findUnDoneRecord(walletAddress: String): ApplySSORecordDo?

    @Query("SELECT * FROM apply_sso_record WHERE wallet_address = :walletAddress AND sso_wallet_address =:ssoWalletAddress LIMIT 1")
    fun findSSOWalletUnDoneRecord(
        walletAddress: String,
        ssoWalletAddress: String
    ): ApplySSORecordDo?

    @Query("UPDATE apply_sso_record set status = :status WHERE wallet_address = :walletAddress AND application_id = :applicationId")
    fun updateRecordStatus(
        walletAddress: String,
        applicationId: String,
        @SSOApplyTokenStatus status: Int
    )

    @Query("UPDATE apply_sso_record set status = :status,tokenIdx = :tokenIdx,sso_wallet_address = :ssoWalletAddress WHERE wallet_address = :walletAddress AND application_id = :applicationId")
    fun updateRecordStatusAndTokenAddress(
        walletAddress: String,
        applicationId: String,
        tokenIdx: Long,
        ssoWalletAddress: String,
        @SSOApplyTokenStatus status: Int
    )

    @Query("SELECT * FROM apply_sso_record WHERE status >= 5 AND wallet_address = :walletAddress AND tokenIdx = :tokenIdx AND sso_wallet_address = :ssoWalletAddress")
    fun findUnMintRecord(
        walletAddress: String,
        tokenIdx: Long,
        ssoWalletAddress: String
    ): ApplySSORecordDo?

    @Query("DELETE from apply_sso_record WHERE application_id = :applicationId AND wallet_address = :accountAddress")
    fun remove(accountAddress: String, applicationId: String)
}