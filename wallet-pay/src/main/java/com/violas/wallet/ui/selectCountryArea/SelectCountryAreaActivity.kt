package com.violas.wallet.ui.selectCountryArea

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.palliums.utils.DensityUtility
import com.palliums.utils.getColorByAttrId
import com.palliums.utils.isFastMultiClick
import com.palliums.widget.groupList.GroupListLayout
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.common.KEY_ONE
import kotlinx.android.synthetic.main.activity_group_list.*
import kotlinx.android.synthetic.main.item_country_area.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by elephant on 2019-11-27 16:12.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 选择国家地区页面
 */
class SelectCountryAreaActivity : BaseAppActivity() {

    companion object {

        private const val EXTRA_KEY_SELECT_AREA_CODE = "EXTRA_KEY_SELECT_AREA_CODE"

        /**
         * 启动
         * @param activity
         * @param requestCode
         * @param selectAreaCode true表示选择电话区号，false表示选择国家或地区
         */
        fun start(activity: Activity, requestCode: Int, selectAreaCode: Boolean = true) {
            val intent = Intent(activity, SelectCountryAreaActivity::class.java).apply {
                putExtra(EXTRA_KEY_SELECT_AREA_CODE, selectAreaCode)
            }
            activity.startActivityForResult(intent, requestCode)
        }
    }

    private var selectAreaCode = true

    override fun getLayoutResId(): Int {
        return R.layout.activity_group_list
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            selectAreaCode = savedInstanceState.getBoolean(EXTRA_KEY_SELECT_AREA_CODE, true)
        } else if (intent != null) {
            selectAreaCode = intent.getBooleanExtra(EXTRA_KEY_SELECT_AREA_CODE, true)
        }

        setTitle(
            if (selectAreaCode) {
                R.string.title_select_phone_area_code
            } else {
                R.string.title_select_country_or_area
            }
        )

        initView()
        initData()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean(EXTRA_KEY_SELECT_AREA_CODE, selectAreaCode)
    }

    private fun initView() {
        vGroupList.slideBar.indexColorNormal =
            getColorByAttrId(android.R.attr.textColorTertiary, this)
        vGroupList.slideBar.indexColorSelected =
            getColorByAttrId(android.R.attr.textColorSecondary, this)

        vGroupList.showFloatGroup = true
        vGroupList.showSlideBar(true)

        vGroupList.itemFactory = GroupListItemFactory(selectAreaCode) {
            val intent = Intent().apply {
                putExtra(KEY_ONE, it)
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    private fun initData() {
        launch(Dispatchers.IO) {
            val data = getGroupedCountryAreas()

            withContext(Dispatchers.Main) {
                vGroupList.setData(data)
            }
        }
    }

    class GroupListItemFactory(
        private val showAreaCode: Boolean,
        private val onItemClick: (CountryAreaVO) -> Unit
    ) : GroupListLayout.ItemFactory() {

        override fun createContentItemLayout(
            context: Context,
            viewType: Int
        ): GroupListLayout.ItemLayout<out GroupListLayout.ItemData> {
            return GroupListContentItem(context, showAreaCode, onItemClick)
        }

        override fun createTitleItemLayout(
            context: Context,
            isFloat: Boolean
        ): GroupListLayout.ItemLayout<out GroupListLayout.ItemData>? {
            return GroupListTitleItem(context, isFloat)
        }
    }

    class GroupListTitleItem(context: Context, isFloat: Boolean) :
        GroupListLayout.ItemLayout<CountryAreaVO> {

        private val tvTitle: TextView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(
                DensityUtility.dp2px(context, 16),
                DensityUtility.dp2px(context, 5),
                DensityUtility.dp2px(context, 15),
                DensityUtility.dp2px(context, 4)
            )
            setTextColor(
                getColorByAttrId(
                    if (isFloat) android.R.attr.textColorSecondary else android.R.attr.textColorSecondary,
                    context
                )
            )
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
            setBackgroundColor(getColorByAttrId(R.attr.colorListGroup, context))
        }

        override fun refreshView(itemData: GroupListLayout.ItemData?, lastGroupItem: Boolean?) {
            if (itemData == null || itemData.getGroupName().isNullOrEmpty()) {
                tvTitle.visibility = View.GONE
            } else {
                tvTitle.visibility = View.VISIBLE
                tvTitle.text = itemData.getGroupName()
            }
        }

        override fun getItemView(): View {
            return tvTitle
        }
    }

    class GroupListContentItem(
        context: Context,
        private val showAreaCode: Boolean,
        private val onItemClick: (CountryAreaVO) -> Unit
    ) : GroupListLayout.ItemLayout<CountryAreaVO>, View.OnClickListener {

        private val rootView: View = View.inflate(context, R.layout.item_country_area, null)

        private var countryAreaVO: CountryAreaVO? = null

        init {
            rootView.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            rootView.setOnClickListener(this)
            if (!showAreaCode) {
                rootView.tvAreaCode.visibility = View.GONE
            }
        }

        override fun refreshView(itemData: GroupListLayout.ItemData?, lastGroupItem: Boolean?) {
            countryAreaVO = itemData as? CountryAreaVO

            countryAreaVO?.let {
                if (showAreaCode) {
                    rootView.tvAreaCode.text = "+${it.areaCode}"
                }
                rootView.tvCountryName.text = it.countryName
            }

            lastGroupItem?.let {
                rootView.vDivider.visibility = if (it) View.GONE else View.VISIBLE
            }
        }

        override fun getItemView(): View {
            return rootView
        }

        override fun onClick(view: View) {
            if (!isFastMultiClick(view)) {
                countryAreaVO?.let {
                    onItemClick.invoke(it)
                }
            }
        }
    }
}