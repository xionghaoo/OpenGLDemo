package xh.zero.view

import android.content.Context
import android.content.res.Configuration
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.opengl.GLES20
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import timber.log.Timber
import xh.zero.widgets.BaseSurfaceView
import kotlin.math.abs
import kotlin.math.min

/**
 * Camera1相机
 */
abstract class Camera1Fragment<VIEW: ViewBinding> : Fragment(), Camera.PreviewCallback, SurfaceTexture.OnFrameAvailableListener {

    protected lateinit var binding: VIEW
    protected abstract val cameraId: Int
    private val cameraManager: CameraManager by lazy {
        requireActivity().getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }
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
        binding = getBindingView(inflater, container)
        return binding.root
    }

    abstract fun getBindingView(inflater: LayoutInflater, container: ViewGroup?): VIEW

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        surfaceView().setOnSurfaceCreated { surfaceTexture ->
            setSurfaceBufferSize(surfaceTexture)
            openCamera(cameraId, surfaceTexture)
            startPreview()
        }
    }

    /**
     * 设置纹理缓冲区大小，用来接收相机输出的图像帧缓冲。
     * 相机的图像输出会根据设置的目标Surface来生成缓冲区
     * 如果相机输出的缓冲区和我们设置的Surface buffer size尺寸不一致，那么输出到Surface时的图像就会变形
     * 如果我们Surface buffer size的尺寸和SurfaceView的尺寸不一致，那么输出的图像也会变形
     */
    private fun setSurfaceBufferSize(surfaceTexture: SurfaceTexture) {
        val characteristic = cameraManager.getCameraCharacteristics(cameraId.toString())
        val configurationMap = characteristic.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        configurationMap?.getOutputSizes(ImageFormat.JPEG)
            ?.filter { size ->
                // 尺寸要求不大于 GL_MAX_VIEWPORT_DIMS and GL_MAX_TEXTURE_SIZE
                val limit = min(GLES20.GL_MAX_VIEWPORT_DIMS, GLES20.GL_MAX_TEXTURE_SIZE)
                val isFitGLSize = size.width <= limit && size.height <= limit
                val isPortrait = requireContext().resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
                val isFitScreenSize = if (isPortrait) {
                    size.width <= requireContext().resources.displayMetrics.widthPixels
                } else {
                    size.height <= requireContext().resources.displayMetrics.heightPixels
                }
                isFitGLSize && isFitScreenSize
            }
            ?.filter { size ->
                // 寻找4:3的预览尺寸比例
                abs(size.width / 4f - size.height / 3f) < 0.01f || abs(size.height / 4f - size.width / 3f) < 0.01f
            }
            ?.maxByOrNull { size -> size.height * size.width }
            ?.also { maxBufferSize ->
                surfaceTexture.setDefaultBufferSize(maxBufferSize.width, maxBufferSize.height)
                Timber.d("纹理缓冲区尺寸：${maxBufferSize}")
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
            surfaceView().width.toFloat() / surfaceView().height
        } else {
            surfaceView().height.toFloat() / surfaceView().width
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

    abstract fun surfaceView(): BaseSurfaceView

    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {

    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        Log.d(TAG, "onFrameAvailable: $surfaceTexture")
    }

    companion object {
        const val TAG = "Camera1Fragment"
    }
}