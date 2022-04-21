package xh.zero.camerax

import android.content.Context
import android.content.res.Configuration
import android.graphics.ImageFormat
import android.graphics.Point
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Size
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.window.WindowManager
import timber.log.Timber
import xh.zero.R
import xh.zero.core.replaceFragment
import xh.zero.core.utils.SystemUtil
import xh.zero.databinding.ActivityCameraXactivityBinding

class CameraXActivity : AppCompatActivity() {

    companion object {
        // 屏幕缩放比例
        private const val SCREEN_SCALE = 0.5
    }

    private lateinit var binding: ActivityCameraXactivityBinding
    private lateinit var fragment: CameraXFragment
    private var isInit = true

    private val leftTop = Point(0, 0)
    private val leftBottom = Point(0, 0)
    private val rightTop = Point(0, 0)
    private val rightBottom = Point(0, 0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SystemUtil.toFullScreenMode(this)
        binding = ActivityCameraXactivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCapture.setOnClickListener {
            fragment.takePhoto(leftTop, leftBottom, rightTop, rightBottom)
        }

        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            if (isInit) {
                isInit = false
                val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
                cameraManager.cameraIdList.forEachIndexed { index, cameraId ->
                    val characteristic = cameraManager.getCameraCharacteristics(cameraId)
                    if (index == 0) {
                        characteristic.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                            ?.getOutputSizes(ImageFormat.JPEG)
                            ?.maxByOrNull { it.width * it.height }
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
                                    Timber.d("相机预览视图尺寸：${lp.width} x ${lp.height}")
                                }
                                lp.gravity = Gravity.CENTER

                                fragment =  CameraXFragment.newInstance(cameraId)
                                replaceFragment(fragment, R.id.fragment_container)
                            }

                        val cameraOrientation = characteristic.get(CameraCharacteristics.SENSOR_ORIENTATION)
                        Timber.d("摄像头角度：$cameraOrientation")




//                        val sizeList = configurationMap?.getOutputSizes(ImageFormat.JPEG)
//                        var maxCameraSize = Size(0, 0)
//                        // 相机支持的尺寸
//                        sizeList?.forEach { size ->
//                            Timber.d("camera support: [${size.width}, ${size.height}]")
//                            if (size.height > maxCameraSize.height) {
//                                maxCameraSize = size
//                            }
//                        }
//
//                        val metrics = WindowManager(this).getCurrentWindowMetrics().bounds
//
//                        // 屏幕尺寸不小于摄像头成像尺寸
//                        if (metrics.width() >= maxCameraSize.width || metrics.height() >= maxCameraSize.height) {
//
//                        }
//
//                        // 屏幕缩放尺寸
//                        val screenWidth = metrics.width() * SCREEN_SCALE
//                        val screenHeight = metrics.height() * SCREEN_SCALE
//
//                        val lp = binding.vScreenRatioRect.layoutParams as FrameLayout.LayoutParams
//                        lp.width = screenWidth.toInt()
//                        lp.height = screenHeight.toInt()
//
//                        // 计算屏幕在图片上的位置，左上角的坐标就是最终需要的转换坐标
//                        leftTop.x = ((maxCameraSize.width - screenWidth) / 2).toInt()
//                        leftTop.y = ((maxCameraSize.height - screenHeight) / 2).toInt()
//                        leftBottom.x = leftTop.x
//                        leftBottom.y = (leftTop.y + screenHeight).toInt()
//                        rightTop.x = (leftTop.x + screenWidth).toInt()
//                        rightTop.y = leftTop.y
//                        rightBottom.x = rightTop.x
//                        rightBottom.y = (rightTop.y + screenHeight).toInt()

                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

//        binding.fragmentContainer.postDelayed({
//            hideSystemUI()
//        }, IMMERSIVE_FLAG_TIMEOUT)
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.fragmentContainer).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}