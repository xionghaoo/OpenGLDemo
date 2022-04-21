package xh.zero

import android.Manifest
import android.content.Context
import android.content.res.Resources
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Size
import android.widget.Button
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber
import xh.zero.camera1.Camera1Activity
import xh.zero.camera2.Camera2Activity
import xh.zero.camerax.CameraXActivity
import xh.zero.core.startPlainActivity
import xh.zero.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCamerax.setOnClickListener {
            startPlainActivity(CameraXActivity::class.java)
        }

        binding.btnCamera1.setOnClickListener {
            startPlainActivity(Camera1Activity::class.java)
        }

        binding.btnCamera2.setOnClickListener {
            startPlainActivity(Camera2Activity::class.java)
        }

        binding.btnCameraFilterChain.setOnClickListener {

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