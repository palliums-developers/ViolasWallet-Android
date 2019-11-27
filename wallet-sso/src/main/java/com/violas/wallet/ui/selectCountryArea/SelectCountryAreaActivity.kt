package com.violas.wallet.ui.selectCountryArea

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.palliums.utils.DensityUtility
import com.palliums.utils.getColor
import com.palliums.utils.isFastMultiClick
import com.palliums.widget.groupList.GroupListLayout
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.common.EXTRA_KEY_COUNTRY_AREA
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
        fun start(activity: Activity, requestCode: Int) {
            val intent = Intent(activity, SelectCountryAreaActivity::class.java)
            activity.startActivityForResult(intent, requestCode)
        }
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_group_list
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initView()
        initData(true)
    }

    private fun initView() {
        vGroupList.slideBar.indexColorNormal = getColor(R.color.color_3C3848, this)
        vGroupList.slideBar.indexColorSelected = getColor(R.color.colorAccent, this)
        vGroupList.showFloatGroup = true
        vGroupList.showSlideBar(true)
        vGroupList.itemFactory = object : GroupListLayout.ItemFactory() {

            override fun createContentItemLayout(
                context: Context,
                viewType: Int
            ): GroupListLayout.ItemLayout<out GroupListLayout.ItemData> {
                return ContentItem(context)
            }

            override fun createTitleItemLayout(
                context: Context,
                isFloat: Boolean
            ): GroupListLayout.ItemLayout<out GroupListLayout.ItemData>? {
                return TitleItem(context, isFloat)
            }
        }
    }

    private fun initData(first:Boolean) {
        launch(Dispatchers.IO) {
            val data = getGroupedCountryAreas()

            if(!first) return@launch

            withContext(Dispatchers.Main) {
                vGroupList.setData(data)
            }
        }
    }

    class TitleItem(context: Context, isFloat: Boolean) :
        GroupListLayout.ItemLayout<CountryAreaVO> {

        private val tvTitle: TextView = TextView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
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
                getColor(
                    if (isFloat) R.color.colorAccent else R.color.def_text_title,
                    context
                )
            )
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setBackgroundColor(getColor(R.color.color_F4F4F4, context))
        }

        override fun refreshView(itemData: GroupListLayout.ItemData?) {
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

    inner class ContentItem(context: Context) : GroupListLayout.ItemLayout<CountryAreaVO>,
        View.OnClickListener {

        private val rootView: View = View.inflate(context, R.layout.item_country_area, null)

        private var countryAreaVO: CountryAreaVO? = null

        init {
            rootView.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            rootView.setOnClickListener(this)
        }

        override fun refreshView(itemData: GroupListLayout.ItemData?) {
            countryAreaVO = itemData as? CountryAreaVO

            countryAreaVO?.let {
                rootView.vAreaCode.text = "+${it.areaCode}"
                rootView.vCountryName.text = it.countryName
            }
        }

        override fun getItemView(): View {
            return rootView
        }

        override fun onClick(view: View) {
            if (!isFastMultiClick(view)) {
//                countryAreaVO?.let {
//                    val intent = Intent().apply {
//                        putExtra(EXTRA_KEY_COUNTRY_AREA, it)
//                    }
//                    setResult(Activity.RESULT_OK, intent)
//                    finish()
//                }

                initData(false)
            }
        }
    }
}