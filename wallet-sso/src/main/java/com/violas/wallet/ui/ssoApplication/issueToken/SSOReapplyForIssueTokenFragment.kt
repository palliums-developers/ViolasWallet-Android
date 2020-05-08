package com.violas.wallet.ui.ssoApplication.issueToken

import android.view.View
import com.palliums.utils.formatDate
import com.palliums.utils.getColor
import com.violas.wallet.R
import com.violas.wallet.biz.SSOApplicationState
import com.violas.wallet.repository.http.governor.SSOApplicationDetailsDTO
import com.violas.wallet.ui.selectCurrency.CurrencyFactory
import com.violas.wallet.utils.convertViolasTokenUnit
import kotlinx.android.synthetic.main.layout_apply_for_issue_token.*
import kotlinx.android.synthetic.main.layout_token_application_status.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 发行商重新申请发币视图（审核失败和审核失败时，基于上一次提交的信息继续申请）
 */
class SSOReapplyForIssueTokenFragment : BaseSSOApplyForIssueTokenFragment() {

    override fun getLayoutResId(): Int {
        return R.layout.fragment_sso_reapply_for_issue_token
    }

    override fun initView() {
        super.initView()

        mSSOApplicationDetails?.let {
            setApplicationInfo(it)
        }
    }

    private fun setApplicationInfo(details: SSOApplicationDetailsDTO) {
        tvStatusDesc.setTextColor(getColor(R.color.color_F55753))
        llSubDesc.visibility = View.VISIBLE
        if (details.applicationStatus == SSOApplicationState.GOVERNOR_UNAPPROVED
            || details.applicationStatus == SSOApplicationState.CHAIRMAN_UNAPPROVED
        ) {
            ivIcon.setBackgroundResource(R.drawable.ic_application_failed)
            tvStatusDesc.setText(R.string.sso_application_details_status_4)
            tvSubDescLabel.setText(R.string.token_application_status_label_reason)
            tvSubDesc.text = details.unapprovedReason
        } else {
            // 超时计算的规则: 州长未审核前才计算超时
            ivIcon.setBackgroundResource(R.drawable.ic_application_timeout)
            tvStatusDesc.setText(R.string.sso_application_details_status_5)
            tvSubDescLabel.setText(R.string.token_application_status_label_time)
            tvSubDesc.text = formatDate(details.expirationDate, pattern = "yyyy/MM/dd HH:mm")
        }

        launch(Dispatchers.Main) {
            mCurrencyBean = withContext(Dispatchers.IO) {
                val currency =
                    CurrencyFactory.parseCurrency(requireContext().assets.open("currency.json"))
                currency.forEach {
                    if (details.fiatCurrencyType == it.indicator)
                        return@withContext it
                }
                return@withContext null
            }

            mCurrencyBean?.let {
                tvContent.text = it.currency
                tvStableCurrencyValue.setContent("${it.exchange}")
            }
        }

        itemCoinNumber.setContent(convertViolasTokenUnit(details.tokenAmount))
        tvCoinNameContent.setText(details.tokenName)

        reservesImage = getImageRelativeUrl(details.reservePhotoUrl)
        upLoadViewReserves.setContentImage(details.reservePhotoUrl)

        accountPositiveImage = getImageRelativeUrl(details.bankChequePhotoPositiveUrl)
        upLoadViewAccountPositive.setContentImage(details.bankChequePhotoPositiveUrl)

        accountReverseImage = getImageRelativeUrl(details.bankChequePhotoBackUrl)
        upLoadViewAccountReverse.setContentImage(details.bankChequePhotoBackUrl)
    }

    /**
     * 获取图片的相对地址，因为上传图片文件返回的是相对地址，获取的图片是绝对地址
     */
    private fun getImageRelativeUrl(imageUrl: String): String {
        val strings = imageUrl.split("violas/photo/")
        return if (strings.size == 2) strings[1] else imageUrl
    }
}
