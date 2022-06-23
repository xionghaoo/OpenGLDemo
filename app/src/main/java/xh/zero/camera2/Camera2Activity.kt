package xh.zero.camera2

import android.content.Context
import android.content.res.Configuration
import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Size
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.window.WindowManager
import timber.log.Timber
import xh.zero.ImageActivity
import xh.zero.R
import xh.zero.core.replaceFragment
import xh.zero.core.utils.SystemUtil
import xh.zero.databinding.ActivityCamera2Binding
import xh.zero.view.BaseCameraActivity
import xh.zero.view.BaseCameraFragment

class Camera2Activity : BaseCameraActivity<ActivityCamera2Binding>() {

    companion object {
        private const val INITIAL_RECT_RATIO = 65
    }

    enum class RectPos {
        CENTER, BOTTOM
    }

    private lateinit var fragment: Camera2PreviewFragment

    private var screenSize: Size? = null
    private var viewW: Int = 0
    private var viewH: Int = 0
    private var imageW: Int = 0
    private var imageH: Int = 0

    private val imgRect = Rect()
    private val viewRect = Rect()

    private var rectPos: RectPos = RectPos.CENTER

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.btnCapture.setOnClickListener {
            fragment.takePicture(imgRect, true) { imgPath ->
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


        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            binding.sbRectPercent.visibility = View.GONE
            binding.tvRectPercent.visibility = View.GONE
            binding.btnResetRect.visibility = View.GONE
        } else {
            binding.sbRectPercent.progress = INITIAL_RECT_RATIO
            binding.tvRectPercent.text = "${INITIAL_RECT_RATIO}%"
            binding.sbRectPercent.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    initialIndicatorRect(progress, rectPos)
                    binding.tvRectPercent.text = "${progress}%"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }
            })

            binding.btnResetRect.setOnClickListener {
                binding.sbRectPercent.progress = INITIAL_RECT_RATIO
            }
        }

    }

    override fun getBindingView(): ActivityCamera2Binding = ActivityCamera2Binding.inflate(layoutInflater)

    override fun getCameraFragmentLayout(): ViewGroup = binding.fragmentContainer

    override fun onCameraAreaCreated(cameraId: String, area: Size, screen: Size, supportImage: Size) {
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewW = area.width
            viewH = area.height
            imageW = supportImage.width
            imageH = supportImage.height
            screenSize = Size(screen.width, screen.height)
            // 只在水平方向加
            initialIndicatorRect(binding.sbRectPercent.progress, rectPos)
        }

        fragment = Camera2PreviewFragment.newInstance(cameraId)
        replaceFragment(fragment, R.id.fragment_container)
    }

    /**
     * 给预览和成像加上指示器矩形
     */
    private fun initialIndicatorRect(percent: Int, position: RectPos) {
        binding.vIndicatorRect.visibility = View.VISIBLE

        val r = screenSize!!.width.toFloat() / screenSize!!.height
        val ratio = percent * 0.01f
        val indicatorW = (ratio * viewW).toInt()
        val indicatorH = (indicatorW / r).toInt()

        viewRect.left = (screenSize!!.width - indicatorW) / 2
        viewRect.right = viewRect.left + indicatorW
        viewRect.top = when(position) {
            RectPos.CENTER -> (screenSize!!.height - indicatorH) / 2
            RectPos.BOTTOM -> (screenSize!!.height / 2 - indicatorH) / 2 + screenSize!!.height / 2
        }
        viewRect.bottom = viewRect.top + indicatorH
        binding.vIndicatorRect.drawRect(viewRect)

        val imageDrawRectW = (ratio * imageW).toInt()
        val imageDrawRectH = (imageDrawRectW / r).toInt()
        imgRect.left = ((imageW - imageDrawRectW) / 2f).toInt()
        imgRect.top = when(position) {
            RectPos.CENTER -> ((imageH - imageDrawRectH) / 2f).toInt()
            RectPos.BOTTOM -> ((imageH / 2 - imageDrawRectH) / 2f).toInt() + imageH / 2
        }
        imgRect.bottom = imgRect.top + imageDrawRectH
        imgRect.right = imgRect.left + imageDrawRectW
    }
}