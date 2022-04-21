package xh.zero.filterchain

import android.content.Context
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import xh.zero.R
import xh.zero.core.replaceFragment
import xh.zero.core.utils.SystemUtil

/**
 * 过滤器链
 */
class FilterChainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SystemUtil.toFullScreenMode(this)
        setContentView(R.layout.activity_filter_chain)
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraManager.cameraIdList.forEachIndexed { index, cameraId ->
            if (index == 0) {
                replaceFragment(FilterChainFragment.newInstance(cameraId), R.id.fragment_container)
            }
        }

    }
}