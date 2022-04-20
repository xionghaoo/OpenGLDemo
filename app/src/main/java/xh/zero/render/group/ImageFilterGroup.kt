package xh.zero.render.group

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import timber.log.Timber
import xh.zero.utils.OpenGLUtil
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ImageFilterGroup(private val context: Context): GLSurfaceView.Renderer {

    private val filters = ArrayList<GpuImageFilter>()
//    private var fboTextureId: Int = 0
//    private var fboId: Int = 0
    private lateinit var frameBuffer: FrameBuffer

    fun addFilter(filter: GpuImageFilter) {
        filters.add(filter)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
//        val pair = OpenGLUtil.createFBO(0, 0)
//        fboId = pair.first
//        fboTextureId = pair.second
        val display = context.resources.displayMetrics
        frameBuffer = FrameBuffer(display.widthPixels, display.heightPixels)

        filters.forEach { filter ->
            filter.onSurfaceCreated()
        }

    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Timber.d("onSurfaceChanged: $width, $height")
        // 视口变换，属于OpenGL坐标系变换的最后一步
        // 在对观察者坐标进行裁剪以后，再进行正视投影或者透视投影得到OpenGL标准化坐标
        // ，再把OpenGL的标准化坐标转变为屏幕坐标，和屏幕上的像素点一一对应
        GLES20.glViewport(0, 0, width, height)
        filters.forEach { filter ->
            filter.onSurfaceChanged(width, height)
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        var previousTextureId = frameBuffer.textureId
        val end = filters.size - 1
        for (i in filters.indices) {
            val filter = filters[i]
            val isNotLast = i < end
            // 开始绑定
            if (isNotLast) {
                // 设置本次纹理绘制的位置，这里是FrameBuffer
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer.id)
            }

            if (i == 0) {
                // 相机输入
                filter.onDraw(previousTextureId)
            } else if (isNotLast) {
                // 中间绘制
                filter.onDraw(previousTextureId)
            } else {
                // 输出绘制
                filter.onDraw(previousTextureId)
            }


            // 结束绑定
            if (isNotLast) {
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
                previousTextureId = frameBuffer.textureId
            } else {
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
            }
        }
    }
}