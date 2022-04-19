package xh.zero.utils

import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class OpenGLUtil {
    companion object {
        // 每个float 4byte
        fun createByteBuffer(data: FloatArray): FloatBuffer =
            ByteBuffer.allocateDirect(data.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(data)
                    position(0)
                }
            }


        /**
         * 创建外部渲染纹理
         */
        fun createExternalTexture() : Int {
            val textureIds = IntArray(1)
            GLES20.glGenTextures(1, textureIds, 0)
//            cameraRenderTextureId = textureIds[0]
            val textureId = textureIds[0]
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
            // 设置纹理的环绕方式
            // 环绕（超出纹理坐标范围）  （s==x t==y GL_REPEAT 重复）
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_REPEAT
            )
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_REPEAT
            )
            // 在纹理放大和缩小时需要设置纹理的过滤方式，有两种最重要的：
            // GL_NEAREST: 邻近过滤，离纹理坐标最近的像素命中
            // GL_LINEAR: 线性过滤，根据坐标点附近的像素来计算一个颜色插值，离坐标点最近的颜色贡献最大
            // 过滤（纹理像素映射到坐标点）  （缩小、放大：GL_LINEAR线性）
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR
            )
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR
            )

            return textureId
        }

        /**
         * 创建fbo(Frame Buffer Object)
         */
        fun createFBO(w: Int, h: Int) : Pair<Int, Int> {
            //1. 创建FBO
            val fbos = IntArray(1)
            GLES20.glGenFramebuffers(1, fbos, 0)
            val fboId = fbos[0]
            //2. 绑定FBO
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)

            //3. 创建FBO纹理 接收视频数据缓冲
            val textureIds = IntArray(1)
            //创建纹理
            GLES20.glGenTextures(1, textureIds, 0)
            val fboTextureId = textureIds[0]
            //绑定纹理，这里就和GPU程序的纹理绑定了
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTextureId)
            //环绕（超出纹理坐标范围）  （s==x t==y GL_REPEAT 重复）
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
            //过滤（纹理像素映射到坐标点）  （缩小、放大：GL_LINEAR线性）
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

            //4. 把纹理绑定到FBO
            GLES20.glFramebufferTexture2D(
                GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, fboTextureId, 0
            )

            //5. 设置FBO分配内存大小
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, w, h,
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

            return Pair(fboId, fboTextureId)
        }
    }
}