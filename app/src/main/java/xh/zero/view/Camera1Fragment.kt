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
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import timber.log.Timber
import xh.zero.widgets.BaseSurfaceView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlin.math.min

/**
 * Camera1相机
 */
abstract class Camera1Fragment<VIEW: ViewBinding> : BaseCameraFragment<VIEW>(), Camera.PreviewCallback, SurfaceTexture.OnFrameAvailableListener {


    private var camera: Camera? = null
    private var parameters: Camera.Parameters? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        releaseCamera()
        super.onDestroy()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        getSurfaceView().setOnSurfaceCreated { surfaceTexture ->
            setSurfaceBufferSize(surfaceTexture = surfaceTexture)
            openCamera(cameraId.toInt(), surfaceTexture)
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
            getSurfaceView().width.toFloat() / getSurfaceView().height
        } else {
            getSurfaceView().height.toFloat() / getSurfaceView().width
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
//        val cameraInfo = CameraInfo()
//        for (i in 0 until Camera.getNumberOfCameras()) {
//            Camera.getCameraInfo(i, cameraInfo)
//            if (cameraInfo.facing == backOrFront) {
//                return true
//            }
//        }
        return true
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

    fun takePhoto(result: (path: String) -> Unit) {
        camera?.takePicture(null, null) { data, camera ->
            try {
                val output = createFile(requireContext(), "jpg")
                FileOutputStream(output).use { it.write(data) }
                result(output.absolutePath)
            } catch (exc: IOException) {
                Log.e(TAG, "Unable to write JPEG image to file", exc)
            }
        }
    }

    private fun createFile(context: Context, extension: String): File {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US)
        val rootDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val pictureDir = File(rootDir, "roboland")
        if (!pictureDir.exists()) pictureDir.mkdir()
//            return File(Environment.getExternalStorageDirectory(), "IMG_${sdf.format(Date())}.$extension")
        return File(pictureDir, "IMG_${sdf.format(Date())}.$extension")
    }

    companion object {
        const val TAG = "Camera1Fragment"
    }
}