/*
 * Copyright © Yan Zhenjie
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.bertsir.zbar;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * <p>QRCode Camera preview, include QRCode recognition.</p>
 */
public class CameraPreview extends FrameLayout implements TextureView.SurfaceTextureListener {

    private CameraManager mCameraManager;
    private CameraScanAnalysis mPreviewCallback;
    private TextureView mSurfaceView;

    public CameraPreview(Context context) {
        this(context, null);
    }

    public CameraPreview(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraPreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mCameraManager = new CameraManager(context);
        mPreviewCallback = new CameraScanAnalysis(context);
    }

    /**
     * Set Scan results callback.
     *
     * @param callback {@link ScanCallback}.
     */
    public void setScanCallback(ScanCallback callback) {
        mPreviewCallback.setScanCallback(callback);
    }

    public void nextScan() {
        mPreviewCallback.onStart();
    }

    /**
     * Camera start preview.
     */
    public boolean start() {
        try {
            mCameraManager.openDriver();
        } catch (Exception e) {
//            Toast.makeText(getContext(), "摄像头权限被拒绝！", Toast.LENGTH_SHORT).show();
            return false;
        }
        mPreviewCallback.onStart();

        if (mSurfaceView == null) {
            mSurfaceView = new TextureView(getContext());
            addView(mSurfaceView, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            SurfaceTexture holder = mSurfaceView.getSurfaceTexture();
            mSurfaceView.setSurfaceTextureListener(this);
        }
        startCameraPreview(mSurfaceView.getSurfaceTexture());
        return true;
    }

    /**
     * Camera stop preview.
     */
    public void stop() {
        removeCallbacks(mAutoFocusTask);
        mPreviewCallback.onStop();

        mCameraManager.stopPreview();
        mCameraManager.closeDriver();
    }

    private void startCameraPreview(SurfaceTexture holder) {
        try {
            mCameraManager.startPreview(holder, mPreviewCallback);
            mCameraManager.autoFocus(mFocusCallback);
        } catch (Exception e) {
            e.printStackTrace();
            //如果异常延迟200ms再试
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mCameraManager.autoFocus(mFocusCallback);
                }
            }, 200);
        }
    }

    private Camera.AutoFocusCallback mFocusCallback = new Camera.AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            postDelayed(mAutoFocusTask, 1000);
        }
    };

    private Runnable mAutoFocusTask = new Runnable() {
        public void run() {
            mCameraManager.autoFocus(mFocusCallback);
        }
    };

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    public void setFlash() {
        mCameraManager.setFlash();
    }

    public void setFlash(boolean open) {
        mCameraManager.setFlash(open);
    }

    public void setZoom(float zoom) {
        mCameraManager.setCameraZoom(zoom);
    }

    public void handleZoom(boolean isZoomIn) {
        mCameraManager.handleZoom(isZoomIn);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (surface == null) {
            return;
        }
        startCameraPreview(surface);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        if (surface == null) {
            return;
        }
        mCameraManager.stopPreview();
        startCameraPreview(surface);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
}