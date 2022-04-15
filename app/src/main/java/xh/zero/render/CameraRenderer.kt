package xh.zero.render

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import timber.log.Timber
import xh.zero.utils.OpenGLUtil
import xh.zero.utils.ShaderProgram
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

typealias OnTextureCreated = (SurfaceTexture) -> Unit

class CameraRenderer(
    private val context: Context
) : GLSurfaceView.Renderer {

    /**
     * GLSL程序，GPU程序段
     * 顶点着色器
     */
    private val vertexShaderCode =
        "attribute vec4 aPosition;\n" +
        "attribute vec2 aCoord;\n" +
        "varying vec2 vCoord;\n" +
        "uniform mat4 uMatrix;\n" +
        "void main() {\n" +
        "   gl_Position = aPosition;\n" +
        "   vCoord = (uMatrix * vec4(aCoord, 1.0, 1.0)).xy;" +
        "}\n"

    /**
     * GLSL程序，GPU程序段
     * 片段着色器
     */
    private val fragmentShaderCode =
        "#extension GL_OES_EGL_image_external : require\n" +
        "precision mediump float;\n" +
        "varying vec2 vCoord;\n" +
        // 采样器，相机的输入数据先传递给此采样器
        "uniform samplerExternalOES uTexture;\n" +
        "void main() {\n" +
        "   gl_FragColor = texture2D(uTexture, vCoord);\n" +
        "   vec4 rgba = texture2D(uTexture, vCoord);\n" +
        "}\n"

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

    private lateinit var shaderProgram: ShaderProgram
    private val vertexBuffer = OpenGLUtil.createByteBuffer(vertexData)
    private val textureBuffer = OpenGLUtil.createByteBuffer(textureData)
    // 着色器默认使用的纹理就是0
    private var externalTextureID: Int = 0

    private var aPos: Int = -1
    private var aTextureCoord: Int = -1

    // 相机预览或者视频解码专用的Texture，提供给OpenGL处理
    private lateinit var surfaceTexture: SurfaceTexture
    private val matrix = FloatArray(16)

    private var onTextureCreated: OnTextureCreated? = null

    fun setOnTextureCreated(callback: OnTextureCreated) {
        this.onTextureCreated = callback
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        shaderProgram = ShaderProgram(context, vertexShaderCode = vertexShaderCode, fragmentShaderCode = fragmentShaderCode)

        aPos = shaderProgram.getAttribute("aPosition")
        aTextureCoord = shaderProgram.getAttribute("aCoord")

        externalTextureID = OpenGLUtil.createExternalTexture()
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        // 创建一个接收相机预览的texture
        surfaceTexture = SurfaceTexture(externalTextureID)

        onTextureCreated?.invoke(surfaceTexture)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        // 视口变换，属于OpenGL坐标系变换的最后一步
        // 在对观察者坐标进行裁剪以后，再进行正视投影或者透视投影得到OpenGL标准化坐标
        // ，再把OpenGL的标准化坐标转变为屏幕坐标，和屏幕上的像素点一一对应
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        surfaceTexture.updateTexImage()
        surfaceTexture.getTransformMatrix(matrix)

//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        shaderProgram.use()

        // 给顶点赋值
        GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(aPos)
        // 给纹理坐标顶点赋值
        GLES20.glVertexAttribPointer(aTextureCoord, 2, GLES20.GL_FLOAT, false, 0, textureBuffer)
        GLES20.glEnableVertexAttribArray(aTextureCoord)

        shaderProgram.setMat4("uMatrix", matrix)

        // 激活外部纹理（这个步骤不是必须的）
//        GLES20.glActiveTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES)
//        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, externalTextureID)
//        // 绑定外部纹理到glsl，设置uniform属性必须要先使用程序，因为它是在当前激活的着色器程序中使用
//        shaderProgram.setInt("uTexture", 0)

        // 以相同的方向绘制三角形
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPos)
        GLES20.glDisableVertexAttribArray(aTextureCoord)

    }


}