package xh.zero.render.group

import android.content.Context
import android.content.res.Configuration
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Size
import timber.log.Timber
import xh.zero.render.CameraRenderer
import xh.zero.utils.OpenGLUtil
import xh.zero.utils.ShaderProgram

typealias OnTextureCreated = (SurfaceTexture) -> Unit

class CameraInputFilter(
    private val context: Context,
    private var listener: OnViewSizeAvailableListener
) : GpuImageFilter(context, vertexShaderCode, fragmentShaderCode) {

    companion object {
        /**
         * GLSL程序，GPU程序段
         * 顶点着色器
         */
        private const val vertexShaderCode =
            "attribute vec4 aPos;\n" +
            "attribute vec2 aTexPos;\n" +
            "varying vec2 vTexPos;\n" +
            "uniform mat4 uMatrix;\n" +
            "uniform mat4 posMatrix;\n" +
            "void main() {\n" +
            "   gl_Position = posMatrix * aPos;\n" +
            "   vTexPos = (uMatrix * vec4(aTexPos, 1.0, 1.0)).xy;" +
            "}\n"

        /**
         * GLSL程序，GPU程序段
         * 片段着色器
         */
        private const val fragmentShaderCode =
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "varying vec2 vTexPos;\n" +
            // 采样器，相机的输入数据先传递给此采样器
            "uniform samplerExternalOES uTexture;\n" +
            "void main() {\n" +
            "   gl_FragColor = texture2D(uTexture, vTexPos);\n" +
            "}\n"
    }

    // 相机预览或者视频解码专用的Texture，提供给OpenGL处理
    private lateinit var surfaceTexture: SurfaceTexture
    private val textureMatrix = FloatArray(16)
    private val horizontalAdjustMatrix = FloatArray(16)
    // 着色器默认使用的纹理就是0
    private var externalTextureID: Int = 0
    private var onTextureCreated: OnTextureCreated? = null

    fun setOnSurfaceCreated(callback: OnTextureCreated) {
        this.onTextureCreated = callback
    }

    override fun onCreate() {
        externalTextureID = OpenGLUtil.createExternalTexture()
        // 创建一个接收相机预览的texture
        surfaceTexture = SurfaceTexture(externalTextureID)
        onTextureCreated?.invoke(surfaceTexture)

        initialHorizontalAdjustMatrix(needScale = false, ignore = true)
    }

    override fun onDraw(fboTextureId: Int) {
        surfaceTexture.updateTexImage()
        surfaceTexture.getTransformMatrix(textureMatrix)
        super.onDraw(fboTextureId)
    }

    override fun beforeDrawArrays(textureId: Int) {
//        Timber.d("CameraInputFilter: beforeDrawArrays")
        // 纹理坐标矫正矩阵
        program.setMat4("uMatrix", textureMatrix)
        // 顶点坐标矫正矩阵
        program.setMat4("posMatrix", horizontalAdjustMatrix)
    }

    override fun getTextureTarget(): Int = GLES11Ext.GL_TEXTURE_EXTERNAL_OES

    override fun destroy() {

    }

    private fun initialHorizontalAdjustMatrix(needScale: Boolean, ignore: Boolean) {
        if (context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT || ignore) {
            Matrix.setIdentityM(horizontalAdjustMatrix, 0)
        } else {
            // 缩放矩阵
            val scale: Float = listener.getViewSize().width.toFloat() / listener.getViewSize().height
            Timber.d("是否缩放：${needScale}，缩放比例：${scale}")
            val scaleMatrix = FloatArray(16)
            // 生成单位矩阵
            Matrix.setIdentityM(scaleMatrix, 0)
            Matrix.scaleM(scaleMatrix, 0, 1f, if (needScale) scale else 1f, 1f)

            val rotateMatrix = FloatArray(16)
            // 矩阵屏幕朝里方向顺时针旋转90度，相当于画面逆时针旋转90度
            Matrix.setRotateM(rotateMatrix, 0, 90f, 0f, 0f, 1f)
            // 矩阵乘顺序：从右到左，缩放 -> 旋转 -> 平移
            Matrix.multiplyMM(horizontalAdjustMatrix, 0, rotateMatrix, 0, scaleMatrix, 0)
        }
    }

    interface OnViewSizeAvailableListener {
        fun getViewSize() : Size
    }

}