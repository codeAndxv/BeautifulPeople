package com.dudu.beautifulpeople;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.format.DateFormat;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.Random;

public class ScreenshotService extends Service {
    Random random = new Random();
    public final String saveImagePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath() + File.separator + "screenshot";

    private Handler handler;
    private final int INTERVAL = 3000; // 3秒

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                takeScreenshot(getApplicationContext());
                handler.postDelayed(this, INTERVAL);
            }
        }, INTERVAL);
        return START_STICKY;
    }

    private void takeScreenshot(Context context) {
        Date now = new Date();
        // 生成随机的两位数
        int randomNumber = random.nextInt(90) + 10; // 生成 [10, 99] 之间的随机数

        String name = DateFormat.format("yyyyMMdd_HHmmss", now).toString() + "_" + randomNumber;

        try {
            // 获取屏幕视图
            View rootView = View.inflate(context, R.layout.activity_main, null);
            rootView.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(rootView.getDrawingCache());
            rootView.setDrawingCacheEnabled(false);

            File directory  = new File(saveImagePath);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            // 将截图保存到设备
            File imagePath = new File(saveImagePath,  name + ".png");
            FileOutputStream fos = new FileOutputStream(imagePath);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}