package xh.zero.camerax

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView
import android.util.AttributeSet

class CameraSurfaceView : GLSurfaceView {

    private val renderer: CameraRenderer

    private var onTextureCreated: ((SurfaceTexture) -> Unit)? = null

    init {
        setEGLContextClientVersion(2)
        renderer = CameraRenderer(context, onTextureCreated)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)


    fun setOnTextureCreated(callback: (SurfaceTexture) -> Unit) {
        onTextureCreated = callback
    }

}