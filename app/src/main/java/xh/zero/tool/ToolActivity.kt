package xh.zero.tool

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Base64
import android.util.Size
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import com.bumptech.glide.Glide
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import timber.log.Timber
import xh.zero.ImageActivity
import xh.zero.R
import xh.zero.camera2.Camera2Activity
import xh.zero.core.replaceFragment
import xh.zero.databinding.ActivityToolBinding
import xh.zero.utils.WebSocketClient
import xh.zero.view.BaseCameraActivity
import java.io.ByteArrayOutputStream

class ToolActivity : BaseCameraActivity<ActivityToolBinding>() {

    private lateinit var fragment: ToolFragment
    private val wsClient = WebSocketClient {originTxt ->
        // JSONObject jsonObject = (new JSONObject(response)).getJSONObject("");
        //textView.setText(jsonObject.toString(2));
        val parser = JsonParser()
        val jsonElement = parser.parse(originTxt)
        val gson = GsonBuilder().setPrettyPrinting().create()
        val prettyJsonString = gson.toJson(jsonElement)
        binding.tvWsResult.text = prettyJsonString
    }
    private var cameraWidth: Int? = null
    private var cameraHeight: Int? = null
    private var selectedCameraId: String = "0"
    private var cameraSize: Array<Size>? = null
    private var hostTxt: String? = null
    private var portNumber: Int? = null
    private var selectedPos = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.btnCapture.setOnClickListener {
            showContent(false)
            CoroutineScope(Dispatchers.Default).launch {
                delay(500)
                withContext(Dispatchers.Main) {
                    fragment.takePicture(null, false) { imgPath ->
                        Timber.d("拍照完成")
                        CoroutineScope(Dispatchers.IO).launch {
                            wsClient.send(encodeImage(BitmapFactory.decodeFile(imgPath)))
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
            binding.tvWsResult.text = ""
            binding.ivResult.setImageDrawable(null)
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

        hostTxt = "120.76.175.224"
        portNumber = 9001
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

    }

    private fun showContent(isShow: Boolean) {
        if (!isShow) {
            binding.fragmentContainer.visibility = View.INVISIBLE
            binding.ivResult.visibility = View.INVISIBLE
            binding.btnCapture.visibility = View.INVISIBLE
            binding.sbZoom.visibility = View.INVISIBLE
            binding.tvZoom.visibility = View.INVISIBLE
            binding.edtWsUrl.visibility = View.INVISIBLE
            binding.btnWsConnect.visibility = View.INVISIBLE
            binding.tvWsResult.visibility = View.INVISIBLE
            binding.spResolution.visibility = View.INVISIBLE
            binding.btnClear.visibility = View.INVISIBLE
            binding.edtWsParam.visibility = View.INVISIBLE
        } else {
            binding.fragmentContainer.visibility = View.VISIBLE
            binding.ivResult.visibility = View.VISIBLE
            binding.btnCapture.visibility = View.VISIBLE
            binding.sbZoom.visibility = View.VISIBLE
            binding.tvZoom.visibility = View.VISIBLE
            binding.edtWsUrl.visibility = View.VISIBLE
            binding.btnWsConnect.visibility = View.VISIBLE
            binding.tvWsResult.visibility = View.VISIBLE
            binding.spResolution.visibility = View.VISIBLE
            binding.btnClear.visibility = View.VISIBLE
            binding.edtWsParam.visibility = View.VISIBLE
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
                    if (size.width == 640 && size.height == 480) {
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
}