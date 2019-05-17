package com.example.ailibin.androidopenglview.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ailibin.androidopenglview.R;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.content.Context.INPUT_METHOD_SERVICE;

public class Utils {

    /**
     * 得到设备屏幕的宽度
     */
    public static int getScreenWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    /**
     * 得到设备屏幕的高度
     */
    public static int getScreenHeight(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    /**
     * 得到设备的密度
     */
    public static float getScreenDensity(Context context) {
        return context.getResources().getDisplayMetrics().density;
    }

    /**
     * 把密度转换为像素
     */
    public static int dp2px(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    /**
     * 把像素转换为密度
     */
    public static int px2dp(Context context, float px) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (px / scale + 0.5f);
    }

    public static void showToastCenter(Context context, String message) {
        Toast t = Toast.makeText(context, message, Toast.LENGTH_LONG);
        t.setGravity(Gravity.CENTER, 0, 0);
        t.show();
    }

    public static void showToast(Context context, String message) {
        Toast t = Toast.makeText(context, message, Toast.LENGTH_LONG);
        t.setGravity(Gravity.CENTER, 0, 0);
        View v = LayoutInflater.from(context).inflate(R.layout.toast_custom_layout, null, false);
        TextView tv = v.findViewById(R.id.tv_toast);
        tv.setText(message);
        t.setView(v);
        t.show();
    }

    public static void showLongToast(Context context, String message) {
        Toast t = Toast.makeText(context, message, Toast.LENGTH_LONG);
        t.show();
    }

    /**
     * 将图片转换成十六进制字符串
     *
     * @param photo
     * @return
     */
    public static String sendPhoto(ImageView photo) {
        Drawable d = photo.getDrawable();
        Bitmap bitmap = ((BitmapDrawable) d).getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);// (0 -
        // 100)压缩文件
        byte[] bt = stream.toByteArray();
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // String photoStr = byte2hex(bt);
        String photoStr = Base64.encodeToString(bt, Base64.DEFAULT); // 图片转base4字符串
        return photoStr;
    }


    // 将bitmap转为byte格式的数组
    public static byte[] bmpToByteArray(final Bitmap bitmap, final boolean needRecycle) {
        //创建字节数组输出流
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        //Bitmap.compress()方法的参数format可设置JPEG或PNG格式；quality可选择压缩质量；fOut是输出流（OutputStream）
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        if (needRecycle) {
            bitmap.recycle();
        }
        //将字节数组输出流转为byte数组
        byte[] result = outputStream.toByteArray();
        try {
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;

    }


    /**
     * 将图片转换成十六进制字符串
     *
     * @param photo
     * @return
     */
    public static String sendBitmap(Bitmap photo) {
        Bitmap bitmap = photo;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);// (0 -
        // 100)压缩文件
        byte[] bt = stream.toByteArray();
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // String photoStr = byte2hex(bt);
        String photoStr = Base64.encodeToString(bt, Base64.DEFAULT); // 图片转base4字符串
        return photoStr;

    }

    /**
     * 将String字符串转换成十六进制字符串
     *
     * @return
     */
    public static String sendString(String str) {
        String string = str;
        String strBase64 = new String(Base64.encode(string.getBytes(), Base64.DEFAULT));
        return strBase64;

    }

    /**
     * 将图片转换成二进制字符串
     *
     * @param photo
     * @return
     */
    public static String sendBinaryPhoto(ImageView photo) {
        Drawable d = photo.getDrawable();
        Bitmap bitmap = ((BitmapDrawable) d).getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);// (0 -
        // 100)压缩文件
        byte[] bt = stream.toByteArray();
        String photoStr = byte2binary(bt);
        return photoStr;
    }


    /**
     * 将Bitmap图片转换成二进制字符串
     *
     * @param bitmap
     * @return
     */
    public static byte[] sendBinaryBitmap(Bitmap bitmap) {
//        Drawable d = photo.getDrawable();
//        Bitmap bitmap = ((BitmapDrawable) d).getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);// (0 -
        // 100)压缩文件
        byte[] bt = stream.toByteArray();
//        String photoStr = byte2binary(bt);
        return bt;
    }


    /**
     * 字节数组转二进制字符串
     */
    public static String byte2binary(byte[] b) {
        String ZERO = "00000000";
        StringBuffer sb = new StringBuffer();
        String stmp = "";
        for (int i = 0; i < b.length; i++) {
            stmp = Integer.toBinaryString(b[i]);
            if (stmp.length() > 8) {
                sb.append(stmp.substring(stmp.length() - 8));
            } else if (stmp.length() < 8) {
                sb.append(ZERO.substring(stmp.length()) + stmp);
            }
        }
        return sb.toString();
    }

    /**
     * 字节数组转十六进制字符串
     *
     * @param b
     * @return
     */
    public static String byte2hex(byte[] b) {
        StringBuffer sb = new StringBuffer();
        String stmp = "";
        for (int n = 0; n < b.length; n++) {
            stmp = Integer.toHexString(b[n] & 0XFF);
            if (stmp.length() == 1) {
                sb.append("0" + stmp);
            } else {
                sb.append(stmp);
            }

        }
        return sb.toString();
    }


    /**
     * 获取当前时间
     *
     * @return
     */
    public static String getCurrentTime() {
        SimpleDateFormat mDateFormat = new SimpleDateFormat("MM-dd HH:mm");
        long timeStamp = System.currentTimeMillis();
        String time = "";
        if (timeStamp != 0) {
            time = mDateFormat.format(new Date(System.currentTimeMillis()));
        }
        return time;
    }

    /**
     * 格式化价格
     *
     * @param f
     * @return
     */
    public static String priceFormat(float f) {
        NumberFormat nf = NumberFormat.getInstance();
        return nf.format(f);
    }

    public static String priceFormat(double f) {
        NumberFormat nf = NumberFormat.getInstance();
        return nf.format(f);
    }

    public static String priceFormat(String s) {
        float f = Float.parseFloat(s);
        NumberFormat nf = NumberFormat.getInstance();
        return nf.format(f);
    }

    /**
     * 反格式化，去掉，号
     *
     * @param s
     * @return
     */
    public static double priceParse(String s) {
        try {
            return new DecimalFormat().parse(s).doubleValue();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    /**
     * 格式化价格
     *
     * @param f
     * @return
     */
    public static String formatFloat2(float f) {
        DecimalFormat df = new DecimalFormat("##0.0");
        return df.format(f);
    }


    /**
     * 获取Assets目录下的文件内容
     *
     * @param context
     * @param fileName
     * @return
     * @throws IOException
     */
    public static String readAssetsFile(Context context, String fileName) throws IOException {
        InputStream input = context.getResources().getAssets().open(fileName);
        StringBuffer out = new StringBuffer();
        byte[] b = new byte[4096];
        for (int n; (n = input.read(b)) != -1; ) {
            out.append(new String(b, 0, n));
        }
        return out.toString();
    }

    /**
     * @param date
     * @return
     */
    public static String date2string(Date date) {
        String strDate = "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        strDate = sdf.format(date);
        return strDate;
    }

    public static String getSerialNumber(Context context) {
        String SerialNumber = android.os.Build.SERIAL;
        return SerialNumber;
    }


    public static boolean isMobileNO(String mobileNums) {
        /**
         * 判断字符串是否符合手机号码格式
         * 移动号段: 134,135,136,137,138,139,147,150,151,152,157,158,159,170,178,182,183,184,187,188
         * 联通号段: 130,131,132,145,155,156,170,171,175,176,185,186
         * 电信号段: 133,149,153,170,173,177,180,181,189,199
         * @param str
         * @return 待检测的字符串
         */
        String telRegex = "^((13[0-9])|(14[5,7,9])|(15[^4])|(16[0-9])|(18[0-9])|(19[0-9])|(17[0,1,3,5,6,7,8]))\\d{8}$";// "[1]"代表下一位为数字可以是几，"[0-9]"代表可以为0-9中的一个，"[5,7,9]"表示可以是5,7,9中的任意一位,[^4]表示除4以外的任何一个,\\d{9}"代表后面是可以是0～9的数字，有9位。
        if (TextUtils.isEmpty(mobileNums)) {
            return false;
        } else {
            return mobileNums.matches(telRegex);
        }
    }


    /**
     * 得到自定义的progressDialog
     *
     * @param context
     * @param msg
     * @return
     */
    public static Dialog createLoadingDialog(Context context, String msg) {

        LayoutInflater inflater = LayoutInflater.from(context);
        // 得到加载view
        View v = inflater.inflate(R.layout.loading_dialog, null);
        // 加载布局
        LinearLayout layout = (LinearLayout) v.findViewById(R.id.dialog_view);
        // main.xml中的ImageView
        ImageView spaceshipImage = (ImageView) v.findViewById(R.id.img);
        // 提示文字
        TextView tipTextView = (TextView) v.findViewById(R.id.tipTextView);
        // 加载动画
        Animation hyperspaceJumpAnimation = AnimationUtils.loadAnimation(
                context, R.anim.loading_animation);
        // 使用ImageView显示动画
        spaceshipImage.startAnimation(hyperspaceJumpAnimation);
        // 设置加载信息
        tipTextView.setText(msg);

        // 创建自定义样式dialog
        Dialog loadingDialog = new Dialog(context, R.style.loading_dialog);

        // 不可以用“返回键”取消
        loadingDialog.setCancelable(false);
        // 设置布局
        loadingDialog.setContentView(layout, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        return loadingDialog;

    }


    /**
     * 获取Bitmap并压缩
     *
     * @param context
     * @param uri
     * @return
     */
    public static Bitmap getBitmapFromUri(Context context, Uri uri) {
        try {
            // 读取uri所在的图片
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
            bitmap = compressImage(bitmap); // 压缩bitmap
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String createCompressImageFile(Context context, Uri uri) {
        try {
//            Bitmap bitmap = getBitmapFromUri(context, uri);
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);

            String name = String.valueOf(System.currentTimeMillis()) + ".jpg";
            File file = new File(Environment.getExternalStorageDirectory() + "/Pictures/" + name);
            FileOutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 10, outputStream);
            outputStream.flush();
            outputStream.close();
            return file.getAbsolutePath();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 质量压缩方法
     *
     * @param image
     * @return
     */
    public static Bitmap compressImage(Bitmap image) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        int options = 100;
        while (baos.toByteArray().length / 1024 > 100) {  //循环判断如果压缩后图片是否大于100kb,大于继续压缩
            baos.reset();//重置baos即清空baos
            //第一个参数 ：图片格式 ，第二个参数： 图片质量，100为最高，0为最差  ，第三个参数：保存压缩后的数据的流
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);//这里压缩options%，把压缩后的数据存放到baos中
            options -= 50;//每次都减少50
        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());//把压缩后的数据baos存放到ByteArrayInputStream中
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);//把ByteArrayInputStream数据生成图片
        return bitmap;
    }

    /**
     * 手机号码加密（星号）
     *
     * @param number
     * @return
     */
    public static String starMobileNumber(String number) {
        return number.substring(0, 3) + "****" + number.substring(7);
    }

    /**
     * 隐藏键盘
     *
     * @param activity
     */
    public static void hideKeyboard(Activity activity) {
//        ((InputMethodManager) activity.getSystemService(INPUT_METHOD_SERVICE))
//                .hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

        ((InputMethodManager) activity.getSystemService(INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    public static void hideKeyboard(Activity activity, View view) {
//        ((InputMethodManager) activity.getSystemService(INPUT_METHOD_SERVICE))
//                .hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

        ((InputMethodManager) activity.getSystemService(INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }


    /**
     * 至少包含大小写字母及数字中的两种
     * 是否包含
     *
     * @param str
     * @return
     */
    public static boolean isLetterDigit(String str) {
        boolean isDigit = false;//定义一个boolean值，用来表示是否包含数字
        boolean isLetter = false;//定义一个boolean值，用来表示是否包含字母
        for (int i = 0; i < str.length(); i++) {
            if (Character.isDigit(str.charAt(i))) {   //用char包装类中的判断数字的方法判断每一个字符
                isDigit = true;
            } else if (Character.isLetter(str.charAt(i))) {  //用char包装类中的判断字母的方法判断每一个字符
                isLetter = true;
            }
        }
        String regex = "^[a-zA-Z0-9]+$";
        boolean isRight = isDigit && isLetter && str.matches(regex);
        return isRight;
    }


    // 两次点击按钮之间的点击间隔不能少于1000毫秒
    private static final int MIN_CLICK_DELAY_TIME = 1000;
    private static long lastClickTime;

    public static boolean isFastClick() {
        boolean flag = false;
        long curClickTime = System.currentTimeMillis();
        if ((curClickTime - lastClickTime) >= MIN_CLICK_DELAY_TIME) {
            flag = true;
        }
        lastClickTime = curClickTime;
        return flag;
    }

}
