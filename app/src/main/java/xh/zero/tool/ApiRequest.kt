package xh.zero.tool

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber


class ApiRequest {
    companion object {
        // https://rvi.ubtrobot.com/aipocket_dev/music_cards/auto_recognition
        private const val HOST_DEV = "https://rvi.ubtrobot.com/aipocket_dev/"
        private const val HOST_PROD = "https://rvi.ubtrobot.com/aipocket/"

        private val client = OkHttpClient()

        fun post(isProd: Boolean, url: String, json: String, success: (res: String) -> Unit, failure: (e: String) -> Unit) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val host = if (isProd) HOST_PROD else HOST_DEV
                    Timber.d("url: ${host + url}")
                    val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
                    val request: Request = Request.Builder()
                        .url(host + url)
                        .post(body)
                        .build()
                    val response: Response = client.newCall(request).execute()
                    if (response.code == 200) {
                        withContext(Dispatchers.Main) {
                            response.body?.string()?.let { success(it) }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            failure("上传失败: ${response.code}")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        failure("上传失败: ${e.localizedMessage}")
                    }
                }
            }
        }
    }
}