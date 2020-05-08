package com.violas.wallet.ui.ssoApplication.issueToken

import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.palliums.utils.formatDate
import com.violas.wallet.R
import com.violas.wallet.image.GlideApp
import com.violas.wallet.repository.http.governor.SSOApplicationDetailsDTO
import com.violas.wallet.utils.convertViolasTokenUnit
import kotlinx.android.synthetic.main.layout_approval_issue_token_info.*

/**
 * Created by elephant on 2020/5/7 17:57.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 发行商申请发币审核中视图
 */
class SSOIssueTokenAuditingFragment : BaseSSOIssueTokenFragment() {

    override fun getLayoutResId(): Int {
        return R.layout.fragment_sso_issue_token_auditing
    }

    override fun initView() {
        super.initView()
        mSSOApplicationDetails?.let { setApplicationInfo(it) }
    }

    private fun setApplicationInfo(details: SSOApplicationDetailsDTO) {
        asivFiatCurrencyType.setContent(details.fiatCurrencyType)
        asivTokenAmount.setContent(convertViolasTokenUnit(details.tokenAmount))
        asivTokenValue.setContent("${details.tokenValue}${details.fiatCurrencyType}")
        asivTokenName.setContent(details.tokenName)
        asivSSOWalletAddress.setContent(details.ssoWalletAddress)
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

        GlideApp.with(this)
            .load(details.reservePhotoUrl)
            .centerCrop()
            .transition(DrawableTransitionOptions.withCrossFade())
            .placeholder(R.drawable.bg_id_card_front)
            .error(R.drawable.bg_id_card_front)
            .into(ivReservePhoto)
        GlideApp.with(this)
            .load(details.bankChequePhotoPositiveUrl)
            .centerCrop()
            .transition(DrawableTransitionOptions.withCrossFade())
            .placeholder(R.drawable.bg_id_card_front)
            .error(R.drawable.bg_id_card_front)
            .into(ivBankChequePhotoPositive)
        GlideApp.with(this)
            .load(details.bankChequePhotoBackUrl)
            .centerCrop()
            .transition(DrawableTransitionOptions.withCrossFade())
            .placeholder(R.drawable.bg_id_card_front)
            .error(R.drawable.bg_id_card_front)
            .into(ivBankChequePhotoBack)
    }
}