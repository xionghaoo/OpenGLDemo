package xh.zero.render.group

import android.content.Context
import android.opengl.GLES20
import timber.log.Timber

class OutputFilter(context: Context) : GpuImageFilter(context) {
    override fun onCreate() {
    }

    override fun beforeDrawArrays(textureId: Int) {
        // 修改默认位置0的纹理渲染，改成FrameBuffer
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
    }
}