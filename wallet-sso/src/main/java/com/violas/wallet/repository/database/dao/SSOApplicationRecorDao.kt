package com.violas.wallet.repository.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.violas.wallet.biz.governorApproval.GovernorApprovalStatus
import com.violas.wallet.repository.database.entity.SSOApplicationRecordDo

@Dao
interface SSOApplicationRecorDao : BaseDao<SSOApplicationRecordDo> {

    @Query("SELECT * from sso_application_record WHERE wallet_address = :accountAddress AND application_id = :applicationId LIMIT 1")
    fun find(
        accountAddress: String,
        applicationId: String
    ): SSOApplicationRecordDo?

    @Query("SELECT * FROM sso_application_record WHERE wallet_address = :walletAddress AND application_id =:applicationId AND status <= 2 LIMIT 1")
    fun findUnApproveRecord(
        walletAddress: String,
        applicationId: String
    ): SSOApplicationRecordDo?

    @Query("SELECT * FROM sso_application_record WHERE wallet_address = :walletAddress AND application_id = :applicationId AND status >= 3 LIMIT 1")
    fun findUnMintRecord(
        walletAddress: String,
        applicationId: String
    ): SSOApplicationRecordDo?

    @Query("UPDATE sso_application_record set status = :status WHERE wallet_address = :walletAddress AND application_id = :applicationId")
    fun updateRecordStatus(
        walletAddress: String,
        applicationId: String,
        @GovernorApprovalStatus status: Int
    )

    @Query("DELETE FROM sso_application_record WHERE wallet_address = :accountAddress AND application_id = :applicationId")
    fun remove(accountAddress: String, applicationId: String)
}