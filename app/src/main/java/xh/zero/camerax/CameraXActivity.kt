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
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.window.WindowManager
import timber.log.Timber
import xh.zero.ImageActivity
import xh.zero.R
import xh.zero.core.replaceFragment
import xh.zero.core.utils.SystemUtil
import xh.zero.core.utils.ToastUtil
import xh.zero.databinding.ActivityCameraXactivityBinding
import xh.zero.view.BaseCameraActivity

class CameraXActivity : BaseCameraActivity<ActivityCameraXactivityBinding>() {

    companion object {
        // 屏幕缩放比例
        private const val SCREEN_SCALE = 0.5
    }

    private lateinit var fragment: CameraXPreviewFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.btnCapture.setOnClickListener {
            fragment.takePhoto { path ->
                ToastUtil.show(this, "照片已保存到：$path")
                ImageActivity.start(this, path, requestedOrientation)
            }
        }
    }

    override fun getBindingView(): ActivityCameraXactivityBinding = ActivityCameraXactivityBinding.inflate(layoutInflater)

    override fun getCameraFragmentLayout(): ViewGroup = binding.fragmentContainer

    override fun onCameraAreaCreated(
        cameraId: String,
        area: Size,
        screen: Size,
        supportImage: Size
    ) {
        fragment =  CameraXPreviewFragment.newInstance(cameraId)
        replaceFragment(fragment, R.id.fragment_container)
    }

    override fun onResume() {
        super.onResume()
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.fragmentContainer).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}