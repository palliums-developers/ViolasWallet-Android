package com.violas.wallet.ui.bank

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

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
    val Q: String,
    val A: String
)

data class BusinessUserAmountInfo(
    @DrawableRes
    val icon: Int,
    val title: String,
    val value1: String,
    val unit: String,
    val value2: String? = null
)

class BankBusinessViewModel : ViewModel() {
    /**
     * 页面标题
     */
    val mPageTitleLiveData = MutableLiveData<String>()

    // <editor-fold defaultstate="collapsed" desc="我要存款框里面的数据">
    /**
     * 业务名称
     */
    val mBusinessNameLiveData = MutableLiveData<String>()

    /**
     * 输入框标题
     */
    val mBusinessHintLiveData = MutableLiveData<String>()

    /**
     * 可用资金提示，可用余额、可借额度
     */
    val mBusinessUsableAmountLiveData = MutableLiveData<BusinessUserAmountInfo>()

    /**
     * 金额限制
     */
    val mBusinessLimitAmountLiveData = MutableLiveData<BusinessUserAmountInfo?>()

    //</editor-fold>

    // <editor-fold defaultstate="collapsed" desc="底部悬浮栏里面的内容">
    /**
     * 操作提示
     */
    val mBusinessActionHintLiveData = MutableLiveData<String>()

    /**
     * 按钮字符
     */
    val mBusinessActionLiveData = MutableLiveData<String>()
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
}