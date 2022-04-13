package xh.zero.camerax

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet

class CameraSurfaceView : GLSurfaceView {

    private val renderer: CameraRenderer

    init {
        setEGLContextClientVersion(2)
        renderer = CameraRenderer(context)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)


}