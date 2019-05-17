package com.example.ailibin.androidopenglview;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.ailibin.androidopenglview.util.ScreenService;
import com.example.ailibin.androidopenglview.util.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;


public class LightingActivity extends AppCompatActivity implements View.OnClickListener {


    private Dialog loadingDialog;
    private Bitmap baseBitmap, copyBitmap;
    float two = 1;
    private Paint paint;
    private Canvas canvas;

    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private ScreenService recordService;
    private static int RECORD_REQUEST_CODE = 5;
    private PopupWindow popupWindow;
    private SeekBar seekbar;
    private RelativeLayout mContentRootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_lighting);
        seekbar = findViewById(R.id.seekbar);
        mContentRootView = findViewById(R.id.rl_content_root);
        loadingDialog = Utils.createLoadingDialog(this, "");

        seekbar.setMax(60);
        baseBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.test_light);
        //既然是复制一张与原图一模一样的图片那么这张复制图片的画纸的宽度和高度以及分辨率都要与原图一样,copyBitmap就为一张与原图相同尺寸分辨率的空白画纸
        copyBitmap = Bitmap.createBitmap(baseBitmap.getWidth(), baseBitmap.getHeight(), baseBitmap.getConfig());

        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {

                two = (progress + 20) * 1.0f / ((seekBar.getMax()) - 10);
                paint = new Paint();
                canvas = new Canvas(copyBitmap);
                ColorMatrix colorMatrixL = new ColorMatrix();
                colorMatrixL.setScale(two, two, two, 1);
                ColorMatrix colorMatriximg = new ColorMatrix();
                //通过postConcat()方法可以将以上效果叠加到一起
                colorMatriximg.postConcat(colorMatrixL);
                ColorMatrixColorFilter colorMatrixColorFilter = new ColorMatrixColorFilter(colorMatriximg);
                paint.setColorFilter(colorMatrixColorFilter);

                Executors.newCachedThreadPool().execute(new Runnable() {
                    //子线程执行
                    @Override
                    public void run() {
                        canvas.drawBitmap(baseBitmap, new Matrix(), paint);
                        //主线程
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mContentRootView.setBackground(new BitmapDrawable(LightingActivity.this.getResources(), copyBitmap));
                            }
                        });
                    }
                });

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            Intent intent = new Intent(this, ScreenService.class);
            bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
            Intent captureIntent = projectionManager.createScreenCaptureIntent();
            startActivityForResult(captureIntent, RECORD_REQUEST_CODE);
        }

    }

    private boolean isSelfHandle = true;

    /**
     * 添加贴纸
     * @param url
     */
    private void addStickerView(String url) {

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        final ThreeDView myGLScene = new ThreeDView(this);
        myGLScene.setUrl(url);
        myGLScene.setActivity(this);
        myGLScene.setOnClickListener(new ThreeDView.onClickListener() {
            @Override
            public void onClick() {
//                mViews.remove(mViews.size() - 1);
//                for (int i = 0; i < mViews.size(); i++) {
//                    if (i == (mViews.size() - 1)) {
//                        ThreeDView view = (ThreeDView) mViews.get(i);
//                        view.setIsChoose(true);
//                        view.setIsFocus(true);
//                    }
//                }
                mContentRootView.removeView(myGLScene);
            }

            @Override
            public void onMessageClick(int id) {
            }
        });
        mContentRootView.addView(myGLScene);
//        mViews.add(myGLScene);

    }

    // 保存图片
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void generateBitmap() {

        //截图
//        runOnUiThread(() -> {
//            for (int i = 0; i < mViews.size(); i++) {
//                ThreeDView threeDView = (ThreeDView) mViews.get(i);
//                threeDView.setIsChoose(false);
//            }
//        });

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Bitmap bitmap = recordService.getBitmap();
        // 获取内置SD卡路径
        String sdCardPath = LightingActivity.this.getExternalFilesDir("").getPath();
        saveImageToGallery(LightingActivity.this, bitmap, sdCardPath);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Utils.showToast(LightingActivity.this, "Save");
            }
        });

    }

//    @OnClick(R.id.btn_back)
//    public void onBack() {
//        this.finish();
//    }

//    @OnClick(R.id.btn_menu)
    public void showMenu(View v) {
        popupWindow = new PopupWindow(this);
        popupWindow.setWidth(RelativeLayout.LayoutParams.WRAP_CONTENT);
        popupWindow.setHeight(RelativeLayout.LayoutParams.WRAP_CONTENT);
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_lighting_menu, null);
        popupWindow.setContentView(popupView);
        popupWindow.setOutsideTouchable(false);
        popupWindow.setBackgroundDrawable(new ColorDrawable(0x00000000));
        popupWindow.setFocusable(true);
        popupWindow.showAsDropDown(v, Utils.dp2px(this, -28), Utils.dp2px(this, 4));

        TextView popupScene = popupView.findViewById(R.id.popup_scene);
        TextView popupProduct = popupView.findViewById(R.id.popup_product);
        TextView popupBrightness = popupView.findViewById(R.id.popup_brightness);
        TextView popupSave = popupView.findViewById(R.id.popup_save);

        popupScene.setOnClickListener(this);
        popupProduct.setOnClickListener(this);
        popupBrightness.setOnClickListener(this);
        popupSave.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.popup_scene:
                break;
            case R.id.popup_product:
                break;
            case R.id.popup_brightness:
                popupWindow.dismiss();
                break;
            case R.id.popup_save:
                popupWindow.dismiss();
                break;
            default:
                break;
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    /**
     * 寻找指定目录下，具有指定后缀名的所有文件。
     *
     * @param filenameSuffix      : 文件后缀名
     * @param currentDirUsed      : 当前使用的文件目录
     * @param currentFilenameList ：当前文件名称的列表
     */
    public void findFiles(String filenameSuffix, String currentDirUsed,
                          List<String> currentFilenameList) {
        File dir = new File(currentDirUsed);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                /**
                 * 如果目录则递归继续遍历
                 */
                findFiles(filenameSuffix, file.getAbsolutePath(), currentFilenameList);
            } else {
                /**
                 * 如果不是目录。
                 * 那么判断文件后缀名是否符合。
                 */
                if (file.getAbsolutePath().endsWith(filenameSuffix)) {
                    currentFilenameList.add(file.getAbsolutePath());
                }
            }
        }
    }

    /**
     * 根据指定的Activity截图（去除状态栏）
     *
     * @param activity 要截图的Activity
     * @return Bitmap
     */
    public Bitmap shotActivityNoStatusBar(Activity activity) {
        // 获取windows中最顶层的view
        View view = activity.getWindow().getDecorView();
        view.buildDrawingCache();
        // 获取状态栏高度
        Rect rect = new Rect();
        view.getWindowVisibleDisplayFrame(rect);
        int statusBarHeights = rect.top;
        Display display = activity.getWindowManager().getDefaultDisplay();
        // 获取屏幕宽和高
        int widths = display.getWidth();
        int heights = display.getHeight();
        // 允许当前窗口保存缓存信息
        view.setDrawingCacheEnabled(true);
        // 去掉状态栏
        Bitmap bmp = Bitmap.createBitmap(view.getDrawingCache(), 0,
                statusBarHeights, widths, heights - statusBarHeights);
        // 销毁缓存信息
        view.destroyDrawingCache();
        return bmp;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RECORD_REQUEST_CODE && resultCode == RESULT_OK) {

            //######## 截屏逻辑 ########
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaProjection = projectionManager.getMediaProjection(resultCode, data);
            }
//            recordService.setMediaProject(mediaProjection);
//            recordService.initImageReader();
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
//            ScreenService.RecordBinder binder = (ScreenService.RecordBinder) service;
//            recordService = binder.getRecordService();
//            recordService.setConfig(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi);
            //mButton.setEnabled(true);
            //mButton.setText(recordService.isRunning() ? "结束" : "开始");
        }


        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
    }

    public static void saveImageToGallery(Context context, Bitmap bmp, String path) {
        // 首先保存图片
        File appDir = new File(path, "bitmap");
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = "kaiyan" + System.currentTimeMillis() + ".png";
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 其次把文件插入到系统图库
        try {
            MediaStore.Images.Media.insertImage(context.getContentResolver(),
                    file.getAbsolutePath(), fileName, null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // 最后通知图库更新
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + path + "/bitmap")));
    }

}
