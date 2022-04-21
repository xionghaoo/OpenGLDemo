package xh.zero.filterchain

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Size
import xh.zero.filterchain.filters.*
import xh.zero.widgets.BaseSurfaceView
import xh.zero.widgets.OnTextureCreated

class FilterSurfaceView : BaseSurfaceView, CameraInputFilter.OnViewSizeAvailableListener {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    private val renderer: GpuImageFilterGroup

    private val cameraInput: CameraInputFilter

    init {
        setEGLContextClientVersion(2)
        renderer = GpuImageFilterGroup(context)
        cameraInput = CameraInputFilter(context, this)
        renderer.addFilter(cameraInput)
        renderer.addFilter(CustomImageFilter(context))
        renderer.addFilter(OutputFilter(context))
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun getViewSize(): Size = Size(width, height)

    override fun setOnSurfaceCreated(callback: OnTextureCreated) {
        cameraInput.setOnSurfaceCreated(callback)
    }

}