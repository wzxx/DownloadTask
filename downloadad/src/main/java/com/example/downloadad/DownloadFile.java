package com.example.downloadad;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.example.downloadad.dataManager.DataKeeper;
import com.example.downloadad.dataManager.FileHelper;
import com.example.downloadad.dataManager.SQLDownloadInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 类功能描述：下载管理类(主)
 *
 * Created by wzxx on 16/9/8.
 *
 * @author wzxx
 * @email wzxxkcer@foxmail.com
 * @version 1.0
 *
 */
public class DownloadFile {

    private final static String TAG=".DownloadFile";

    private Context mContext;

    /**任务队列（包含一个个下载任务）*/
    private ArrayList<Downloader> taskList=new ArrayList<Downloader>();

    private final int MAX_DOWNLOADING_TASK=5;//最大同时下载数量（也许还可以设置更多）

    private Downloader.DownloadSuccess downloadSuccessListener=null;

    /**服务器是否支持断点续传*/
    private boolean iSupportBreakpoint=false;//默认不支持

    private ThreadPoolExecutor poolExecutor;//线程池（既然要下载多个任务完了以后前台还只要直接调用），开多个线程下载就是我的事儿啦）

    private DownloadListener downloadTasksListener;

    private static String Rootdir;

    /**
     * 单例模式
     */
    private static DownloadFile downloadFile=null;

    /**
     * 构造器(初始化)
     */
    private DownloadFile(Context context){
        mContext= context;

        /**
         * 新建一个线程池
         *
         * 参数1:线程池维护线程的最少数量
         * 参数2:线程池维护线程的最大刷领
         * 参数3：线程池维护线程所允许的空闲时间
         * 参数4:线程池维护线程所允许的空闲时间的单位（秒）
         * 参数5:线程池所使用的缓冲队列
         */
        poolExecutor=new ThreadPoolExecutor(MAX_DOWNLOADING_TASK,MAX_DOWNLOADING_TASK,30, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(2000));

        /**
         * 新建一个下载完成的监听器
         *
         * 在任务执行完成之后通知下载管理器，以便下载管理器将已完成的任务移出任务列表
         */
        downloadSuccessListener=new Downloader.DownloadSuccess() {
            //重写
            @Override
            public void onTaskSuccess(String TaskID) {
                int taskSize=taskList.size();
                for (int i=0;i<taskSize;i++){
                    Downloader downloaderToBeDeleted=taskList.get(i);
                    if (downloaderToBeDeleted.getTaskID().equals(TaskID)){
                        taskList.remove(downloaderToBeDeleted);
                        return;
                    }
                }
            }
        };
        recoverData(mContext);//恢复数据
    }

    public static DownloadFile getInstance(Context context){
        if (downloadFile==null){
            downloadFile=new DownloadFile(context);
            return downloadFile;
        }else {
            return downloadFile;
        }
    }

    /**
     * 从数据库恢复下载任务信息
     * @param context
     */
    private void recoverData(Context context){
        stopAllTask();
        taskList=new ArrayList<Downloader>();
        DataKeeper dataKeeper=new DataKeeper(context);
        ArrayList<SQLDownloadInfo> sqlDownloadInfoList=null;
        sqlDownloadInfoList=dataKeeper.getAllDownloadInfo();

        if (sqlDownloadInfoList.size()>0){
            int listSize=sqlDownloadInfoList.size();
            for (int i=0;i<listSize;i++){//恢复每一个下载任务
                SQLDownloadInfo sqlDownloadInfo=sqlDownloadInfoList.get(i);
                Downloader sqlDownloader=new Downloader(context,sqlDownloadInfo,poolExecutor,iSupportBreakpoint,false);
                sqlDownloader.setDownloadSuccessListener(downloadSuccessListener);
                sqlDownloader.setDownloadListener("public",downloadTasksListener);
                taskList.add(sqlDownloader);
            }
        }
    }



    /**
     * 增加一个任务，默认开始执行下载任务
     * @param TaskID 任务号
     * @param url  请求下载的路径
     * @param fileName  文件名
     * @return  返回文件下载的路径
     */
    public int addTask(String TaskID,String url,String fileName){
        return addTask(TaskID,url,fileName,Rootdir);
    }

    /**
     * 增加一个任务，默认开始执行下载任务
     * @param TaskID
     * @param url
     * @param fileName
     * @param filePath
     * @return 返回下载路径
     */
    public int addTask(String TaskID,String url,String fileName,String filePath){

//        TaskInfo info=new TaskInfo();
//        info.setFileName(fileName);
//        info.setTaskID(TaskID);
//        info.setOnDownloading(true);

        if (TaskID==null){
            TaskID=fileName;
        }
        int state=getAttachmentState(TaskID,fileName,filePath);//返回我加入的下载任务状态
        if (state!=1){
            Toast.makeText(mContext,"文件已经存在任务列表或已存在下载路径中",Toast.LENGTH_SHORT).show();
            return state;//存在任务列表或者文件已经存在的都不用理。
        }

        /**
         * 任务信息存入数据库中
         */
        SQLDownloadInfo downloadInfo=new SQLDownloadInfo();
        /**设置数据库表的内容*/
        downloadInfo.setDownloadSize(0);
        downloadInfo.setFileSize(0);
        downloadInfo.setTaskID(TaskID);
        downloadInfo.setFileName(fileName);
        downloadInfo.setUrl(url);
        if (filePath==null){
            downloadInfo.setFilePath(FileHelper.getFileDefaultPath()+"/("+FileHelper.filterIDChars(TaskID)+")"+fileName);
        }else {
            downloadInfo.setFilePath(filePath);
        }

        Downloader taskDownloader=new Downloader(mContext,downloadInfo,poolExecutor,iSupportBreakpoint,true);
        taskDownloader.setDownloadSuccessListener(downloadSuccessListener);
        if (iSupportBreakpoint){
            taskDownloader.setSupportBreakpoint(true);
        }else {
            taskDownloader.setSupportBreakpoint(false);
        }
        taskDownloader.start();
        taskDownloader.setDownloadListener("public",downloadTasksListener);
        taskList.add(taskDownloader);
        return 1;
    }

    /**
     * 获取文件状态
     *
     * @param TaskID  任务号
     * @param fileName 文件名
     * @param filepath 下载到本地的路径
     * @return -1 : 文件已存在 ，0 ： 已存在任务列表 ， 1 ： 添加进任务列表
     */
    private int getAttachmentState(String TaskID,String fileName,String filepath){

        int taskSize=taskList.size();
        for (int i=0;i<taskSize;i++){//检查这个文件是不是任务列表中的
            Downloader downloader=taskList.get(i);//获取每个下载任务
            if (downloader.getTaskID().equals(TaskID)){
                return 0;//假如要添加的文件已经在下载的任务列表中就返回0
            }
        }

        File file=null;
        if (filepath==null){//假如设置的文件路径为空
            file=new File(FileHelper.getFileDefaultPath()+"/("+FileHelper.filterIDChars(TaskID)+")"+fileName);//根据默认路径来新建一个file
            if (file.exists()){
                return -1;//文件已经存在就返回－1
            }
        }else {//假如设置的文件路径不为空
            file=new File(filepath);//根据这个路径来新建一个file
            if (file.exists()){
                return -1;//文件已经存在就返回－1
            }
        }
        return 1 ;//两种情况都不符合就表示这个文件是新的，可以加入下载任务列表中
    }

    /**
     * 删除一个任务，包括已下载的本地文件
     * @param taskID
     */
    public void deleteTask(String taskID){
        int taskSize=taskList.size();
        for (int i=0;i<taskSize;i++){
            Downloader deletedDownloader=taskList.get(i);

            if (deletedDownloader.getTaskID().equals(taskID)){
                deletedDownloader.destroy();
                taskList.remove(deletedDownloader);
                break;
            }
        }
    }

    /**
     * 获取当前任务列表的所有任务ID
     * @return
     */
     public ArrayList<String> getAllTaskID(){
         ArrayList<String> taskIDlist=new ArrayList<String>();
         int listSize=taskList.size();
         for (int i=0;i<listSize;i++){
             Downloader downloader=taskList.get(i);
             taskIDlist.add(downloader.getTaskID());
         }
         return taskIDlist;
     }

    /**
     * 获取当前任务列表的所有任务，以TaskInfo列表形式返回
     * @return
     */
    public ArrayList<TaskInfo> getAllTask(){
        ArrayList<TaskInfo> taskInfoArrayList=new ArrayList<TaskInfo>();
        int listSize=taskList.size();

        for (int i=0;i<listSize;i++){
            Downloader downloader=taskList.get(i);
            SQLDownloadInfo sqlDownloadInfo=downloader.getSqlDownloadInfo();
            TaskInfo taskInfo=new TaskInfo();
            taskInfo.setFileName(sqlDownloadInfo.getFileName());
            taskInfo.setOnDownloading(downloader.isDonwloading());
            taskInfo.setTaskID(sqlDownloadInfo.getTaskID());
            taskInfo.setFileSize(sqlDownloadInfo.getFileSize());
            taskInfo.setDownFileSize(sqlDownloadInfo.getDownloadSize());
            taskInfoArrayList.add(taskInfo);
        }

        return taskInfoArrayList;
    }

    /**
     * 根据任务ID开始执行下载任务
     *
     * @param taskID
     */
    public void startTask(String taskID){
        int listSize=taskList.size();

        for (int i=0;i<listSize;i++){
            Downloader downloader=taskList.get(i);
            if (downloader.getTaskID().equals(taskID)){
                downloader.start();
                break;
            }
        }
    }

    /**
     * 根据任务ID停止相应的下载任务
     *
     * @param taskID
     */
    public void stopTask(String taskID){
        int listSize=taskList.size();

        for (int i=0;i<listSize;i++){
            Downloader downloader=taskList.get(i);

            if (downloader.getTaskID().equals(taskID)){
                downloader.stop();
                break;
            }
        }
    }

    /**
     * 开始当前任务列表里的所有任务
     */
    public void startAllTask(){
        int listSize=taskList.size();

        for (int i=0;i<listSize;i++){
            Downloader downloader=taskList.get(i);
            downloader.start();
        }
    }

    /**
     * 停止当前任务列表里的所有任务
     */
    public void stopAllTask(){
        int listSize=taskList.size();

        for (int i=0;i<listSize;i++){
            Downloader downloader=taskList.get(i);
            downloader.stop();
        }
    }

    /**
     * 根据任务ID将监听器设置相对应的下载任务
     *
     * @param taskID
     * @param listener
     */
    public void setSingleTaskListener(String taskID,DownloadListener listener){
        int listSize=taskList.size();

        for (int i=0;i<listSize;i++){
            Downloader downloader=taskList.get(i);

            if (downloader.getTaskID().equals(taskID)){
                downloader.setDownloadListener("private",listener);
                break;
            }
        }
    }

    /**
     * 将监听器设置到当前任务列表所有任务
     * @param listener
     */
    public void setAllTaskListener(DownloadListener listener){
        downloadTasksListener=listener;
        int listSize=taskList.size();

        for (int i=0;i<listSize;i++){
            Downloader downloader=taskList.get(i);
            downloader.setDownloadListener("public",listener);
        }
    }

    /**
     * 根据任务ID移除相对应的下载任务的监听器
     *
     * @param taskID
     */
    public void removeDownloadListener(String taskID){
        Downloader downloader=getDownloader(taskID);

        if (downloader!=null){
            downloader.removeDownloadListener("private");
        }
    }

    /**
     * 删除监听所有任务的监听器
     */
    public void removeAllDownloadListner(){
        int listSize=taskList.size();

        for (int i=0;i<listSize;i++){
            Downloader deletedDownloader=taskList.get(i);
            deletedDownloader.removeDownloadListener("public");
        }
    }

    /**
     * 根据任务号获取当前任务是否正在下载
     *
     * @param taskID
     * @return
     */
    public boolean isTaskDownloading(String taskID){
        Downloader downloader=getDownloader(taskID);
        if (downloader!=null){
            return downloader.isDonwloading();
        }
        return false;
    }

    /**
     * 根据附件ID获取下载器
     *
     * @param taskID
     * @return
     */
    private Downloader getDownloader(String taskID){
        for (int i=0;i<taskList.size();i++){
            Downloader downloader=taskList.get(i);

            if (taskID!=null&&taskID.equals(downloader.getTaskID())){
                return downloader;
            }
        }
        return null;
    }

    /**
     * 根据ID获取下载任务列表中某个任务
     *
     * @param taskID
     * @return
     */
    public TaskInfo getTaskInfo(String taskID){
        Downloader downloader=getDownloader(taskID);
        if (downloader==null){
            return null;
        }
        SQLDownloadInfo sqlDownloadInfo=downloader.getSqlDownloadInfo();
        if (sqlDownloadInfo==null){
            return null;
        }

        TaskInfo taskInfo=new TaskInfo();
        taskInfo.setFileName(sqlDownloadInfo.getFileName());
        taskInfo.setOnDownloading(downloader.isDonwloading());
        taskInfo.setTaskID(sqlDownloadInfo.getTaskID());
        taskInfo.setDownFileSize(sqlDownloadInfo.getDownloadSize());
        taskInfo.setFileSize(sqlDownloadInfo.getFileSize());
        return taskInfo;
    }

    /**
     * 外部调用者设置下载管理是否支持断点续传
     *
     * @param isSupportBreakpoint
     */
    public void setSupportBreakpoint(boolean isSupportBreakpoint){
        if ((!this.iSupportBreakpoint)&&isSupportBreakpoint){
            int taskSize=taskList.size();
            for (int i=0;i<taskSize;i++){
                Downloader downloader=taskList.get(i);
                downloader.setSupportBreakpoint(true);
            }
        }
        this.iSupportBreakpoint=isSupportBreakpoint;

    }


}
