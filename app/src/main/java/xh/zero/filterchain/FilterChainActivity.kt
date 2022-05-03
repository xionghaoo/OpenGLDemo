package xh.zero.filterchain

import android.content.Context
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Size
import android.view.ViewGroup
import xh.zero.R
import xh.zero.core.replaceFragment
import xh.zero.core.utils.SystemUtil
import xh.zero.databinding.ActivityFilterChainBinding
import xh.zero.view.BaseCameraActivity

/**
 * 过滤器链
 */
class FilterChainActivity : BaseCameraActivity<ActivityFilterChainBinding>() {

    override fun getBindingView(): ActivityFilterChainBinding = ActivityFilterChainBinding.inflate(layoutInflater)

    override fun getCameraFragmentLayout(): ViewGroup = binding.fragmentContainer

    override fun onCameraAreaCreated(
        cameraId: String,
        area: Size,
        screen: Size,
        supportImage: Size
    ) {
        replaceFragment(FilterChainFragment.newInstance(cameraId), R.id.fragment_container)
    }
}