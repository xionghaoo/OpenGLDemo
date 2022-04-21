package xh.zero.tools

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import xh.zero.ImageActivity
import xh.zero.R
import xh.zero.core.replaceFragment
import xh.zero.core.utils.SystemUtil
import xh.zero.core.utils.ToastUtil
import xh.zero.databinding.ActivitySilentCaptureBinding

class SilentCaptureActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySilentCaptureBinding
    private lateinit var fragment: SilentCaptureFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SystemUtil.toFullScreenMode(this)
        binding = ActivitySilentCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        // 获取可用的相机id
        val cameraIds = cameraManager.cameraIdList.filter {
            val characteristics = cameraManager.getCameraCharacteristics(it)
            val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
            capabilities?.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE) ?: false
        }
        fragment = SilentCaptureFragment.newInstance(cameraIds[0])
        replaceFragment(fragment, R.id.fragment_container)


        binding.btnCapture.setOnClickListener {
            fragment.takePicture(null, null, null, false) { imgPath ->
                ToastUtil.show(this, "拍照完成：$imgPath")
                ImageActivity.start(this, imgPath)
            }
        }
    }
}