package xh.zero.camerax

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
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
import androidx.core.view.children
import androidx.window.WindowManager
import kotlinx.coroutines.*
import timber.log.Timber
import xh.zero.ImageActivity
import xh.zero.R
import xh.zero.core.replaceFragment
import xh.zero.core.utils.SystemUtil
import xh.zero.core.utils.ToastUtil
import xh.zero.databinding.ActivityCameraXactivityBinding
import xh.zero.view.BaseCameraActivity
import xh.zero.view.BaseCameraFragment
import java.io.File
import java.io.FileOutputStream

class CameraXActivity : BaseCameraActivity<ActivityCameraXactivityBinding>(),
    CameraXPreviewFragment.OnFragmentActionListener
{

    companion object {
        // 屏幕缩放比例
        private const val SCREEN_SCALE = 0.5
    }

    private lateinit var fragment: CameraXPreviewFragment
    private var isCapture = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.btnCapture.setOnClickListener {
//            fragment.takePhoto { path ->
//                ToastUtil.show(this, "照片已保存到：$path")
//                ImageActivity.start(this, path, requestedOrientation)
//            }
            isCapture = true
//            showContent(false)
//            CoroutineScope(Dispatchers.Default).launch {
//                delay(500)
//                withContext(Dispatchers.Main) {
//
//                }
//            }
        }
    }

    override fun getBindingView(): ActivityCameraXactivityBinding = ActivityCameraXactivityBinding.inflate(layoutInflater)

    override fun getCameraFragmentLayout(): ViewGroup? = null

    override fun onCameraAreaCreated(
        cameraId: String,
        area: Size,
        screen: Size,
        supportImage: Size
    ) {
        fragment =  CameraXPreviewFragment.newInstance(cameraId)
        replaceFragment(fragment, R.id.fragment_container)
    }

    override fun onAnalysisImage(bitmap: Bitmap) {
        if (isCapture) {
            storeBitmap(bitmap)
        }
    }

    private fun storeBitmap(bitmap: Bitmap){
        val file = BaseCameraFragment.createFile(this, "jpg")
        val out = FileOutputStream(file)
        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
            out.close()
            isCapture = false

            ImageActivity.start(this, file.absolutePath, requestedOrientation)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showContent(isShow: Boolean) {
        if (isShow) {
            binding.root.children.forEach { v ->
                v.visibility = View.VISIBLE
            }
        } else {
            binding.root.children.forEach { v ->
                v.visibility = View.INVISIBLE
            }
        }
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