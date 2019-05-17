package org.andresoviedo.util.core.render;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import org.andresoviedo.android_3d_model_engine.animation.Animator;
import org.andresoviedo.android_3d_model_engine.model.Camera;
import org.andresoviedo.android_3d_model_engine.model.Object3DData;
import org.andresoviedo.android_3d_model_engine.services.LoaderTask;
import org.andresoviedo.android_3d_model_engine.services.Object3DBuilder;
import org.andresoviedo.android_3d_model_engine.services.collada.ColladaLoaderTask;
import org.andresoviedo.android_3d_model_engine.services.stl.STLLoaderTask;
import org.andresoviedo.android_3d_model_engine.services.wavefront.WavefrontLoaderTask;
import org.andresoviedo.util.android.ContentUtils;
import org.andresoviedo.util.io.IOUtils;
import org.andresoviedo.util.core.util.ObjLoaderUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This class loads a 3D scena as an example of what can be done with the app
 *
 * @author andresoviedo
 */
public class SceneLoader implements LoaderTask.Callback /*LoaderTask1.Callback8*/ {

    /**
     * Default model color: yellow
     */
    private static float[] DEFAULT_COLOR = {1.0f, 1.0f, 0, 1.0f};
    /**
     * Parent component
     */
    /**
     * List of data objects containing info for building the opengl objects
     */
    private List<Object3DData> objects = new ArrayList<Object3DData>();
    /**
     * Point of view camera
     */
    private Camera camera;
    /**
     * Light toggle feature: we have 3 states: no light, light, light + rotation
     */
    private boolean rotatingLight = true;
    /**
     * Light toggle feature: whether to draw using lights
     */
    private boolean drawLighting = false;
    /**
     * Animate model (dae only) or not
     */
    private boolean animateModel = true;
    /**
     * Whether to draw using textures
     */
    private boolean drawTextures = true;
    /**
     * Initial light position
     */
    private final float[] lightPosition = new float[]{0, 0, 6, 1};
    /**
     * Light bulb 3d data
     */
    private final Object3DData lightPoint = Object3DBuilder.buildPoint(lightPosition).setId("light");
    /**
     * Animator
     */
    private Animator animator = new Animator();
    /**
     * Did the user touched the model for the first time?
     */
    private boolean userHasInteracted;
    /**
     * time when model loading has started (for stats)
     */
    private long startTime;
    /**
     * Object selected by the user
     */
    private Object3DData selectedObject = null;
    /**
     * Toggle collision detection
     */
    private boolean isCollision = false;

    private Uri mUri;
    private String mUrl;
    private Context context;
    private ModelSurfaceView modelSurfaceView;
    private static final String TAG = "ailibin";
    private List<ObjLoaderUtil.ObjData> objectsData = new ArrayList();

    public SceneLoader(String url, Uri uri, Context context, ModelSurfaceView modelSurfaceView) {
        mUri = uri;
        mUrl = url;
        this.context = context;
        this.modelSurfaceView = modelSurfaceView;
    }

    public ModelSurfaceView getModelSurfaceView() {
        return modelSurfaceView;
    }

    public void init() {

        // Set up ContentUtils so referenced materials and/or textures could be find
        // Camera to show a point of view
        camera = new Camera();
        startTime = SystemClock.uptimeMillis();

        Log.e(TAG, "Loading model " + mUri + ". async and parallel..");
        if (mUri.toString().toLowerCase().endsWith(".obj")) {
            new WavefrontLoaderTask(context, mUri, this).execute();
        } else if (mUri.toString().toLowerCase().endsWith(".stl")) {
            Log.e(TAG, "Loading STL object from: " + mUri);
            new STLLoaderTask(context, mUri, this).execute();
        } else if (mUri.toString().toLowerCase().endsWith(".dae")) {
            Log.e(TAG, "Loading Collada object from: " + mUri);
            new ColladaLoaderTask(context, mUri, this).execute();
        }
    }

    public Camera getCamera() {
        return camera;
    }

    private void makeToastText(final String text, final int toastDuration) {
        ((Activity) context).runOnUiThread(() -> Toast.makeText(context, text, toastDuration).show());
    }

    /**
     * Hook for animating the objects before the rendering
     */
    public void onDrawFrame() {

        animateLight();

        // smooth camera transition
        camera.animate();

        // initial camera animation. animate if user didn't touch the screen
        if (!userHasInteracted) {
            animateCamera();
        }

        if (objects.isEmpty()) {
            return;
        }

        if (animateModel) {
            for (int i = 0; i < objects.size(); i++) {
                Object3DData obj = objects.get(i);
                animator.update(obj);
            }
        }
    }

    private void animateLight() {
        if (!rotatingLight) {
            return;
        }
        // animate light - Do a complete rotation every 5 seconds.
        long time = SystemClock.uptimeMillis() % 5000L;
        float angleInDegrees = (360.0f / 5000.0f) * ((int) time);
        lightPoint.setRotationY(angleInDegrees);
    }

    private void animateCamera() {
        camera.translateCamera(0.0025f, 0f);
    }

    synchronized void addObject(Object3DData obj) {
        List<Object3DData> newList = new ArrayList<Object3DData>(objects);
        newList.add(obj);
        this.objects = newList;
        requestRender();
    }

    private void requestRender() {
        // request render only if GL view is already initialized
        if (modelSurfaceView != null) {
            modelSurfaceView.requestRender();
        }
    }

    public synchronized List<Object3DData> getObjects() {
        return objects;
    }

    public synchronized List<ObjLoaderUtil.ObjData> getObjectDatas() {
        return objectsData;
    }


    public boolean isDrawAnimation() {
        return animateModel;
    }

    public boolean isDrawLighting() {
        return drawLighting;
    }

    public boolean isDrawTextures() {
        return drawTextures;
    }

    public void toggleCollision() {
        this.isCollision = !isCollision;
        makeToastText("Collisions: " + isCollision, Toast.LENGTH_SHORT);
    }

    public boolean isCollision() {
        return isCollision;
    }

    public Object3DData getSelectedObject() {
        return selectedObject;
    }

    private void setSelectedObject(Object3DData selectedObject) {
        this.selectedObject = selectedObject;
    }

    public Object3DData getLightBulb() {
        return lightPoint;
    }

    public float[] getLightPosition() {
        return lightPosition;
    }

    @Override
    public void onStart() {
        ContentUtils.setThreadActivity((Activity) context);
    }

    @Override
    public void onLoadComplete(List<Object3DData> datas) {
        for (Object3DData data : datas) {
            if (data.getTextureData() == null && data.getTextureFile() != null) {
                Log.e(TAG, "Loading texture... " + data.getTextureFile());
                try (InputStream stream = ContentUtils.getInputStream(data.getTextureFile())) {
                    if (stream != null) {
                        //添加纹理数据
                        data.setTextureData(IOUtils.read(stream));
                    }
                } catch (IOException ex) {
                    data.addError("Problem loading texture " + data.getTextureFile());
                }
            }
        }
        List<String> allErrors = new ArrayList<>();
        for (Object3DData data : datas) {
            addObject(data);
            allErrors.addAll(data.getErrors());
        }
        if (!allErrors.isEmpty()) {
            makeToastText(allErrors.toString(), Toast.LENGTH_LONG);
        }
        //提示不要
        final String elapsed = (SystemClock.uptimeMillis() - startTime) / 1000 + " secs";
//        makeToastText("Build complete (" + elapsed + ")", Toast.LENGTH_LONG);
        ContentUtils.setThreadActivity(null);
    }


    @Override
    public void onLoadError(Exception ex) {
        Log.e(TAG, ex.getMessage(), ex);
        makeToastText("There was a problem building the model: " + ex.getMessage(), Toast.LENGTH_LONG);
        ContentUtils.setThreadActivity(null);
    }

    public void loadTexture(Object3DData obj, Uri uri) throws IOException {
        if (obj == null && objects.size() != 1) {
            makeToastText("Unavailable", Toast.LENGTH_SHORT);
            return;
        }
        obj = obj != null ? obj : objects.get(0);
        obj.setTextureData(IOUtils.read(ContentUtils.getInputStream(uri)));
    }

    public void processMove(float dx1, float dy1) {
        userHasInteracted = true;
    }


}
