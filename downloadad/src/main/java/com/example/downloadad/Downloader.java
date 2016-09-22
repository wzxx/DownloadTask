package com.example.downloadad;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.example.downloadad.dataManager.DataKeeper;
import com.example.downloadad.dataManager.FileHelper;
import com.example.downloadad.dataManager.SQLDownloadInfo;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 类功能描述：下载操作类。包括每一个下载任务dataKeeper对象的操作。
 *
 * 麻烦麻烦，好好想想怎么写一个执行下载的类。容易写乱.
 * so，我还是写乱了，唧唧歪歪不清不楚
 *
 * Created by wzxx on 16/9/13.
 *
 * @author wzxx
 * @email wzxxkcer@foxmail.com
 * @version 1.0
 */
public class Downloader {
    /**
     * 下载任务的状态
     */
    private int TASK_START = 0;//开始
    private int TASK_STOP = 1;//结束
    private int TASK_PROGESS = 2;//进行
    private int TASK_ERROR = 3;//出错
    private int TASK_SUCCESS = 4;//成功

    /**文件临时路径*/
    private final String TEMP_FILEPATH= FileHelper.getTempDirPath();

    /**标识服务器是否支持断点续传（默认不可，还没做到这里，先做个标记）*/
    private boolean isSupportBreakpoint=false;

    private DataKeeper dataKeeper;
    private HashMap<String,DownloadListener> listenerHashMap;
    private DownloadSuccess downloadSuccess;
    private SQLDownloadInfo sqlDownloadInfo;//存储信息的数据库表内容
    private DownloadThread downloadThread;//下载的线程

    private long fileSize=0;//文件总大小
    private long downloadSizee=0;//已下载的文件大小
    private int downloadTimes=0;//当前尝试请求下载的次数
    private int maxDownloadTimes=3;//失败重新请求下载的次数

    private boolean onDownload=false;//当前任务的状态，已下载完毕为true，未下载为false

    /**线程池 */
    private ThreadPoolExecutor pool;

    /**
     * 构造器
     *
     * 需要传入的参数：
     * @param context 上下文
     * @param sqlFileInfo 任务对象
     * @param pool 线程池
     * @param isSupportBreakpoint 服务器是否支持断点续传
     * @param isNewTask 标识是新任务还是根据数据库构建的任务
     */
    public Downloader(Context context, SQLDownloadInfo sqlFileInfo, ThreadPoolExecutor pool,boolean isSupportBreakpoint,boolean isNewTask){
        //初始化各种数据
        this.isSupportBreakpoint=isSupportBreakpoint;
        this.pool=pool;
        fileSize=sqlFileInfo.getFileSize();
        downloadSizee=sqlFileInfo.getDownloadSize();
        dataKeeper=new DataKeeper(context);
        listenerHashMap=new HashMap<String,DownloadListener>();
        sqlDownloadInfo=sqlFileInfo;

        if (isNewTask){//如果是新的任务
            saveDownloadInfo();//嘿嘿，里面还要进行判断的。如果不需要断点续传就不用保存任务信息到数据库啦
        }
    }

    /**
     * 将线程加入线程池,就可以运行了。
     */
    public void start(){
        if (downloadThread==null){//假如没有下载的线程
            downloadTimes=0;
            onDownload=true;
            handler.sendEmptyMessage(TASK_START);
            downloadThread=new DownloadThread();
            pool.execute(downloadThread);
        }
    }

    /**
     * 把线程移除出线程池
     */
    public void stop(){
        if (downloadThread!=null){
            onDownload=false;
            downloadThread.stopDownload();
            pool.remove(downloadThread);
            downloadThread=null;
        }
    }

    /**
     * 设置下载任务的监听器方法
     *
     * @param key
     * @param listener
     */
    public void setDownloadListener(String key,DownloadListener listener){
        if (listener==null){//加入监听器不存在就移出
            removeDownloadListener(key);
        }else {
            listenerHashMap.put(key,listener);
        }
    }

    /**
     * 从监听器列表中移除监听器
     * @param key
     */
    public void removeDownloadListener(String key){
        if (listenerHashMap.containsKey(key)){
            listenerHashMap.remove(key);
        }
    }

    /**
     * 设置下载成功监听器
     * @param downloadSuccess
     */
    public void setDownloadSuccessListener(DownloadSuccess downloadSuccess){
        this.downloadSuccess=downloadSuccess;
    }

    /**
     * 不仅要停止线程，还要把任务信息从数据库中删除、从临时路径中删除
     */
    public void destroy(){
        if (downloadThread!=null){
            downloadThread.stopDownload();
            downloadThread=null;
        }
        dataKeeper.deleteDownloadInfo(sqlDownloadInfo.getTaskID());
        File downloadFile=new File(TEMP_FILEPATH+"/("+FileHelper.filterIDChars(sqlDownloadInfo.getTaskID())+")+" +
                sqlDownloadInfo.getFileName());
        if (downloadFile.exists()){
            downloadFile.delete();
        }
    }

    /**
     * 返回当前任务进行的状态
     * @return
     */
    public boolean isDonwloading(){
        return onDownload;
    }

    public String getTaskID(){
        return sqlDownloadInfo.getTaskID();
    }

    /**
     * 设置是否支持断点续传
     * @param isSupportBreakpoint
     */
    public void setSupportBreakpoint(boolean isSupportBreakpoint){
        this.isSupportBreakpoint=isSupportBreakpoint;
    }

    /**
     * 移动文件。将文件从临时路径移动到数据库保存的路径中。
     * @return
     */
    public boolean RenameFile(){
        File newFile=new File(sqlDownloadInfo.getFilePath());
        if (newFile.exists()){
            newFile.delete();
        }
        File oldFile=new File(TEMP_FILEPATH+"/("+FileHelper.filterIDChars(sqlDownloadInfo.getTaskID())+")"+sqlDownloadInfo.getFileName());

        String filePath=sqlDownloadInfo.getFilePath();
        filePath=filePath.substring(0,filePath.lastIndexOf("/"));
        File file=new File(filePath);
        if (!file.exists()){
            file.mkdirs();
        }
        return oldFile.renameTo(newFile);
    }



    /******************************************内部方法**********************************************/

    /**
     * 假如是支持断点续传，就需要把下载信息保存到数据库中
     */
    private void saveDownloadInfo(){
        if (isSupportBreakpoint){
            sqlDownloadInfo.setDownloadSize(downloadSizee);//设置已下载的文件的大小
            dataKeeper.saveDownloadInfo(sqlDownloadInfo);//将任务信息存入数据库中
        }
    }

    /**
     * 判断文件夹是否存在，不存在就创建
     * @return
     */
    private boolean isFolderExist(){
        boolean result=false;
        try{
            String filePath=TEMP_FILEPATH;//临时路径在这里
            File file=new File(filePath);//根据临时路径创建文件
            if (!file.exists()){//假如文件不存在
                if (file.mkdirs()){
                    result=true;//就创建以后设置结果为存在true
                }
            }else {
                result=true;//文件存在就直接返回true
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 获取当前任务对象
     * @return
     */
    public SQLDownloadInfo getSqlDownloadInfo(){
        sqlDownloadInfo.setDownloadSize(downloadSizee);
        return sqlDownloadInfo;
    }

    /**
     * 通知监听器，任务已经开始下载
     */
    private void startNotice(){
        if (!listenerHashMap.isEmpty()){
            Collection<DownloadListener> c=listenerHashMap.values();
            Iterator<DownloadListener> it=c.iterator();
            while(it.hasNext()){
                DownloadListener listener=(DownloadListener)it.next();
                listener.onStart(getSqlDownloadInfo());
            }
        }
    }

    /**
     * 通知监听器，当前任务进度
     */
    private void onProgressNotice(){
        if (!listenerHashMap.isEmpty()){
            Collection<DownloadListener> c=listenerHashMap.values();
            Iterator<DownloadListener> it=c.iterator();
            while(it.hasNext()){
                DownloadListener listener=(DownloadListener)it.next();
                listener.onProgress(getSqlDownloadInfo(),isSupportBreakpoint);
            }
        }
    }

    /**
     * 通知监听器，当前任务已经停止
     */
    private void stopNotice(){
        if (!isSupportBreakpoint){//假如当前不支持断点续传，已下载文件的大小就重置为0
            downloadSizee=0;
        }
        if (!listenerHashMap.isEmpty()){
            Collection<DownloadListener> c=listenerHashMap.values();
            Iterator<DownloadListener> it=c.iterator();
            while (it.hasNext()){
                DownloadListener listener=(DownloadListener)it.next();
                listener.onStop(getSqlDownloadInfo(),isSupportBreakpoint);
            }
        }
    }

    /**
     * 通知监听器，当前任务异常，并进入停止状态
     */
    private void errorNotice(){
        if (!listenerHashMap.isEmpty()){
            Collection<DownloadListener> c=listenerHashMap.values();
            Iterator<DownloadListener> it=c.iterator();
            while(it.hasNext()){
                DownloadListener listener=(DownloadListener)it.next();
                listener.onError(getSqlDownloadInfo());
            }
        }
    }

    /**
     * 通知监听器，当前任务成功执行完毕
     */
    private void successNotice(){
        if (!listenerHashMap.isEmpty()){
            Collection<DownloadListener> c=listenerHashMap.values();
            Iterator<DownloadListener> it=c.iterator();
            while(it.hasNext()){
                DownloadListener listener=(DownloadListener)it.next();
                listener.onSuccess(getSqlDownloadInfo());
            }
        }
        if (downloadSuccess!=null){//假如下载任务成功
            downloadSuccess.onTaskSuccess(sqlDownloadInfo.getTaskID());
        }
    }


    /************************************************************************************************************/




/***********************************************接口，内部类,handler****************************************************/

    /**
     * 下载成功的接口。
     * 接口功能描述：用于在任务执行完成之后通知下载管理器，以便下载管理器将已完成的任务移出任务列表
     */
    public interface DownloadSuccess{
        void onTaskSuccess(String TaskID);
    }

    Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg){
            if (msg.what==TASK_START){//开始下载
                startNotice();
            }else if (msg.what==TASK_STOP){//停止下载
                stopNotice();
            }else if (msg.what==TASK_PROGESS){//改变进程
                onProgressNotice();
            }else if (msg.what==TASK_ERROR){//下载出错
                errorNotice();
            }else if (msg.what==TASK_SUCCESS){//下载完成
                successNotice();
            }
        }
    };

    /**
     * 类功能描述：文件下载线程
     */
    class DownloadThread implements Runnable{
        private boolean isDownloading;
        private URL url;
        private RandomAccessFile localFile;//用来访问保存数据记录的文件
        private HttpURLConnection urlConnection;
        private InputStream inputStream;
        private int progress=-1;

        public DownloadThread(){
            isDownloading=true;//正在下载
        }


        /**
         * 重写run方法：下载的操作
         */
        @Override
        public void run(){
            while (downloadTimes<maxDownloadTimes){//3次请求尝试
                try {
                    if (downloadSizee==fileSize&&fileSize>0){//如果已经下载完成
                        onDownload=false;
                        Message msg=new Message();
                        msg.what=TASK_PROGESS;//消息码
                        msg.arg1=100;
                        handler.sendMessage(msg);
                        downloadTimes=maxDownloadTimes;
                        downloadThread=null;//此下载线程结束
                        return;
                    }

                    url=new URL(sqlDownloadInfo.getUrl());//错误java.net.MalformedURLException
                    urlConnection=(HttpURLConnection)url.openConnection();//java.io.Exception
                    //自由定制
                    urlConnection.setConnectTimeout(5000);//设置连接超时
                    urlConnection.setReadTimeout(10000);//读取超时的毫秒数
                    if (fileSize<1){//第一次下载，就要先初始化
                        openConnection();//java.lang.Exception
                    }else {
                        if (new File(TEMP_FILEPATH+"/("+FileHelper.filterIDChars(sqlDownloadInfo.getTaskID())+")"+
                                sqlDownloadInfo.getFileName()).exists()){//假如这个临时路径的文件存在
                            //java.io.FileNotFoundException
                            localFile=new RandomAccessFile(TEMP_FILEPATH+"/("+FileHelper.filterIDChars(sqlDownloadInfo.getTaskID())+")"
                                    +sqlDownloadInfo.getFileName(),"rwd");
                            localFile.seek(downloadSizee);//java.io.Exception.将文件指针定位到已下载的位置
                            urlConnection.setRequestProperty("Range","bytes="+downloadSizee+"-");
                        }else {//假如不存在
                            fileSize=0;
                            downloadSizee=0;
                            saveDownloadInfo();
                            openConnection();//java.lang.Exception
                        }
                    }
                    inputStream=urlConnection.getInputStream();
                    byte[] buffer=new byte[1024*4];
                    int length=-1;
                    while((length=inputStream.read(buffer))!=-1&&isDownloading){
                        localFile.write(buffer,0,length);//java.io.IOException
                        downloadSizee+=length;
                        int nowProgress=(int)((100*downloadSizee)/fileSize);
                        if (nowProgress>progress){
                            progress=nowProgress;
                            handler.sendEmptyMessage(TASK_PROGESS);
                        }
                    }
                    //下载完了
                    if (downloadSizee==fileSize){
                        boolean renameResult=RenameFile();

                        if (renameResult){
                            handler.sendEmptyMessage(TASK_SUCCESS);
                        }else {
                            new File(TEMP_FILEPATH+"/("+FileHelper.filterIDChars(sqlDownloadInfo.getTaskID())+")"+
                            sqlDownloadInfo.getFileName()).delete();
                            handler.sendEmptyMessage(TASK_ERROR);
                        }

                        //清除数据库任务
                        dataKeeper.deleteDownloadInfo(sqlDownloadInfo.getTaskID());
                        downloadThread=null;
                        onDownload=false;
                    }
                    downloadTimes=maxDownloadTimes;
                }catch (Exception e){
                    if (isDownloading){

                        if (isSupportBreakpoint){
                            downloadTimes++;

                            if (downloadTimes>=maxDownloadTimes){
                                if (fileSize>0){
                                    saveDownloadInfo();
                                }
                                pool.remove(downloadThread);
                                downloadThread=null;
                                onDownload=false;
                                handler.sendEmptyMessage(TASK_ERROR);
                            }
                        }else {
                            downloadSizee=0;
                            downloadTimes=maxDownloadTimes;
                            onDownload=false;
                            downloadThread=null;
                            handler.sendEmptyMessage(TASK_ERROR);
                        }
                    }else {
                        downloadTimes=maxDownloadTimes;
                    }
                    e.printStackTrace();
                }finally {
                    try {
                        if (urlConnection!=null){
                            urlConnection.disconnect();
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                    try {
                        if (inputStream!=null){
                            inputStream.close();
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                    try {
                        if (localFile!=null){
                            localFile.close();
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }

        /**
         * 停止下载
         */
        public void stopDownload(){
            isDownloading=false;
            downloadTimes=maxDownloadTimes;
            if (fileSize>0){
                saveDownloadInfo();//停止任务消息发送之前要把所有信息存入数据库中。当然具体要不要存入数据库中就由该方法决定了
            }
            handler.sendEmptyMessage(TASK_STOP);//handler发送任务停止消息
        }

        /**
         * 打开网络连接
         * @throws Exception
         */
        private void openConnection() throws Exception{
            long urlFileSize=urlConnection.getContentLength();//下载的网络文件的大小
            if (urlFileSize>0){
                //判断文件是否存在
                isFolderExist();
                localFile=new RandomAccessFile(TEMP_FILEPATH+"/("+FileHelper.filterIDChars(sqlDownloadInfo.getTaskID())+")"+
                        sqlDownloadInfo.getFileName(),"rwd");
                localFile.setLength(urlFileSize);
                sqlDownloadInfo.setFileSize(urlFileSize);
                fileSize=urlFileSize;
                if (isDownloading){
                    saveDownloadInfo();
                }
            }
        }
    }

    /***************************************************************************************************/

}
