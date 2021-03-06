# OpenGl ES + Camera1/Camera2/CameraX
1. Camera API结合OpenGL ES竖直方向和水平方向无形变预览
2. OpenGL ES外部纹理贴图

# Camera模型示意图
![camera模型][1]

# 水平方向预览

### 摄像头传感器的角度为90
水平方向预览时，摄像头传感器的角度保持不变，因此需要手动旋转预览方向，通过乘以一个90度旋转矩阵来进行矫正。
另外画面在有些设备上可能发生变形，需要另外乘以一个缩放矩阵来进行矫正。
   
### 摄像头传感器的角度为0
水平方向预览不需要乘以矫正矩阵

### 纹理

OpenGL至少保证有16个纹理单元供你使用，也就是说你可以激活从GL_TEXTURE0到GL_TEXTRUE15。
纹理的定义方式:
```c
uniform sampler2D texture2;
```
这里虽然是uniform类型，但是我们的赋值方式是glUniformli，这里是给采样纹理赋值的一个位置，也就是纹理单元。
我们可以使用多个这样的纹理单元，其中默认被激活的纹理单元是GL_TEXTURE0。

### 着色器程序
Program由多个着色器组成，每一个着色器的输入都是下一级着色器的输出。通常一个程序会拥有顶点着色器和片段着色器。

屏幕或FBO上可以用多个Program来绘制图形。

每个Program可以使用多个纹理，可以通过glsl的mix函数混合。


[1]:https://github.com/xionghaoo/OpenGLDemo/blob/master/doc/camera_model.png?raw=true