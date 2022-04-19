package xh.zero.render.group

import android.content.Context
import xh.zero.utils.ShaderProgram

abstract class GpuImageFilter {

    protected val program: ShaderProgram

    constructor(context: Context): super() {
        program = ShaderProgram(context, vertexShaderCode = vertexShaderCode, fragmentShaderCode = fragmentShaderCode)
    }

    constructor(context: Context, vShaderCode: String, fShaderCode: String): super() {
        program = ShaderProgram(context, vertexShaderCode = vShaderCode, fragmentShaderCode = fShaderCode)
    }

    companion object {
        /**
         * GLSL程序，GPU程序段
         * 顶点着色器
         */
        private const val vertexShaderCode =
            "attribute vec4 aPos;\n" +
            "attribute vec2 aTexPos;\n" +
            "varying vec2 vTexPos;\n" +
            "void main() {\n" +
            "   gl_Position = aPosition;\n" +
            "   vTexPos = aTexPos;" +
            "}\n"

        /**
         * GLSL程序，GPU程序段
         * 片段着色器
         */
        private const val fragmentShaderCode =
            "precision mediump float;\n" +
            "varying vec2 vTexPos;\n" +
            "uniform sampler2D uTexture;\n" +
            "void main() {\n" +
            "   gl_FragColor = texture2D(uTexture, vTexPos);\n" +
            "}\n"
    }



    fun onSurfaceCreated() {

    }

    abstract fun onCreated()

    open fun onDraw(fboTextureId: Int) {
        program.use()

    }

    open fun destroy() {
        program.destroy()
    }
}