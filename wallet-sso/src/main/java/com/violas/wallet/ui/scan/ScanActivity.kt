package com.violas.wallet.ui.scan

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.media.AudioManager
import android.media.SoundPool
import android.os.Bundle
import androidx.fragment.app.Fragment
import cn.bertsir.zbar.Qr.Symbol
import cn.bertsir.zbar.QrConfig
import cn.bertsir.zbar.ScanCallback
import cn.bertsir.zbar.utils.QRUtils
import com.palliums.utils.start
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import kotlinx.android.synthetic.main.activity_scan.*
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import qiu.niorgai.StatusBarCompat

class ScanActivity : BaseAppActivity(), EasyPermissions.PermissionCallbacks {
    companion object {
        public const val RESULT_QR_CODE_DATA = "a1"
        fun start(context: Activity, requestCde: Int) {
            Intent(context, ScanActivity::class.java).start(context, requestCde)
        }

        fun start(context: Fragment, requestCde: Int) {
            Intent(context.activity, ScanActivity::class.java).start(context, requestCde)
        }
    }

    override fun getLayoutResId() = R.layout.activity_scan

    private var soundPool: SoundPool? = null
    private val REQUEST_PERMISSION_CAMERA = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        StatusBarCompat.translucentStatusBar(this, true)
        super.onCreate(savedInstanceState)

        Symbol.scanType = 1
        Symbol.is_only_scan_custom = true
        Symbol.is_auto_zoom = false
        Symbol.doubleEngine = true
        Symbol.screenWidth = QRUtils.getInstance().getScreenWidth(this)
        Symbol.screenHeight = QRUtils.getInstance().getScreenHeight(this)

        if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        startCamera()

        title = getString(R.string.title_scan)

        initView()

        soundPool = SoundPool(10, AudioManager.STREAM_SYSTEM, 1)
        soundPool?.load(this, R.raw.qrcode, 1)
    }

    private fun startCamera() {
        val perms = arrayOf(Manifest.permission.CAMERA)
        if (EasyPermissions.hasPermissions(this, *perms)) {
            try {
                cameraPreview.apply {
                    setScanCallback(resultCallback)
                    start()
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        } else {
            EasyPermissions.requestPermissions(
                this, getString(R.string.hint_scan_need_camera_permissions),
                REQUEST_PERMISSION_CAMERA, *perms
            )
        }
    }

    private val resultCallback = ScanCallback { result ->
        soundPool?.play(1, 1f, 1f, 0, 0, 1f)
        onScanSuccess(result)
    }

    private fun onScanSuccess(result: String) {
        cameraPreview.setFlash(false)

        setResult(Activity.RESULT_OK,
            Intent().apply {
                putExtra(RESULT_QR_CODE_DATA, result)
            })
        finish()
    }

    private fun initView() {
        scanView.setType(QrConfig.SCANVIEW_TYPE_QRCODE)
        scanView.startScan()

//        scanView.setScanBackgroundDrawable(
//            ResourcesCompat.getDrawable(
//                resources,
//                R.drawable.shape_scan_border_bg,
//                null
//            )
//        )
        scanView.setCornerWidth(8)
        scanView.setCornerColor(Color.parseColor("#504ACB"))
        scanView.setLineColor(Color.parseColor("#504ACB"))
    }


    override fun onResume() {
        // Before setting full screen flags, we must wait a bit to let UI settle; otherwise, we may
        // be trying to set app to immersive mode before it's ready and the flags do not stick
        cameraPreview.apply {
            setScanCallback(resultCallback)
            start()
        }
        scanView.onResume()
        super.onResume()
    }

    override fun onPause() {
        if (cameraPreview != null) {
            cameraPreview.stop()
        }
        scanView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (cameraPreview != null) {
            cameraPreview.setFlash(false)
            cameraPreview.stop()
        }
        soundPool?.release()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this)
                .setTitle(getString(R.string.hint_get_the_camera_permissions))
                .setRationale(getString(R.string.hint_set_permissions_to_open))
                .build().show();
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        when (requestCode) {
            REQUEST_PERMISSION_CAMERA -> {
                startCamera()
            }
        }
    }
}
