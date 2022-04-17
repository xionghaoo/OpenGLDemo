package xh.zero.render

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Size

class CameraSurfaceView : GLSurfaceView, CameraRenderer.OnViewSizeAvailableListener {

    private val renderer: CameraRenderer

    init {
        setEGLContextClientVersion(2)
        renderer = CameraRenderer(context, this)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun getViewSize(): Size = Size(width, height)

    fun setOnSurfaceCreated(callback: OnTextureCreated) {
        renderer.setOnSurfaceCreated(callback)
    }

}