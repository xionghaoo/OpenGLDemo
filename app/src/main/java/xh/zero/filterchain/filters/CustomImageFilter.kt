package xh.zero.filterchain.filters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix

class CustomImageFilter(private val context: Context) : GpuImageFilter(context, vertexShaderCode, fragmentShaderCode) {

    private var imgWidth: Int = 0
    private var imgHeight: Int = 0
    private var scaleMatrix = FloatArray(16)
    private var imgTexture = 0

//    private val vPMatrix = FloatArray(16)

    override fun onCreate() {
        val bitmap = BitmapFactory.decodeStream(context.assets.open("test_img.jpeg"))
        imgWidth = bitmap.width
        imgHeight = bitmap.height
        val matrix = android.graphics.Matrix()
        matrix.postScale(1f, -1f)
        imgTexture = createImageTexture(Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true))
    }

    override fun beforeDrawArrays(textureId: Int) {


        program.setMat4("scaleMatrix", scaleMatrix)
        // 修改位置0的纹理渲染，改成渲染图片
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, imgTexture)
        // 把图片纹理传给片段着色器
//        GLES20.glUniform1i(uTexture, 1)
        program.setInt("uTexture", 0)
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        // 构建正视投影矩阵
        scaleMatrix = changeMvpMatrixInside(width.toFloat(), height.toFloat(), imgWidth.toFloat(), imgHeight.toFloat())
    }

    override fun getVertexPosition(): FloatArray = vertexData

    private fun createImageTexture(bitmap: Bitmap?): Int {
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)
        // 绑定图片纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        // 生成mip贴图
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
        // 解绑图片纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, GLES20.GL_NONE)
        return textureIds[0]
    }

    private fun changeMvpMatrixInside(
        viewWidth: Float,
        viewHeight: Float,
        textureWidth: Float,
        textureHeight: Float
    ): FloatArray {
        val scale = viewWidth * textureHeight / viewHeight / textureWidth
        val mvp = FloatArray(16)
        Matrix.setIdentityM(mvp, 0)
        Matrix.scaleM(mvp, 0, if (scale > 1) 1f / scale else 1f, if (scale > 1) 1f else scale, 1f)
        return mvp
    }

    companion object {
        private const val vertexShaderCode =
            // 顶点坐标
            "attribute vec4 aPos;\n" +
            // 纹理坐标
            "attribute vec2 aTexPos;\n" +
            "uniform mat4 scaleMatrix;\n" +
            "varying vec2 vTexPos;\n" +
            "void main() {\n" +
            "   gl_Position = scaleMatrix * aPos;" +
            "   vTexPos = aTexPos;\n" +
            "}\n"

        private const val fragmentShaderCode =
            "precision mediump float;\n" +
            "uniform sampler2D uTexture;\n" +
            "varying vec2 vTexPos;\n" +
            "void main() {\n" +
            "   gl_FragColor = texture2D(uTexture, vTexPos);\n" +
            "}\n"

        private val vertexData = floatArrayOf(
            0f, 0f, // bottomLeft
            1f, 0f, // bottomRight
            0f, 1f, // topLeft
            1f, 1f // topRight
        )
    }
}