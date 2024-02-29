package com.dudu.beautifulpeople.utils;

import android.graphics.Bitmap;
import android.graphics.Rect;

import com.dudu.beautifulpeople.mtcnn.Align;
import com.dudu.beautifulpeople.mtcnn.Box;
import com.dudu.beautifulpeople.mtcnn.MTCNN;

import java.util.Vector;

public class ModelHelper {

    private float runModel2Inference(Bitmap croppedBitmap) {
        // Implement model 2 inference using the cropped bitmap
        // This method will return the score obtained from model 2
        return 0.0f; // Placeholder
    }

    /**
     * 人脸检测并裁减
     */
    public Bitmap detectAndCropPerson(Bitmap bitmap1) {
        MTCNN mtcnn = GlobalVariables.mtcnn;
        if (bitmap1 == null) {
            return null;
        }

        Bitmap bitmapTemp1 = bitmap1.copy(bitmap1.getConfig(), false);

        // 检测出人脸数据
        long start = System.currentTimeMillis();
        // 只有这句代码检测人脸，下面都是根据Box在图片中裁减出人脸
        Vector<Box> boxes1 = mtcnn.detectFaces(bitmapTemp1, bitmapTemp1.getWidth() / 5);
        long end = System.currentTimeMillis();
//        resultTextView.setText("人脸检测前向传播耗时：" + (end - start));
        if (boxes1.size() == 0) {
            return null;
        }

        // 这里因为使用的每张照片里只有一张人脸，所以取第一个值，用来剪裁人脸
        Box box1 = boxes1.get(0);

        // 人脸矫正
        bitmapTemp1 = Align.face_align(bitmapTemp1, box1.landmark);
        boxes1 = mtcnn.detectFaces(bitmapTemp1, bitmapTemp1.getWidth() / 5);
        box1 = boxes1.get(0);

        box1.toSquareShape();
        box1.limitSquare(bitmapTemp1.getWidth(), bitmapTemp1.getHeight());
        Rect rect1 = box1.transform2Rect();

        // 剪裁人脸
        return MyUtil.crop(bitmapTemp1, rect1);

        // 绘制人脸框和五点
//        Utils.drawBox(bitmapTemp1, box1, 10);
//        Utils.drawBox(bitmapTemp2, box2, 10);

//        bitmapCrop1 = MyUtil.readFromAssets(this, "42.png");
//        bitmapCrop2 = MyUtil.readFromAssets(this, "52.png");

    }

}
