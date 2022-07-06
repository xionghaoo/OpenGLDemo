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
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.bumptech.glide.Glide
import timber.log.Timber
import xh.zero.R
import xh.zero.core.replaceFragment
import xh.zero.databinding.ActivityCropBinding
import xh.zero.view.BaseCameraActivity

class CropActivity : BaseCameraActivity<ActivityCropBinding>() {
    private lateinit var fragment: CropFragment
    private var cameraWidth: Int? = null
    private var cameraHeight: Int? = null
    private var selectedCameraId: String = "0"
    private var cameraSize: Array<Size>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.btnCapture.setOnClickListener {
            val l = 170
            val t = 200
            val w = 960
            val h = 720
            fragment.takePicture(null, false, Rect(l, t, l + w, t + h)) { imgPath ->
                Timber.d("拍照完成")
                Glide.with(this)
                    .load(imgPath)
                    .into(binding.ivResult)
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
        // 设置分辨率
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraManager.cameraIdList.forEachIndexed { index, cameraId ->
            val characteristic = cameraManager.getCameraCharacteristics(cameraId)
            if (index == 0) {
                val configurationMap = characteristic.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                cameraSize = configurationMap?.getOutputSizes(ImageFormat.JPEG)
                cameraSize?.forEach { size ->
                    Timber.d("摄像头尺寸： $size")
                    if (size.width == 1280 && size.height == 960) {
                        Timber.d("初始化尺寸：$index")
                        cameraWidth = size.width
                        cameraHeight = size.height
                    }
                }
            }
        }

        fragment = CropFragment.newInstance(cameraId)
        fragment.setSize(cameraWidth, cameraHeight)
        replaceFragment(fragment, R.id.fragment_container)
    }
}