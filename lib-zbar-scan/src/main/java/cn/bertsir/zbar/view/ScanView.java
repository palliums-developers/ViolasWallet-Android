package cn.bertsir.zbar.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;

import cn.bertsir.zbar.Qr.Symbol;
import cn.bertsir.zbar.QrConfig;
import cn.bertsir.zbar.R;


/**
 * Created by Bert on 2017/9/20.
 */

public class ScanView extends FrameLayout {

    private LineView iv_scan_line;
    private TranslateAnimation animation;
    private FrameLayout fl_scan;
    private int CURRENT_TYEP = 1;
    private CornerView cnv_left_top;
    private CornerView cnv_left_bottom;
    private CornerView cnv_right_top;
    private CornerView cnv_right_bottom;
    private ImageView topImage;
    private ImageView bottomImage;
    private ArrayList<CornerView> cornerViews;
    private int line_speed = 3000;

    public ScanView(Context context) {
        super(context);
        initView(context);
    }

    public ScanView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public ScanView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context mContext) {
        View scan_view = View.inflate(mContext, R.layout.view_scan, this);

        cnv_left_top = (CornerView) scan_view.findViewById(R.id.cnv_left_top);
        cnv_left_bottom = (CornerView) scan_view.findViewById(R.id.cnv_left_bottom);
        cnv_right_top = (CornerView) scan_view.findViewById(R.id.cnv_right_top);
        cnv_right_bottom = (CornerView) scan_view.findViewById(R.id.cnv_right_bottom);

        topImage = (ImageView) scan_view.findViewById(R.id.topImage);
        bottomImage = (ImageView) scan_view.findViewById(R.id.bottomImage);

        cornerViews = new ArrayList<>();
        cornerViews.add(cnv_left_top);
        cornerViews.add(cnv_left_bottom);
        cornerViews.add(cnv_right_top);
        cornerViews.add(cnv_right_bottom);

        iv_scan_line = (LineView) scan_view.findViewById(R.id.iv_scan_line);

        fl_scan = (FrameLayout) scan_view.findViewById(R.id.fl_scan);

        animation = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation
                .RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT,
                0.95f);
        animation.setDuration(line_speed);
        animation.setRepeatCount(-1);
        animation.setRepeatMode(Animation.RESTART);
    }

    public void setTopWeight(int weight) {
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) topImage.getLayoutParams();
        layoutParams.weight = weight;
        topImage.setLayoutParams(layoutParams);
    }

    public void setBottomWeight(int weight) {
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) bottomImage.getLayoutParams();
        layoutParams.weight = weight;
        bottomImage.setLayoutParams(layoutParams);
    }

    public void setLineSpeed(int speed) {
        animation.setDuration(speed);
    }

    public void startScan() {
        iv_scan_line.startAnimation(animation);
        getViewWidthHeight();
    }

    public void onPause() {
        if (iv_scan_line != null) {
            iv_scan_line.clearAnimation();
            iv_scan_line.setVisibility(View.GONE);
        }
    }

    public void onResume() {
        if (iv_scan_line != null) {
            iv_scan_line.startAnimation(animation);
        }
    }

    public void setScanBackgroundDrawable(Drawable drawable) {
        fl_scan.setBackgroundDrawable(drawable);
    }

    public void setType(int type) {
        CURRENT_TYEP = type;
        LinearLayout.LayoutParams fl_params = (LinearLayout.LayoutParams) fl_scan.getLayoutParams();
        if (CURRENT_TYEP == QrConfig.SCANVIEW_TYPE_QRCODE) {
            fl_params.width = dip2px(220);
            fl_params.height = dip2px(220);
        } else if (CURRENT_TYEP == QrConfig.SCANVIEW_TYPE_BARCODE) {
            fl_params.width = dip2px(300);
            fl_params.height = dip2px(100);
        }
        fl_scan.setLayoutParams(fl_params);
    }

    public void setCornerColor(int color) {
        for (int i = 0; i < cornerViews.size(); i++) {
            cornerViews.get(i).setColor(color);
        }
    }

    public void setCornerWidth(int dp) {
        for (int i = 0; i < cornerViews.size(); i++) {
            cornerViews.get(i).setLineWidth(dp);
        }
    }

    public void setLineColor(int color) {
        iv_scan_line.setLinecolor(color);
    }

    public int dip2px(int dp) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5);
    }

    public void getViewWidthHeight() {
        fl_scan.post(new Runnable() {
            @Override
            public void run() {
                Symbol.cropWidth = fl_scan.getMeasuredWidth();
                Symbol.cropHeight = fl_scan.getMeasuredHeight();
                int[] args = new int[2];
                fl_scan.getLocationOnScreen(args);
                Symbol.cropX = args[0];
                Symbol.cropY = args[1];
            }
        });
    }

}
