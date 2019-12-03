package com.violas.wallet.ui.main.applyFor

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import com.lxj.xpopup.XPopup
import com.palliums.base.BaseFragment
import com.sl.utakephoto.exception.TakeException
import com.sl.utakephoto.manager.ITakePhotoResult
import com.sl.utakephoto.manager.UTakePhoto
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.ApplyManager
import com.violas.wallet.event.RefreshPageEvent
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.ui.selectCurrency.SelectCurrencyActivity
import com.violas.wallet.ui.selectCurrency.bean.CurrencyBean
import com.violas.wallet.utils.getFilePathFromContentUri
import com.violas.wallet.widget.dialog.EmailPhoneValidationDialog
import com.violas.wallet.widget.dialog.TakePhotoPopup
import kotlinx.android.synthetic.main.fragment_apply_submit.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import java.io.File

class ApplySubmitFragment : BaseFragment() {
    companion object {
        private const val REQUEST_CURRENCY_CODE = 0
        private const val REQUEST_PHOTO_RESERVES = 1
        private const val REQUEST_PHOTO_ACCOUNT_POSITIVE = 2
        private const val REQUEST_PHOTO_ACCOUNT_REVERSE = 3
    }

    private var reservesImage: String? = null
    private var accountPositiveImage: String? = null
    private var accountReverseImage: String? = null
    private var mAccount: AccountDO? = null
    private var mCurrencyBean: CurrencyBean? = null

    private val mApplyManager by lazy {
        ApplyManager()
    }

    private val mAccountManager by lazy {
        AccountManager()
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_apply_submit
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        launch(Dispatchers.IO) {
            mAccount = mAccountManager.currentAccount()
            if (mAccount == null) {
                finishActivity()
            }
            withContext(Dispatchers.Main) {
                mAccount?.address?.let { itemWalletAddress.setContent(it) }
            }
        }

        tvContent.setOnClickListener {
            SelectCurrencyActivity.start(this@ApplySubmitFragment, REQUEST_CURRENCY_CODE)
        }

        upLoadViewReserves.setOnClickListener {
            showTakePhotoPopup(REQUEST_PHOTO_RESERVES)
        }
        upLoadViewAccountPositive.setOnClickListener {
            showTakePhotoPopup(REQUEST_PHOTO_ACCOUNT_POSITIVE)
        }
        upLoadViewAccountReverse.setOnClickListener {
            showTakePhotoPopup(REQUEST_PHOTO_ACCOUNT_REVERSE)
        }
        upLoadViewReserves.setCloseContentCallback {
            reservesImage = null
        }
        upLoadViewAccountPositive.setCloseContentCallback {
            accountPositiveImage = null
        }
        upLoadViewAccountReverse.setCloseContentCallback {
            accountReverseImage = null
        }
        btnSubmit.setOnClickListener {
            if (mCurrencyBean == null) {
                showToast(getString(R.string.hint_select_issuing_type))
                return@setOnClickListener
            }
            if (itemCoinNumber.getContent()?.toString()?.isEmpty() == true) {
                showToast(getString(R.string.hint_issuing_number))
                return@setOnClickListener
            }
            if (itemName.getContent()?.toString()?.isEmpty() == true) {
                showToast(getString(R.string.hint_fill_token_name))
                return@setOnClickListener
            }
            if (reservesImage?.isEmpty() == true) {
                showToast(getString(R.string.hint_upload_reserves))
                return@setOnClickListener
            }
            if (accountPositiveImage?.isEmpty() == true) {
                showToast(getString(R.string.hint_pay_account_positive_image))
                return@setOnClickListener
            }
            if (accountReverseImage?.isEmpty() == true) {
                showToast(getString(R.string.hint_pay_account_reverse_image))
                return@setOnClickListener
            }
            showValidationDialog()
        }
    }

    private fun showValidationDialog() {
        EmailPhoneValidationDialog()
            .setConfirmListener { phone, email, dialog ->
                submitData(phone, email)
                dialog.dismiss()
            }
            .show(childFragmentManager)
    }

    private fun submitData(phone: String, email: String) {
        launch(Dispatchers.IO) {
            val applyForIssuing = mApplyManager.applyForIssuing(
                mAccount!!.address,
                mCurrencyBean!!.indicator,
                itemCoinNumber.getContent()!!.toString().toLong(),
                mCurrencyBean!!.exchange,
                itemName.getContent()!!.toString(),
                reservesImage!!,
                accountPositiveImage!!,
                accountReverseImage!!,
                phone,
                email
            )
            if (applyForIssuing != null) {
                EventBus.getDefault().post(RefreshPageEvent())
            } else {
                showToast(getString(R.string.hint_net_work_error))
            }
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
            UTakePhoto.with(this)
                .openCamera()
        } else {
            UTakePhoto.with(this)
                .openAlbum()
        }.build(object : ITakePhotoResult {
            override fun takeSuccess(uriList: List<Uri?>?) {
                launch(Dispatchers.IO) {
                    if (null == uriList || uriList.isEmpty()) {
                        return@launch
                    }
                    uriList[0]?.let {
                        val openInputStream = context?.contentResolver?.openInputStream(it)
                        val createFromStream =
                            Drawable.createFromStream(openInputStream, it.toString())
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
                        withContext(Dispatchers.Main) {
                            uploadImageView.setContentImage(createFromStream)
                            uploadImageView.startLoadingImage()
                        }
                        withContext(Dispatchers.IO) {
                            if (context?.contentResolver != null) {
                                val filePathFromContentUri =
                                    getFilePathFromContentUri(it, context?.contentResolver!!)
                                val uploadImage =
                                    mApplyManager.uploadImage(File(filePathFromContentUri))
                                if (uploadImage != null) {
                                    withContext(Dispatchers.Main) {
                                        uploadImageView.endLoadingImage()
                                    }
                                    when (type) {
                                        REQUEST_PHOTO_RESERVES -> {
                                            reservesImage = uploadImage.data
                                        }
                                        REQUEST_PHOTO_ACCOUNT_POSITIVE -> {
                                            accountPositiveImage = uploadImage.data
                                        }
                                        REQUEST_PHOTO_ACCOUNT_REVERSE -> {
                                            accountReverseImage = uploadImage.data
                                        }

                                    }
                                } else {
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
        when (requestCode) {
            REQUEST_CURRENCY_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    val parcelable =
                        data?.getParcelableExtra<CurrencyBean>(SelectCurrencyActivity.EXT_CURRENCY_ITEM)
                    mCurrencyBean = parcelable
                    tvContent.text = parcelable?.currency
                    tvStableCurrencyValue.setContent("${parcelable?.exchange}")
                }
            }
        }
    }
}
