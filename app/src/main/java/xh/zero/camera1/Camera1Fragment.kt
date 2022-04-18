package xh.zero.camera1

import android.content.Context
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
        parameters?.supportedPreviewFormats
        parameters?.supportedPictureFormats
        parameters?.setPreviewSize(binding.viewfinder.width, binding.viewfinder.height)
        // 设置照片尺寸
        val cameraManager = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val characteristic = cameraManager.getCameraCharacteristics(cameraId.toString())
        characteristic.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?.getOutputSizes(ImageFormat.JPEG)
            ?.maxByOrNull { it.width * it.height }
            ?.also { size ->
                parameters?.setPictureSize(size.width, size.height)
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