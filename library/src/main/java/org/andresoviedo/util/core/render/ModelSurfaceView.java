package org.andresoviedo.util.core.render;

import android.content.Context;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.util.AttributeSet;

import org.andresoviedo.util.core.util.LeGLBaseScene;


/**
 * This is the actual opengl view. From here we can detect touch gestures for example
 *
 * @author andresoviedo
 */
public class ModelSurfaceView extends  LeGLBaseScene {

    private Context context;
    private ModelRenderer mRenderer;
    private Uri uri;
    private String url;
    private SceneLoader scene;

    // 宽
    private float mSceneWidth = 720;
    // 高
    private float mSceneHeight = 1280;

    public ModelSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public ModelSurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        this.context = context;
    }

    public ModelSurfaceView(Context context) {
        super(context);
        this.context = context;
    }


    public void setUri(Uri uri, String url) {
        this.uri = uri;
        this.url = url;
        initRender();
    }

    private void initRender() {

        // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2);

        this.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        //将背景透明，必须在render之前调用
        this.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        //貌似不设置置顶透明失效！
        this.setZOrderOnTop(true);

        // This is the actual renderer of the 3D space
        mRenderer = new ModelRenderer(this, url);
        setRenderer(mRenderer);

        // Render the view only when there is a change in the drawing data
       //被动渲染模式
//        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        //主动渲染模式
//        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        this.setSceneWidthAndHeight(this.getMeasuredWidth(),
                this.getMeasuredHeight());
    }

    public void setSceneWidthAndHeight(float mSceneWidth, float mSceneHeight) {
        this.mSceneWidth = mSceneWidth;
        this.mSceneHeight = mSceneHeight;
    }



    public void setScene(SceneLoader scene) {
        this.scene = scene;
    }

    public SceneLoader getScene() {
        return scene;
    }


    public ModelRenderer getModelRenderer() {
        return mRenderer;
    }

}