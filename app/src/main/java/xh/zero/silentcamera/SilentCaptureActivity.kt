package xh.zero.silentcamera

import android.content.Context
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import xh.zero.ImageActivity
import xh.zero.R
import xh.zero.core.replaceFragment
import xh.zero.core.utils.SystemUtil
import xh.zero.core.utils.ToastUtil
import xh.zero.databinding.ActivitySilentCaptureBinding

/**
 * 无预览拍照
 */
class SilentCaptureActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySilentCaptureBinding
    private lateinit var fragment: SilentCaptureFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SystemUtil.toFullScreenMode(this)
        binding = ActivitySilentCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraManager.cameraIdList.forEachIndexed { index, cameraId ->
            if (index == 0) {
                // 打开第一个摄像头
                fragment = SilentCaptureFragment.newInstance(cameraId)
                replaceFragment(fragment, R.id.fragment_container)

                binding.btnCapture.setOnClickListener {
                    fragment.takePicture { imgPath ->
                        ToastUtil.show(this, "拍照完成：$imgPath")
                        ImageActivity.start(this, imgPath, requestedOrientation)
                    }
                }
            }
        }
    }
}