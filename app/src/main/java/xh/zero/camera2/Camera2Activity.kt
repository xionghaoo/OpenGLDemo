package xh.zero.camera2

import android.content.Context
import android.content.res.Configuration
import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Size
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.window.WindowManager
import timber.log.Timber
import xh.zero.ImageActivity
import xh.zero.R
import xh.zero.core.replaceFragment
import xh.zero.core.utils.SystemUtil
import xh.zero.databinding.ActivityCamera2Binding
import xh.zero.view.BaseCameraActivity
import xh.zero.view.BaseCameraFragment

class Camera2Activity : BaseCameraActivity<ActivityCamera2Binding>() {

    companion object {
        private const val INITIAL_RECT_RATIO = 70
    }

//    private lateinit var binding: ActivityCamera2Binding
//    private var isInit = true
    private lateinit var fragment: Camera2PreviewFragment

    private var screenSize: Size? = null
    private var viewW: Int = 0
    private var viewH: Int = 0
    private var imageW: Int = 0
    private var imageH: Int = 0

    private val imgRect = Rect()
    private val viewRect = Rect()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
//            if (isInit) {
//                isInit = false
//                initialCameraView()
//            }
//        }

        binding.btnCapture.setOnClickListener {
            fragment.takePicture(imgRect, true) { imgPath ->
                ImageActivity.start(this, imgPath, requestedOrientation)
            }
        }

        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            binding.sbRectPercent.visibility = View.GONE
            binding.tvRectPercent.visibility = View.GONE
            binding.btnResetRect.visibility = View.GONE
        } else {
            binding.sbRectPercent.progress = INITIAL_RECT_RATIO
            binding.tvRectPercent.text = "${INITIAL_RECT_RATIO}%"
            binding.sbRectPercent.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    initialIndicatorRect(progress)
                    binding.tvRectPercent.text = "${progress}%"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }
            })

            binding.btnResetRect.setOnClickListener {
                binding.sbRectPercent.progress = INITIAL_RECT_RATIO
            }
        }

    }

    override fun getBindingView(): ActivityCamera2Binding = ActivityCamera2Binding.inflate(layoutInflater)

    override fun getCameraFragmentLayout(): ViewGroup = binding.fragmentContainer

    override fun onCameraAreaCreated(cameraId: String, area: Size, screen: Size, supportImage: Size) {
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewW = area.width
            viewH = area.height
            imageW = supportImage.width
            imageH = supportImage.height
            screenSize = Size(screen.width, screen.height)
            // ?????????????????????
            initialIndicatorRect(binding.sbRectPercent.progress)
        }

        fragment = Camera2PreviewFragment.newInstance(cameraId)
        replaceFragment(fragment, R.id.fragment_container)
    }

//    private fun initialCameraView() {
//        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
//        cameraManager.cameraIdList.forEachIndexed { index, cameraId ->
//            val characteristic = cameraManager.getCameraCharacteristics(cameraId)
//            // ????????????????????????
//            if (index == 0) {
//                val configurationMap = characteristic.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
//                configurationMap?.getOutputSizes(ImageFormat.JPEG)
//                    ?.maxByOrNull { it.height * it.width }
//                    ?.also { maxImageSize ->
//                        // Nexus6P??????????????????????????????4032x3024
//                        Timber.d("??????????????????????????????${maxImageSize}")
//                        val metrics = WindowManager(this).getCurrentWindowMetrics().bounds
//                        // Nexus6P???????????????1440 x 2560?????????NavigationBar?????????
//                        Timber.d("???????????????${metrics.width()} x ${metrics.height()}")
//                        val lp = binding.fragmentContainer.layoutParams as FrameLayout.LayoutParams
//
//                        Timber.d("????????????: ${if (resources.configuration.orientation == 1) "??????" else "??????"}")
//                        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
//                            // ?????????????????????????????????????????????????????????????????????SurfaceTexture?????????
//                            val ratio = maxImageSize.height.toFloat() / maxImageSize.width.toFloat()
//                            lp.width = metrics.width()
//                            // Nexus6P ??????????????????????????????
//                            // ??????????????????1440 / height = 3024 / 4032
//                            // height = 4032 / 3024 * 1440
//                            lp.height = (metrics.width() / ratio).toInt()
//                        } else {
//                            // ?????????????????????????????????????????????????????????????????????SurfaceTexture?????????
//                            val ratio = maxImageSize.height.toFloat() / maxImageSize.width.toFloat()
//                            // Nexus6P ??????????????????????????????
//                            // ??????????????????width / 1440 = 4032 / 3024
//                            // width = 4032 / 3024 * 1440
//                            lp.width = (metrics.height() / ratio).toInt()
//                            lp.height = metrics.height()
//
//                            viewW = lp.width
//                            viewH = lp.height
//                            imageW = maxImageSize.width
//                            imageH = maxImageSize.height
//                            screenSize = Size(metrics.width(), metrics.height())
//                            // ?????????????????????
//                            initialIndicatorRect(binding.sbRectPercent.progress)
//                        }
//                        lp.gravity = Gravity.CENTER
//                        fragment = Camera2PreviewFragment.newInstance(cameraId)
//                        replaceFragment(fragment, R.id.fragment_container)
//                    }
//
//                characteristic.get(CameraCharacteristics.SENSOR_ORIENTATION)?.let { orientation ->
//                    Timber.d("??????????????????${orientation}")
//                }
//            }
//        }
//    }

    /**
     * ???????????????????????????????????????
     */
    private fun initialIndicatorRect(percent: Int) {
        binding.vIndicatorRect.visibility = View.VISIBLE

        val r = screenSize!!.width.toFloat() / screenSize!!.height
        val ratio = percent * 0.01f
        val indicatorW = (ratio * viewW).toInt()
        val indicatorH = (indicatorW / r).toInt()

        viewRect.left = (screenSize!!.width - indicatorW) / 2
        viewRect.right = viewRect.left + indicatorW
        viewRect.top = (screenSize!!.height - indicatorH) / 2
        viewRect.bottom = viewRect.top + indicatorH
        binding.vIndicatorRect.drawRect(viewRect)

        val imageDrawRectW = (ratio * imageW).toInt()
        val imageDrawRectH = (imageDrawRectW / r).toInt()
        imgRect.left = ((imageW - imageDrawRectW) / 2f).toInt()
        imgRect.top = ((imageH - imageDrawRectH) / 2f).toInt()
        imgRect.bottom = imgRect.top + imageDrawRectH
        imgRect.right = imgRect.left + imageDrawRectW
    }
}