package xh.zero.render.group

import android.content.Context
import android.opengl.GLES20
import xh.zero.utils.OpenGLUtil
import xh.zero.utils.ShaderProgram
import java.nio.FloatBuffer

abstract class GpuImageFilter {

    protected lateinit var program: ShaderProgram

    private var aPos: Int = -1
    private var aTexPos: Int = -1

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

    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var textureBuffer: FloatBuffer
    private val vertexShaderCode: String
    private val fragmentShaderCode: String
    private val context: Context

    constructor(context: Context): super() {
        this.context = context
        vertexShaderCode = VERTEX_SHADER_CODE
        fragmentShaderCode = FRAGMENT_SHADER_CODE
    }

    constructor(context: Context, vShaderCode: String, fShaderCode: String): super() {
        this.context = context
        vertexShaderCode = vShaderCode
        fragmentShaderCode = fShaderCode
    }

    fun onSurfaceCreated() {
        program = ShaderProgram(context, vertexShaderCode = vertexShaderCode, fragmentShaderCode = fragmentShaderCode)

        vertexBuffer = OpenGLUtil.createByteBuffer(getVertexPosition() ?: vertexData)
        textureBuffer = OpenGLUtil.createByteBuffer(getTexturePosition() ?: textureData)
        aPos = program.getAttribute("aPos")
        aTexPos = program.getAttribute("aTexPos")
        onCreate()
    }

    abstract fun onCreate()

    open fun onSurfaceChanged(width: Int, height: Int) {

    }

    open fun getTextureTarget() : Int = GLES20.GL_TEXTURE_2D

    open fun onDraw(fboTextureId: Int) {
        program.use()
        // 传入顶点坐标
        GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(aPos)
        // 传入纹理坐标
        GLES20.glVertexAttribPointer(aTexPos, 2, GLES20.GL_FLOAT, false, 0, textureBuffer)
        GLES20.glEnableVertexAttribArray(aTexPos)

        // 渲染默认位置(位置0)的纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        // 把FrameBuffer渲染到屏幕上
        GLES20.glBindTexture(getTextureTarget(), fboTextureId)
        program.setInt("uTexture", 0)

        beforeDrawArrays(fboTextureId)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(aPos)
        GLES20.glDisableVertexAttribArray(aTexPos)
        GLES20.glBindTexture(getTextureTarget(), 0)
        GLES20.glUseProgram(0)
    }

    open fun destroy() {
        program.destroy()
    }

    open fun getVertexPosition(): FloatArray? = null
    open fun getTexturePosition(): FloatArray? = null

    abstract fun beforeDrawArrays(textureId: Int)

    companion object {
        /**
         * GLSL程序，GPU程序段
         * 顶点着色器
         */
        private const val VERTEX_SHADER_CODE =
            "attribute vec4 aPos;\n" +
            "attribute vec2 aTexPos;\n" +
            "varying vec2 vTexPos;\n" +
            "void main() {\n" +
            "   gl_Position = aPos;\n" +
            "   vTexPos = aTexPos;" +
            "}\n"

        /**
         * GLSL程序，GPU程序段
         * 片段着色器
         */
        private const val FRAGMENT_SHADER_CODE =
            "precision mediump float;\n" +
            "varying vec2 vTexPos;\n" +
            "uniform sampler2D uTexture;\n" +
            "void main() {\n" +
            "   gl_FragColor = texture2D(uTexture, vTexPos);\n" +
            "}\n"
    }
}