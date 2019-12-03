package com.violas.wallet.ui.authentication

import android.os.Bundle
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.image.GlideApp
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.ui.selectCountryArea.getCountryArea
import com.violas.wallet.ui.selectCountryArea.isChinaMainland
import kotlinx.android.synthetic.main.activity_id_infomation.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by elephant on 2019-11-29 09:49.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 身份信息页面
 */
class IDInformationActivity : BaseAppActivity() {

    override fun getLayoutResId(): Int {
        return R.layout.activity_id_infomation
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        launch(Dispatchers.IO) {

            val idInfo = DataRepository.getLocalUserService().getIDInfo()
            val countryAreaVO = getCountryArea(idInfo.idCountryCode)

            withContext(Dispatchers.Main) {
                if (!idInfo.isAuthenticatedID()) {
                    close()
                    return@withContext
                }

                setTitle(R.string.title_id_information)

                mivCountryArea.setEndDescText(countryAreaVO.countryName)
                mivIDName.setEndDescText(idInfo.idName)
                mivIDNumber.setEndDescText(idInfo.idNumber)
                mivIDNumber.setStartTitleText(
                    if (isChinaMainland(countryAreaVO)) {
                        R.string.label_id_number_china
                    } else {
                        R.string.label_id_number_other
                    }
                )

                GlideApp.with(this@IDInformationActivity)
                    .load(idInfo.idPhotoFrontUrl)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.bg_id_card_front)
                    .error(R.drawable.bg_id_card_front)
                    .into(ivIDCardFront)

                GlideApp.with(this@IDInformationActivity)
                    .load(idInfo.idPhotoBackUrl)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.bg_id_card_back)
                    .error(R.drawable.bg_id_card_back)
                    .into(ivIDCardBack)
            }
        }
    }
}