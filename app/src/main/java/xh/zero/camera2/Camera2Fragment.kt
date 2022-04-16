package xh.zero.camera2

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Camera
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import xh.zero.R
import xh.zero.databinding.FragmentCamera2Binding
import java.lang.RuntimeException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class Camera2Fragment : Fragment() {

    private lateinit var binding: FragmentCamera2Binding
    private val cameraId: String by lazy {
        arguments?.getString("id") ?: "0"
    }

    private lateinit var camera: CameraDevice
    private lateinit var session: CameraCaptureSession
    private val cameraManager: CameraManager by lazy {
        requireActivity().getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }
    private val cameraThread = HandlerThread("CameraThread").apply { start() }
    private val cameraHandler = Handler(cameraThread.looper)
    private lateinit var imageReader: ImageReader
    private val characteristics: CameraCharacteristics by lazy {
        cameraManager.getCameraCharacteristics(cameraId)
    }

    private lateinit var surfaceTexture: SurfaceTexture

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onStop() {
        super.onStop()
        stopCamera()
    }

    override fun onDestroy() {
        cameraThread.quitSafely()
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCamera2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 监听Surface的周期
//        binding.viewfinder.holder.addCallback(object : SurfaceHolder.Callback {
//            override fun surfaceCreated(holder: SurfaceHolder) {
////                binding.viewfinder.setAspectRatio(binding.viewfinder.width, binding.viewfinder.width / 2)
//                initializeCamera()
//            }
//
//            override fun surfaceChanged(
//                holder: SurfaceHolder,
//                format: Int,
//                width: Int,
//                height: Int
//            ) {
//
//            }
//
//            override fun surfaceDestroyed(holder: SurfaceHolder) {
//                stopCamera()
//            }
//        })

        binding.viewfinder.setOnTextureCreated {
            surfaceTexture = it
            surfaceTexture.setDefaultBufferSize(binding.viewfinder.width, binding.viewfinder.height)
            initializeCamera()
        }
    }

    private fun initializeCamera() = lifecycleScope.launch(Dispatchers.Main) {
        camera = openCamera(cameraManager, cameraId, cameraHandler)
        val size = characteristics.get(
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            .getOutputSizes(ImageFormat.JPEG).maxByOrNull { it.height * it.width }!!
        imageReader = ImageReader.newInstance(size.width, size.height, ImageFormat.JPEG, 3)

        binding.viewfinder.holder.setFixedSize(binding.viewfinder.width, binding.viewfinder.height)

        val surface = Surface(surfaceTexture)
        val targets = listOf<Surface>(surface, imageReader.surface)
        val session = createCaptureSession(camera, targets, cameraHandler)
        val captureRequest = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            addTarget(surface)
        }
        session.setRepeatingRequest(captureRequest.build(), null, cameraHandler)
    }

    @SuppressLint("MissingPermission")
    private suspend fun openCamera(
        manager: CameraManager,
        cameraId: String,
        handler: Handler? = null
    ) : CameraDevice = suspendCancellableCoroutine { cont ->
        manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) = cont.resume(camera)

            override fun onDisconnected(camera: CameraDevice) {
                requireActivity().finish()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                val msg = when(error) {
                    ERROR_CAMERA_DEVICE -> "Fatal (device)"
                    ERROR_CAMERA_DISABLED -> "Device Policy"
                    ERROR_CAMERA_IN_USE -> "Camera in use"
                    ERROR_CAMERA_SERVICE -> "Fatal (device)"
                    ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                    else -> "Unknown"
                }
                val exc = RuntimeException("Camera $cameraId error: ($error) $msg")
                Timber.e(exc)
                if (cont.isActive) cont.resumeWithException(exc)
            }
        }, handler)
    }

    private suspend fun createCaptureSession(
        device: CameraDevice,
        targets: List<Surface>,
        handler: Handler? = null
    ): CameraCaptureSession = suspendCoroutine { cont ->
        device.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) = cont.resume(session)

            override fun onConfigureFailed(session: CameraCaptureSession) {
                val exc = RuntimeException("Camera ${device.id} session config failure")
                Timber.e(exc)
                cont.resumeWithException(exc)
            }
        }, handler)
    }

    private fun stopCamera() {
        try {
            camera.close()
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    companion object {
        fun newInstance(id: String) = Camera2Fragment().apply {
            arguments = Bundle().apply {
                putString("id", id)
            }
        }
    }
}