package com.violas.wallet.ui.authentication

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lxj.xpopup.XPopup
import com.palliums.base.BaseViewModel
import com.sl.utakephoto.exception.TakeException
import com.sl.utakephoto.manager.ITakePhotoResult
import com.sl.utakephoto.manager.UTakePhoto
import com.violas.wallet.R
import com.violas.wallet.base.BaseViewModelActivity
import com.violas.wallet.common.EXTRA_KEY_COUNTRY_AREA
import com.violas.wallet.ui.selectCountryArea.CountryAreaVO
import com.violas.wallet.ui.selectCountryArea.SelectCountryAreaActivity
import com.violas.wallet.ui.selectCountryArea.isChinaMainland
import com.violas.wallet.widget.IDCardLayout
import com.violas.wallet.widget.dialog.TakePhotoPopup
import kotlinx.android.synthetic.main.activity_id_authentication.*

/**
 * Created by elephant on 2019-11-19 19:24.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 身份认证页面
 */
class IDAuthenticationActivity : BaseViewModelActivity() {

    companion object {
        private const val REQUEST_CODE_SELECT_COUNTRY_AREA = 100
    }

    private val mViewModel by viewModels<IDAuthenticationViewModel> {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return IDAuthenticationViewModel() as T
            }
        }
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_id_authentication
    }

    override fun getPageStyle(): Int {
        return PAGE_STYLE_DARK_TITLE_PLIGHT_CONTENT
    }

    override fun getViewModel(): BaseViewModel {
        return mViewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.authentication_title)

        tvCountryAreaLabel.setOnClickListener(this)
        tvCountryArea.setOnClickListener(this)
        ivSelectCountryArea.setOnClickListener(this)
        btnSubmit.setOnClickListener(this)

        iclIDCardFront.setOnViewClickListener(object : IDCardLayout.OnViewClickListener {
            override fun onClickDelete() {
                mViewModel.idPhotoFront.value = null
            }

            override fun onClickPhotograph() {
                showTakePhotoPopup(true)
            }
        })
        iclIDCardBack.setOnViewClickListener(object : IDCardLayout.OnViewClickListener {
            override fun onClickDelete() {
                mViewModel.idPhotoBack.value = null
            }

            override fun onClickPhotograph() {
                showTakePhotoPopup(false)
            }
        })

        mViewModel.countryAreaVO.observe(this, Observer {
            tvCountryArea.text = it.countryName

            if (isChinaMainland(it)) {
                tvIdNumberLabel.setText(R.string.label_id_number_china)
                etIdNumber.setHint(R.string.hint_enter_id_number_china)
                tvPhotographDesc.setText(R.string.desc_photograph_id_card_china)
                iclIDCardFront.setPhotographDesc(R.string.desc_photograph_id_card_front_china)
                iclIDCardBack.setPhotographDesc(R.string.desc_photograph_id_card_back_china)
            } else {
                tvIdNumberLabel.setText(R.string.label_id_number_other)
                etIdNumber.setHint(R.string.hint_enter_id_number_other)
                tvPhotographDesc.setText(R.string.desc_photograph_id_card_other)
                iclIDCardFront.setPhotographDesc(R.string.desc_photograph_id_card_front_other)
                iclIDCardBack.setPhotographDesc(R.string.desc_photograph_id_card_back_other)
            }
        })
        mViewModel.idPhotoFront.observe(this, Observer {
            if (it == null) {
                iclIDCardFront.clearIDCardImage()
            } else {
                iclIDCardFront.setIDCardImage(it)
            }
        })
        mViewModel.idPhotoBack.observe(this, Observer {
            if (it == null) {
                iclIDCardBack.clearIDCardImage()
            } else {
                iclIDCardBack.setIDCardImage(it)
            }
        })
        mViewModel.authenticationResult.observe(this, Observer {
            if (it) {
                setResult(Activity.RESULT_OK)
                btnSubmit.postDelayed({
                    close()
                }, 2000)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (etIDName.hasFocus()
            && etIDName.text.toString().trim().isEmpty()
        ) {
            showSoftInput(etIDName)
        } else if (etIdNumber.hasFocus()
            && etIdNumber.text.toString().trim().isEmpty()
        ) {
            showSoftInput(etIdNumber)
        }
    }

    override fun onPause() {
        super.onPause()
        hideSoftInput()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_SELECT_COUNTRY_AREA) {
                val countryAreaVO =
                    data?.getParcelableExtra<CountryAreaVO>(EXTRA_KEY_COUNTRY_AREA)
                countryAreaVO?.let { mViewModel.countryAreaVO.value = it }

                if (!etIDName.hasFocus() && !etIdNumber.hasFocus()) {
                    etIDName.requestFocus()
                }
            }
        }
    }

    override fun onViewClick(view: View) {
        when (view.id) {
            R.id.tvCountryArea,
            R.id.ivSelectCountryArea -> {
                SelectCountryAreaActivity.start(
                    this, REQUEST_CODE_SELECT_COUNTRY_AREA, false
                )
            }

            R.id.btnSubmit -> {
                mViewModel.execute(
                    etIDName.text.toString().trim(),
                    etIdNumber.text.toString().trim()
                )
            }
        }
    }

    private fun showTakePhotoPopup(front: Boolean) {
        hideSoftInput()
        XPopup.Builder(this)
            .hasStatusBarShadow(true)
            .asCustom(TakePhotoPopup(this, {
                takePhoto(true, front)
            }, {
                takePhoto(false, front)
            }))
            .show()
    }

    private fun takePhoto(isTakePhoto: Boolean, front: Boolean) {
        if (isTakePhoto) {
            UTakePhoto.with(this).openCamera()
        } else {
            UTakePhoto.with(this).openAlbum()

        }.build(object : ITakePhotoResult {

            override fun takeSuccess(uriList: List<Uri?>?) {
                if (!uriList.isNullOrEmpty()) {
                    uriList[0].let {
                        if (front) {
                            mViewModel.idPhotoFront.value = it
                        } else {
                            mViewModel.idPhotoBack.value = it
                        }
                    }
                }
            }

            override fun takeFailure(ex: TakeException) {
                showToast(getString(R.string.hint_image_load_error))
            }

            override fun takeCancel() {
                // ignore
            }
        })
    }
}