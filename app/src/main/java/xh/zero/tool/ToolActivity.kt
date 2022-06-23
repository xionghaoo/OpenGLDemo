package xh.zero.tool

import android.content.res.Configuration
import android.os.Bundle
import android.util.Size
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import xh.zero.ImageActivity
import xh.zero.R
import xh.zero.camera2.Camera2Activity
import xh.zero.core.replaceFragment
import xh.zero.databinding.ActivityToolBinding
import xh.zero.view.BaseCameraActivity

class ToolActivity : BaseCameraActivity<ActivityToolBinding>() {

    private lateinit var fragment: ToolFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.btnCapture.setOnClickListener {
            fragment.takePicture(null, false) { imgPath ->
                ImageActivity.start(this, imgPath, requestedOrientation)
            }
        }

        binding.tvZoom.text = "画面缩放_0%"
        binding.sbZoom.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                fragment.applyZoom(progress.toFloat() / 100)
                binding.tvZoom.text = "画面缩放_${progress}%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })

    }

    override fun getBindingView(): ActivityToolBinding = ActivityToolBinding.inflate(layoutInflater)

    override fun getCameraFragmentLayout(): ViewGroup? = null

    override fun onCameraAreaCreated(
        cameraId: String,
        area: Size,
        screen: Size,
        supportImage: Size
    ) {
        fragment = ToolFragment.newInstance(cameraId)
        replaceFragment(fragment, R.id.fragment_container)
    }
}