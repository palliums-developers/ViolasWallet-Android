package com.violas.wallet.ui.issuerApplication.issueToken

import com.palliums.utils.formatDate
import com.palliums.utils.getColor
import com.violas.wallet.R
import com.violas.wallet.image.viewImage
import com.violas.wallet.repository.http.issuer.ApplyForSSODetailsDTO
import com.violas.wallet.utils.convertViolasTokenUnit
import kotlinx.android.synthetic.main.layout_approval_issue_token_info.*
import kotlinx.android.synthetic.main.layout_token_application_status.*

/**
 * Created by elephant on 2020/5/7 17:57.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 发行商申请发币审核中视图
 */
class IssuerIssueTokenAuditingFragment : BaseIssuerIssueTokenFragment() {

    override fun getLayoutResId(): Int {
        return R.layout.fragment_issuer_issue_token_auditing
    }

    override fun initView() {
        super.initView()
        mApplyForSSODetails?.let {
            setApplyForSSOInfo(it)
        }
    }

    override fun initEvent() {
        super.initEvent()

        uivReservePhoto.setOnClickListener {
            uivReservePhoto.getContentImageView()?.let {
                mApplyForSSODetails?.reservePhotoUrl?.let { url ->
                    it.viewImage(url)
                }
            }
        }
        uivBankChequePhotoPositive.setOnClickListener {
            uivBankChequePhotoPositive.getContentImageView()?.let {
                mApplyForSSODetails?.bankChequePhotoPositiveUrl?.let { url ->
                    it.viewImage(url)
                }
            }
        }
        uivBankChequePhotoBack.setOnClickListener {
            uivBankChequePhotoBack.getContentImageView()?.let {
                mApplyForSSODetails?.bankChequePhotoBackUrl?.let { url ->
                    it.viewImage(url)
                }
            }
        }
    }

    private fun setApplyForSSOInfo(details: ApplyForSSODetailsDTO) {
        ivIcon.setBackgroundResource(R.drawable.ic_application_processing)
        tvStatusDesc.setText(R.string.sso_application_details_status_1)
        tvStatusDesc.setTextColor(getColor(R.color.color_FAA030))

        asivFiatCurrencyType.setContent(details.fiatCurrencyType)
        asivTokenAmount.setContent(convertViolasTokenUnit(details.tokenAmount))
        asivTokenValue.setContent("${details.tokenValue}${details.fiatCurrencyType}")
        asivTokenName.setContent(details.tokenName)
        asivSSOWalletAddress.setContent(details.issuerWalletAddress)
        asivApplicationDate.setContent(
            formatDate(details.applicationDate, pattern = "yyyy.MM.dd HH:mm")
        )
        asivApplicationPeriod.setContent(getString(R.string.format_days, details.applicationPeriod))
        asivExpirationDate.setContent(
            formatDate(details.expirationDate, pattern = "yyyy.MM.dd HH:mm")
        )

        asivIDName.setContent(details.idName)
        asivNationality.setContent(details.countryName)
        // TODO 证件类型没有区分
        asivIDType.setContent(getString(R.string.id_card))
        asivIDNumber.setContent(details.idNumber)

        /*val phoneAreaCode = if (details.phoneAreaCode.startsWith("+"))
            details.phoneAreaCode
        else
            "+${details.phoneAreaCode}"*/
        val phoneEndIndex = if (details.phoneNumber.length > 7)
            7
        else
            details.phoneNumber.length
        val phoneStartIndex = if (phoneEndIndex >= 3) 3 else phoneEndIndex
        var phoneReplacement = ""
        for (index in phoneStartIndex until phoneEndIndex) {
            phoneReplacement += "*"
        }
        val phoneNumber = details.phoneNumber.replaceRange(
            phoneStartIndex, phoneEndIndex, phoneReplacement
        )
        //asivPhoneNumber.setContent("$phoneAreaCode $phoneNumber")
        asivPhoneNumber.setContent(phoneNumber)

        val emailEndIndex = details.emailAddress.indexOf("@")
        val emailStartIndex = if (emailEndIndex >= 3) 3 else emailEndIndex
        var emailReplacement = ""
        for (index in emailStartIndex until emailEndIndex) {
            emailReplacement += "*"
        }
        val emailAddress = details.emailAddress.replaceRange(
            emailStartIndex, emailEndIndex, emailReplacement
        )
        asivEmail.setContent(emailAddress)

        uivReservePhoto.setContentImage(details.reservePhotoUrl, true)
        uivBankChequePhotoPositive.setContentImage(details.bankChequePhotoPositiveUrl, true)
        uivBankChequePhotoBack.setContentImage(details.bankChequePhotoBackUrl, true)
    }
}