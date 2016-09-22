package com.example.downloadad;

/**
 * 专门用于存储一串数据的信息，用于显示下载任务信息（任务ID ,文件名，文件大小，已下载的文件大小）｝
 *
 * Created by wzxx on 16/9/13.
 *
 * @author wzxx
 * @email wzxxkcer@foxmail.com
 * @version 1.0
 */
public class TaskInfo {

    private boolean isOnDownloading;
    private String taskID;
    private String fileName;
    private long fileSize=0;
    private long downFileSize=0;

    public boolean isOnDownloading(){
        return isOnDownloading;
    }
    public void setOnDownloading(boolean isOnDownloading){
        this.isOnDownloading=isOnDownloading;
    }

    public String getTaskID(){
        return taskID;
    }
    public void setTaskID(String taskID){
        this.taskID=taskID;
    }

    public String getFileName(){
        return fileName;
    }
    public void setFileName(String fileName){
        this.fileName=fileName;
    }

    public long getFileSize(){
        return fileSize;
    }
    public void setFileSize(long fileSize){
        this.fileSize=fileSize;
    }

    public long getDownFileSize(){
        return downFileSize;
    }
    public void setDownFileSize(long downFileSize){
        this.downFileSize=downFileSize;
    }

    /**
     * 获得进度
     * @return
     */
    public int getProgress(){
        if (0==fileSize){
            return 0;
        }else {
            return ((int)(100*downFileSize/fileSize));
        }
    }

    /**
     * 返回文件类型
     * @return
     */
    public String getType(){
        String type=null;
        if (fileName!=null){//假如文件名不为空
            String name=fileName.toLowerCase();//把文件名转化为小写字母
            if (name.contains(".")){
                type=name.substring(name.lastIndexOf(".",name.length()));//酱就截取了文件的文件类型啦
            }
        }
        return type;//返回
    }
}
