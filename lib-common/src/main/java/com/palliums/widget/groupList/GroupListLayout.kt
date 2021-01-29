package com.palliums.widget.groupList

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.palliums.R
import com.palliums.utils.DensityUtility
import com.palliums.utils.isMainThread
import java.util.*

/**
 * Created by elephant on 2019-10-24 10:22.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 列表数据分组的控件，内部包装了RecyclerView
 */
class GroupListLayout(context: Context, attrs: AttributeSet?, defStyle: Int) :
    FrameLayout(context, attrs, defStyle) {

    private val groupData: GroupData = GroupData()
    private val dataAdapter: DataAdapter = DataAdapter()
    private val layoutManager: LinearLayoutManager = LinearLayoutManager(context)
    private val recyclerView: RecyclerView = RecyclerView(context)
    val slideBar = SlideBar(context)

    private var firstAddData: Boolean = true
    private var lastItemGroup: ItemData? = null
    private var floatTitleItem: ItemLayout<out ItemData>? = null

    /**
     * 设置是否显示悬浮分组,默认不显示,该方法需要在设置数据之前调用
     */
    var showFloatGroup: Boolean = false

    /**
     * 配置工厂,添加数据之前必须调用该方法,来确定需要什么样的TitleView与ContentItemView
     */
    var itemFactory: ItemFactory? = null
    var groupSelectedListener: OnGroupSelectedListener? = null

    constructor(context: Context) : this(context, null, 0)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    init {
        recyclerView.layoutParams =
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = dataAdapter
        recyclerView.itemAnimator = null
        recyclerView.setItemViewCacheSize(0)
        addView(recyclerView)

        slideBar.layoutParams = LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).also {
            it.gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }
        slideBar.setPadding(
            DensityUtility.dp2px(context, 10),
            DensityUtility.dp2px(context, 10),
            DensityUtility.dp2px(context, 5),
            DensityUtility.dp2px(context, 10)
        )
        addView(slideBar)

        slideBar.setOnKeyCheckedListener(object : SlideBar.OnKeyCheckedListener {
            override fun onKeyChecked(key: String, fromSetting: Boolean) {
                if (!fromSetting) {
                    selectGroup(key)
                }
            }
        })

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (!showFloatGroup) {
                    return
                }

                var targetY = 0
                val firstChild: View = layoutManager.getChildAt(0) ?: return
                val firstGroup = firstChild.getTag(R.id.group_list_layout_data) as? ItemData
                if (layoutManager.childCount > 1) {
                    val twoChild = layoutManager.getChildAt(1) ?: return
                    val twoGroup = twoChild.getTag(R.id.group_list_layout_data) as? ItemData
                    if (firstGroup != null && twoGroup != null
                        && firstGroup.getGroupName() != twoGroup.getGroupName()
                        && twoChild.top <= floatTitleItem!!.getItemView().height
                    ) {
                        targetY = twoChild.top - floatTitleItem!!.getItemView().height
                    }
                }

                updateTitleLocationY(firstGroup, targetY)
            }
        })
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        initFloatingItem()
    }

    /**
     * 添加悬浮标题
     */
    private fun initFloatingItem() {
        if (itemFactory == null || !showFloatGroup) {
            return
        }

        if (floatTitleItem == null) {
            floatTitleItem = itemFactory!!.createTitle(context, true)
            addView(floatTitleItem!!.getItemView())
        }

        if (groupData.isEmpty()) {
            floatTitleItem!!.getItemView().visibility = View.GONE
            return
        }

        if (floatTitleItem!!.getItemView().measuredHeight == 0 ||
            floatTitleItem!!.getItemView().measuredWidth == 0
        ) {
            val widthSpec = MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY)
            val heightSpec = MeasureSpec.makeMeasureSpec(2000, MeasureSpec.AT_MOST)
            floatTitleItem!!.getItemView().measure(widthSpec, heightSpec)
        }
    }

    /**
     * 滑动的时候,标题跟着滑动
     */
    private fun updateTitleLocationY(itemData: ItemData?, top: Int) {
        if (showFloatGroup && floatTitleItem != null && measuredWidth > 0) {
            floatTitleItem?.let {
                it.refreshView(itemData)
                it.getItemView().translationY = top.toFloat()
                it.getItemView().visibility =
                    if (itemData != null && !itemData.getGroupName().isNullOrEmpty()) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
            }

            groupChanged(itemData, true)
        }
    }

    /**
     * 当前组发生改变
     */
    private fun groupChanged(itemData: ItemData?, fromRecyclerViewScroll: Boolean) {
        if (lastItemGroup != itemData) {
            lastItemGroup = itemData
            groupSelectedListener?.let { it.onSelected(itemData) }

            if (fromRecyclerViewScroll) {
                slideBar.setCurrentKey(lastItemGroup!!.getGroupName())
            } else {
                postDelayed({
                    slideBar.setCurrentKey(lastItemGroup!!.getGroupName())
                }, 200)
            }
        }
    }

    /**
     * 是否显示快速索引,默认显示
     * @param show
     * @return
     */
    fun showSlideBar(show: Boolean) {
        slideBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    fun addItemDecoration(decor: RecyclerView.ItemDecoration) {
        recyclerView.addItemDecoration(decor)
    }

    /**
     * 设置选中组
     *
     * @param groupName 组名
     */
    fun selectGroup(groupName: String) {
        val index = groupData.getGroupIndex(groupName)
        index?.let {
            recyclerView.scrollToPosition(it)
            layoutManager.scrollToPositionWithOffset(it, 0)
        }
    }

    /**
     * 清空数据
     */
    fun clear() {
        if (isMainThread()) {
            groupData.clear()
            slideBar.clear()
            refreshAdapter()
            initFloatingItem()
        } else {
            post { clear() }
        }
    }

    fun <VO : ItemData> setData(data: MutableMap<String, List<VO>>) {
        if (isMainThread()) {
            val keys = groupData.setData(data)
            slideBar.setData(keys)
            groupData.refreshData()
            refreshAdapter()
            initFloatingItem()

            if (groupData.getCount() > 0) {
                groupChanged(groupData.getItemData(0), false)

                if (firstAddData) {
                    firstAddData = false
                } else {
                    selectGroup(keys[0])
                }
            }
        } else {
            post { setData(data) }
        }
    }

    /**
     * 手动刷新数据改变
     */
    private fun refreshAdapter() {
        if (recyclerView.isComputingLayout) {
            post { refreshAdapter() }
            return
        }

        //因为更新数据会影响排序,跟之前的位置很可能是不一样的,所以在改变数据之后,需要刷新屏幕可见的item
        val firstPosition = layoutManager.findFirstVisibleItemPosition()
        val lastPosition = layoutManager.findLastVisibleItemPosition()

        if (dataAdapter.itemCount == 0 || firstPosition == RecyclerView.NO_POSITION || lastPosition == RecyclerView.NO_POSITION) {
            dataAdapter.notifyDataSetChanged()
        } else {
            dataAdapter.notifyItemRangeChanged(firstPosition, lastPosition - firstPosition + 1)
        }
    }

    inner class DataAdapter : RecyclerView.Adapter<DataHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataHolder {
            return DataHolder(GroupItemLayout(parent.context, viewType))
        }

        override fun onBindViewHolder(holder: DataHolder, position: Int) {
            holder.refresh(groupData.getItemData(position), position)
        }

        override fun getItemCount(): Int {
            return groupData.getCount()
        }

        override fun getItemViewType(position: Int): Int {
            return itemFactory!!.getContentViewType(groupData.getItemData(position))
        }
    }

    inner class DataHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun refresh(itemData: ItemData?, position: Int) {
            itemView.setTag(R.id.group_list_layout_data, itemData)
            (itemView as? GroupItemLayout)?.refresh(itemData, position)
        }
    }

    inner class GroupItemLayout(context: Context, viewType: Int) : LinearLayout(context) {
        private var itemTitle: ItemLayout<out ItemData>
        private var itemContent: ItemLayout<out ItemData>

        private var itemData: ItemData? = null

        init {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = VERTICAL

            itemTitle = itemFactory!!.createTitle(context, false)
            addView(itemTitle.getItemView())

            itemContent = itemFactory!!.createContentItemLayout(context, viewType)
            addView(itemContent.getItemView())
        }

        fun refresh(itemData: ItemData?, position: Int) {
            this.itemData = itemData

            if (position == 0
                || groupData.getItemData(position)?.getGroupName()
                != groupData.getItemData(position - 1)?.getGroupName()
            ) {
                itemTitle.refreshView(this.itemData)
            } else {
                itemTitle.refreshView(null)
            }


            if (position == groupData.getCount() - 1
                || groupData.getItemData(position)?.getGroupName()
                != groupData.getItemData(position + 1)?.getGroupName()
            ) {
                itemContent.refreshView(this.itemData, true)
            } else {
                itemContent.refreshView(this.itemData, false)
            }
        }
    }

    inner class GroupData {
        private var dataList = arrayListOf<ItemData>()
        private var groupIdMap = hashMapOf<String, Int>()
        private var groupDataMap: MutableMap<String, List<ItemData>> = linkedMapOf()

        private var tempDataList: ArrayList<ItemData>? = null
        private var tempGroupIdMap: HashMap<String, Int>? = null

        fun <VO : ItemData> setData(groupDataMap: MutableMap<String, List<VO>>): List<String> {
            this.groupDataMap.clear()
            this.groupDataMap.putAll(groupDataMap)
            return updateData()
        }

        /**
         * 更新新数据,但不刷新数据,将改变后的数据用临时变量保存,然后调用[refreshData]刷新改变后的数据
         */
        private fun updateData(): List<String> {
            val tempDataList = arrayListOf<ItemData>()
            val tempGroupIdMap = hashMapOf<String, Int>()

            val keys = arrayListOf<String>()
            val keySet = this.groupDataMap.keys
            keySet.forEach { key ->
                val list = this.groupDataMap[key]
                if (!list.isNullOrEmpty()) {
                    // 记录每一组的开始index
                    tempGroupIdMap[key] = tempDataList.size

                    tempDataList.addAll(list)
                    keys.add(key)
                }
            }

            this.tempDataList = tempDataList
            this.tempGroupIdMap = tempGroupIdMap

            return keys
        }

        fun refreshData() {
            if (this.tempDataList != null && this.tempGroupIdMap != null) {
                this.dataList = this.tempDataList!!
                this.groupIdMap = this.tempGroupIdMap!!

                this.tempDataList = null
                this.tempGroupIdMap = null
            }
        }

        fun clear() {
            this.groupDataMap.clear()
            updateData()
            refreshData()
        }

        fun getCount() = dataList.size

        fun isEmpty() = getCount() == 0

        fun getItemData(index: Int): ItemData? {
            return try {
                dataList[index]
            } catch (e: Exception) {
                null
            }
        }

        fun getGroupIndex(groupName: String): Int? {
            return groupIdMap[groupName]
        }
    }

    /**
     * ContentItem,GroupItem 抽象工厂
     */
    abstract class ItemFactory {

        /**
         * 创建标题，调用[createTitleItemLayout]创建标题item,如果为null,会返回一个默认的标题
         */
        fun createTitle(context: Context, isFloat: Boolean): ItemLayout<out ItemData> {
            return createTitleItemLayout(context, isFloat) ?: object : ItemLayout<ItemData> {

                var tvTitle = createDefaultTitle(context, isFloat)

                override fun getItemView(): View {
                    return tvTitle.apply {
                        setPadding(
                            DensityUtility.dp2px(context, 24),
                            DensityUtility.dp2px(context, if (isFloat) 12 else 0),
                            DensityUtility.dp2px(context, 15),
                            DensityUtility.dp2px(context, 10)
                        )
                    }
                }

                override fun refreshView(itemData: ItemData?, lastGroupItem: Boolean?) {
                    if (itemData == null || itemData.getGroupName().isNullOrEmpty()) {
                        tvTitle.visibility = View.GONE
                    } else {
                        tvTitle.visibility = View.VISIBLE
                        tvTitle.text = itemData.getGroupName()
                    }
                }
            }
        }

        /**
         * 创建一个默认的标题
         */
        private fun createDefaultTitle(context: Context, isFloat: Boolean): TextView {
            return TextView(context).apply {
                layoutParams = if (isFloat)
                    LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                else
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16F)
                setTextColor(Color.parseColor("#3C3848"))
                //setTypeface(Typeface.DEFAULT, Typeface.BOLD)
                //setBackgroundColor(Color.WHITE)
            }
        }

        /**
         * 创建 GroupTitle,如果为null,则使用默认的标题,
         * 父布局 isFloat = true 时为FrameLayout，isFloat = false 时为LinearLayout。
         * @param context
         * @param isFloat 是否是悬浮标题(可以根据是否是悬浮标题定制不同的样式,但是需要注意高度要保持一致)
         */
        open fun createTitleItemLayout(
            context: Context,
            isFloat: Boolean
        ): ItemLayout<out ItemData>? {
            return null
        }

        /**
         * 创建 GroupContent,不能为null,父布局为LinearLayout
         * @param context
         * @param viewType [getContentViewType]
         */
        abstract fun createContentItemLayout(
            context: Context,
            viewType: Int
        ): ItemLayout<out ItemData>

        /**
         * 返回内容View的类型,如果有不同的内容View,则复写此方法,否则请不要复写,注意返回值必须是1递增的数字
         */
        open fun getContentViewType(itemData: ItemData?): Int = 1
    }

    /**
     * item数据接口
     */
    interface ItemData : Comparable<String> {
        /**
         * 获取组名
         */
        fun getGroupName(): String?

        /**
         * 设置组名
         */
        fun setGroupName(groupName: String)
    }

    /**
     * 标题与item布局的获取接口
     */
    interface ItemLayout<VO : ItemData> {

        /**
         * RecyclerView.Adapter#onBindViewHolder 刷新view
         */
        fun refreshView(itemData: ItemData?, lastGroupItem: Boolean? = null)

        /**
         * 获取要填充的布局，父布局为LinearLayout，自行设置布局参数
         */
        fun getItemView(): View
    }

    /**
     * 滑动RecyclerView的时候,选中组改变监听
     */
    interface OnGroupSelectedListener {
        fun onSelected(itemData: ItemData?)
    }
}