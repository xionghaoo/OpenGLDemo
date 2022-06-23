package xh.zero.view

import android.content.Context
import android.content.res.Configuration
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Size
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import androidx.window.WindowManager
import timber.log.Timber
import xh.zero.R
import xh.zero.camera2.Camera2PreviewFragment
import xh.zero.core.replaceFragment
import xh.zero.core.utils.SystemUtil

abstract class BaseCameraActivity<V: ViewBinding> : AppCompatActivity() {
    protected lateinit var binding: V
    private var isInit = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SystemUtil.toFullScreenMode(this)
        binding = getBindingView()
        setContentView(binding.root)

        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            if (isInit) {
                isInit = false
                initialCameraArea()
            }
        }
    }

    abstract fun getBindingView(): V

    abstract fun getCameraFragmentLayout(): ViewGroup?

    abstract fun onCameraAreaCreated(cameraId: String, area: Size, screen: Size, supportImage: Size)

    private fun initialCameraArea() {
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
                        val layout =  getCameraFragmentLayout()
                        var areaSize = Size(0, 0)
                        if (layout != null) {
                            val lp = layout.layoutParams as ViewGroup.LayoutParams

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

//                            viewW = lp.width
//                            viewH = lp.height
//                            imageW = maxImageSize.width
//                            imageH = maxImageSize.height
//                            screenSize = Size(metrics.width(), metrics.height())
//                            // 只在水平方向加
//                            initialIndicatorRect(binding.sbRectPercent.progress)
                            }
                            areaSize = Size(lp.width, lp.height)
                        }
                        onCameraAreaCreated(
                            cameraId,
                            areaSize,
                            Size(metrics.width(), metrics.height()),
                            Size(maxImageSize.width, maxImageSize.height)
                        )
                    }

                characteristic.get(CameraCharacteristics.SENSOR_ORIENTATION)?.let { orientation ->
                    Timber.d("摄像头方向：${orientation}")
                }
            }
        }
    }
}