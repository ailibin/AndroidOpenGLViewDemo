package com.example.ailibin.androidopenglview;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.andresoviedo.android_3d_model_engine.model.Camera;
import org.andresoviedo.util.core.render.ModelSurfaceView;
import org.andresoviedo.util.core.render.SceneLoader;

import java.io.File;


/**
 * @author ailibin
 */
public class ThreeDView extends RelativeLayout implements View.OnTouchListener {

    public static final String TAG = "ailibin";
    public onTouchScale listener;
    private onClickListener onClicklistener;

    private ImageView imgViewThreeScale;
    private ImageView imgViewThreeMultidimensional, imgClose, imgMessage, imgLightness;
    private TextView tvLineOne, tvLineTwo, tvLineThree, tvLineFour;
    private RelativeLayout rlThreeView;
    private ModelSurfaceView mGLView;
    private Activity activity;
    private FrameLayout flMyGlScene;
    private View view;

    private int lastX;
    private int lastY;

    private String url;
    private Context context;
    private SceneLoader scene;

    public void setTouchListener(onTouchScale listener) {
        this.listener = listener;
    }

    public void setOnClickListener(onClickListener onClicklistener) {
        this.onClicklistener = onClicklistener;
    }

    public ThreeDView(Context context) {
        this(context, null);
        this.context = context;
    }

    public ThreeDView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ThreeDView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

    }

    public void setUrl(String url) {
        this.url = url;
        initView(context);
    }

    private void initView(Context context) {

        view = View.inflate(context, R.layout.view_three3view, this);
        imgViewThreeScale = (ImageView) this.findViewById(R.id.imgViewThreeScale);
        imgViewThreeMultidimensional = (ImageView) this.findViewById(R.id.imgViewThreeMultidimensional);
        imgClose = (ImageView) this.findViewById(R.id.imgClose);
        imgLightness = (ImageView) this.findViewById(R.id.imgLightness);
        rlThreeView = (RelativeLayout) this.findViewById(R.id.rlThreeView);
        imgMessage = (ImageView) this.findViewById(R.id.imgMessage);
        tvLineOne = (TextView) this.findViewById(R.id.tvLineOne);
        tvLineTwo = (TextView) this.findViewById(R.id.tvLineTwo);
        tvLineThree = (TextView) this.findViewById(R.id.tvLineThree);
        tvLineFour = (TextView) this.findViewById(R.id.tvLineFour);
        //初始化GLSurfaceView
        mGLView = (ModelSurfaceView) findViewById(R.id.mGLView);
        Uri uri = Uri.fromFile(new File(url));
        Log.e(TAG, "url: " + url);
        Log.e(TAG, "uri: " + uri);
        mGLView.setUri(uri, url);
        initSceneLoader(url, uri, context, mGLView);
        //获取焦点
        mGLView.requestFocus();
        //设置为可触控
        mGLView.setFocusableInTouchMode(true);
        view.setOnTouchListener(this);
        mGLView.setOnTouchListener(this);
        imgViewThreeScale.setOnTouchListener(this);
        imgViewThreeMultidimensional.setOnTouchListener(this);

        imgClose.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onClicklistener != null) {
                    onClicklistener.onClick();
                }
            }
        });

//        imgMessage.setOnClickListener(view -> {
//            if (onClicklistener != null) {
//                int last1 = url.lastIndexOf("kaiyan");
//                int last2 = url.lastIndexOf("/");
//                int id = Integer.parseInt(url.substring(last1 + 6, last2));
//                onClicklistener.onMessageClick(id);
//            }
//        });

    }

    /**
     * 创建场景
     */
    private void initSceneLoader(String url, Uri uri, Context context, ModelSurfaceView modelSurfaceView) {

        scene = new SceneLoader(url, uri, context, modelSurfaceView);
        scene.init();
        modelSurfaceView.setScene(scene);

    }


    public void setIsFocus(boolean isFocus) {
        view.setFocusable(isFocus);
        mGLView.setFocusable(isFocus);
        imgViewThreeScale.setFocusable(isFocus);
        imgViewThreeMultidimensional.setFocusable(isFocus);
    }

    public void setIsChoose(boolean isChoose) {
        if (!isChoose) {
            imgClose.setVisibility(View.GONE);
            imgMessage.setVisibility(View.GONE);
            imgViewThreeMultidimensional.setVisibility(View.GONE);
            imgViewThreeScale.setVisibility(View.GONE);
            tvLineOne.setVisibility(View.GONE);
            tvLineTwo.setVisibility(View.GONE);
            tvLineThree.setVisibility(View.GONE);
            tvLineFour.setVisibility(View.GONE);
            imgLightness.setVisibility(View.GONE);
        } else {
            imgClose.setVisibility(View.VISIBLE);
            imgMessage.setVisibility(View.VISIBLE);
            imgViewThreeMultidimensional.setVisibility(View.VISIBLE);
            imgViewThreeScale.setVisibility(View.VISIBLE);
            tvLineOne.setVisibility(View.VISIBLE);
            tvLineTwo.setVisibility(View.VISIBLE);
            tvLineThree.setVisibility(View.VISIBLE);
            tvLineFour.setVisibility(View.VISIBLE);
            imgLightness.setVisibility(View.VISIBLE);
        }
    }


    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    int left = 0, top = 0, right = 0, bottom = 0;

    private static final int TOUCH_STATUS_MOVING_WORLD = 5;
    private int pointerCount = 0;
    private float x1 = Float.MIN_VALUE;
    private float y1 = Float.MIN_VALUE;
    private float dx1 = Float.MIN_VALUE;
    private float dy1 = Float.MIN_VALUE;

    private boolean gestureChanged = false;
    private boolean moving = false;
    private boolean simpleTouch = false;
    private long lastActionTime;
    private int touchDelay = -2;
    private int touchStatus = -1;

    private float previousX1;
    private float previousY1;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {

        int x = (int) event.getX();
        int y = (int) event.getY();
        float scale = 1.0f;

        int max = Math.max(mGLView.getModelRenderer().getWidth(), mGLView.getModelRenderer().getHeight());

        switch (event.getAction()) {

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_HOVER_EXIT:
            case MotionEvent.ACTION_OUTSIDE:
                // this to handle "1 simple touch"
                handleSimpleTouch();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_HOVER_ENTER:
                Log.e(TAG, "Gesture changed...");
                handleGestureChange();
                break;
            case MotionEvent.ACTION_DOWN:
                // 记录触摸点坐标
                lastX = (int) event.getRawX();
                lastY = (int) event.getRawY();
                handleGestureChange();
                break;

            case MotionEvent.ACTION_MOVE:
                switch (view.getId()) {
                    case R.id.mGLView:
                        moving = true;
                        simpleTouch = false;
                        touchDelay++;
                        break;
                    case R.id.imgViewThreeScale:

                        if (y >= 0) {
                            scale = scale + y / 1000f;
                        }
                        if (y < 0) {
                            scale = scale + y / 1000f;
                        }

                        if (scale > 3) {
                            scale = 3f;
                        } else if (scale < 0.3) {
                            scale = 0.3f;
                        }

                        FrameLayout.LayoutParams glViewParmas =
                                (FrameLayout.LayoutParams) mGLView.getLayoutParams();
                        LayoutParams params =
                                (LayoutParams) rlThreeView.getLayoutParams();

                        int width = (int) (params.width * scale);
                        int height = (int) (params.height * scale);
                        int glWidth = (int) (glViewParmas.width * scale);
                        int glHeight = (int) (glViewParmas.height * scale);

                        int minWidth = ((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                100, getResources().getDisplayMetrics()));
                        int minHeight = ((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                100, getResources().getDisplayMetrics()));

                        int minglWidth = ((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                55, getResources().getDisplayMetrics()));
                        int minglHeight = ((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                55, getResources().getDisplayMetrics()));

                        if (width < minWidth) {
                            width = minWidth;
                        }
                        if (height < minHeight) {
                            height = minHeight;
                        }
                        if (glWidth < minglWidth) {
                            glWidth = minglWidth;
                        }
                        if (glHeight < minglHeight) {
                            glHeight = minglHeight;
                        }

                        glViewParmas.width = glWidth;
                        glViewParmas.height = glHeight;
                        mGLView.setLayoutParams(glViewParmas);

                        params.width = width;
                        params.height = height;
                        rlThreeView.setLayoutParams(params);

                        break;
                    case R.id.imgViewThreeMultidimensional:

                        int dx = (int) event.getRawX() - lastX;
                        int dy = (int) event.getRawY() - lastY;
                        left = rlThreeView.getLeft() + dx;
                        Log.i("location(Top)", "getTop():" + getTop() + "----dy:" + dy);
                        top = rlThreeView.getTop() + dy;
                        right = rlThreeView.getRight() - dx;
                        bottom = rlThreeView.getBottom() - dy;
                        rlThreeView.layout(left, top, 0, 0);
                        Log.i("location(MOVE)", "left:" + left + "----top:" + top + "  right----" + right + "  bottom----" + bottom);
                        lastX = (int) event.getRawX();
                        lastY = (int) event.getRawY();
                        break;
                    default:
                        break;
                }
                break;

            case MotionEvent.ACTION_UP:
                handleSimpleTouch();
                switch (view.getId()) {
                    case R.id.imgViewThreeMultidimensional:
                        Rect frame = new Rect();
                        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
                        int statusBarHeight = frame.top;
                        LayoutParams params =
                                (LayoutParams) rlThreeView.getLayoutParams();
                        //控件相对父控件左上右下的距离
                        params.setMargins(left, top, right, bottom);
                        Log.i("location(width)", "width:" + rlThreeView.getWidth() + "----height:" + rlThreeView.getHeight());
                        rlThreeView.setLayoutParams(params);
                        break;
                    default:
                        break;
                }
                break;
            default:
                gestureChanged = true;
                break;
        }


        //处理手指重新点击会重新绘制的问题
        pointerCount = event.getPointerCount();
        if (view.getId() == R.id.mGLView) {
            if (pointerCount == 1) {
                x1 = x;
                y1 = y;
                if (gestureChanged) {
                    previousX1 = x1;
                    previousY1 = y1;
                    Log.e(TAG, "x:" + x1 + ",y:" + y1);
                }
                dx1 = x1 - previousX1;
                dy1 = y1 - previousY1;
            }

            if (touchDelay > 1) {
                scene.processMove(dx1, dy1);
                Log.e(TAG, "touchDelay: " + touchDelay);
                if (pointerCount == 1) {
                    Camera camera = scene.getCamera();
                    touchStatus = TOUCH_STATUS_MOVING_WORLD;
                    dx1 = (float) (dx1 / max * Math.PI * 2);
                    dy1 = (float) (dy1 / max * Math.PI * 2);
                    camera.translateCamera(dx1, dy1);
                    Log.e(TAG, "translateCamera: ");
                }
            }
        }

        previousX1 = x1;
        previousY1 = y1;
        if (gestureChanged && touchDelay > 1) {
            gestureChanged = false;
        }
        if (view.getId() == R.id.mGLView ) {
            mGLView.requestRender();
        }

        return true;
    }

    /**
     * 手势改变的时候
     */
    private void handleGestureChange() {
        gestureChanged = true;
        touchDelay = 0;
        lastActionTime = SystemClock.uptimeMillis();
        simpleTouch = false;
    }

    /**
     * 处理单击事件
     */
    private void handleSimpleTouch() {
        if (lastActionTime > SystemClock.uptimeMillis() - 250) {
            simpleTouch = true;
        } else {
            handleGestureChange();
        }
        moving = false;
    }


    public interface onTouchScale {
        void onScale(float dy, float dx);
    }

    public interface onClickListener {

        void onClick();

        void onMessageClick(int id);

    }


}
