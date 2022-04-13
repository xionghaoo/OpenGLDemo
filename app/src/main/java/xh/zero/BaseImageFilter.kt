package xh.zero

import android.content.Context
import xh.zero.utils.ShaderProgram

abstract class BaseImageFilter(
    private val context: Context,
    private val vertexPath: String,
    private val fragmentPath: String
) {




    private val program = ShaderProgram(context, vertexPath, fragmentPath)

    fun onDraw() {
        program.use()

    }

    fun destroy() {
        program.destroy()
    }
}