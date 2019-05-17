package org.andresoviedo.android_3d_model_engine.drawer;

import android.content.res.Resources;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import org.andresoviedo.android_3d_model_engine.model.AnimatedModel;
import org.andresoviedo.android_3d_model_engine.model.Object3D;
import org.andresoviedo.android_3d_model_engine.model.Object3DData;
import org.andresoviedo.util.android.GLUtil;
import org.andresoviedo.util.core.util.MatrixState;
import org.andresoviedo.util.core.util.ShaderUtil;
import org.andresoviedo.util.core.render.ModelSurfaceView;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract class that implements all calls to opengl to draw objects
 * <p>
 * Subclasses must provide vertex shader and specify whether the shaders supports specific features
 *
 * @author andresoviedo
 */
public abstract class Object3DImpl implements Object3D {

    private final String id;
    // Transformations
    private final float[] mMatrix = new float[16];
    // mvp matrix
    private final float[] mvMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];
    /**
     * animation data  put 0 to draw progressively, -1 to draw at once
     */
    private long counter = -1;
    private double shift = -1d;

    private static final String TAG = "ailibin";

    /**
     * does the device support drawElements for GL_UNSIGNED_INT or not?
     */
    private boolean drawUsingUnsignedInt = true;

    protected final Map<Integer, String> cache1 = new HashMap<>();


    /**
     * 自定义渲染管线着色器程序id
     */
    int mProgram;
    /**
     * 总变换矩阵引用
     */
    int muMVPMatrixHandle;
    /**
     * 位置、旋转变换矩阵
     */
    int muMMatrixHandle;
    /**
     * 顶点位置属性引用
     */
    int maPositionHandle;
    /**
     * 顶点法向量属性引用
     */
    int maNormalHandle;
    /**
     * 光源位置属性引用
     */
    int maLightLocationHandle;
    /**
     * 摄像机位置属性引用
     */
    int maCameraHandle;
    /**
     * 顶点颜色
     */
    int muColorHandle;
    /**
     * 材质中透明度
     */
    int muOpacityHandle;
    /**
     * 顶点着色器代码脚本
     */
    String mVertexShader;
    /**
     * 片元着色器代码脚本
     */
    String mFragmentShader;
    /**
     * 顶点坐标数据缓冲
     */
    FloatBuffer mVertexBuffer;
    /**
     * 顶点法向量数据缓冲
     */
    FloatBuffer mNormalBuffer;
    /**
     * 材质漫反射光
     */
    protected float[] mDifColor = new float[4];
    /**
     * 材质中alpha
     */
    protected float mAlpha = 1;
    int vCount = 0;

    boolean isUseMatrixState = true;

    public Object3DImpl(String id, String vertexShaderCode, String fragmentShaderCode, String... variables) {
        this.id = id;
        Log.e(TAG, "Compiling 3D Drawer1... " + id);
        // prepare shaders and OpenGL program
        int vertexShader = GLUtil.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = GLUtil.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        mProgram = GLUtil.createAndLinkProgram(vertexShader, fragmentShader, variables);
        Log.e(TAG, "Compiled 3D Drawer1 (" + id + ") with id " + mProgram);
    }

    public Object3DImpl(String id, String vertexShaderCode, String fragmentShaderCode, ModelSurfaceView modelSurfaceView, String... variables) {
        this.id = id;
        Log.e(TAG, "Compiling 3D Drawer... " + id);
        // prepare shaders and OpenGL program
        if (isUseMatrixState) {
            initShader(modelSurfaceView.getResources());
        } else {
            int vertexShader = GLUtil.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
            int fragmentShader = GLUtil.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
            mProgram = GLUtil.createAndLinkProgram(vertexShader, fragmentShader, variables);
        }
    }

    /**
     * 初始化shader
     *
     * @param res
     */
    public void initShader(Resources res) {
        //加载顶点着色器的脚本内容
        mVertexShader = ShaderUtil.loadFromAssetsFile("color_vertex.vert", res);
        //加载片元着色器的脚本内容
        mFragmentShader = ShaderUtil.loadFromAssetsFile("color_frag.frag", res);
        Log.e(TAG, "mVertexShader: " + mVertexShader + " mFragmentShader:" + mFragmentShader);
        //基于顶点着色器与片元着色器创建程序
        mProgram = ShaderUtil.createProgram(mVertexShader, mFragmentShader);
        //获取程序中顶点位置属性引用
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        //获取程序中顶点颜色属性引用
        maNormalHandle = GLES20.glGetAttribLocation(mProgram, "aNormal");
        //获取程序中总变换矩阵引用
        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        //获取位置、旋转变换矩阵引用
        muMMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMMatrix");
        //获取程序中光源位置引用
        maLightLocationHandle = GLES20.glGetUniformLocation(mProgram, "uLightLocation");
        //获取程序中摄像机位置引用
        maCameraHandle = GLES20.glGetUniformLocation(mProgram, "uCamera");
        // 顶点颜色
        muColorHandle = GLES20.glGetUniformLocation(mProgram, "uColor");
        // alpha
        muOpacityHandle = GLES20.glGetUniformLocation(mProgram, "uOpacity");
    }

    @Override
    public void draw(Object3DData obj, float[] pMatrix, float[] vMatrix, int textureId, float[] lightPos) {
        this.draw(obj, pMatrix, vMatrix, obj.getDrawMode(), obj.getDrawSize(), textureId, lightPos);
    }


    @Override
    public void draw(Object3DData obj, float[] pMatrix, float[] vMatrix, int drawMode, int drawSize, int textureId,
                     float[] lightPos) {
        if (isUseMatrixState) {
            vCount = drawShape(obj, drawMode, drawSize, true);
            drawSelfByMatrix(obj);
        } else {
            drawSelf(obj, pMatrix, vMatrix, drawMode, drawSize, textureId, lightPos);
        }
    }


    private void drawSelf(Object3DData obj, float[] pMatrix, float[] vMatrix, int drawMode, int drawSize, int textureId, float[] lightPos) {
        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);

        //保存到需要转化的矩阵中
        float[] mMatrix = getMMatrix(obj);
        //视图矩阵和当前矩阵运算
        float[] mvMatrix = getMvMatrix(mMatrix, vMatrix);
        //获取最终的矩阵(与投影矩阵运算)
        float[] mvpMatrix = getMvpMatrix(mvMatrix, pMatrix);

        setMvpMatrix(mvpMatrix);

        int mPositionHandle = setPosition(obj);

        int mColorHandle = -1;
        if (supportsColors()) {
            mColorHandle = setColors(obj);
        } else {
            setColor(obj);
        }

        int mTextureHandle = -1;
        if (textureId != -1 && supportsTextures()) {
            setTexture(obj, textureId);
        }

        int mNormalHandle = -1;
        if (supportsNormals()) {
            mNormalHandle = setNormals(obj);
        }

        if (supportsMvMatrix()) {
            setMvMatrix(mvMatrix);
        }

        if (lightPos != null && supportsLighting()) {
            setLightPos(lightPos);
        }

        Log.e(TAG, "drawMode: " + drawMode + " drawSize:" + drawSize);
        drawShape(obj, drawMode, drawSize);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);

        if (mColorHandle != -1) {
            GLES20.glDisableVertexAttribArray(mColorHandle);
        }

        // Disable vertex array
        if (mTextureHandle != -1) {
            GLES20.glDisableVertexAttribArray(mTextureHandle);
        }

        if (mNormalHandle != -1) {
            GLES20.glDisableVertexAttribArray(mNormalHandle);
        }
    }

    public void drawSelfByMatrix(Object3DData obj) {
        //制定使用某套着色器程序
        GLES20.glUseProgram(mProgram);
        //将最终变换矩阵传入着色器程序
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, MatrixState.getFinalMatrix(), 0);
        //将位置、旋转变换矩阵传入着色器程序
        GLES20.glUniformMatrix4fv(muMMatrixHandle, 1, false, getMMatrix(obj, true), 0);
        //将光源位置传入着色器程序
        GLES20.glUniform3fv(maLightLocationHandle, 1, MatrixState.lightPositionFB);
        //将摄像机位置传入着色器程序
        GLES20.glUniform3fv(maCameraHandle, 1, MatrixState.cameraFB);

        int diffuseColor = -1;
        mDifColor[0] = (float) Color.red(diffuseColor) / 255.f;
        mDifColor[1] = (float) Color.green(diffuseColor) / 255.f;
        mDifColor[2] = (float) Color.blue(diffuseColor) / 255.f;
        mDifColor[3] = (float) Color.alpha(diffuseColor) / 255.f;
        mAlpha = 1.0f;
        //物体的大小和法向量的buffer和顶点坐标的buffer无关
        FloatBuffer vertexBuffer = obj.getVertexArrayBuffer() != null ? obj.getVertexArrayBuffer()
                : obj.getVertexBuffer();
        vertexBuffer.position(0);
        FloatBuffer normalBuffer = obj.getVertexNormalsArrayBuffer() != null ? obj.getVertexNormalsArrayBuffer() : obj.getNormals();
        normalBuffer.position(0);
        // 将顶点位置数据传入渲染管线
        GLES20.glVertexAttribPointer
                (
                        maPositionHandle,
                        3,
                        GLES20.GL_FLOAT,
                        false,
                        3 * 4,
                        vertexBuffer
                );
        //将顶点法向量数据传入渲染管线
        GLES20.glVertexAttribPointer
                (
                        maNormalHandle,
                        3,
                        GLES20.GL_FLOAT,
                        false,
                        3 * 4,
                        normalBuffer
                );

        // 材质颜色
        GLES20.glUniform3fv(muColorHandle, 1, mDifColor, 0);
        // 材质alpha
        GLES20.glUniform1f(muOpacityHandle, mAlpha);
        //启用顶点位置、法向量、纹理坐标数据
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        GLES20.glEnableVertexAttribArray(maNormalHandle);
        //绘制加载的物体
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vCount);
        Log.e(TAG, "vCount: " + vCount);

    }

    public float[] getMMatrix(Object3DData obj, boolean isUseMatrixState) {

        // calculate object transformation,转化为单位正交矩阵，运算结果值保存在mMatrix中
        Matrix.setIdentityM(MatrixState.getMMatrix(), 0);
        if (obj.getRotation() != null) {
            Matrix.rotateM(MatrixState.getMMatrix(), 0, obj.getRotation()[0], 1f, 0f, 0f);
            Matrix.rotateM(MatrixState.getMMatrix(), 0, obj.getRotation()[1], 0, 1f, 0f);
            Matrix.rotateM(MatrixState.getMMatrix(), 0, obj.getRotationZ(), 0, 0, 1f);
        }
        if (obj.getScale() != null) {
            Matrix.scaleM(MatrixState.getMMatrix(), 0, obj.getScaleX(), obj.getScaleY(), obj.getScaleZ());
        }
        if (obj.getPosition() != null) {
            Matrix.translateM(MatrixState.getMMatrix(), 0, obj.getPositionX(), obj.getPositionY(), obj.getPositionZ());
        }
        return MatrixState.getMMatrix();

    }


    public float[] getMMatrix(Object3DData obj) {

        // calculate object transformation,转化为单位正交矩阵，运算结果值保存在mMatrix中
        Matrix.setIdentityM(mMatrix, 0);
        if (obj.getRotation() != null) {
            Matrix.rotateM(mMatrix, 0, obj.getRotation()[0], 1f, 0f, 0f);
            Matrix.rotateM(mMatrix, 0, obj.getRotation()[1], 0, 1f, 0f);
            Matrix.rotateM(mMatrix, 0, obj.getRotationZ(), 0, 0, 1f);
        }
        if (obj.getScale() != null) {
            Matrix.scaleM(mMatrix, 0, obj.getScaleX(), obj.getScaleY(), obj.getScaleZ());
        }
        if (obj.getPosition() != null) {
            Matrix.translateM(mMatrix, 0, obj.getPositionX(), obj.getPositionY(), obj.getPositionZ());
        }
        return mMatrix;

    }

    public float[] getMvMatrix(float[] mMatrix, float[] vMatrix) {
        Matrix.multiplyMM(mvMatrix, 0, vMatrix, 0, mMatrix, 0);
        return mvMatrix;
    }

    protected float[] getMvpMatrix(float[] mvMatrix, float[] pMatrix) {
        Matrix.multiplyMM(mvpMatrix, 0, pMatrix, 0, mvMatrix, 0);
        return mvpMatrix;
    }

    protected void setMvpMatrix(float[] mvpMatrix) {

        // get handle to shape's transformation matrix
        int mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "u_MVPMatrix");
        GLUtil.checkGlError("glGetUniformLocation");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GLUtil.checkGlError("glUniformMatrix4fv");

    }


    protected boolean supportsColors() {
        return false;
    }

    protected void setColor(Object3DData obj) {

        // get handle to fragment shader's vColor member
        int mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
        GLUtil.checkGlError("glGetUniformLocation");

        // Set color for drawing the triangle,int -1 物体散射光的颜色
        int diffuseColor = -1;
        //材质漫反射光
        mDifColor[0] = (float) Color.red(diffuseColor) / 255.f;
        mDifColor[1] = (float) Color.green(diffuseColor) / 255.f;
        mDifColor[2] = (float) Color.blue(diffuseColor) / 255.f;
        mDifColor[3] = (float) Color.alpha(diffuseColor) / 255.f;
        float[] color = obj.getColor() != null ? obj.getColor() : DEFAULT_COLOR;
        Log.e(TAG, "color: " + obj.getColor());
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);
//        GLES20.glUniform4fv(mColorHandle, 1, mDifColor, 0);
        GLUtil.checkGlError("glUniform4fv");

    }


    protected int setColors(Object3DData obj) {

        // get handle to fragment shader's vColor member
        int mColorHandle = GLES20.glGetAttribLocation(mProgram, "a_Color");
        GLUtil.checkGlError("glGetAttribLocation");

        // Pass in the color information
        GLES20.glEnableVertexAttribArray(mColorHandle);
        GLUtil.checkGlError("glEnableVertexAttribArray");

        obj.getVertexColorsArrayBuffer().position(0);
        GLES20.glVertexAttribPointer(mColorHandle, 4, GLES20.GL_FLOAT, false, 0, obj.getVertexColorsArrayBuffer());
        GLUtil.checkGlError("glVertexAttribPointer");

        return mColorHandle;
    }

    protected int setPosition(Object3DData obj) {

        // get handle to vertex shader's a_Position member
        int mPositionHandle = GLES20.glGetAttribLocation(mProgram, "a_Position");
        GLUtil.checkGlError("glGetAttribLocation");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLUtil.checkGlError("glEnableVertexAttribArray");

        FloatBuffer vertexBuffer = obj.getVertexArrayBuffer() != null ? obj.getVertexArrayBuffer()
                : obj.getVertexBuffer();
        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, VERTEX_STRIDE,
                vertexBuffer);
        GLUtil.checkGlError("glVertexAttribPointer");

        return mPositionHandle;
    }

    protected boolean supportsNormals() {
        return false;
    }

    protected int setNormals(Object3DData obj) {
        int mNormalHandle = GLES20.glGetAttribLocation(mProgram, "a_Normal");
        GLUtil.checkGlError("glGetAttribLocation");

        GLES20.glEnableVertexAttribArray(mNormalHandle);
        GLUtil.checkGlError("glEnableVertexAttribArray");

        // Pass in the normal information
        FloatBuffer buffer = obj.getVertexNormalsArrayBuffer() != null ? obj.getVertexNormalsArrayBuffer() : obj.getNormals();
        buffer.position(0);
        GLES20.glVertexAttribPointer(mNormalHandle, 3, GLES20.GL_FLOAT, false, 0, buffer);

        return mNormalHandle;
    }

    protected boolean supportsLighting() {
        return true;
    }

    protected void setLightPos(float[] lightPosInEyeSpace) {
        int mLightPosHandle = GLES20.glGetUniformLocation(mProgram, "u_LightPos");
        // Pass in the light position in eye space.
        GLES20.glUniform3f(mLightPosHandle, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);
    }

    protected boolean supportsMvMatrix() {
        return false;
    }

    protected void setMvMatrix(float[] mvMatrix) {
        int mMVMatrixHandle = GLES20.glGetUniformLocation(mProgram, "u_MVMatrix");
        GLUtil.checkGlError("glGetUniformLocation");

        // Pass in the modelview matrix.
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mvMatrix, 0);
        GLUtil.checkGlError("glUniformMatrix4fv");
    }

    protected boolean supportsTextures() {
        return false;
    }

    protected int setTexture(Object3DData obj, int textureId) {
        int mTextureUniformHandle = GLES20.glGetUniformLocation(mProgram, "u_Texture");
        GLUtil.checkGlError("glGetUniformLocation");

        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLUtil.checkGlError("glActiveTexture");

        // Bind to the texture in OpenGL
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLUtil.checkGlError("glBindTexture");

        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, 0);
        GLUtil.checkGlError("glUniform1i");

        int mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "a_TexCoordinate");
        GLUtil.checkGlError("glGetAttribLocation");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
        GLUtil.checkGlError("glEnableVertexAttribArray");

        // Prepare the triangle coordinate data
        obj.getTextureCoordsArrayBuffer().position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, 2, GLES20.GL_FLOAT, false, 0,
                obj.getTextureCoordsArrayBuffer());
        GLUtil.checkGlError("glVertexAttribPointer");

        return mTextureCoordinateHandle;
    }

    protected int drawShape(Object3DData obj, int drawMode, int drawSize, boolean isReturnCount) {

        int count = 0;
        FloatBuffer vertexBuffer = obj.getVertexArrayBuffer() != null ? obj.getVertexArrayBuffer()
                : obj.getVertexBuffer();
        vertexBuffer.position(0);
        List<int[]> drawModeList = obj.getDrawModeList();

        Buffer drawOrderBuffer = obj.getDrawOrder();
        int drawBufferType = GLES20.GL_UNSIGNED_INT;
        if (!drawUsingUnsignedInt) {
            drawOrderBuffer = obj.getDrawOrderAsShort();
            drawBufferType = GLES20.GL_UNSIGNED_SHORT;
        }

        if (obj.isDrawUsingArrays()) {
            drawOrderBuffer = null;
        }

        if (drawModeList != null) {
            if (drawOrderBuffer == null) {
                Log.e(TAG, "Drawing single polygons using arrays...");
                for (int j = 0; j < drawModeList.size(); j++) {
                    int[] polygon = drawModeList.get(j);
                    int drawModePolygon = polygon[0];
                    int vertexPos = polygon[1];
                    int drawSizePolygon = polygon[2];
                    if (drawMode == GLES20.GL_LINE_LOOP && polygon[2] > 3) {
                        // is this wireframe?
                        Log.e(TAG, "Drawing wireframe for '" + obj.getId() + "' (" + drawSizePolygon + ")...");
                        for (int i = 0; i < polygon[2] - 2; i++) {
                            Log.e(TAG, "Drawing wireframe triangle '" + i + "' for '" + obj.getId() + "'...");
                            GLES20.glDrawArrays(drawMode, polygon[1] + i, 3);
                        }
                    } else {
                        GLES20.glDrawArrays(drawMode, polygon[1], polygon[2]);
                    }
                }
            } else {
                Log.e(TAG, "Drawing single polygons using elements...");
                for (int i = 0; i < drawModeList.size(); i++) {
                    int[] drawPart = drawModeList.get(i);
                    int drawModePolygon = drawPart[0];
                    int vertexPos = drawPart[1];
                    int drawSizePolygon = drawPart[2];
                    drawOrderBuffer.position(vertexPos);
                    GLES20.glDrawElements(drawModePolygon, drawSizePolygon, drawBufferType, drawOrderBuffer);
                    if (drawUsingUnsignedInt && GLUtil.checkGlError("glDrawElements")) {
                        drawUsingUnsignedInt = false;
                    }
                }
            }
        } else {
            if (drawOrderBuffer != null) {
                if (drawSize <= 0) {
                    // String mode = drawMode == GLES20.GL_POINTS ? "Points" : drawMode == GLES20.GL_LINES? "Lines": "Triangles?";
                    Log.e(TAG, "Drawing all elements with mode '" + drawMode + "'...");
                    drawOrderBuffer.position(0);
                    GLES20.glDrawElements(drawMode, drawOrderBuffer.capacity(), drawBufferType,
                            drawOrderBuffer);
                    if (drawUsingUnsignedInt && GLUtil.checkGlError("glDrawElements")) {
                        drawUsingUnsignedInt = false;
                    }
                } else {
                    Log.e(TAG, "Drawing single elements of size '" + drawSize + "'...");
                    for (int i = 0; i < drawOrderBuffer.capacity(); i += drawSize) {
                        drawOrderBuffer.position(i);
                        GLES20.glDrawElements(drawMode, drawSize, drawBufferType, drawOrderBuffer);
                    }
                    if (drawUsingUnsignedInt && GLUtil.checkGlError("glDrawElements")) {
                        drawUsingUnsignedInt = false;
                    }
                }
            } else {
                //进来这里的方法了
                if (drawSize <= 0) {
                    int drawCount = vertexBuffer.capacity() / COORDS_PER_VERTEX;
                    // if we want to animate, initialize counter=0 at variable declaration
                    if (this.shift >= 0) {
                        double rotation = ((SystemClock.uptimeMillis() % 10000) / 10000f) * (Math.PI * 2);
                        if (this.shift == 0d) {
                            this.shift = rotation;
                        }
                        drawCount = (int) ((Math.sin(rotation - this.shift + Math.PI / 2 * 3) + 1) / 2f * drawCount);
                    }
                    Log.e(TAG, "Drawing all triangles using arrays... counter(" + drawCount + ")");
                    //这里使用GLES20
                    count = drawCount;
                    GLES20.glDrawArrays(drawMode, 0, drawCount);
                } else {
                    Log.e(TAG, "Drawing single triangles using arrays...");
                    for (int i = 0; i < vertexBuffer.capacity() / COORDS_PER_VERTEX; i += drawSize) {
                        GLES20.glDrawArrays(drawMode, i, drawSize);
                    }
                }
            }
        }
        return count;
    }

    protected void drawShape(Object3DData obj, int drawMode, int drawSize) {

        this.drawShape(obj, drawMode, drawSize, false);

    }
}

/**
 * Draw using single color
 *
 * @author andresoviedo
 */
class Object3DV1 extends Object3DImpl {

//    private final static String vertexShaderCode =
//            "uniform mat4 u_MVPMatrix;" +
//                    "attribute vec4 a_Position;" +
//                    "void main() {" +
//                    "  gl_Position = u_MVPMatrix * a_Position;\n" +
//                    "  gl_PointSize = 20.0;  \n" +
//                    "}";

//    private final static String fragmentShaderCode =
//            "precision mediump float;" +
//                    "uniform vec4 vColor;" +
//                    "void main() {" +
//                    "  gl_FragColor = vColor;" +
//                    "}";

    private final static String vertexShaderCode =
            "uniform mat4 u_MVPMatrix;" +
                    "uniform mat4 uMMatrix;" +
                    "uniform vec3 uLightLocation;" +
                    "uniform vec3 uCamera;" +
                    "attribute vec3 aPosition;" +
                    "attribute vec3 aNormal;" +
                    "varying vec4 ambient;" +
                    "varying vec4 diffuse;" +
                    "varying vec4 specular;" +

                    "void pointLight(\n" +
                    "  in vec3 normal," +
                    "  inout vec4 ambient," +
                    "  inout vec4 diffuse," +
                    "  inout vec4 specular," +
                    "  in vec3 lightLocation," +
                    "  in vec4 lightAmbient," +
                    "  in vec4 lightDiffuse," +
                    "  in vec4 lightSpecular" +
                    "){\n" +
                    "  ambient=lightAmbient;" +
                    "  vec3 normalTarget=aPosition+normal;" +
                    "  vec3 newNormal=(uMMatrix*vec4(normalTarget,1)).xyz-(uMMatrix*vec4(aPosition,1)).xyz;" +
                    "  newNormal=normalize(newNormal);" +
                    "  vec3 eye= normalize(uCamera-(uMMatrix*vec4(aPosition,1)).xyz);" +
                    "  vec3 vp= normalize(lightLocation-(uMMatrix*vec4(aPosition,1)).xyz);" +
                    "  vp=normalize(vp);" +
                    "  vec3 halfVector=normalize(vp+eye);" +
                    "  float shininess=50.0;" +
                    "  float nDotViewPosition=max(0.0,dot(newNormal,vp));" +
                    "  diffuse=lightDiffuse*nDotViewPosition;" +
                    "  float nDotViewHalfVector=dot(newNormal,halfVector);" +
                    "  float powerFactor=max(0.0,pow(nDotViewHalfVector,shininess)); " +
                    "  specular=lightSpecular*powerFactor;" +
                    "}" +
                    "void main() {" +
                    "  gl_Position = u_MVPMatrix * a_Position;\n" +
                    "  gl_PointSize = 20.0;  \n" +
                    "}";

    private final static String fragmentShaderCode =
            "precision mediump float;" +
                    "varying vec4 ambient;" +
                    "varying vec4 diffuse;" +
                    "varying vec4 specular;" +
                    "uniform vec3 uColor;" +
                    "uniform float uOpacity;" +
                    "void main()" +
                    "{\n" +
                    "    vec4 finalColor=vec4(uColor,1.0);\n" +
                    "    finalColor.a *= uOpacity;\n" +
                    "    gl_FragColor = finalColor*ambient+finalColor*specular+finalColor*diffuse;\n" +
                    "}";

    public Object3DV1() {
        super("V1", vertexShaderCode, fragmentShaderCode, "a_Position");
    }

    public Object3DV1(ModelSurfaceView modelSurfaceView) {
        super("V1", vertexShaderCode, fragmentShaderCode, modelSurfaceView, "a_Position");
    }

    @Override
    protected boolean supportsColors() {
        return false;
    }
}

/**
 * Drawer using multiple colors & !light & !texture
 *
 * @author andresoviedo
 */
class Object3DV2 extends Object3DImpl {
    // @formatter:off
    private final static String vertexShaderCode =
            "uniform mat4 u_MVPMatrix;" +
                    "attribute vec4 a_Position;" +
                    "attribute vec4 a_Color;" +
                    "varying vec4 vColor;" +
                    "void main() {" +
                    "  vColor = a_Color;" +
                    "  gl_Position = u_MVPMatrix * a_Position;" +
                    "  gl_PointSize = 2.5;  \n" +
                    "}";
    // @formatter:on

    // @formatter:off
    private final static String fragmentShaderCode =
            "precision mediump float;" +
                    "varying vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";
    // @formatter:on

    public Object3DV2() {
        super("V2", vertexShaderCode, fragmentShaderCode, "a_Position", "a_Color");
    }

    @Override
    protected boolean supportsColors() {
        return true;
    }
}

/**
 * Drawer using single color & textures & !light
 *
 * @author andresoviedo
 */
class Object3DV3 extends Object3DImpl {
    // @formatter:off
    private final static String vertexShaderCode =
            "uniform mat4 u_MVPMatrix;" +
                    "attribute vec4 a_Position;" +
                    "attribute vec2 a_TexCoordinate;" + // Per-vertex texture coordinate information we will pass in.
                    "varying vec2 v_TexCoordinate;" +   // This will be passed into the fragment shader.
                    "void main() {" +
                    "  v_TexCoordinate = a_TexCoordinate;" +
                    "  gl_Position = u_MVPMatrix * a_Position;" +
                    "  gl_PointSize = 2.5;  \n" +
                    "}";
    // @formatter:on

    // @formatter:off
    private final static String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "uniform sampler2D u_Texture;" +
                    "varying vec2 v_TexCoordinate;" +
                    "void main() {" +
                    "  gl_FragColor = vColor * texture2D(u_Texture, v_TexCoordinate);" +
                    "}";
    // @formatter:on

    public Object3DV3() {
        super("V3", vertexShaderCode, fragmentShaderCode, "a_Position", "a_TexCoordinate");
    }

    @Override
    protected boolean supportsTextures() {
        return true;
    }
}

/**
 * Drawer using textures & colors & !light
 *
 * @author andresoviedo
 */
class Object3DV4 extends Object3DImpl {
    // @formatter:off
    protected final static String vertexShaderCode =
            "uniform mat4 u_MVPMatrix;" +
                    "attribute vec4 a_Position;" +
                    "attribute vec4 a_Color;" +
                    "varying vec4 vColor;" +
                    "attribute vec2 a_TexCoordinate;" +
                    "varying vec2 v_TexCoordinate;" +
                    "void main() {" +
                    "  vColor = a_Color;" +
                    "  v_TexCoordinate = a_TexCoordinate;" +
                    "  gl_Position = u_MVPMatrix * a_Position;" +
                    "  gl_PointSize = 2.5;  \n" +
                    "}";
    // @formatter:on

    // @formatter:off
    protected final static String fragmentShaderCode =
            "precision mediump float;" +
                    "varying vec4 vColor;" +
                    "uniform sampler2D u_Texture;" +
                    "varying vec2 v_TexCoordinate;" +
                    "void main() {" +
                    "  gl_FragColor = vColor * texture2D(u_Texture, v_TexCoordinate);" +
                    "}";
    // @formatter:on

    public Object3DV4() {
        super("V4", vertexShaderCode, fragmentShaderCode, "a_Position", "a_Color", "a_TexCoordinate");
    }

    @Override
    protected boolean supportsColors() {
        return true;
    }

    @Override
    protected boolean supportsTextures() {
        return true;
    }

}

/**
 * Drawer using colors & lights & no texture
 *
 * @author andresoviedo
 */
class Object3DV5 extends Object3DImpl {
    // @formatter:off
    private final static String vertexShaderCode =
            "uniform mat4 u_MVPMatrix;\n" +
                    "attribute vec4 a_Position;\n" +
                    // light variables
                    "uniform mat4 u_MVMatrix;\n" +
                    "uniform vec3 u_LightPos;\n" +
                    "attribute vec4 a_Color;\n" +
                    "attribute vec3 a_Normal;\n" +
                    // calculated color
                    "varying vec4 v_Color;\n" +
                    "void main() {\n" +
                    // Transform the vertex into eye space.
                    "   vec3 modelViewVertex = vec3(u_MVMatrix * a_Position);\n          " +
                    // Get a lighting direction vector from the light to the vertex.
                    "   vec3 lightVector = normalize(u_LightPos - modelViewVertex);\n    " +
                    // Transform the normal's orientation into eye space.
                    "   vec3 modelViewNormal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));\n " +
                    // Calculate the dot product of the light vector and vertex normal. If the normal and light vector are
                    // pointing in the same direction then it will get max illumination.
                    "   float diffuse = max(dot(modelViewNormal, lightVector), 0.1);\n   " +
                    // Attenuate the light based on distance.
                    "   float distance = length(u_LightPos - modelViewVertex);\n         " +
                    "   diffuse = diffuse * (1.0 / (1.0 + (0.05 * distance * distance)));\n" +
                    //  Add ambient lighting
                    "  diffuse = diffuse + 0.5;" +
                    // Multiply the color by the illumination level. It will be interpolated across the triangle.
                    "   v_Color = a_Color * diffuse;\n" +
                    "   v_Color[3] = a_Color[3];" + // correct alpha
                    "  gl_Position = u_MVPMatrix * a_Position;\n" +
                    "  gl_PointSize = 2.5;  \n" +
                    "}";
    // @formatter:on

    // @formatter:off
    private final static String fragmentShaderCode =
            "precision mediump float;\n" +
                    "varying vec4 v_Color;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = v_Color;\n" +
                    "}";
    // @formatter:on

    public Object3DV5() {
        super("V5", vertexShaderCode, fragmentShaderCode, "a_Position", "a_Color", "a_Normal");
    }

    @Override
    protected boolean supportsColors() {
        return true;
    }

    @Override
    protected boolean supportsNormals() {
        return true;
    }

    @Override
    protected boolean supportsLighting() {
        return true;
    }

    @Override
    protected boolean supportsMvMatrix() {
        return true;
    }

}

/**
 * Drawer using colors, textures & lights
 *
 * @author andres
 */
class Object3DV6 extends Object3DImpl {
    // @formatter:off
    private final static String vertexShaderCode =
            "uniform mat4 u_MVPMatrix;\n" +
                    "attribute vec4 a_Position;\n" +
                    // texture variables
                    "attribute vec2 a_TexCoordinate;" +
                    "varying vec2 v_TexCoordinate;" +
                    // light variables
                    "uniform mat4 u_MVMatrix;\n" +
                    "uniform vec3 u_LightPos;\n" +
                    "attribute vec4 a_Color;\n" +
                    "attribute vec3 a_Normal;\n" +
                    // calculated color
                    "varying vec4 v_Color;\n" +
                    "void main() {\n" +
                    // texture
                    "  v_TexCoordinate = a_TexCoordinate;" +
                    // Transform the vertex into eye space.
                    "   vec3 modelViewVertex = vec3(u_MVMatrix * a_Position);\n          " +
                    // Get a lighting direction vector from the light to the vertex.
                    "   vec3 lightVector = normalize(u_LightPos - modelViewVertex);\n    " +
                    // Transform the normal's orientation into eye space.
                    "   vec3 modelViewNormal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));\n " +
                    // Calculate the dot product of the light vector and vertex normal. If the normal and light vector are
                    // pointing in the same direction then it will get max illumination.
                    "   float diffuse = max(dot(modelViewNormal, lightVector), 0.1);\n   " +
                    // Attenuate the light based on distance.
                    "   float distance = length(u_LightPos - modelViewVertex);\n         " +
                    "   diffuse = diffuse * (1.0 / (1.0 + (0.05 * distance * distance)));\n" +
                    //  Add ambient lighting
                    "  diffuse = diffuse + 0.5;" +
                    // Multiply the color by the illumination level. It will be interpolated across the triangle.
                    "   v_Color = a_Color * diffuse;\n" +
                    "   v_Color[3] = a_Color[3];" + // correct alpha
                    "  gl_Position = u_MVPMatrix * a_Position;\n" +
                    "  gl_PointSize = 2.5;  \n" +
                    "}";
    // @formatter:on

    // @formatter:off
    private final static String fragmentShaderCode =
            "precision mediump float;\n" +
                    "varying vec4 v_Color;\n" +
                    // textures
                    "uniform sampler2D u_Texture;" +
                    "varying vec2 v_TexCoordinate;" +
                    //
                    "void main() {\n" +
                    "  gl_FragColor = v_Color * texture2D(u_Texture, v_TexCoordinate);" +
                    "}";
    // @formatter:on

    public Object3DV6() {
        super("V6", vertexShaderCode, fragmentShaderCode, "a_Position", "a_Color", "a_TexCoordinate", "a_Normal");
    }

    @Override
    protected boolean supportsColors() {
        return true;
    }

    @Override
    protected boolean supportsTextures() {
        return true;
    }

    @Override
    protected boolean supportsNormals() {
        return true;
    }

    @Override
    protected boolean supportsLighting() {
        return true;
    }

    @Override
    protected boolean supportsMvMatrix() {
        return true;
    }

}

/**
 * Drawer using color & lights & !texture
 *
 * @author andresoviedo
 */
class Object3DV7 extends Object3DImpl {
    // @formatter:off
    private final static String vertexShaderCode =
            "uniform mat4 u_MVPMatrix;\n" +
                    "attribute vec4 a_Position;\n" +
                    // color
                    "uniform vec4 vColor;\n" +
                    // light variables
                    "uniform mat4 u_MVMatrix;\n" +
                    "uniform vec3 u_LightPos;\n" +
                    "attribute vec3 a_Normal;\n" +
                    // calculated color
                    "varying vec4 v_Color;\n" +
                    "void main() {\n" +
                    // Transform the vertex into eye space.
                    "   vec3 modelViewVertex = vec3(u_MVMatrix * a_Position);\n          " +
                    // Get a lighting direction vector from the light to the vertex.
                    "   vec3 lightVector = normalize(u_LightPos - modelViewVertex);\n    " +
                    // Transform the normal's orientation into eye space.
                    "   vec3 modelViewNormal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));\n " +
                    // Calculate the dot product of the light vector and vertex normal. If the normal and light vector are
                    // pointing in the same direction then it will get max illumination.
                    "   float diffuse = max(dot(modelViewNormal, lightVector), 0.1);\n   " +
                    // Attenuate the light based on distance.
                    "   float distance = length(u_LightPos - modelViewVertex);\n         " +
                    "   diffuse = diffuse * (1.0 / (1.0 + (0.05 * distance * distance)));\n" +
                    //  Add ambient lighting
                    "  diffuse = diffuse + 0.5;" +
                    // Multiply the color by the illumination level. It will be interpolated across the triangle.
                    "   v_Color = vColor * diffuse;\n" +
                    "   v_Color[3] = vColor[3];" + // correct alpha
                    "  gl_Position = u_MVPMatrix * a_Position;\n" +
                    "  gl_PointSize = 2.5;  \n" +
                    "}";
    // @formatter:on

    // @formatter:off
    private final static String fragmentShaderCode =
            "precision mediump float;\n" +
                    // calculated color
                    "varying vec4 v_Color;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = v_Color;\n" +
                    "}";
    // @formatter:on

    public Object3DV7() {
        super("V7", vertexShaderCode, fragmentShaderCode, "a_Position", "a_Normal");
    }

    @Override
    protected boolean supportsColors() {
        return false;
    }

    @Override
    protected boolean supportsNormals() {
        return true;
    }

    @Override
    protected boolean supportsLighting() {
        return true;
    }

    @Override
    protected boolean supportsMvMatrix() {
        return true;
    }

}

/**
 * Draw using single color and skeleton and light and no texture
 *
 * @author andresoviedo
 */
class Object3DV10 extends Object3DImpl {

    // @formatter:off
    private final static String vertexShaderCode =
            "const int MAX_JOINTS = 60;\n"
                    + "const int MAX_WEIGHTS = 3;\n"
                    + "uniform mat4 u_MVPMatrix;      \n"
                    + "attribute vec4 a_Position;     \n"
                    + "attribute vec3 in_jointIndices;\n"
                    + "attribute vec3 in_weights;\n"
                    + "uniform mat4 jointTransforms[MAX_JOINTS];\n"
                    // light variables
                    + "uniform mat4 u_MVMatrix;\n"
                    + "uniform vec3 u_LightPos;\n"
                    + "attribute vec3 a_Normal;\n"
                    + "uniform vec4 vColor;\n"
                    + "varying vec4 v_Color;\n"
                    + "void main()                    \n"
                    + "{                              \n"
                    + "  vec4 totalLocalPos = vec4(0.0);\n"
                    + "  vec4 totalNormal = vec4(0.0);\n"

		/*+ "  for(int i=0;i<MAX_WEIGHTS;i++){\n"
		+ "    mat4 jointTransform = jointTransforms[in_jointIndices[i]];\n"
		+ "    vec4 posePosition = jointTransform * a_Position;\n"
		+ "    totalLocalPos += posePosition * in_weights[i];\n"
		+ "  }\n"*/

                    + "    mat4 jointTransform = jointTransforms[int(in_jointIndices[0])];\n"
                    + "    vec4 posePosition = jointTransform * a_Position;\n"
                    + "    totalLocalPos += posePosition * in_weights[0];\n"

                    + "    jointTransform = jointTransforms[int(in_jointIndices[1])];\n"
                    + "    posePosition = jointTransform * a_Position;\n"
                    + "    totalLocalPos += posePosition * in_weights[1];\n"

                    + "    jointTransform = jointTransforms[int(in_jointIndices[2])];\n"
                    + "    posePosition = jointTransform * a_Position;\n"
                    + "    totalLocalPos += posePosition * in_weights[2];\n"


                    + "  gl_Position = u_MVPMatrix * totalLocalPos;\n"
                    + "  gl_PointSize = 2.5;         \n"

                    + "   vec3 modelViewVertex = vec3(u_MVMatrix * a_Position);\n          " +
                    // Get a lighting direction vector from the light to the vertex.
                    "   vec3 lightVector = normalize(u_LightPos - modelViewVertex);\n    " +
                    // Transform the normal's orientation into eye space.
                    "   vec3 modelViewNormal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));\n " +
                    // Calculate the dot product of the light vector and vertex normal. If the normal and light vector are
                    // pointing in the same direction then it will get max illumination.
                    "   float diffuse = max(dot(modelViewNormal, lightVector), 0.1);\n   " +
                    // Attenuate the light based on distance.
                    "   float distance = length(u_LightPos - modelViewVertex);\n         " +
                    "   diffuse = diffuse * (1.0 / (1.0 + (0.05 * distance * distance)));\n" +
                    //  Add ambient lighting
                    "  diffuse = diffuse + 0.5;" +
                    // Multiply the color by the illumination level. It will be interpolated across the triangle.
                    "   v_Color = vColor * diffuse;\n" +
                    "   v_Color[3] = vColor[3];" // correct alpha
                    + "}                              \n";
    // @formatter:on

    // @formatter:off
    private final static String fragmentShaderCode =
            "precision mediump float;\n" +
                    "varying vec4 v_Color;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = v_Color;\n" +
                    "}";
    // @formatter:on

    public Object3DV10() {
        super("V10", vertexShaderCode, fragmentShaderCode, "a_Position", "in_jointIndices", "in_weights",
                "jointTransforms", "a_Normal", "vColor");
    }

    @Override
    public void draw(Object3DData obj, float[] pMatrix, float[] vMatrix, int drawMode, int drawSize, int textureId,
                     float[] lightPos) {


        AnimatedModel animatedModel = (AnimatedModel) obj;

        GLES20.glUseProgram(mProgram);

        int in_weightsHandle = GLES20.glGetAttribLocation(mProgram, "in_weights");
        GLUtil.checkGlError("glGetAttribLocation");
        if (in_weightsHandle < 0) {
            throw new RuntimeException("handle 'in_weights' not found");
        }
        GLES20.glEnableVertexAttribArray(in_weightsHandle);
        GLUtil.checkGlError("glEnableVertexAttribArray");
        animatedModel.getVertexWeights().position(0);
        GLES20.glVertexAttribPointer(in_weightsHandle, 3, GLES20.GL_FLOAT, false, 0, animatedModel.getVertexWeights());
        GLUtil.checkGlError("glVertexAttribPointer");

        int in_jointIndicesHandle = GLES20.glGetAttribLocation(mProgram, "in_jointIndices");
        GLUtil.checkGlError("glGetAttribLocation");
        if (in_jointIndicesHandle < 0) {
            throw new RuntimeException("handle 'in_jointIndicesHandle' not found");
        }
        GLES20.glEnableVertexAttribArray(in_jointIndicesHandle);
        GLUtil.checkGlError("glEnableVertexAttribArray");
        animatedModel.getJointIds().position(0);
        GLES20.glVertexAttribPointer(in_jointIndicesHandle, 3, GLES20.GL_FLOAT, false, 0, animatedModel.getJointIds());
        GLUtil.checkGlError("glVertexAttribPointer");


        float[][] jointTransformsArray = animatedModel.getJointTransforms();
        // get handle to fragment shader's vColor member

        List<Integer> handles = new ArrayList<>();
        for (int i = 0; i < jointTransformsArray.length; i++) {
            float[] jointTransforms = jointTransformsArray[i];
            int jointTransformsHandle = GLES20.glGetUniformLocation(mProgram, "jointTransforms[" + i + "]");
            if (jointTransformsHandle < 0) {
                throw new RuntimeException("handle 'jointTransformsHandle[" + i + "]' not found");
            }
            GLUtil.checkGlError("glGetUniformLocation");
            GLES20.glUniformMatrix4fv(jointTransformsHandle, 1, false, jointTransforms, 0);
            handles.add(jointTransformsHandle);
        }

        super.draw(obj, pMatrix, vMatrix, drawMode, drawSize, textureId, lightPos);

        GLES20.glDisableVertexAttribArray(in_weightsHandle);
        GLES20.glDisableVertexAttribArray(in_jointIndicesHandle);
        for (int i : handles) {
            //GLES20.glDisableVertexAttribArray(i);
        }
    }

    @Override
    protected boolean supportsNormals() {
        return true;
    }

    @Override
    protected boolean supportsLighting() {
        return true;
    }

    @Override
    protected boolean supportsMvMatrix() {
        return true;
    }
}

/**
 * Draw using multiple colors and skeleton and light and no texture
 *
 * @author andresoviedo
 */
class Object3DV11 extends Object3DImpl {

    // @formatter:off
    private final static String vertexShaderCode =
            "const int MAX_JOINTS = 60;\n"
                    + "const int MAX_WEIGHTS = 3;\n"
                    + "uniform mat4 u_MVPMatrix;      \n"
                    + "attribute vec4 a_Position;     \n"
                    + "attribute vec3 in_jointIndices;\n"
                    + "attribute vec3 in_weights;\n"
                    + "uniform mat4 jointTransforms[MAX_JOINTS];\n"
                    // light variables
                    + "uniform mat4 u_MVMatrix;\n"
                    + "uniform vec3 u_LightPos;\n"
                    + "attribute vec3 a_Normal;\n"
                    + "attribute vec4 a_Color;\n"
                    + "varying vec4 v_Color;\n"
                    + "void main()                    \n"
                    + "{                              \n"
                    + "  vec4 totalLocalPos = vec4(0.0);\n"
                    + "  vec4 totalNormal = vec4(0.0);\n"

                    + "    mat4 jointTransform = jointTransforms[int(in_jointIndices[0])];\n"
                    + "    vec4 posePosition = jointTransform * a_Position;\n"
                    + "    totalLocalPos += posePosition * in_weights[0];\n"

                    + "    jointTransform = jointTransforms[int(in_jointIndices[1])];\n"
                    + "    posePosition = jointTransform * a_Position;\n"
                    + "    totalLocalPos += posePosition * in_weights[1];\n"

                    + "    jointTransform = jointTransforms[int(in_jointIndices[2])];\n"
                    + "    posePosition = jointTransform * a_Position;\n"
                    + "    totalLocalPos += posePosition * in_weights[2];\n"


                    + "  gl_Position = u_MVPMatrix * totalLocalPos;\n"
                    + "  gl_PointSize = 2.5;         \n"

                    + "   vec3 modelViewVertex = vec3(u_MVMatrix * a_Position);\n          " +
                    // Get a lighting direction vector from the light to the vertex.
                    "   vec3 lightVector = normalize(u_LightPos - modelViewVertex);\n    " +
                    // Transform the normal's orientation into eye space.
                    "   vec3 modelViewNormal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));\n " +
                    // Calculate the dot product of the light vector and vertex normal. If the normal and light vector are
                    // pointing in the same direction then it will get max illumination.
                    "   float diffuse = max(dot(modelViewNormal, lightVector), 0.1);\n   " +
                    // Attenuate the light based on distance.
                    "   float distance = length(u_LightPos - modelViewVertex);\n         " +
                    "   diffuse = diffuse * (1.0 / (1.0 + (0.05 * distance * distance)));\n" +
                    //  Add ambient lighting
                    "  diffuse = diffuse + 0.5;" +
                    // Multiply the color by the illumination level. It will be interpolated across the triangle.
                    "   v_Color = a_Color * diffuse;\n" +
                    "   v_Color[3] = a_Color[3];" // correct alpha
                    + "}                              \n";
    // @formatter:on

    // @formatter:off
    private final static String fragmentShaderCode =
            "precision mediump float;\n" +
                    "varying vec4 v_Color;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = v_Color;\n" +
                    "}";
    // @formatter:on

    public Object3DV11() {
        super("V11", vertexShaderCode, fragmentShaderCode, "a_Position", "in_jointIndices", "in_weights",
                "jointTransforms", "a_Normal", "a_Color");
    }

    @Override
    public void draw(Object3DData obj, float[] pMatrix, float[] vMatrix, int drawMode, int drawSize, int textureId,
                     float[] lightPos) {


        AnimatedModel animatedModel = (AnimatedModel) obj;

        GLES20.glUseProgram(mProgram);

        int in_weightsHandle = GLES20.glGetAttribLocation(mProgram, "in_weights");
        GLUtil.checkGlError("glGetAttribLocation");
        if (in_weightsHandle < 0) {
            throw new RuntimeException("handle 'in_weights' not found");
        }
        GLES20.glEnableVertexAttribArray(in_weightsHandle);
        GLUtil.checkGlError("glEnableVertexAttribArray");
        animatedModel.getVertexWeights().position(0);
        GLES20.glVertexAttribPointer(in_weightsHandle, 3, GLES20.GL_FLOAT, false, 0, animatedModel.getVertexWeights());
        GLUtil.checkGlError("glVertexAttribPointer");

        int in_jointIndicesHandle = GLES20.glGetAttribLocation(mProgram, "in_jointIndices");
        GLUtil.checkGlError("glGetAttribLocation");
        if (in_jointIndicesHandle < 0) {
            throw new RuntimeException("handle 'in_jointIndicesHandle' not found");
        }
        GLES20.glEnableVertexAttribArray(in_jointIndicesHandle);
        GLUtil.checkGlError("glEnableVertexAttribArray");
        animatedModel.getJointIds().position(0);
        GLES20.glVertexAttribPointer(in_jointIndicesHandle, 3, GLES20.GL_FLOAT, false, 0, animatedModel.getJointIds());
        GLUtil.checkGlError("glVertexAttribPointer");


        float[][] jointTransformsArray = animatedModel.getJointTransforms();
        // get handle to fragment shader's vColor member

        List<Integer> handles = new ArrayList<Integer>();
        for (int i = 0; i < jointTransformsArray.length; i++) {
            float[] jointTransforms = jointTransformsArray[i];
            int jointTransformsHandle = GLES20.glGetUniformLocation(mProgram, "jointTransforms[" + i + "]");
            if (jointTransformsHandle < 0) {
                throw new RuntimeException("handle 'jointTransformsHandle[" + i + "]' not found");
            }
            GLUtil.checkGlError("glGetUniformLocation");
            GLES20.glUniformMatrix4fv(jointTransformsHandle, 1, false, jointTransforms, 0);
            handles.add(jointTransformsHandle);
        }

        super.draw(obj, pMatrix, vMatrix, drawMode, drawSize, textureId, lightPos);

        GLES20.glDisableVertexAttribArray(in_weightsHandle);
        GLES20.glDisableVertexAttribArray(in_jointIndicesHandle);
        for (int i : handles) {
            //GLES20.glDisableVertexAttribArray(i);
        }
    }

    @Override
    protected boolean supportsColors() {
        return true;
    }

    @Override
    protected boolean supportsNormals() {
        return true;
    }

    @Override
    protected boolean supportsLighting() {
        return true;
    }

    @Override
    protected boolean supportsMvMatrix() {
        return true;
    }
}

/**
 * Draw using single color and skeleton and no light no texture
 *
 * @author andresoviedo
 */
class Object3DV12 extends Object3DImpl {

    // @formatter:off
    private final static String vertexShaderCode =
            "const int MAX_JOINTS = 60;\n"
                    + "const int MAX_WEIGHTS = 3;\n"
                    + "uniform mat4 u_MVPMatrix;      \n"
                    + "attribute vec4 a_Position;     \n"
                    + "attribute vec3 in_jointIndices;\n"
                    + "attribute vec3 in_weights;\n"
                    + "uniform mat4 jointTransforms[MAX_JOINTS];\n"
                    + "uniform mat4 u_MVMatrix;\n"
                    + "uniform vec4 vColor;\n"
                    + "varying vec4 v_Color;\n"
                    + "void main()                    \n"
                    + "{                              \n"
                    + "  vec4 totalLocalPos = vec4(0.0);\n"
                    + "  vec4 totalNormal = vec4(0.0);\n"

		/*+ "  for(int i=0;i<MAX_WEIGHTS;i++){\n"
		+ "    mat4 jointTransform = jointTransforms[in_jointIndices[i]];\n"
		+ "    vec4 posePosition = jointTransform * a_Position;\n"
		+ "    totalLocalPos += posePosition * in_weights[i];\n"
		+ "  }\n"*/

                    + "    mat4 jointTransform = jointTransforms[int(in_jointIndices[0])];\n"
                    + "    vec4 posePosition = jointTransform * a_Position;\n"
                    + "    totalLocalPos = posePosition * in_weights[0];\n"

                    + "    jointTransform = jointTransforms[int(in_jointIndices[1])];\n"
                    + "    posePosition = jointTransform * a_Position;\n"
                    + "    totalLocalPos += posePosition * in_weights[1];\n"

                    + "    jointTransform = jointTransforms[int(in_jointIndices[2])];\n"
                    + "    posePosition = jointTransform * a_Position;\n"
                    + "    totalLocalPos += posePosition * in_weights[2];\n"


                    + "  gl_Position = u_MVPMatrix * totalLocalPos;\n"
                    + "  gl_PointSize = 2.5;         \n" +

                    "   v_Color = vColor;\n" +
                    "   v_Color[3] = vColor[3];" // correct alpha
                    + "}                              \n";
    // @formatter:on

    // @formatter:off
    private final static String fragmentShaderCode =
            "precision mediump float;\n" +
                    "varying vec4 v_Color;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = v_Color;\n" +
                    "}";
    // @formatter:on

    public Object3DV12() {
        super("V12", vertexShaderCode, fragmentShaderCode, "a_Position", "in_jointIndices", "in_weights",
                "jointTransforms", "vColor");
    }

    @Override
    public void draw(Object3DData obj, float[] pMatrix, float[] vMatrix, int drawMode, int drawSize, int textureId,
                     float[] lightPos) {


        AnimatedModel animatedModel = (AnimatedModel) obj;

        GLES20.glUseProgram(mProgram);

        int in_weightsHandle = GLES20.glGetAttribLocation(mProgram, "in_weights");
        GLUtil.checkGlError("glGetAttribLocation");
        if (in_weightsHandle < 0) {
            throw new RuntimeException("handle 'in_weights' not found");
        }
        GLES20.glEnableVertexAttribArray(in_weightsHandle);
        GLUtil.checkGlError("glEnableVertexAttribArray");
        animatedModel.getVertexWeights().position(0);
        GLES20.glVertexAttribPointer(in_weightsHandle, 3, GLES20.GL_FLOAT, false, 0, animatedModel.getVertexWeights());
        GLUtil.checkGlError("glVertexAttribPointer");

        int in_jointIndicesHandle = GLES20.glGetAttribLocation(mProgram, "in_jointIndices");
        GLUtil.checkGlError("glGetAttribLocation");
        if (in_jointIndicesHandle < 0) {
            throw new RuntimeException("handle 'in_jointIndicesHandle' not found");
        }
        GLES20.glEnableVertexAttribArray(in_jointIndicesHandle);
        GLUtil.checkGlError("glEnableVertexAttribArray");
        animatedModel.getJointIds().position(0);
        GLES20.glVertexAttribPointer(in_jointIndicesHandle, 3, GLES20.GL_FLOAT, false, 0, animatedModel.getJointIds());
        GLUtil.checkGlError("glVertexAttribPointer");


        float[][] jointTransformsArray = animatedModel.getJointTransforms();
        // get handle to fragment shader's vColor member

        List<Integer> handles = new ArrayList<>();
        for (int i = 0; i < jointTransformsArray.length; i++) {
            float[] jointTransforms = jointTransformsArray[i];
            int jointTransformsHandle = GLES20.glGetUniformLocation(mProgram, "jointTransforms[" + i + "]");
            if (jointTransformsHandle < 0) {
                throw new RuntimeException("handle 'jointTransformsHandle[" + i + "]' not found");
            }
            GLUtil.checkGlError("glGetUniformLocation");
            GLES20.glUniformMatrix4fv(jointTransformsHandle, 1, false, jointTransforms, 0);
            handles.add(jointTransformsHandle);
        }

        super.draw(obj, pMatrix, vMatrix, drawMode, drawSize, textureId, lightPos);

        GLES20.glDisableVertexAttribArray(in_weightsHandle);
        GLES20.glDisableVertexAttribArray(in_jointIndicesHandle);
        for (int i : handles) {
            //GLES20.glDisableVertexAttribArray(i);
        }
    }
}