package xh.zero.render.group

import android.opengl.GLES20

class FrameBuffer(
    private val width: Int,
    private val height: Int
) {

    var id: Int = -1
        private set
    var textureId: Int = -1
        private set

    init {
        //1. 创建FBO
        val fbos = IntArray(1)
        GLES20.glGenFramebuffers(1, fbos, 0)
        id = fbos[0]
        //2. 绑定FBO
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, id)

        //3. 创建FBO纹理 接收视频数据缓冲
        val textureIds = IntArray(1)
        //创建纹理
        GLES20.glGenTextures(1, textureIds, 0)
        textureId = textureIds[0]
        //绑定纹理，这里就和GPU程序的纹理绑定了
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        //环绕（超出纹理坐标范围）  （s==x t==y GL_REPEAT 重复）
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
        //过滤（纹理像素映射到坐标点）  （缩小、放大：GL_LINEAR线性）
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        //4. 把纹理绑定到FBO
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D, textureId, 0
        )

        //5. 设置FBO分配内存大小
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height,
            0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null
        )

        //6. 检测是否绑定从成功
        if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
            != GLES20.GL_FRAMEBUFFER_COMPLETE
        ) {
//                Log.e(TAG, "glFramebufferTexture2D error")
        }
        //7. 解绑纹理和FBO
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    fun destroy() {
        if (id != -1) {
            GLES20.glDeleteFramebuffers(1, intArrayOf(id), 0)
            id = -1
        }
        if (textureId != -1) {
            GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
            textureId = -1
        }
    }
}