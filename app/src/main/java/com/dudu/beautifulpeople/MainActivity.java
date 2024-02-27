package com.dudu.beautifulpeople;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.dudu.beautifulpeople.service.ScreenRecordService;


/**
 * @author by talon, Date on 19/6/23.
 * note:
 */
public class MainActivity extends Activity {
    private static final int REQUEST_CODE = 1;
    private static final int REQUEST_CODE_SELECT_DIRECTORY = 111;
    private MediaProjectionManager mMediaProjectionManager;
    private static final String TAG = "BEAUTIFUL";
    private String selectedDirectoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) + "/" + "record";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermission(this); //检查权限

        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
    }

    public void startRecorder(View view) {
        createScreenCapture();
    }

    public void stopRecorder(View view){
        Intent service = new Intent(this, ScreenRecordService.class);
        stopService(service);
    }


    public void selectDirectory(android.view.View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, REQUEST_CODE_SELECT_DIRECTORY);
    }


    public static void checkPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= 23) {
            int checkPermission =
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
                            + ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_PHONE_STATE)
                            + ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            + ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
            if (checkPermission != PackageManager.PERMISSION_GRANTED) {
                //动态申请
                ActivityCompat.requestPermissions(activity, new String[]{
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE}, 123);
            }
        }
    }

    private void createScreenCapture() {
        Intent captureIntent = mMediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, REQUEST_CODE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SELECT_DIRECTORY) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    selectedDirectoryPath = uri.getPath(); // 获取用户选择的目录路径
                }
            } else {
                Toast.makeText(this, "未选择目录", Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            try {
                Toast.makeText(this, "允许录屏", Toast.LENGTH_SHORT).show();
                try {
                    WindowManager mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                    DisplayMetrics metrics = new DisplayMetrics();
                    mWindowManager.getDefaultDisplay().getMetrics(metrics);
                } catch (Exception e){
                    Log.e(TAG, "MediaProjection error");
                }

                Intent service = new Intent(this, ScreenRecordService.class);
                service.putExtra("directoryPath", selectedDirectoryPath); // 将目录路径放入额外数据中
                service.putExtra("resultCode", resultCode);
                service.putExtra("data", data);
                // Start the foreground service
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(service);
                } else {
                    startService(service);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "拒绝录屏", Toast.LENGTH_SHORT).show();
        }
    }

}
