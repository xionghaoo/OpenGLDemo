package xh.zero.camera2

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import androidx.window.WindowManager
import timber.log.Timber
import xh.zero.R
import xh.zero.core.replaceFragment
import xh.zero.core.utils.SystemUtil
import xh.zero.databinding.ActivityCamera2Binding

class Camera2Activity : AppCompatActivity() {

    private lateinit var binding: ActivityCamera2Binding
    private var isInit = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SystemUtil.toFullScreenMode(this)
        binding = ActivityCamera2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            if (isInit) {
                isInit = false
                initialCameraView()
            }
        }

    }

    private fun initialCameraView() {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraManager.cameraIdList.forEachIndexed { index, cameraId ->
            val characteristic = cameraManager.getCameraCharacteristics(cameraId)
            // 打开第一个摄像头
            if (index == 0) {
                val configurationMap = characteristic.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                configurationMap?.getOutputSizes(ImageFormat.JPEG)
                    ?.maxByOrNull { it.height * it.width }
                    ?.also { maxImageSize ->
                        // Nexus6P相机支持的最大尺寸：4032x3024
                        Timber.d("相机支持的最大尺寸：${maxImageSize}")
                        val metrics = WindowManager(this).getCurrentWindowMetrics().bounds
                        // Nexus6P屏幕尺寸：1440 x 2560，包含NavigationBar的高度
                        Timber.d("屏幕尺寸：${metrics.width()} x ${metrics.height()}")
                        val lp = binding.fragmentContainer.layoutParams as FrameLayout.LayoutParams

                        Timber.d("屏幕方向: ${if (resources.configuration.orientation == 1) "竖直" else "水平"}")
                        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                            // 竖直方向：设置预览区域的尺寸，这个尺寸用于接收SurfaceTexture的显示
                            val ratio = maxImageSize.height.toFloat() / maxImageSize.width.toFloat()
                            lp.width = metrics.width()
                            // Nexus6P 竖直方向屏幕计算高度
                            // 等比例关系：1440 / height = 3024 / 4032
                            // height = 4032 / 3024 * 1440
                            lp.height = (metrics.width() / ratio).toInt()
                        } else {
                            // 水平方向：设置预览区域的尺寸，这个尺寸用于接收SurfaceTexture的显示
                            val ratio = maxImageSize.height.toFloat() / maxImageSize.width.toFloat()
                            // Nexus6P 竖直方向屏幕计算高度
                            // 等比例关系：width / 1440 = 4032 / 3024
                            // width = 4032 / 3024 * 1440
                            lp.width = (metrics.height() / ratio).toInt()
                            lp.height = metrics.height()
                        }
                        lp.gravity = Gravity.CENTER

                        replaceFragment(Camera2Fragment.newInstance(index.toString()), R.id.fragment_container)
                    }
            }
        }
    }
}