package xh.zero.camera1

import android.content.Context
import android.content.res.Configuration
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Size
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.window.WindowManager
import timber.log.Timber
import xh.zero.R
import xh.zero.core.replaceFragment
import xh.zero.core.utils.SystemUtil
import xh.zero.databinding.ActivityCamera1Binding
import xh.zero.view.BaseCameraActivity

class Camera1Activity : BaseCameraActivity<ActivityCamera1Binding>() {

    override fun getBindingView(): ActivityCamera1Binding = ActivityCamera1Binding.inflate(layoutInflater)

    override fun getCameraFragmentLayout(): ViewGroup = binding.fragmentContainer

    override fun onCameraAreaCreated(cameraId: String, area: Size, screen: Size, supportImage: Size) {
        replaceFragment(Camera1PreviewFragment.newInstance(cameraId.toInt()), R.id.fragment_container)
    }

}