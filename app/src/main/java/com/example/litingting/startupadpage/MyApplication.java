package com.example.litingting.startupadpage;

import android.app.Application;
import android.content.Intent;

import com.example.downloadad.DownloadService;

/**
 * Created by wzxx on 16/9/17.
 */
public class MyApplication extends Application{

    @Override
    public void onCreate(){
        super.onCreate();
        this.startService(new Intent(this, DownloadService.class));
    }
}
