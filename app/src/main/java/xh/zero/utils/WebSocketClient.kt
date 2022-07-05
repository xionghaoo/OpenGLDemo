package xh.zero.utils

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import timber.log.Timber
import xh.zero.tool.TextOcrResult
import java.lang.Exception

class WebSocketClient(
    private val onReceived: (originTxt: String?) -> Unit
) {
    private val client = HttpClient(CIO) {
        install(WebSockets)
    }

    private var host: String? = null
    private var port: Int? = null
    private var model: String? = null

    private var session: ClientWebSocketSession? = null

    fun setAddr(host: String, port: Int) {
        this.host = host
        this.port = port
    }

    fun setModel(model: String) {
        this.model = model
    }

    fun start(success: () -> Unit, failure: (e: String?) -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            kotlin.runCatching {
                Timber.d("启动websocket服务")
                client.webSocket(host = host, port = port) {
                    session = this
                    Timber.d("连接成功")
                    withContext(Dispatchers.Main) {
                        success()
                    }
                    for(frame in incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                val receivedText = frame.readText()
//                                Timber.d("receivedText: $receivedText")
//                                val r = try {
//                                    Gson().fromJson<TextOcrResult>(receivedText, TextOcrResult::class.java)
//                                } catch (e: Exception) {
//                                    Timber.e(e)
//                                    null
//                                }
                                withContext(Dispatchers.Main) {
                                    onReceived(receivedText)
                                }
                            }
                            is Frame.Binary -> {
                                Timber.d("接收到二进制消息")
                            }
                            is Frame.Close -> {
                                Timber.d("接收到Frame.Close消息")
                            }
                            else -> {

                            }
                        }
                    }

                }
            }.onFailure { e ->
                Timber.d("启动websocket服务失败")
                Timber.e(e)
                withContext(Dispatchers.Main) {
                    failure(e.localizedMessage)
                }
            }
        }
    }

    suspend fun send(image: String?) {
        if (image == null) return
        Timber.d("发送图片: $host, $port, $model")
        val resTxt = JsonObject()
        resTxt.addProperty("model", model)
        resTxt.addProperty("image", image)
        session?.send(resTxt.toString())
    }

    fun stop() {
        client.close()
    }
}