package org.andresoviedo.util.core.render;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import org.andresoviedo.android_3d_model_engine.animation.Animator;
import org.andresoviedo.android_3d_model_engine.drawer.DrawerFactory;
import org.andresoviedo.android_3d_model_engine.model.Camera;
import org.andresoviedo.android_3d_model_engine.model.Object3D;
import org.andresoviedo.android_3d_model_engine.model.Object3DData;
import org.andresoviedo.util.core.util.MatrixState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES10.GL_LIGHTING;

public class ModelRenderer implements GLSurfaceView.Renderer {

    private final static String TAG = "ailibin";

    private ModelSurfaceView modelSurfaceView;
    // width of the screen
    private int width;
    // height of the screen
    private int height;
    // frustrum - nearest pixel
//    private static final float near = 1f;
    private static final float near = 2f;
    // frustrum - fartest pixel
//    private static final float far = 100f;
    private static final float far = 1000f;

    private DrawerFactory drawer;
    // The loaded textures
    private Map<byte[], Integer> textures = new HashMap<byte[], Integer>();

    // 3D matrices to project our 3D world
    private final float[] modelProjectionMatrix = new float[16];
    private final float[] modelViewMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // light position required to render with lighting
    private final float[] lightPosInEyeSpace = new float[4];
    /**
     * Whether the info of the model has been written to console log
     */
    private boolean infoLogged = false;

    /**
     * Skeleton Animator
     */
    private Animator animator = new Animator();

    private String url;

    /**
     * Construct a new renderer for the specified surface view
     *
     * @param modelSurfaceView the 3D window
     */
    public ModelRenderer(ModelSurfaceView modelSurfaceView, String url) {
        this.modelSurfaceView = modelSurfaceView;
        this.url = url;
    }

    /**
     * 近平面2~-22之间
     * @return
     */
    public float getNear() {
        return near;
    }

    public float getFar() {
        return far;
    }

    private float[] backgroundColor = new float[]{0f, 0f, 0f, 1.0f};

    public boolean isUseMatrixState = true;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        //使用原色
        if (isUseMatrixState) {
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            // 设置为打开背面剪裁
            GLES20.glEnable(GLES20.GL_CULL_FACE);
            // 初始化变换矩阵
            MatrixState.setInitStack();
            //初始化定位光光源
            MatrixState.setLightLocation(40, 10, 10);
        } else {
            //深度检查
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            //开启混合
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        }

        // This component will draw the actual models using OpenGL
        drawer = new DrawerFactory();
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {

        this.width = width;
        this.height = height;

        // Adjust the viewport based on geometry changes, such as screen rotation
        GLES20.glViewport(0, 0, width, height);

        // INFO: Set the camera position (View matrix)
        // The camera has 3 vectors (the position, the vector where we are looking at, and the up position (sky)
        SceneLoader scene = modelSurfaceView.getScene();
        Camera camera = scene.getCamera();

        // the projection matrix is the 3D virtual space (cube) that we want to project
        float ratio = (float) width / height;
        Log.d(TAG, "projection: [" + -ratio + "," + ratio + ",-1,1]-near/far[1,10]");

        if (isUseMatrixState) {
            // camera位置设置
            MatrixState.setCamera(camera.xPos, camera.yPos, camera.zPos,
                    camera.xView, camera.yView, camera.zView,
                    camera.xUp, camera.yUp, camera.zUp);
            //调用此方法计算产生透视投影矩阵
            MatrixState.setProjectFrustum(-ratio, ratio, -1, 1,
                    getNear(), getFar());
        } else {

            Matrix.setLookAtM(modelViewMatrix, 0, camera.xPos, camera.yPos, camera.zPos, camera.xView, camera.yView,
                    camera.zView, camera.xUp, camera.yUp, camera.zUp);
            //调用此方法计算产生透视投影矩阵
            Matrix.frustumM(modelProjectionMatrix, 0, -ratio, ratio, -1, 1, getNear(), getFar());
            // Calculate the projection and view transformation(计算投影和视图转换)
            Matrix.multiplyMM(mvpMatrix, 0, modelProjectionMatrix, 0, modelViewMatrix, 0);

        }


    }

    @Override
    public void onDrawFrame(GL10 gl) {

        // Draw background color，这一句代码必须加上,不然背景很乱, 清除深度缓冲与颜色缓冲
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (isUseMatrixState) {
            //开启混合
            gl.glEnable(GL10.GL_BLEND);
            gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        } else {
            gl.glEnable(GL_LIGHTING);
        }


        SceneLoader scene = modelSurfaceView.getScene();
        if (scene == null) {
            return;
        }

        // animate scene
//        scene.onDrawFrame();

        // recalculate mvp matrix according to where we are looking at now
        Camera camera = scene.getCamera();
        if (camera.hasChanged()) {

            if (isUseMatrixState) {
                // camera位置设置
                MatrixState.setCamera(camera.xPos, camera.yPos, camera.zPos,
                        camera.xView, camera.yView, camera.zView,
                        camera.xUp, camera.yUp, camera.zUp);
                //矩阵的计算
                camera.setChanged(false);
            } else {

                Matrix.setLookAtM(modelViewMatrix, 0, camera.xPos, camera.yPos, camera.zPos, camera.xView, camera.yView,
                        camera.zView, camera.xUp, camera.yUp, camera.zUp);
                Log.e(TAG, "Changed! :" + camera.ToStringVector());
                Matrix.multiplyMM(mvpMatrix, 0, modelProjectionMatrix, 0, modelViewMatrix, 0);
                camera.setChanged(false);

            }
        }

//        // draw light
//        if (scene.isDrawLighting()) {
//
//            Object3DImpl lightBulbDrawer = (Object3DImpl) drawer.getPointDrawer();
//            float[] lightModelViewMatrix = lightBulbDrawer.getMvMatrix(lightBulbDrawer.getMMatrix(scene.getLightBulb()), modelViewMatrix);
//            // Calculate position of the light in eye space to support lighting
//            Matrix.multiplyMV(lightPosInEyeSpace, 0, lightModelViewMatrix, 0, scene.getLightPosition(), 0);
//            // Draw a point that represents the light bulb
//            lightBulbDrawer.draw(scene.getLightBulb(), modelProjectionMatrix, modelViewMatrix, -1, lightPosInEyeSpace);
//
//        }

        List<Object3DData> objects = scene.getObjects();
        for (int i = 0; i < objects.size(); i++) {
            Object3DData objData = null;
            try {
                objData = objects.get(i);

                Object3D drawerObject = drawer.getDrawer(objData, false, scene.isDrawLighting(),
                        scene.isDrawAnimation(), modelSurfaceView);

                if (!infoLogged) {
                    Log.e(TAG, "Using drawer " + drawerObject.getClass());
                    infoLogged = true;
                }

                Integer textureId = textures.get(objData.getTextureData());
                Log.e(TAG, "textureId " + textureId);

                if (isUseMatrixState) {
                    drawerObject.draw(objData, MatrixState.getPMatrix(), MatrixState.getVMatrix(),
                            textureId != null ? textureId : -1, lightPosInEyeSpace);
                } else {
                    drawerObject.draw(objData, modelProjectionMatrix, modelViewMatrix,
                            textureId != null ? textureId : -1, lightPosInEyeSpace);
                }

            } catch (Exception ex) {
                Log.e("ModelRenderer", "There was a problem rendering the object '" + objData.getId() + "':" + ex.getMessage(), ex);
            }
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public float[] getModelProjectionMatrix() {
        return modelProjectionMatrix;
    }

    public float[] getModelViewMatrix() {
        return modelViewMatrix;
    }
}