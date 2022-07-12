package xh.zero.tool.crop

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Size
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import timber.log.Timber
import xh.zero.R
import xh.zero.camera2.Camera2Activity
import xh.zero.core.replaceFragment
import xh.zero.databinding.ActivityCropBinding
import xh.zero.view.BaseCameraActivity

class CropActivity : BaseCameraActivity<ActivityCropBinding>() {

    private lateinit var fragment: CropFragment
    private var cameraWidth: Int? = null
    private var cameraHeight: Int? = null
    private var selectedCameraId: String = "0"
    private var cameraSize: Array<Size>? = null

    private val rect = Rect()
    private val viewRect = Rect()
    private var screenSize: Size? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.btnCapture.setOnClickListener {
            val l = 160
            val t = 225
            val w = 960
            val h = 710
            rect.left = l
            rect.top = t
            rect.right = l + w
            rect.bottom = t + h

            binding.fragmentContainer.visibility = View.INVISIBLE
            binding.btnCapture.visibility = View.INVISIBLE
            binding.btnClear.visibility = View.INVISIBLE

            fragment.takePicture(null, false, rect) { imgPath ->
                Timber.d("拍照完成")
                Glide.with(this)
                    .load(imgPath)
                    .into(binding.ivResult)

                binding.fragmentContainer.visibility = View.VISIBLE
                binding.btnCapture.visibility = View.VISIBLE
                binding.btnClear.visibility = View.VISIBLE
            }
        }
        binding.btnClear.setOnClickListener {
            binding.ivResult.setImageDrawable(null)
        }
    }

    override fun getBindingView(): ActivityCropBinding = ActivityCropBinding.inflate(layoutInflater)

    override fun getCameraFragmentLayout(): ViewGroup? = null

    override fun onCameraAreaCreated(
        cameraId: String,
        area: Size,
        screen: Size,
        supportImage: Size
    ) {
        screenSize = Size(screen.width, screen.height)

        // 设置分辨率
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraManager.cameraIdList.forEachIndexed { index, cameraId ->
            val characteristic = cameraManager.getCameraCharacteristics(cameraId)
            if (index == 0) {
                val configurationMap = characteristic.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                cameraSize = configurationMap?.getOutputSizes(ImageFormat.JPEG)
                cameraSize?.forEach { size ->
                    Timber.d("摄像头尺寸： $size")
                    if (size.width == 1280 && size.height == 1024) {
                        Timber.d("初始化尺寸：$index")
                        cameraWidth = size.width
                        cameraHeight = size.height
                    }
                }
            }
        }

//        val lp = binding.fragmentContainer.layoutParams as ConstraintLayout.LayoutParams
//        lp.width = ViewGroup.LayoutParams.MATCH_PARENT
//        lp.height = ViewGroup.LayoutParams.MATCH_PARENT



        fragment = CropFragment.newInstance(cameraId)


        fragment.setSize(cameraWidth, cameraHeight)
        replaceFragment(fragment, R.id.fragment_container)
    }

    /**
     * 给预览和成像加上指示器矩形
     */
//    private fun initialIndicatorRect(position: Camera2Activity.RectPos) {
//        binding.vIndicatorRect.visibility = View.VISIBLE
//
//        val rW = rect.width() / 1280f
//        val rH = rect.height() / 960f
////        val ratio = 1280f / viewRect.width()
//        val indicatorW = screenSize!!.width * rW
//        val indicatorH = screenSize!!.height * rH
//
//        val rT = rect.top / rW
//        viewRect.left = (screenSize!!.width - indicatorW) / 2
//        viewRect.right = viewRect.left + indicatorW
//        viewRect.top = (rect.top / rW).toInt()
//        viewRect.bottom = viewRect.top + indicatorH
//        binding.vIndicatorRect.drawRect(viewRect)
//    }
}