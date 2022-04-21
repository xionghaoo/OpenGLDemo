package xh.zero.widgets

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView
import android.util.AttributeSet

typealias OnTextureCreated = (SurfaceTexture) -> Unit

abstract class BaseSurfaceView: GLSurfaceView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    abstract fun setOnSurfaceCreated(callback: OnTextureCreated)
}