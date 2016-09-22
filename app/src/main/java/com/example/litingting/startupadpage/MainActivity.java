package com.example.litingting.startupadpage;

import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.downloadad.DownloadFile;
import com.example.downloadad.DownloadListener;
import com.example.downloadad.DownloadService;
import com.example.downloadad.TaskInfo;
import com.example.downloadad.dataManager.SQLDownloadInfo;

/**
 * 我的想法是有一个button，点击，mainActivity就会自动下载几个任务，并且显示在listview上（首要任务）
 *
 * 基础功能：
 * 1.listView上每一个view都会显示它们自己的进度
 * 2.点击listView上每一个view就会暂停，再点一次还能继续下载（断点续传）
 * 3.点击istView上每一个view后的红叉即可取消下载
 * 4.返回home键还能restore
 *
 * 下面这个是一个demo
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG=MainActivity.class.getSimpleName();

//    private TextView textViewHint;
//    private TextView textViewProgress;

    private Button addButton;//添加任务
    private Button cancelButton;
    private ListView listView;//显示下载任务列表
    private ArrayAdapter<String> adapter;

    private static DownloadFile downloadFile;
    TaskInfo info=new TaskInfo();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        handler.sendEmptyMessageDelayed(1,50);

        addButton=(Button)findViewById(R.id.downloadBtn);
        cancelButton=(Button)findViewById(R.id.cancelBtn);
        listView=(ListView)findViewById(R.id.listView);

//        downloadChangeObserver=new DownloadChangeObserver();
//        downloadFile.setRootDir("/download/");
//        filePath=downloadFile.downloadUrl(this,"http://img04.tooopen.com/images/20121104/tooopen_201211040112213930.jpg",
//                "panda.jpg");
//        textViewHint.setText(filePath);


        /**
         * 先只下载一个文件
         */
        final String fileName="wzxxDownload";
        final String TaskID="wzxxDownloadID";
        final String url="http://broadview.com.cn/files/23245/Download/23245.zip";
        final String filePath="/wzxx";
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                  downloadFile.setRootDir(filePath);
                info.setFileName(fileName);
                info.setTaskID(TaskID);
                info.setOnDownloading(true);
//                  int info=downloadFile.addTask("wzxID",url,"wzx");//有默认途径，第四个参数也可以设置为自己想要的路径

//                  downloadFile.addTask("wzxID2","https://mail.qq.com/cgi-bin/groupattachment?att=12BF677D0000034100010239B","wzxx2");
//                downloadFile.addTask("wzxID3","https://raw.githubusercontent.com/LiuGuiLinAndroid/Coding-Developer-Book/m","wzxx3");
//                  Log.i(TAG,"RETURN: "+info);
                downloadFile.addTask(TaskID,url,fileName);
            }
        });

    }

//    @Override
//    protected void onResume(){
//        super.onResume();
//        getContentResolver().registerContentObserver(Uri.parse(filePath),true,downloadChangeObserver);
//    }
//
//    @Override
//    protected void onPause(){
//        super.onPause();
//        getContentResolver().unregisterContentObserver(downloadChangeObserver);
//    }
//
//    class DownloadChangeObserver extends ContentObserver{
//        public DownloadChangeObserver(){
//            super(handler);
//        }
//
//        @Override
//        public void onChange(boolean selfChange){
//            updateView();
//        }
//    }
//
//    private void updateView(){
//        int[] bytesAndStatus=DownloadFile.getInstance().getBytesAndStatus(downloadFile.downloadId);
//        handler.sendMessage(handler.obtainMessage(0,bytesAndStatus[0],bytesAndStatus[1],bytesAndStatus[2]));
//    }

    Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            /*获取下载管理器*/
            downloadFile=DownloadService.getDownloadFile();
            /*断点续传需要服务器支持，设置这一项时要确保服务器支持断点续传功能*/
            downloadFile.setSupportBreakpoint(true);
//            adapter=new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_expandable_list_item_1,info.getProgress());
//            listView.setAdapter(adapter);
        }
    };

    private class DownloadFileListener implements DownloadListener{
        @Override
        public void onStart(SQLDownloadInfo sqlDownloadInfo){

        }

        @Override
        public void onProgress(SQLDownloadInfo sqlDownloadInfo,boolean isSupportBreakpoint){

        }

        @Override
        public void onStop(SQLDownloadInfo sqlDownloadInfo,boolean isSupportBreakpoint){

        }

        @Override
        public void onSuccess(SQLDownloadInfo sqlDownloadInfo){

        }

        @Override
        public void onError(SQLDownloadInfo sqlDownloadInfo){

        }
    }
}
