package xh.zero.render.test

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Size
import xh.zero.render.group.*

class TestSurfaceView : GLSurfaceView, CameraInputFilter.OnViewSizeAvailableListener {
    private val renderer: ImageFilterGroup

    private val cameraInput: CameraInputFilter

    init {
        setEGLContextClientVersion(2)
        renderer = ImageFilterGroup(context)
        cameraInput = CameraInputFilter(context, this)
        renderer.addFilter(cameraInput)
        renderer.addFilter(CustomImageFilter(context))
        renderer.addFilter(OutputFilter(context))
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun getViewSize(): Size = Size(width, height)

    fun setOnSurfaceCreated(callback: OnTextureCreated) {
        cameraInput.setOnSurfaceCreated(callback)
    }

}