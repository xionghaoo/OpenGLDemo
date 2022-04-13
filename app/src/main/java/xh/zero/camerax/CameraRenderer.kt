package xh.zero.camerax

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import xh.zero.utils.OpenGLUtil
import xh.zero.utils.ShaderProgram
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class CameraRenderer(private val context: Context) : GLSurfaceView.Renderer {

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
    private val fragmentBuffer = OpenGLUtil.createByteBuffer(textureData)

    private var surfaceTexture: SurfaceTexture? = null
    private val matrix = FloatArray(16)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        shaderProgram = ShaderProgram(context, vertexShaderCode = vertexShaderCode, fragmentShaderCode = fragmentShaderCode)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        // 视口变换，属于OpenGL坐标系变换的最后一步
        // 在对观察者坐标进行裁剪以后，再进行正视投影或者透视投影得到OpenGL标准化坐标
        // ，再把OpenGL的标准化坐标转变为屏幕坐标，和屏幕上的像素点一一对应
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        shaderProgram.use()

        surfaceTexture?.updateTexImage()
        surfaceTexture?.getTransformMatrix(matrix)

    }


}