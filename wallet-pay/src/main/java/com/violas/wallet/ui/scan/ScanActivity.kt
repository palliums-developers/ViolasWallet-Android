package com.violas.wallet.ui.scan

import android.Manifest
import android.app.Activity
import android.content.Context
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
import com.violas.wallet.biz.*
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.ui.transfer.TransferActivity
import com.violas.wallet.ui.walletconnect.WalletConnectAuthorizationActivity
import kotlinx.android.synthetic.main.activity_scan.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest

/**
 * 二维码扫描页面
 */
class ScanActivity : BaseAppActivity(), EasyPermissions.PermissionCallbacks {

    companion object {
        const val RESULT_QR_CODE_DATA = "a1"

        fun start(context: Activity, requestCde: Int) {
            Intent(context, ScanActivity::class.java)
                .apply { putExtra(KEY_ONE, true) }
                .start(context, requestCde)
        }

        fun start(context: Fragment, requestCde: Int) {
            Intent(context.activity, ScanActivity::class.java)
                .apply { putExtra(KEY_ONE, true) }
                .start(context, requestCde)
        }

        fun start(context: Context) {
            Intent(context, ScanActivity::class.java)
                .apply { putExtra(KEY_ONE, false) }
                .start(context)
        }
    }

    override fun getLayoutResId() = R.layout.activity_scan

    private var soundPool: SoundPool? = null
    private val REQUEST_PERMISSION_CAMERA = 2
    private val returnResult by lazy { intent?.getBooleanExtra(KEY_ONE, true) ?: true }

    override fun onCreate(savedInstanceState: Bundle?) {
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

        title = getString(R.string.qr_code_scan_title)

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
                PermissionRequest.Builder(this, REQUEST_PERMISSION_CAMERA, *perms)
                    .setRationale(R.string.qr_code_scan_need_permissions_desc)
                    .setNegativeButtonText(R.string.common_action_cancel)
                    .setPositiveButtonText(R.string.common_action_ok)
                    .setTheme(R.style.AppAlertDialog)
                    .build()
            )
        }
    }

    private val resultCallback = ScanCallback { result ->
        soundPool?.play(1, 1f, 1f, 0, 0, 1f)
        cameraPreview?.setFlash(false)
        onScanSuccess(result)
    }

    private fun onScanSuccess(result: String) {
        launch {
            val qrCode = decodeQRCode(result)

            if (returnResult) {
                setResult(
                    Activity.RESULT_OK,
                    Intent().apply { putExtra(RESULT_QR_CODE_DATA, qrCode) }
                )
                close()
                return@launch
            }

            when (qrCode) {
                is TransferQRCode -> {
                    TransferActivity.start(
                        this@ScanActivity,
                        qrCode.coinType,
                        qrCode.address,
                        qrCode.subAddress,
                        qrCode.amount,
                        qrCode.tokenName
                    )
                }

                is WalletConnectQRCode -> {
                    WalletConnectAuthorizationActivity.start(this@ScanActivity, qrCode.content)
                }

                is CommonQRCode -> {
                    ScanResultActivity.start(this@ScanActivity, qrCode.content)
                }
            }
            overridePendingTransition(R.anim.activity_bottom_in, R.anim.activity_none)

            delay(300)
            close()
            overridePendingTransition(R.anim.activity_none, R.anim.activity_none)
        }
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
                .setTitle(getString(R.string.qr_code_scan_set_permissions_title))
                .setRationale(getString(R.string.qr_code_scan_set_permissions_desc))
                .setNegativeButton(R.string.common_action_cancel)
                .setPositiveButton(R.string.common_action_ok)
                .setThemeResId(R.style.AppAlertDialog)
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
