package com.violas.wallet.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.violas.wallet.ContextProviderKt;
import com.violas.wallet.R;
import com.violas.wallet.utils.DensityUtility;


/**
 * Created by QRoronoa on 2019/3/21.
 * Copyright © 2019. All rights reserved.
 * *
 * Modify by elephant on 2019-10-29 11:22.
 * *
 * desc: 通用菜单项
 */
public class MenuItemView extends FrameLayout {

    private ImageView mStartIcon, mEndIcon, mEndArrow;
    private TextView mStartTitle, mStartDesc, mEndDesc;
    private Switch mEndSwitch;
    private View mTopLine, mBottomLine;

    public MenuItemView(@NonNull Context context) {
        this(context, null);
    }

    public MenuItemView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MenuItemView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(21)
    public MenuItemView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setLayoutParams(lp);

        View.inflate(context, R.layout.widget_menu_item_view, this);
        View root = findViewById(R.id.widget_menu_item_root);

        mStartIcon = findViewById(R.id.widget_menu_item_start_icon);
        mStartTitle = findViewById(R.id.widget_menu_item_start_title);
        mStartDesc = findViewById(R.id.widget_menu_item_start_desc);
        mEndIcon = findViewById(R.id.widget_menu_item_end_icon);
        mEndDesc = findViewById(R.id.widget_menu_item_end_desc);
        mEndArrow = findViewById(R.id.widget_menu_item_end_arrow);
        mEndSwitch = findViewById(R.id.widget_menu_item_end_switch);
        mTopLine = findViewById(R.id.widget_menu_item_top_line);
        mBottomLine = findViewById(R.id.widget_menu_item_bottom_line);

        // 开始处图标
        Drawable startIcon = null;
        // 结束处图标
        Drawable endIcon = null;

        // 起始处标题
        String startTitleText = null;
        // 起始处标题的字体大小
        int startTitleTextSize = DensityUtility.sp2px(context, 16);
        // 起始处标题的文本颜色
        int startTitleTextColor = ContextProviderKt.getColor(R.color.def_text_title, context);

        // 起始处描述
        String startDescText = null;
        // 起始处描述的字体大小
        int startDescTextSize = DensityUtility.sp2px(getContext(), 12);
        // 起始处描述的文本颜色
        int startDescTextColor = ContextProviderKt.getColor(R.color.def_text_desc, context);

        // 结束处描述
        String endDescText = null;
        // 结束处描述的字体大小
        int endDescTextSize = DensityUtility.sp2px(getContext(), 16);
        // 结束处描述的文本颜色
        int endDescTextColor = ContextProviderKt.getColor(R.color.def_text_key, context);
        // 结束处描述的文本对齐方式
        int endDescGravity = 2; // 1:left; 2:right
        // 结束处描述的图标间距
        int endDescDrawablePadding = 0;
        // 结束处描述的左侧图标
        Drawable endDescDrawableLeft = null;

        // 是否显示结束处箭头
        boolean showEndArrow = true;
        // 是否显示结束处开关
        boolean showEndSwitch = false;
        // 是否显示顶部分界线
        boolean showTopLine = false;
        // 是否显示底部分界线
        boolean showBottomLine = true;

        if (attrs != null) {
            TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.MenuItemView, defStyleAttr, defStyleRes);

            startIcon = array.getDrawable(R.styleable.MenuItemView_mivStartIcon);
            endIcon = array.getDrawable(R.styleable.MenuItemView_mivEndIcon);

            startTitleText = array.getString(R.styleable.MenuItemView_mivStartTitleText);
            startTitleTextSize = array.getDimensionPixelSize(R.styleable.MenuItemView_mivStartTitleTextSize, startTitleTextSize);
            startTitleTextColor = array.getColor(R.styleable.MenuItemView_mivStartTitleTextColor, startTitleTextColor);

            startDescText = array.getString(R.styleable.MenuItemView_mivStartDescText);
            startDescTextSize = array.getDimensionPixelSize(R.styleable.MenuItemView_mivStartDescTextSize, startTitleTextSize);
            startDescTextColor = array.getColor(R.styleable.MenuItemView_mivStartDescTextColor, startTitleTextColor);

            endDescText = array.getString(R.styleable.MenuItemView_mivEndDescText);
            endDescTextSize = array.getDimensionPixelSize(R.styleable.MenuItemView_mivEndDescTextSize, endDescTextSize);
            endDescTextColor = array.getColor(R.styleable.MenuItemView_mivEndDescTextColor, endDescTextColor);
            endDescGravity = array.getInt(R.styleable.MenuItemView_mivEndDescGravity, endDescGravity);
            endDescDrawableLeft = array.getDrawable(R.styleable.MenuItemView_mivEndDescDrawableLeft);
            endDescDrawablePadding = array.getDimensionPixelSize(R.styleable.MenuItemView_mivEndDescDrawablePadding, endDescDrawablePadding);

            showEndArrow = array.getBoolean(R.styleable.MenuItemView_mivShowEndArrow, true);
            showEndSwitch = array.getBoolean(R.styleable.MenuItemView_mivShowEndSwitch, false);
            showTopLine = array.getBoolean(R.styleable.MenuItemView_mivShowTopLine, false);
            showBottomLine = array.getBoolean(R.styleable.MenuItemView_mivShowBottomLine, true);

            array.recycle();
        }

        setStartIcon(startIcon);
        setEndIcon(endIcon);

        setStartTitleText(startTitleText);
        setStartTitleTextSize(startTitleTextSize);
        setStartTitleTextColor(startTitleTextColor);

        setStartDescText(startDescText);
        setStartDescTextSize(startDescTextSize);
        setStartDescTextColor(startDescTextColor);

        setEndDescText(endDescText);
        setEndDescTextColor(endDescTextColor);
        setEndDescTextSize(endDescTextSize);
        setEndDescGravity(endDescGravity);
        setEdnDescDrawableLeft(endDescDrawableLeft);
        setEndDescDrawablePadding(endDescDrawablePadding);

        setShowEndArrow(showEndArrow);
        setShowEndSwitch(showEndSwitch);
        setShowTopLine(showTopLine);
        setShowBottomLine(showBottomLine);
    }

    /**
     * 设置开始处图标，如果为0则隐藏
     *
     * @param resId
     */
    public void setStartIcon(@DrawableRes int resId) {
        if (resId != 0) {
            mStartIcon.setVisibility(VISIBLE);
            mStartIcon.setImageResource(resId);
        } else {
            mStartIcon.setVisibility(GONE);
        }
    }

    /**
     * 设置开始处图标，如果为null则隐藏
     *
     * @param drawable
     */
    public void setStartIcon(Drawable drawable) {
        if (drawable != null) {
            mStartIcon.setVisibility(VISIBLE);
            mStartIcon.setImageDrawable(drawable);
        } else {
            mStartIcon.setVisibility(GONE);
        }
    }

    /**
     * 设置结束处图标，如果为0则隐藏，不能与结束处描述同时使用
     *
     * @param resId
     */
    public void setEndIcon(@DrawableRes int resId) {
        if (resId != 0) {
            mEndDesc.setVisibility(GONE);
            mEndIcon.setVisibility(VISIBLE);
            mEndIcon.setImageResource(resId);
        } else {
            mEndIcon.setVisibility(GONE);
        }
    }

    /**
     * 设置结束处图标，如果为null则隐藏，不能与结束处描述同时使用
     *
     * @param drawable
     */
    public void setEndIcon(Drawable drawable) {
        if (drawable != null) {
            mEndDesc.setVisibility(GONE);
            mEndIcon.setVisibility(VISIBLE);
            mEndIcon.setImageDrawable(drawable);
        } else {
            mEndIcon.setVisibility(GONE);
        }
    }

    /**
     * 设置开始处标题的文本，如果为0则隐藏
     *
     * @param resId
     */
    public void setStartTitleText(@StringRes int resId) {
        if (resId != 0) {
            mStartTitle.setVisibility(VISIBLE);
            mStartTitle.setText(resId);
        } else {
            mStartTitle.setVisibility(GONE);
        }
    }

    /**
     * 设置开始处标题的文本，如果为null则隐藏
     *
     * @param text
     */
    public void setStartTitleText(String text) {
        if (!TextUtils.isEmpty(text)) {
            mStartTitle.setVisibility(VISIBLE);
            mStartTitle.setText(text);
        } else {
            mStartTitle.setVisibility(GONE);
        }
    }

    /**
     * 设置开始处标题的字体大小
     *
     * @param size
     */
    public void setStartTitleTextSize(int size) {
        mStartTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
    }

    /**
     * 设置开始处标题的文字颜色
     *
     * @param color
     */
    public void setStartTitleTextColor(int color) {
        mStartTitle.setTextColor(color);
    }

    /**
     * 设置开始处描述的文本，如果为0则隐藏，不能与结束处描述同时使用
     *
     * @param resId
     */
    public void setStartDescText(@StringRes int resId) {
        if (resId != 0) {
            mEndDesc.setVisibility(GONE);
            mStartDesc.setVisibility(VISIBLE);
            mStartDesc.setText(resId);
        } else {
            mStartDesc.setVisibility(GONE);
        }
    }

    /**
     * 设置开始处描述的文本，如果为null则隐藏，不能与结束处描述同时使用
     *
     * @param text
     */
    public void setStartDescText(String text) {
        if (!TextUtils.isEmpty(text)) {
            mEndDesc.setVisibility(GONE);
            mStartDesc.setVisibility(VISIBLE);
            mStartDesc.setText(text);
        } else {
            mStartDesc.setVisibility(GONE);
        }
    }

    /**
     * 设置开始处描述的字体大小
     *
     * @param size
     */
    public void setStartDescTextSize(int size) {
        mStartDesc.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
    }

    /**
     * 设置开始处描述的文本颜色
     *
     * @param color
     */
    public void setStartDescTextColor(int color) {
        mStartDesc.setTextColor(color);
    }

    /**
     * 设置结束处描述的文本，如果为0则隐藏，不能与开始处描述、结束处图标同时使用
     *
     * @param resId
     */
    public void setEndDescText(@StringRes int resId) {
        if (resId != 0) {
            mStartDesc.setVisibility(GONE);
            mEndIcon.setVisibility(GONE);
            mEndDesc.setVisibility(VISIBLE);
            mEndDesc.setText(resId);
        } else {
            mEndDesc.setVisibility(GONE);
        }
    }

    /**
     * 设置结束处描述的文本，如果为null则隐藏，不能与开始处描述、结束处图标同时使用
     *
     * @param text
     */
    public void setEndDescText(String text) {
        if (!TextUtils.isEmpty(text)) {
            mStartDesc.setVisibility(GONE);
            mEndIcon.setVisibility(GONE);
            mEndDesc.setVisibility(VISIBLE);
            mEndDesc.setText(text);
        } else {
            mEndDesc.setVisibility(GONE);
        }
    }

    /**
     * 设置束位处描述的字体大小
     *
     * @param size
     */
    public void setEndDescTextSize(int size) {
        mEndDesc.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
    }

    /**
     * 设置结束处描述的文本颜色
     *
     * @param color
     */
    public void setEndDescTextColor(int color) {
        mEndDesc.setTextColor(color);
    }

    /**
     * 设置结束处描述的文本对齐方式
     *
     * @param gravity
     */
    public void setEndDescGravity(int gravity) {
        if (gravity == 1) {
            mEndDesc.setGravity(Gravity.LEFT);
        } else {
            mEndDesc.setGravity(Gravity.RIGHT);
        }
    }

    /**
     * 设置结束处描述的左侧图标
     *
     * @param drawableLeft
     */
    public void setEdnDescDrawableLeft(Drawable drawableLeft) {
        if (drawableLeft != null) {
            ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) mEndDesc.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;

            mEndDesc.setCompoundDrawablesWithIntrinsicBounds(drawableLeft, null, null, null);
        }
    }

    /**
     * 设置结束处描述的图标间距
     *
     * @param drawablePadding
     */
    public void setEndDescDrawablePadding(int drawablePadding) {
        if (drawablePadding > 0) {
            mEndDesc.setCompoundDrawablePadding(drawablePadding);
        }
    }

    /**
     * 设置是否显示结束处箭头，不能与结束处开关同时使用
     *
     * @param show
     */
    public void setShowEndArrow(boolean show) {
        if (show) {
            mEndSwitch.setVisibility(GONE);
            mEndArrow.setVisibility(VISIBLE);
        } else {
            mEndArrow.setVisibility(GONE);
        }
    }

    /**
     * 设置是否显示结束处开关，不能与结束处箭头同时使用
     *
     * @param show
     */
    public void setShowEndSwitch(boolean show) {
        if (show) {
            mEndArrow.setVisibility(GONE);
            mEndSwitch.setVisibility(VISIBLE);
        } else {
            mEndSwitch.setVisibility(GONE);
        }
    }

    /**
     * 获取结束处开关
     *
     * @return
     */
    public Switch getEndSwitch() {
        return mEndSwitch;
    }

    /**
     * 设置结束处开关的状态
     *
     * @param checked
     */
    public void setEndSwitchChecked(boolean checked) {
        mEndSwitch.setChecked(checked);
    }

    /**
     * 设置结束处开关的切换监听
     *
     * @param listener
     */
    public void setEndSwitchOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener listener) {
        mEndSwitch.setOnCheckedChangeListener(listener);
    }

    /**
     * 设置是否显示顶部分界线
     *
     * @param show
     */
    public void setShowTopLine(boolean show) {
        mTopLine.setVisibility(show ? VISIBLE : GONE);
    }

    /**
     * 设置顶部分界线的边距，单位为dp
     *
     * @param left
     * @param top
     * @param right
     * @param bottom
     */
    public void setTopLineMargins(int left, int top, int right, int bottom) {
        LayoutParams layoutParams = (LayoutParams) mTopLine.getLayoutParams();
        layoutParams.setMargins(dip2px(left), dip2px(top), dip2px(right), dip2px(bottom));
        mTopLine.setLayoutParams(layoutParams);
    }

    /**
     * 设置是否显示底部分界线
     *
     * @param show
     */
    public void setShowBottomLine(boolean show) {
        mBottomLine.setVisibility(show ? VISIBLE : GONE);
    }

    /**
     * 设置底部分界线的边距，单位为dp
     *
     * @param left
     * @param top
     * @param right
     * @param bottom
     */
    public void setBottomLineMargins(int left, int top, int right, int bottom) {
        LayoutParams layoutParams = (LayoutParams) mBottomLine.getLayoutParams();
        layoutParams.setMargins(dip2px(left), dip2px(top), dip2px(right), dip2px(bottom));
        mBottomLine.setLayoutParams(layoutParams);
    }

    private int dip2px(int dp) {
        if (dp > 0) {
            return DensityUtility.dp2px(getContext(), dp);
        } else {
            return 0;
        }
    }
}
