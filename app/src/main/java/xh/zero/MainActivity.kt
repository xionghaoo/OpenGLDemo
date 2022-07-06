package xh.zero

import android.Manifest
import android.content.Context
import android.content.res.Resources
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.widget.Button
import com.google.android.flexbox.FlexboxLayout
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber
import xh.zero.camera1.Camera1Activity
import xh.zero.camera2.Camera2Activity
import xh.zero.camerax.CameraXActivity
import xh.zero.core.startPlainActivity
import xh.zero.core.utils.SystemUtil
import xh.zero.databinding.ActivityMainBinding
import xh.zero.filterchain.FilterChainActivity
import xh.zero.silentcamera.SilentCaptureActivity
import xh.zero.tool.ToolActivity
import xh.zero.tool.crop.CropActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val items = arrayOf(
        "Camera2测试", "相机工具", "裁剪工具", "CameraX测试",
        "Camera1测试", "过滤链测试", "无预览拍照测试",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SystemUtil.toFullScreenMode(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.containerButtons.removeAllViews()
        items.forEachIndexed { index, item ->
            val btn = Button(ContextThemeWrapper(this, R.style.ToolMenu), null, 0)
            btn.text = item
            btn.setBackgroundResource(R.drawable.shape_menu)
            binding.containerButtons.addView(btn)
            val lp = btn.layoutParams as FlexboxLayout.LayoutParams
            lp.width = resources.getDimension(R.dimen.size_button).toInt()
            lp.height = resources.getDimension(R.dimen.size_button).toInt()
            lp.leftMargin = resources.getDimension(R.dimen.margin_button).toInt()
            lp.rightMargin = resources.getDimension(R.dimen.margin_button).toInt()
            lp.topMargin = resources.getDimension(R.dimen.margin_button).toInt()
            lp.bottomMargin = resources.getDimension(R.dimen.margin_button).toInt()
            btn.setOnClickListener {
                when(index) {
                    0 -> startPlainActivity(Camera2Activity::class.java)
                    1 -> startPlainActivity(ToolActivity::class.java)
                    2 -> startPlainActivity(CropActivity::class.java)
                    3 -> startPlainActivity(CameraXActivity::class.java)
                    4 -> startPlainActivity(Camera1Activity::class.java)
                    5 -> startPlainActivity(FilterChainActivity::class.java)
                    6 -> startPlainActivity(SilentCaptureActivity::class.java)
                }
            }
        }
        // 必要权限申请
        permissionTask()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    @AfterPermissionGranted(REQUEST_CODE_ALL_PERMISSION)
    private fun permissionTask() {
        if (hasPermission()) {

            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            cameraManager.cameraIdList.forEachIndexed { index, cameraId ->
                val characteristic = cameraManager.getCameraCharacteristics(cameraId)
                if (index == 0) {
                    val configurationMap = characteristic.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    val sizeList = configurationMap?.getOutputSizes(ImageFormat.JPEG)
                    // 相机支持的尺寸
                    sizeList?.forEach { size ->
                        Timber.d("camera support: [${size.width}, ${size.height}]")
                    }

                }
            }
            binding.tvAvailableCamera.text = "可用的摄像数量：${cameraManager.cameraIdList.size}"
        } else {
            EasyPermissions.requestPermissions(
                this,
                "App需要相关权限，请授予",
                REQUEST_CODE_ALL_PERMISSION,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    private fun hasPermission() : Boolean {
        return EasyPermissions.hasPermissions(
            this,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private fun getNavigationBarHeight(resources: Resources): Int {
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else 0
    }

    companion object {
        private const val REQUEST_CODE_ALL_PERMISSION = 1
        const val TAG = "MainActivity"
    }
}