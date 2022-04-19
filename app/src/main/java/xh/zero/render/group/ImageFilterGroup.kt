package xh.zero.render.group

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import timber.log.Timber
import xh.zero.utils.OpenGLUtil
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ImageFilterGroup(): GLSurfaceView.Renderer {

    private val filters = ArrayList<GpuImageFilter>()
    private var fboTextureId: Int = 0
    private var fboId: Int = 0

    fun addFilter(filter: GpuImageFilter) {
        filters.add(filter)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val pair = OpenGLUtil.createFBO(0, 0)
        fboId = pair.first
        fboTextureId = pair.second

        filters.forEach { filter ->
            filter.onCreated()
        }

    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Timber.d("onSurfaceChanged: $width, $height")
        // 视口变换，属于OpenGL坐标系变换的最后一步
        // 在对观察者坐标进行裁剪以后，再进行正视投影或者透视投影得到OpenGL标准化坐标
        // ，再把OpenGL的标准化坐标转变为屏幕坐标，和屏幕上的像素点一一对应
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        var textureId = fboTextureId
        for (i in filters.indices) {
            val filter = filters[i]
            // 开始绑定
            if (i == 0) {
                // 设置本次纹理绘制的位置，这里是FrameBuffer
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
            } else {
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            }

            filter.onDraw(textureId)

            // 结束绑定
            if (i == 0) {
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
            } else {
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            }
        }
    }
}