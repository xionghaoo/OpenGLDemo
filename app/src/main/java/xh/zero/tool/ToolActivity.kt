package xh.zero.tool

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.util.Size
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.children
import com.bumptech.glide.Glide
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import timber.log.Timber
import xh.zero.BuildConfig
import xh.zero.R
import xh.zero.core.replaceFragment
import xh.zero.databinding.ActivityToolBinding
import xh.zero.utils.WebSocketClient
import xh.zero.view.BaseCameraActivity
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URLDecoder


class ToolActivity : BaseCameraActivity<ActivityToolBinding>() {

    companion object {
        private const val REQUEST_SELECT_FILE = 1
    }

    private lateinit var fragment: ToolFragment
    private val wsClient = WebSocketClient {originTxt ->
        // JSONObject jsonObject = (new JSONObject(response)).getJSONObject("");
        //textView.setText(jsonObject.toString(2));

        binding.tvWsResult.text = formatJson(originTxt)
    }
    private var cameraWidth: Int? = null
    private var cameraHeight: Int? = null
    private var selectedCameraId: String = "0"
    private var cameraSize: Array<Size>? = null
    private var hostTxt: String? = null
    private var portNumber: Int? = null
    private var selectedPos = 0

    // 0: websocket, 1: http_dev, 2: http_prod
    private var apiSelection = 0
    private var isHttpProd = false

    private val prefs by lazy {
        SharedPreferenceStorage(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.btnCapture.setOnClickListener {
            showContent(false)
            CoroutineScope(Dispatchers.Default).launch {
                delay(500)
                withContext(Dispatchers.Main) {
                    fragment.takePicture(null, false) { imgPath ->
                        Timber.d("拍照完成")
                        val imgData = encodeImage(BitmapFactory.decodeFile(imgPath))
                        if (apiSelection == 0) {
                            // 提交Websocket数据
                            CoroutineScope(Dispatchers.IO).launch {
                                wsClient.send(imgData)
                            }
                            prefs.wsUrl = binding.edtWsUrl.text.toString()
                        } else {
                            // 提交Http数据
                            val json = JsonObject()
                            json.addProperty("image_base64", imgData)
                            ApiRequest.post(
                                isProd = isHttpProd,
                                url = binding.edtWsUrl.text.toString(),
                                json = json.toString(),
                                success = { r ->
                                    binding.tvWsResult.text = formatJson(r)
                                },
                                failure = { e ->
                                    binding.tvWsResult.text = e
                                }
                            )
                            prefs.httpUrl = binding.edtWsUrl.text.toString()
                        }

                        Glide.with(this@ToolActivity)
                            .load(imgPath)
                            .into(binding.ivResult)

                        CoroutineScope(Dispatchers.Default).launch {
                            delay(500)
                            withContext(Dispatchers.Main) {
                                showContent(true)
                            }
                        }
                    }
                }
            }


        }
        binding.btnClear.setOnClickListener {
            clearScreen()
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

        val wsUrls = prefs.wsUrl!!.split(":")
        hostTxt = wsUrls[0]
        portNumber = wsUrls[1].toInt()
        wsClient.setAddr(hostTxt!!, portNumber!!)
        binding.edtWsUrl.setText("$hostTxt:$portNumber")


        binding.edtWsParam.setText("chinese_ocr")
        wsClient.setModel(binding.edtWsParam.text.toString())
        binding.btnWsConnect.setOnClickListener {
            val t = binding.edtWsUrl.text?.split(":")
            if (t != null && t.size > 1) {
                hostTxt = t[0]
                portNumber = t[1].toInt()
                wsClient.setAddr(hostTxt!!, portNumber!!)
            } else {
                Toast.makeText(this, "地址格式错误", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            wsClient.setAddr(hostTxt!!, portNumber!!)
            wsClient.setModel(binding.edtWsParam.text.toString())
            wsClient.start(
                success = {
                    binding.tvWsResult.text = "连接成功"
                },
                failure = { e ->
                    binding.tvWsResult.text = e
                }
            )
        }

        // 接口类型选择
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, arrayOf("Websocket", "HttpDev", "HttpProd"))
        binding.spApiSelect.adapter = adapter
        binding.spApiSelect.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                apiSelection = position
                when (apiSelection) {
                    0 -> {
                        // websocket
//                        binding.edtWsUrl.visibility = View.VISIBLE
                        binding.edtWsUrl.setText("${hostTxt}:${portNumber}")
                        binding.edtWsParam.visibility = View.VISIBLE
                        binding.btnWsConnect.visibility = View.VISIBLE
                    }
                    1, 2 -> {
                        // http
                        binding.edtWsUrl.setText(prefs.httpUrl)
                        binding.edtWsParam.visibility = View.GONE
                        binding.btnWsConnect.visibility = View.GONE
                        // 是否生产模式
                        isHttpProd = apiSelection == 2
                    }
                }
                clearScreen()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }

        binding.btnSelectBg.setOnClickListener {
            selectFile()
        }

    }

    private fun clearScreen() {
        binding.tvWsResult.text = ""
        binding.ivResult.setImageDrawable(null)
    }

    private fun showContent(isShow: Boolean) {
        if (!isShow) {
            binding.root.children.forEach { child ->
                if (child.id != binding.ivBg.id) {
                    child.visibility = View.INVISIBLE
                }
            }
        } else {
            binding.root.children.forEach { child ->
                child.visibility = View.VISIBLE
            }
        }

        when (apiSelection) {
            1, 2 -> {
                // http
                binding.edtWsParam.visibility = View.GONE
                binding.btnWsConnect.visibility = View.GONE
            }
        }
    }

    private fun formatJson(originTxt: String?): String {
        val parser = JsonParser()
        val jsonElement = parser.parse(originTxt)
        val gson = GsonBuilder().setPrettyPrinting().create()
        return gson.toJson(jsonElement)
    }

    private fun selectFile() {
        val cameraToolDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "CameraTool")
        if (!cameraToolDir.exists()) {
            cameraToolDir.mkdir()
        }
        val selectedUri = Uri.parse(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/CameraTool/"
        )
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.setDataAndType(selectedUri, "*/*")
        try {
            startActivityForResult(intent, REQUEST_SELECT_FILE)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Cannot Open File Chooser", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == REQUEST_SELECT_FILE) {
            val path = intent?.data?.encodedPath
            val tag = "CameraTool%2F"
            val start = path?.indexOf(tag)
            val filename = if (start == null || start == -1) {
                null
            } else {
                URLDecoder.decode(intent.data?.encodedPath?.substring(start + tag.length), "UTF-8")
            }
            val downloadDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "CameraTool")
            val imageFile = File(downloadDir, filename)

            Glide.with(this)
                .load(imageFile)
                .into(binding.ivBg)
        }
    }

    override fun getBindingView(): ActivityToolBinding = ActivityToolBinding.inflate(layoutInflater)

    override fun getCameraFragmentLayout(): ViewGroup? = null

    override fun onCameraAreaCreated(
        cameraId: String,
        area: Size,
        screen: Size,
        supportImage: Size
    ) {
        // 设置分辨率
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraManager.cameraIdList.forEachIndexed { index, cameraId ->
            val characteristic = cameraManager.getCameraCharacteristics(cameraId)
            if (index == 0) {
                val configurationMap = characteristic.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                cameraSize = configurationMap?.getOutputSizes(ImageFormat.JPEG)
                val sizes = cameraSize?.mapIndexed { index, size ->
                    Timber.d("摄像头尺寸： $size")
                    if (size.width == prefs.captureWidth && size.height == prefs.captureHeight) {
                        Timber.d("初始化尺寸：$index")
                        cameraWidth = size.width
                        cameraHeight = size.height
                        selectedPos = index
                    }
                    "${size.width} x ${size.height}"
                } ?: listOf()
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sizes)
                binding.spResolution.adapter = adapter
            }
        }

        binding.spResolution.onItemSelectedListener =  object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (cameraSize!![position].width != cameraWidth || cameraSize!![position].height != cameraHeight) {
                    Timber.d("onItemSelected: ${cameraSize!![position]}")

                    fragment = ToolFragment.newInstance(selectedCameraId)
                    fragment.setSize(cameraSize!![position].width, cameraSize!![position].height)
                    prefs.captureWidth = cameraSize!![position].width
                    prefs.captureHeight = cameraSize!![position].height
                    replaceFragment(fragment, R.id.fragment_container)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                Timber.d("onNothingSelecte")
            }
        }

        binding.spResolution.setSelection(selectedPos)

        selectedCameraId = cameraId
        fragment = ToolFragment.newInstance(cameraId)
        fragment.setSize(cameraWidth, cameraHeight)
        replaceFragment(fragment, R.id.fragment_container)
    }

    private fun encodeImage(bm: Bitmap): String? {
        val baos = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val b = baos.toByteArray()
        return Base64.encodeToString(b, Base64.DEFAULT)
    }

    fun getUriFromFile(context: Context?, file: File?): Uri? {
        if (context == null || file == null) {
            throw NullPointerException()
        }
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context.applicationContext,
                BuildConfig.APPLICATION_ID.toString() + ".fileprovider",
                file
            )
        } else {
            Uri.fromFile(file)
        }
        return uri
    }
}