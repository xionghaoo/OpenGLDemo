package xh.zero.filterchain.filters

import android.content.Context

/**
 * 输出过滤器
 * 默认是输出到屏幕，也可以改成其他位置
 */
class OutputFilter(context: Context) : GpuImageFilter(context) {
    override fun onCreate() {
    }

    override fun beforeDrawArrays(textureId: Int) {
        // 修改默认位置0的纹理渲染，改成FrameBuffer
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
    }
}