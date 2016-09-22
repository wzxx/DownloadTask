package com.example.downloadad;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * 类功能描述：下载器的后台服务
 *
 * Created by wzxx on 16/9/17.
 *
 * @author wzxx
 * @email wzxxkcer@foxmial.com
 * @version 1.0
 */
public class DownloadService extends Service{

    private static DownloadFile downloadFile;

    @Override
    public IBinder onBind(Intent intent){
        return null;
    }

    @Override
    public void onCreate(){
        super.onCreate();
        downloadFile=DownloadFile.getInstance(DownloadService.this);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        //释放downloadFile管理下载任务的对象
        downloadFile.stopAllTask();
        downloadFile=null;
    }

    @Override
    public void onStart(Intent intent,int startId){
        super.onStart(intent,startId);
        if (downloadFile==null){
            downloadFile=DownloadFile.getInstance(DownloadService.this);
        }
    }

    public static DownloadFile getDownloadFile(){
        return downloadFile;
    }
}
