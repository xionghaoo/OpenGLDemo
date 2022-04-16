package xh.zero.camera2

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import timber.log.Timber
import xh.zero.R
import xh.zero.camera1.Camera1Fragment
import xh.zero.core.replaceFragment
import xh.zero.core.utils.SystemUtil

class Camera2Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SystemUtil.toFullScreenMode(this)
        setContentView(R.layout.activity_camera2)

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
                replaceFragment(Camera2Fragment.newInstance("0"), R.id.fragment_container)
            }
        }
    }
}