package com.violas.wallet.ui.bank

import android.text.SpannableStringBuilder
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.violas.wallet.ui.main.market.bean.IAssetsMark
import com.violas.wallet.viewModel.bean.AssetsVo

data class BusinessParameter(
    val title: String,
    val content: String,
    val declare: String? = null,
    @ColorInt
    val contentColor: Int? = null
)

data class ProductExplanation(
    val title: String,
    val content: String
)

data class FAQ(
    val q: String,
    val a: String
)

data class BusinessUserAmountInfo(
    @DrawableRes
    val icon: Int,
    val title: String,
    val value1: String,
    val unit: String,
    val value2: String? = null
)

data class BusinessUserInfo(
    val businessName: String,
    val businessInputHint: String,
    val businessUsableAmount: BusinessUserAmountInfo,
    val businessLimitAmount: BusinessUserAmountInfo? = null
)

class BankBusinessViewModel : ViewModel() {
    /**
     * 页面标题
     */
    val mPageTitleLiveData = MutableLiveData<String>()

    /**
     * 我要存款框里面的数据
     */
    val mBusinessUserInfoLiveData = MutableLiveData<BusinessUserInfo>()

    /**
     * 当前选中的币种
     */
    val mCurrentAssetsLiveData = MutableLiveData<AssetsVo>()

    // <editor-fold defaultstate="collapsed" desc="底部悬浮栏里面的内容">
    /**
     * 操作提示
     */
    val mBusinessActionHintLiveData = MutableLiveData<String?>()

    /**
     * 按钮字符
     */
    val mBusinessActionLiveData = MutableLiveData<String>()

    /**
     * 隐私条款
     */
    val mBusinessPolicyLiveData = MutableLiveData<SpannableStringBuilder?>()
    //</editor-fold>

    /**
     * 业务参数，各种利率
     */
    val mBusinessParameterListLiveData = MutableLiveData<List<BusinessParameter>>()

    /**
     * 产品说明
     */
    val mProductExplanationListLiveData = MutableLiveData<List<ProductExplanation>>()

    /**
     * 常见问题
     */
    val mFAQListLiveData = MutableLiveData<List<FAQ>>()


    // <editor-fold defaultstate="collapsed" desc="业务数据">
    val mSupportAssetsTokensLiveData = MutableLiveData<List<AssetsVo>?>()
    //</editor-fold>
}