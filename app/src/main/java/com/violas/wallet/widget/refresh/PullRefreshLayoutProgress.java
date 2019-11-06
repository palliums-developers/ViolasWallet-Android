package com.violas.wallet.widget.refresh;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

import com.violas.wallet.R;
import com.violas.wallet.utils.DensityUtility;


/**
 * Created by elephant on 2019-08-04 14:17.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: PullRefreshLayoutProgress
 */
public class PullRefreshLayoutProgress extends View {

    private Paint mPaint;
    private int   mMaxProgress     = 100;
    private int   mCurrentProgress = 0;

    private RectF         mRectF;
    private ValueAnimator mValueAnimator;
    private boolean       mIsRunningAnim = false;
    private boolean       mNeedReAnim    = false;

    private int mAnimValue;

    public PullRefreshLayoutProgress(Context context) {
        this(context, null);
    }

    public PullRefreshLayoutProgress(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PullRefreshLayoutProgress(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(21)
    public PullRefreshLayoutProgress(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    public void init(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {

        mPaint = new Paint();
        mPaint.setDither(true);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(DensityUtility.dp2px(context, 1.5F));
        mPaint.setColor(getResources().getColor(R.color.colorAccent));
        mPaint.setStyle(Paint.Style.STROKE);
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mRectF = new RectF(mPaint.getStrokeWidth() / 2,
                mPaint.getStrokeWidth() / 2,
                getWidth() - mPaint.getStrokeWidth() / 2,
                getHeight() - mPaint.getStrokeWidth() / 2);
    }

    public void setCurrentProgress(int progress) {
        mCurrentProgress = progress;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        //当前的进度角度
        if (!mIsRunningAnim) {
            float sweepAngle = 360 * (mCurrentProgress / (float) mMaxProgress);
            canvas.drawArc(mRectF, -90, sweepAngle, false, mPaint);
        } else {
            canvas.drawArc(mRectF, mAnimValue, 340, false, mPaint);
        }
    }


    /**
     * 开启一个转圈动画
     */
    public void startLoadingAnim() {

        if (isRunning()) {
            return;
        }

        stopAnim();

        mValueAnimator = ValueAnimator.ofInt(-90, 270);
        mValueAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mValueAnimator.setRepeatMode(ValueAnimator.RESTART);
        mValueAnimator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            mAnimValue = value;
            invalidate();
        });
        mValueAnimator.setInterpolator(new LinearInterpolator());
        mValueAnimator.setDuration(1000);
        mValueAnimator.start();

        mIsRunningAnim = true;
    }

    /**
     * 结束loading 动画
     */
    public void stopAnim() {

        mIsRunningAnim = false;

        if (mValueAnimator != null) {
            mValueAnimator.cancel();
            mValueAnimator = null;
        }

        mCurrentProgress = 99;

        invalidate();
    }


    public boolean isRunning() {
        return mIsRunningAnim;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (mNeedReAnim) {
            startLoadingAnim();
            mNeedReAnim = false;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (isRunning()) {
            stopAnim();
            mNeedReAnim = true;
        }
    }
}
