package com.dudu.beautifulpeople.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.dudu.beautifulpeople.MainActivity;
import com.dudu.beautifulpeople.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ShooterService extends Service {
    private ImageReader mImageReader;

    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private OnShotListener mOnShotListener;
    private int mHeight;
    private int mWidth;
    private int mResultCode;
    private Intent mResultData;

    public ShooterService() {

    }

    private Handler handler;
    private Runnable runnable;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                // 打印Toast消息
                startScreenShot(new OnShotListener() {
                    @Override
                    public void onFinish(String path) {

                    }

                    @Override
                    public void onError() {

                    }
                });
                // 每两秒后再次执行
                handler.postDelayed(this, 2000);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mResultCode = intent.getIntExtra("code", -1);
            mResultData = intent.getParcelableExtra("data");

            mMediaProjection = getMediaProjectionManager().getMediaProjection(mResultCode, mResultData);

            WindowManager window = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            Display mDisplay = window.getDefaultDisplay();
            DisplayMetrics metrics = new DisplayMetrics();
            mDisplay.getRealMetrics(metrics);
            mWidth = metrics.widthPixels;//size.x;
            mHeight = metrics.heightPixels;//size.y;

            mImageReader = ImageReader.newInstance(
                    mWidth,
                    mHeight,
                    PixelFormat.RGBA_8888,//this is necessary to equal buffer format in #copyPixelsFromBuffer.
                    3);

            virtualDisplay();
        }

        handler.post(runnable);

        return START_STICKY;
    }


    private void createNotificationChannel() {
        Notification.Builder builder = new Notification.Builder(this.getApplicationContext()); //获取一个Notification构造器
        Intent nfIntent = new Intent(this, MainActivity.class); //点击后跳转的界面，可以设置跳转数据

        builder.setContentIntent(PendingIntent.getActivity(this, 0, nfIntent, 0)) // 设置PendingIntent
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher)) // 设置下拉列表中的图标(大图标)
                //.setContentTitle("SMI InstantView") // 设置下拉列表里的标题
                .setSmallIcon(R.mipmap.ic_launcher) // 设置状态栏内的小图标
                .setContentText("is running......") // 设置上下文内容
                .setWhen(System.currentTimeMillis()); // 设置该通知发生的时间

        /*以下是对Android 8.0的适配*/
        //普通notification适配
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("notification_id");
        }
        //前台服务notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel("notification_id", "notification_name", NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = builder.build(); // 获取构建好的Notification
        notification.defaults = Notification.DEFAULT_SOUND; //设置为默认的声音
        startForeground(110, notification);
    }


    private MediaProjectionManager getMediaProjectionManager() {
        return ((MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE));
    }



    public void startScreenShot(OnShotListener onShotListener) {
        mOnShotListener = onShotListener;

        Image image = mImageReader.acquireLatestImage();
        new SaveTask().doInBackground(image);
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void virtualDisplay() {

        mVirtualDisplay = mMediaProjection.createVirtualDisplay(
                "screen-mirror",
                mWidth,
                mHeight,
                Resources.getSystem().getDisplayMetrics().densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mImageReader.getSurface(), null, null
        );

    }



    public class SaveTask extends AsyncTask<Image, Void, Bitmap> {

        @TargetApi(Build.VERSION_CODES.KITKAT)
        @Override
        protected Bitmap doInBackground(Image... params) {

            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
            Date curDate = new Date(System.currentTimeMillis());
            String curTime = formatter.format(curDate).replace(" ", "");

            if (params == null || params.length < 1 || params[0] == null) {
                return null;
            }

            Image image = params[0];

            int width = image.getWidth();
            int height = image.getHeight();
            final Image.Plane[] planes = image.getPlanes();
            final ByteBuffer buffer = planes[0].getBuffer();
            //每个像素的间距
            int pixelStride = planes[0].getPixelStride();
            //总的间距
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * width;
            Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height,
                    Bitmap.Config.ARGB_8888);//even though ARGB8888 will consume more memory,it has better compatibility on device.
            bitmap.copyPixelsFromBuffer(buffer);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
            image.close();
            File fileImage = null;
            if (bitmap != null) {
                try {


                    fileImage = new File(getSavedPath() + "/" + curTime + ".png");

                    if (!fileImage.exists()) {
                        fileImage.createNewFile();
                    }
                    FileOutputStream out = new FileOutputStream(fileImage);
                    if (out != null) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                        out.flush();
                        out.close();
                    }

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    if (mOnShotListener != null) mOnShotListener.onError();
                    release();
                    return null;
                } catch (IOException e) {
                    e.printStackTrace();
                    if (mOnShotListener != null) mOnShotListener.onError();

                    release();
                    return null;
                }
            }

            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }

            if (mVirtualDisplay != null) {
                mVirtualDisplay.release();
            }
            if (mMediaProjection != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mMediaProjection.stop();
                }
            }

            if (mOnShotListener != null) {
                mOnShotListener.onFinish(getSavedPath() + "/" + curTime + ".png");
            }

            return null;
        }

        @TargetApi(Build.VERSION_CODES.KITKAT)
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
        }
    }



    private String getSavedPath() {
        String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/" + "record";
        File directoryFile = new File(directory);
        if (!directoryFile.exists()) {
            directoryFile.mkdirs();
        }
        return directory;
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        release();
    }


    public void release(){
        if (mVirtualDisplay != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mVirtualDisplay.release();
            }
        }
        if (mMediaProjection != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mMediaProjection.stop();
            }
        }
        // 移除Runnable，防止内存泄漏
        handler.removeCallbacks(runnable);
    }

    public interface OnShotListener {
        void onFinish(String path);
        void onError();
    }
}
