package com.violas.wallet.ui.issuerApplication.issueToken

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.Editable
import androidx.lifecycle.Observer
import com.lxj.xpopup.XPopup
import com.palliums.utils.TextWatcherSimple
import com.sl.utakephoto.exception.TakeException
import com.sl.utakephoto.manager.ITakePhotoResult
import com.sl.utakephoto.manager.UTakePhoto
import com.violas.wallet.R
import com.violas.wallet.image.viewImage
import com.violas.wallet.repository.http.issuer.GovernorDTO
import com.violas.wallet.ui.selectCurrency.SelectCurrencyActivity
import com.violas.wallet.ui.selectCurrency.bean.CurrencyBean
import com.violas.wallet.ui.selectGovernor.GovernorListActivity
import com.violas.wallet.ui.issuerApplication.IssuerApplicationChildViewModel.Companion.ACTION_APPLY_FOR_ISSUE_TOKEN
import com.violas.wallet.utils.getFilePathFromContentUri
import com.violas.wallet.widget.dialog.EmailPhoneValidationDialog
import com.violas.wallet.widget.dialog.TakePhotoPopup
import kotlinx.android.synthetic.main.fragment_issuer_apply_for_issue_token.*
import kotlinx.android.synthetic.main.layout_apply_for_issue_token.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 发行商申请发币 base fragment
 */
abstract class BaseIssuerApplyForIssueTokenFragment : BaseIssuerIssueTokenFragment() {

    companion object {
        private const val REQUEST_CURRENCY_CODE = 0
        private const val REQUEST_GOVERNOR_CODE = 4
        private const val REQUEST_PHOTO_RESERVES = 1
        private const val REQUEST_PHOTO_ACCOUNT_POSITIVE = 2
        private const val REQUEST_PHOTO_ACCOUNT_REVERSE = 3
    }

    protected var reservesImage: String? = null
    protected var accountPositiveImage: String? = null
    protected var accountReverseImage: String? = null
    protected var mCurrencyBean: CurrencyBean? = null
    protected var mCurrencyGovernorBean: GovernorDTO? = null

    protected var reservesImageFullPath: String? = null
    protected var accountPositiveImageFullPath: String? = null
    protected var accountReverseImageFullPath: String? = null

    override fun initView() {
        super.initView()

        mViewModel.mAccountDOLiveData.observe(this, Observer {
            itemWalletAddress.setContent(it.address)
        })
    }

    override fun initEvent() {
        super.initEvent()

        tvCoinNameContent.addTextChangedListener(object : TextWatcherSimple() {
            override fun afterTextChanged(s: Editable?) {
                tvCoinNameContent.removeTextChangedListener(this)
                val text = s?.toString()?.toUpperCase() ?: ""
                tvCoinNameContent.setText(text)
                tvCoinNameContent.setSelection(text.length)
                tvCoinNameContent.addTextChangedListener(this)
            }
        })

        tvContent.setOnClickListener {
            SelectCurrencyActivity.start(
                this@BaseIssuerApplyForIssueTokenFragment,
                REQUEST_CURRENCY_CODE
            )
        }

        tvGovernorContent.setOnClickListener {
            GovernorListActivity.start(
                this@BaseIssuerApplyForIssueTokenFragment,
                REQUEST_GOVERNOR_CODE
            )
        }

        upLoadViewReserves.setOnClickListener {
            if (reservesImageFullPath.isNullOrEmpty()) {
                showTakePhotoPopup(REQUEST_PHOTO_RESERVES)
            } else {
                upLoadViewReserves.getContentImageView()?.let {
                    it.viewImage(reservesImageFullPath!!)
                }
            }
        }
        upLoadViewAccountPositive.setOnClickListener {
            if (accountPositiveImageFullPath.isNullOrEmpty()) {
                showTakePhotoPopup(REQUEST_PHOTO_ACCOUNT_POSITIVE)
            } else {
                upLoadViewAccountPositive.getContentImageView()?.let {
                    it.viewImage(accountPositiveImageFullPath!!)
                }
            }
        }
        upLoadViewAccountReverse.setOnClickListener {
            if (accountReverseImageFullPath.isNullOrEmpty()) {
                showTakePhotoPopup(REQUEST_PHOTO_ACCOUNT_REVERSE)
            } else {
                upLoadViewAccountReverse.getContentImageView()?.let {
                    it.viewImage(accountReverseImageFullPath!!)
                }
            }
        }

        upLoadViewReserves.setCloseContentCallback {
            reservesImage = null
            reservesImageFullPath = null
        }
        upLoadViewAccountPositive.setCloseContentCallback {
            accountPositiveImage = null
            accountPositiveImageFullPath = null
        }
        upLoadViewAccountReverse.setCloseContentCallback {
            accountReverseImage = null
            accountReverseImageFullPath = null
        }

        btnSubmit.setOnClickListener {
            if (mCurrencyGovernorBean == null) {
                showToast(getString(R.string.hint_select_governor_issuing))
                return@setOnClickListener
            }
            if (mCurrencyBean == null) {
                showToast(getString(R.string.hint_select_issuing_type))
                return@setOnClickListener
            }
            if (itemCoinNumber.getContent()?.toString()?.isEmpty() == true) {
                showToast(getString(R.string.hint_issuing_number))
                return@setOnClickListener
            }
            if (tvCoinNameContent.text?.toString()?.isEmpty() == true) {
                showToast(getString(R.string.hint_fill_token_name))
                return@setOnClickListener
            }
            if (reservesImage == null || reservesImage?.isEmpty() == true) {
                showToast(getString(R.string.hint_upload_reserves))
                return@setOnClickListener
            }
            if (accountPositiveImage == null || accountPositiveImage?.isEmpty() == true) {
                showToast(getString(R.string.hint_pay_account_positive_image))
                return@setOnClickListener
            }
            if (accountReverseImage == null || accountReverseImage?.isEmpty() == true) {
                showToast(getString(R.string.hint_pay_account_reverse_image))
                return@setOnClickListener
            }
            showValidationDialog()
        }
    }

    private fun showValidationDialog() {
        EmailPhoneValidationDialog()
            .setConfirmListener { phoneVerifyCode, emailVerifyCode, dialog ->
                dialog.dismiss()
                submitData(phoneVerifyCode, emailVerifyCode)
            }
            .show(childFragmentManager)
    }

    private fun submitData(phoneVerifyCode: String, emailVerifyCode: String) {
        mViewModel.execute(
            mCurrencyBean!!.indicator,
            itemCoinNumber.getContent()!!.toString().toBigDecimal(),
            mCurrencyBean!!.exchange,
            tvCoinNameContent.text.toString().trim(),
            reservesImage!!,
            accountPositiveImage!!,
            accountReverseImage!!,
            mCurrencyGovernorBean!!.walletAddress,
            phoneVerifyCode,
            emailVerifyCode,
            action = ACTION_APPLY_FOR_ISSUE_TOKEN
        ) {
            //showToast(getString(R.string.hint_mint_condition_success))
            startNewApplicationActivity()
        }
    }

    private fun showTakePhotoPopup(type: Int) {
        context?.let {
            XPopup.Builder(context)
                .hasStatusBarShadow(true)
                .asCustom(TakePhotoPopup(context!!, {
                    takePhoto(true, type)
                }, {
                    takePhoto(false, type)
                }))
                .show()
        }
    }

    private fun takePhoto(isTakePhoto: Boolean = false, type: Int) {
        if (isTakePhoto) {
            UTakePhoto.with(this).openCamera()
        } else {
            UTakePhoto.with(this).openAlbum()
        }.build(object : ITakePhotoResult {
            override fun takeSuccess(uriList: List<Uri?>?) {
                launch(Dispatchers.IO + coroutineContext) {
                    if (null == uriList || uriList.isEmpty()) {
                        return@launch
                    }
                    uriList[0]?.let {
                        val uploadImageView = when (type) {
                            REQUEST_PHOTO_RESERVES -> {
                                upLoadViewReserves
                            }
                            REQUEST_PHOTO_ACCOUNT_POSITIVE -> {
                                upLoadViewAccountPositive
                            }
                            REQUEST_PHOTO_ACCOUNT_REVERSE -> {
                                upLoadViewAccountReverse
                            }
                            else -> {
                                upLoadViewAccountReverse
                            }
                        }

                        if (context != null) {
                            try {
                                val filePathFromContentUri =
                                    getFilePathFromContentUri(it, context!!)
                                val createFromStream =
                                    Drawable.createFromStream(
                                        filePathFromContentUri.inputStream(),
                                        it.toString()
                                    )
                                withContext(Dispatchers.Main) {
                                    uploadImageView.setContentImage(createFromStream)
                                    uploadImageView.startLoadingImage()
                                }

                                val uploadImage =
                                    mViewModel.mIssuerManager.uploadImage(filePathFromContentUri)
                                withContext(Dispatchers.Main) {
                                    uploadImageView.endLoadingImage()
                                }
                                when (type) {
                                    REQUEST_PHOTO_RESERVES -> {
                                        reservesImage = uploadImage.data
                                        reservesImageFullPath =
                                            filePathFromContentUri.absolutePath
                                    }
                                    REQUEST_PHOTO_ACCOUNT_POSITIVE -> {
                                        accountPositiveImage = uploadImage.data
                                        accountPositiveImageFullPath =
                                            filePathFromContentUri.absolutePath
                                    }
                                    REQUEST_PHOTO_ACCOUNT_REVERSE -> {
                                        accountReverseImage = uploadImage.data
                                        accountReverseImageFullPath =
                                            filePathFromContentUri.absolutePath
                                    }

                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    showToast(getString(R.string.hint_upload_failure))
                                    uploadImageView.closeContentImage()
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                showToast(getString(R.string.hint_upload_failure))
                                uploadImageView.closeContentImage()
                            }
                        }
                    }
                }
            }

            override fun takeFailure(ex: TakeException) {
                showToast(getString(R.string.hint_image_load_error))
            }

            override fun takeCancel() {
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CURRENCY_CODE -> {
                    val parcelable =
                        data?.getParcelableExtra<CurrencyBean>(SelectCurrencyActivity.EXT_CURRENCY_ITEM)
                    mCurrencyBean = parcelable
                    tvContent.text = parcelable?.currency
                    tvStableCurrencyValue.setContent("${parcelable?.exchange}")
                }
                REQUEST_GOVERNOR_CODE -> {
                    val parcelable =
                        data?.getParcelableExtra<GovernorDTO>(GovernorListActivity.EXT_GOVERNOR_ITEM)
                    mCurrencyGovernorBean = parcelable
                    tvGovernorContent.text = parcelable?.name
                }
            }
        }
    }
}
