package com.violas.wallet.biz

import com.palliums.violas.http.Response
import com.violas.wallet.repository.DataRepository.getSSOService
import com.violas.wallet.repository.http.sso.ApplyForStatusDTO
import java.io.File

class ApplyManager {
    private val mSSOService by lazy {
        getSSOService()
    }

    suspend fun getApplyStatus(address: String): Response<ApplyForStatusDTO>? {
        return mSSOService.selectApplyForStatus(address)
    }

    suspend fun uploadImage(file: File) = mSSOService.uploadImage(file)

}
