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
    private var listener: CameraRenderer.OnViewSizeAvailableListener
) : GpuImageFilter(context, vertexShaderCode, fragmentShaderCode) {

    companion object {
        /**
         * GLSL程序，GPU程序段
         * 顶点着色器
         */
        private const val vertexShaderCode =
            "attribute vec4 aPosition;\n" +
                    "attribute vec2 aCoord;\n" +
                    "varying vec2 vCoord;\n" +
                    "uniform mat4 uMatrix;\n" +
                    "uniform mat4 posMatrix;\n" +
                    "void main() {\n" +
                    "   gl_Position = posMatrix * aPosition;\n" +
                    "   vCoord = (uMatrix * vec4(aCoord, 1.0, 1.0)).xy;" +
                    "}\n"

        /**
         * GLSL程序，GPU程序段
         * 片段着色器
         */
        private const val fragmentShaderCode =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vCoord;\n" +
                    // 采样器，相机的输入数据先传递给此采样器
                    "uniform samplerExternalOES uTexture;\n" +
                    "void main() {\n" +
                    "   gl_FragColor = texture2D(uTexture, vCoord);\n" +
                    "   vec4 rgba = texture2D(uTexture, vCoord);\n" +
                    "}\n"
    }



    private val vertexData = floatArrayOf(
        -1f, -1f, // bottomLeft
        1f, -1f, // bottomRight
        -1f, 1f, // topLeft
        1f, 1f // topRight
    )

    private val textureData = floatArrayOf(
        0f, 0f, // bottomLeft
        1f, 0f, // bottomRight
        0f, 1f, // topLeft
        1f, 1f // topRight
    )

    private val vertexBuffer = OpenGLUtil.createByteBuffer(vertexData)
    private val textureBuffer = OpenGLUtil.createByteBuffer(textureData)

    private var aPos: Int = -1
    private var aTextureCoord: Int = -1

//    private lateinit var shaderProgram: ShaderProgram
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

    override fun onCreated() {
        aPos = program.getAttribute("aPosition")
        aTextureCoord = program.getAttribute("aCoord")

        externalTextureID = OpenGLUtil.createExternalTexture()
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        // 创建一个接收相机预览的texture
        surfaceTexture = SurfaceTexture(externalTextureID)
        onTextureCreated?.invoke(surfaceTexture)
    }

    override fun onDraw(fboTextureId: Int) {
        surfaceTexture.updateTexImage()
        surfaceTexture.getTransformMatrix(textureMatrix)

        // 给顶点赋值
        GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(aPos)
        // 给纹理坐标顶点赋值
        GLES20.glVertexAttribPointer(aTextureCoord, 2, GLES20.GL_FLOAT, false, 0, textureBuffer)
        GLES20.glEnableVertexAttribArray(aTextureCoord)

        // 纹理坐标矫正矩阵
        program.setMat4("uMatrix", textureMatrix)

        // 顶点坐标矫正矩阵
        program.setMat4("posMatrix", horizontalAdjustMatrix)

        // 以相同的方向绘制三角形
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPos)
        GLES20.glDisableVertexAttribArray(aTextureCoord)
    }



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