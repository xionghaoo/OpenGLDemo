package xh.zero.camerax

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Point
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Size
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

    private val leftTop = Point()
    private val leftBottom = Point()
    private val rightTop = Point()
    private val rightBottom = Point()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        SystemUtil.toFullScreenMode(this)
        binding = ActivityCameraXactivityBinding.inflate(layoutInflater)
        setContentView(binding.root)



        fragment =  CameraXFragment.newInstance()
        replaceFragment(fragment, R.id.fragment_container)

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
                        val configurationMap = characteristic.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        val sizeList = configurationMap?.getOutputSizes(ImageFormat.JPEG)
                        var maxCameraSize = Size(0, 0)
                        // 相机支持的尺寸
                        sizeList?.forEach { size ->
                            Timber.d("camera support: [${size.width}, ${size.height}]")
                            if (size.height > maxCameraSize.height) {
                                maxCameraSize = size
                            }
                        }

                        val metrics = WindowManager(this).getCurrentWindowMetrics().bounds

                        // 屏幕尺寸不小于摄像头成像尺寸
                        if (metrics.width() >= maxCameraSize.width || metrics.height() >= maxCameraSize.height) {

                        }

                        // 屏幕缩放尺寸
                        val screenWidth = metrics.width() * SCREEN_SCALE
                        val screenHeight = metrics.height() * SCREEN_SCALE

                        val lp = binding.vScreenRatioRect.layoutParams as FrameLayout.LayoutParams
                        lp.width = screenWidth.toInt()
                        lp.height = screenHeight.toInt()

                        // 计算屏幕在图片上的位置，左上角的坐标就是最终需要的转换坐标
                        leftTop.x = ((maxCameraSize.width - screenWidth) / 2).toInt()
                        leftTop.y = ((maxCameraSize.height - screenHeight) / 2).toInt()
                        leftBottom.x = leftTop.x
                        leftBottom.y = (leftTop.y + screenHeight).toInt()
                        rightTop.x = (leftTop.x + screenWidth).toInt()
                        rightTop.y = leftTop.y
                        rightBottom.x = rightTop.x
                        rightBottom.y = (rightTop.y + screenHeight).toInt()

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