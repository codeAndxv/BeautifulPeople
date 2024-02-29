package com.dudu.beautifulpeople;

import android.app.Application;

import com.dudu.beautifulpeople.mtcnn.MTCNN;
import com.dudu.beautifulpeople.utils.GlobalVariables;
import com.dudu.beautifulpeople.utils.ModelHelper;

import java.io.IOException;

public class BeautifulPeopleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            GlobalVariables.mtcnn = new MTCNN(getAssets());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        GlobalVariables.modelHelper = new ModelHelper();
    }

}
