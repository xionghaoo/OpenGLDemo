package xh.zero.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.opengl.GLES20
import android.opengl.GLES20.GL_MAX_TEXTURE_SIZE
import android.opengl.GLES20.GL_MAX_VIEWPORT_DIMS
import android.os.*
import android.util.Log
import android.view.*
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import xh.zero.R
import xh.zero.utils.OrientationLiveData
import xh.zero.widgets.BaseSurfaceView
import xh.zero.widgets.IndicatorRectView
import java.io.*
import java.lang.RuntimeException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeoutException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs
import kotlin.math.min

/**
 * Camera2相机
 */
abstract class Camera2Fragment<VIEW: ViewBinding> : BaseCameraFragment<VIEW>() {

    private lateinit var camera: CameraDevice
    private lateinit var session: CameraCaptureSession

    private val cameraThread = HandlerThread("CameraThread").apply { start() }
    private val cameraHandler = Handler(cameraThread.looper)
    private lateinit var imageReader: ImageReader
    private val characteristics: CameraCharacteristics by lazy {
        cameraManager.getCameraCharacteristics(cameraId)
    }

    private lateinit var surfaceTexture: SurfaceTexture

    private val imageReaderThread = HandlerThread("imageReaderThread").apply { start() }
    private val imageReaderHandler = Handler(imageReaderThread.looper)
    private lateinit var relativeOrientation: OrientationLiveData
    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private var cropRect: Rect? = null

    protected var cameraWidth: Int? = null
    protected var cameraHeight: Int? = null

    override fun onDestroy() {
        stopCamera()
        cameraThread.quitSafely()
        imageReaderThread.quitSafely()
        super.onDestroy()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getSurfaceView().setOnSurfaceCreated { sf ->
            surfaceTexture = sf
            setSurfaceBufferSize(surfaceTexture)
            initializeCamera()
        }

        // Used to rotate the output media to match device orientation
        relativeOrientation = OrientationLiveData(requireContext(), characteristics).apply {
            observe(viewLifecycleOwner, Observer { orientation ->
                Log.d(TAG, "Orientation changed: $orientation")
            })
        }
    }

    private fun initializeCamera() = lifecycleScope.launch(Dispatchers.Main) {
        camera = openCamera(cameraManager, cameraId, cameraHandler)
        val size = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            .getOutputSizes(ImageFormat.JPEG)
            .maxByOrNull { it.height * it.width }!!
        if (cameraWidth == null || cameraHeight == null) {
            cameraWidth = size.width
            cameraHeight = size.height
        }
        Timber.d("initializeCamera: ${cameraWidth} x ${cameraHeight}")
        imageReader = ImageReader.newInstance(cameraWidth!!, cameraHeight!!, ImageFormat.JPEG, IMAGE_BUFFER_SIZE)

        getSurfaceView().holder.setFixedSize(getSurfaceView().width, getSurfaceView().height)

        val surface = Surface(surfaceTexture)
        val targets = listOf<Surface>(surface, imageReader.surface)
        session = createCaptureSession(camera, targets, cameraHandler)
        captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            addTarget(surface)
        }
        session.setRepeatingRequest(captureRequestBuilder!!.build(), object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult
            ) {
                // 一次请求的捕获完成
            }
        }, cameraHandler)
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

    fun applyZoom(zoom: Float) {
        var zoomValue = zoom
        val cameraManager = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val characteristic = cameraManager.getCameraCharacteristics(cameraId)
        val maxZoom = characteristic.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f
        if (zoom > maxZoom) zoomValue = maxZoom
        Timber.d("max zoom: ${maxZoom}")
        val calZoom = zoomValue * (maxZoom - 1.0f) + 1.0f
        cropRect = getZoomRect(calZoom, maxZoom)
        captureRequestBuilder?.set(CaptureRequest.SCALER_CROP_REGION, cropRect)
        session.setRepeatingRequest(captureRequestBuilder!!.build(), null, cameraHandler)
    }

    /**
     * 获取缩放矩形
     */
    private fun getZoomRect(zoomLevel: Float, maxDigitalZoom: Float): Rect {
        Timber.d("zoomlevel: $zoomLevel, maxDigitalZoom: $maxDigitalZoom")
        val characteristic = cameraManager.getCameraCharacteristics(cameraId)
        var activeRect = characteristic.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE) ?: Rect()
        val minW = (activeRect.width() / maxDigitalZoom).toInt()
        val minH = (activeRect.height() / maxDigitalZoom).toInt()
        val difW = activeRect.width() - minW
        val difH = activeRect.height() - minH

        // When zoom is 1, we want to return new Rect(0, 0, width, height).
        // When zoom is maxZoom, we want to return a centered rect with minW and minH
        val cropW = (difW * (zoomLevel - 1) / (maxDigitalZoom - 1) / 2f).toInt()
        val cropH = (difH * (zoomLevel - 1) / (maxDigitalZoom - 1) / 2f).toInt()
        return Rect(
            cropW, cropH, activeRect.width() - cropW,
            activeRect.height() - cropH
        )
    }

    /**
     * 拍照
     */
    fun takePicture(rect: Rect?, drawRect: Boolean = false, complete: (String) -> Unit) {
        // Disable click listener to prevent multiple requests simultaneously in flight
//        it.isEnabled = false

        // Perform I/O heavy operations in a different scope
        lifecycleScope.launch(Dispatchers.IO) {
            takePhoto().use { result ->
                Timber.d("Result received: $result")

                // Save the result to disk
                val output = saveResult(result)
                Timber.d("Image saved: ${output.absolutePath}")

                // If the result is a JPEG file, update EXIF metadata with orientation info
                if (output.extension == "jpg") {
                    val exif = ExifInterface(output.absolutePath)
                    exif.setAttribute(
                        ExifInterface.TAG_ORIENTATION, result.orientation.toString())
                    exif.saveAttributes()
                    Timber.d("EXIF metadata saved: ${output.absolutePath}")
                }

                if (drawRect) {
                    Timber.d("给图片加上指示器矩形: $rect")
                    // 给图片加上指示器矩形
                    val bitmap = BitmapFactory.decodeFile(output.absolutePath)
                        .copy(Bitmap.Config.ARGB_8888, true)
                    val canvas = Canvas(bitmap)
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        strokeWidth = 2f
                        style = Paint.Style.STROKE
                        color = Color.argb(255, 255, 0, 0)
                    }
                    val rectTextPaint = IndicatorRectView.RectTextPaint(requireContext(), resources.getDimension(R.dimen.image_rect_text_size))
                    rectTextPaint.setRect(rect!!)
                    canvas.drawRect(rect, paint)
                    rectTextPaint.draw(rect, canvas)
                    canvas.setBitmap(bitmap)
                    val bos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos)
                    FileOutputStream(output).use { it.write(bos.toByteArray()) }
                }

                withContext(Dispatchers.Main) {
                    complete(output.absolutePath)
                }

//                    // Display the photo taken to user
//                    lifecycleScope.launch(Dispatchers.Main) {
//                        navController.navigate(CameraFragmentDirections
//                            .actionCameraToJpegViewer(output.absolutePath)
//                            .setOrientation(result.orientation)
//                            .setDepth(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
//                                    result.format == ImageFormat.DEPTH_JPEG))
//                    }
            }

            // Re-enable click listener after photo is taken
//            it.post { it.isEnabled = true }
        }
    }

    /** Helper function used to save a [CombinedCaptureResult] into a [File] */
    private suspend fun saveResult(result: CombinedCaptureResult): File = suspendCoroutine { cont ->
        when (result.format) {

            // When the format is JPEG or DEPTH JPEG we can simply save the bytes as-is
            ImageFormat.JPEG, ImageFormat.DEPTH_JPEG -> {
                val buffer = result.image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining()).apply { buffer.get(this) }
                Timber.d("保存成JPEG")
                try {
                    val output = createFile(requireContext(), "jpg")
                    FileOutputStream(output).use { it.write(bytes) }
                    cont.resume(output)
                } catch (exc: IOException) {
                    Log.e(TAG, "Unable to write JPEG image to file", exc)
                    cont.resumeWithException(exc)
                }
            }

            // When the format is RAW we use the DngCreator utility library
            ImageFormat.RAW_SENSOR -> {
                val dngCreator = DngCreator(characteristics, result.metadata)
                try {
                    val output = createFile(requireContext(), "dng")
                    FileOutputStream(output).use { dngCreator.writeImage(it, result.image) }
                    cont.resume(output)
                } catch (exc: IOException) {
                    Log.e(TAG, "Unable to write DNG image to file", exc)
                    cont.resumeWithException(exc)
                }
            }

            // No other formats are supported by this sample
            else -> {
                val exc = RuntimeException("Unknown image format: ${result.image.format}")
                Log.e(TAG, exc.message, exc)
                cont.resumeWithException(exc)
            }
        }
    }

    private suspend fun takePhoto(): CombinedCaptureResult = suspendCoroutine { cont ->
        @Suppress("ControlFlowWithEmptyBody")
        while (imageReader.acquireNextImage() != null) {
        }

        // Start a new image queue
        val imageQueue = ArrayBlockingQueue<Image>(IMAGE_BUFFER_SIZE)
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireNextImage()
            Log.d(TAG, "Image available in queue: ${image.timestamp}")
            imageQueue.add(image)
        }, imageReaderHandler)

        val captureRequest = session.device.createCaptureRequest(
            CameraDevice.TEMPLATE_STILL_CAPTURE).apply { addTarget(imageReader.surface) }
        if (cropRect != null) captureRequest.set(CaptureRequest.SCALER_CROP_REGION, cropRect)
        session.capture(captureRequest.build(), object : CameraCaptureSession.CaptureCallback() {

            override fun onCaptureStarted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                timestamp: Long,
                frameNumber: Long) {
                super.onCaptureStarted(session, request, timestamp, frameNumber)
//                fragmentCameraBinding.viewFinder.post(animationTask)
            }

            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult
            ) {
                super.onCaptureCompleted(session, request, result)
                val resultTimestamp = result.get(CaptureResult.SENSOR_TIMESTAMP)
                Log.d(TAG, "Capture result received: $resultTimestamp")

                // Set a timeout in case image captured is dropped from the pipeline
                val exc = TimeoutException("Image dequeuing took too long")
                val timeoutRunnable = Runnable { cont.resumeWithException(exc) }
                imageReaderHandler.postDelayed(timeoutRunnable, 5000)

                // Loop in the coroutine's context until an image with matching timestamp comes
                // We need to launch the coroutine context again because the callback is done in
                //  the handler provided to the `capture` method, not in our coroutine context
                @Suppress("BlockingMethodInNonBlockingContext")
                lifecycleScope.launch(cont.context) {
                    while (true) {

                        // Dequeue images while timestamps don't match
                        val image = imageQueue.take()
                        // TODO(owahltinez): b/142011420
                        // if (image.timestamp != resultTimestamp) continue
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                            image.format != ImageFormat.DEPTH_JPEG &&
                            image.timestamp != resultTimestamp) continue
                        Log.d(TAG, "Matching image dequeued: ${image.timestamp}")

                        // Unset the image reader listener
                        imageReaderHandler.removeCallbacks(timeoutRunnable)
                        imageReader.setOnImageAvailableListener(null, null)

                        Timber.d("imageQueue: ${imageQueue.size}")

                        // Clear the queue of images, if there are left
                        while (imageQueue.size > 0) {
                            imageQueue.take().close()
                        }

                        // Compute EXIF orientation metadata
                        val rotation = relativeOrientation.value ?: 0
                        val mirrored = characteristics.get(CameraCharacteristics.LENS_FACING) ==
                                CameraCharacteristics.LENS_FACING_FRONT
                        Timber.d("mirrored: $mirrored")
                        val exifOrientation = computeExifOrientation(rotation, mirrored)
                        Timber.d("imageQueue after: ${imageQueue.size}")
                        // Build the result and resume progress
                        cont.resume(
                            CombinedCaptureResult(
                                image, result, exifOrientation, imageReader.imageFormat)
                        )

                        // There is no need to break out of the loop, this coroutine will suspend
                    }
                }
            }
        }, cameraHandler)
    }

    /** Transforms rotation and mirroring information into one of the [ExifInterface] constants */
    fun computeExifOrientation(rotationDegrees: Int, mirrored: Boolean) = when {
        rotationDegrees == 0 && !mirrored -> android.media.ExifInterface.ORIENTATION_NORMAL
        rotationDegrees == 0 && mirrored -> android.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL
        rotationDegrees == 180 && !mirrored -> android.media.ExifInterface.ORIENTATION_ROTATE_180
        rotationDegrees == 180 && mirrored -> android.media.ExifInterface.ORIENTATION_FLIP_VERTICAL
        rotationDegrees == 270 && mirrored -> android.media.ExifInterface.ORIENTATION_TRANSVERSE
        rotationDegrees == 90 && !mirrored -> android.media.ExifInterface.ORIENTATION_ROTATE_90
        rotationDegrees == 90 && mirrored -> android.media.ExifInterface.ORIENTATION_TRANSPOSE
        rotationDegrees == 270 && mirrored -> android.media.ExifInterface.ORIENTATION_ROTATE_270
        rotationDegrees == 270 && !mirrored -> android.media.ExifInterface.ORIENTATION_TRANSVERSE
        else -> android.media.ExifInterface.ORIENTATION_UNDEFINED
    }

    companion object {

        private const val TAG = "CameraFragment"
        private const val ARG_CAMERA_ID = "ARG_CAMERA_ID"

        // 摄像头输出的图片缓冲队列中一个缓冲区有3张图片
        private const val IMAGE_BUFFER_SIZE: Int = 3

        /** Helper data class used to hold capture metadata with their associated image */
        data class CombinedCaptureResult(
            val image: Image,
            val metadata: CaptureResult,
            val orientation: Int,
            val format: Int
        ) : Closeable {
            override fun close() = image.close()
        }

        /**
         * Create a [File] named a using formatted timestamp with the current date and time.
         *
         * @return [File] created.
         */
        private fun createFile(context: Context, extension: String): File {
            val sdf = SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.US)
            val rootDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val pictureDir = File(rootDir, "roboland")
            if (!pictureDir.exists()) pictureDir.mkdir()
//            return File(Environment.getExternalStorageDirectory(), "IMG_${sdf.format(Date())}.$extension")
            return File(pictureDir, "IMG_${sdf.format(Date())}.$extension")
        }
    }
}