package xh.zero.camera1

import android.content.Context
import android.content.res.Configuration
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import timber.log.Timber
import xh.zero.core.checkAllMatched
import xh.zero.databinding.FragmentCamera1Binding

/**
 * Camera1 API
 */
class Camera1Fragment : Fragment(), Camera.PreviewCallback, SurfaceTexture.OnFrameAvailableListener {

    private val cameraId: Int by lazy {
        arguments?.getInt(ARG_CAMERA_ID) ?: 0
    }
    private lateinit var binding: FragmentCamera1Binding

    private var camera: Camera? = null
    private var parameters: Camera.Parameters? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        releaseCamera()
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCamera1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.viewfinder.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {

            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {

            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                releaseCamera()
            }
        })

        binding.viewfinder.setOnSurfaceCreated { surfaceTexture ->
            surfaceTexture.setDefaultBufferSize(binding.viewfinder.width, binding.viewfinder.height)
            openCamera(cameraId, surfaceTexture)
            startPreview()
        }
    }

    private fun openCamera(cameraId: Int, surfaceTexture: SurfaceTexture?) {
        if (isSupport(cameraId)) {
            Timber.d( "openCamera: ${cameraId}")
            try {
                camera = Camera.open(cameraId)
                camera?.setPreviewTexture(surfaceTexture)
                camera?.setDisplayOrientation(90)
                initialParameters(camera)
                camera?.setPreviewCallback(this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun releaseCamera() {
        camera?.stopPreview()
        camera?.setPreviewCallback(null)
        camera?.release()
        camera = null
    }

    private fun initialParameters(camera: Camera?) {
        parameters = camera?.parameters
        parameters?.previewFormat = ImageFormat.NV21

        // 竖直和水平方向的宽高比是相反的，这里要分开计算
        val orientation = requireContext().resources.configuration.orientation
        val ratio = if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            binding.viewfinder.width.toFloat() / binding.viewfinder.height
        } else {
            binding.viewfinder.height.toFloat() / binding.viewfinder.width
        }
        Timber.d("画面比例：$ratio")
        parameters?.supportedPreviewSizes
            // 适合找到SurfaceView大小比例的预览尺寸
            ?.filter { it.height.toFloat() / it.width == ratio }
            ?.maxByOrNull { it.width * it .height }
            ?.also { maxSize ->
                // 设置预览尺寸，这里要求是符合SurfaceView比例的最大预览尺寸
                parameters?.setPreviewSize(maxSize.width, maxSize.height)
                Timber.d("最大的预览尺寸：${maxSize.width} x ${maxSize.height}")
            }
        parameters?.supportedPictureSizes
            ?.maxByOrNull { it.width * it.height }
            ?.also { maxSize ->
                // 设置输出的图像为最大尺寸
                parameters?.setPictureSize(maxSize.width, maxSize.height)
            }
        camera?.parameters = parameters
    }

    private fun isSupport(backOrFront: Int): Boolean {
        val cameraInfo = CameraInfo()
        for (i in 0 until Camera.getNumberOfCameras()) {
            Camera.getCameraInfo(i, cameraInfo)
            if (cameraInfo.facing == backOrFront) {
                return true
            }
        }
        return false
    }

    private fun startPreview() {
        try {
            camera?.startPreview()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {

    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        Log.d(TAG, "onFrameAvailable: $surfaceTexture")
    }

    companion object {
        const val TAG = "Camera1Fragment"
        private const val ARG_CAMERA_ID = "ARG_CAMERA_ID"

        private const val IMAGE_BUFFER_SIZE: Int = 3

        fun newInstance(cameraId: Int) =
            Camera1Fragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_CAMERA_ID, cameraId)
                }
            }
    }
}