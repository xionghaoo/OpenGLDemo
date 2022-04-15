package xh.zero.camera1

import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import xh.zero.databinding.FragmentCamera1Binding

/**
 * Camera1 API
 */
class Camera1Fragment : Fragment(), Camera.PreviewCallback, SurfaceTexture.OnFrameAvailableListener {

    private var cameraId: String? = null
    private lateinit var binding: FragmentCamera1Binding

    private var camera: Camera? = null
    private var parameters: Camera.Parameters? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            cameraId = it.getString(ARG_CAMERA_ID)
        }
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
        binding.surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
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

        binding.surfaceView.setOnTextureCreated { surfaceTexture ->
            openCamera(cameraId!!.toInt(), surfaceTexture)

            startPreview()
        }
    }

    private fun openCamera(cameraId: Int, surfaceTexture: SurfaceTexture?) {
        if (isSupport(cameraId)) {
            Log.d(TAG, "openCamera: ${cameraId}")
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
        parameters?.setPreviewSize(1280, 720)
        parameters?.setPictureSize(1280, 720)
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

        fun newInstance(cameraId: String) =
            Camera1Fragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CAMERA_ID, cameraId)
                }
            }
    }
}