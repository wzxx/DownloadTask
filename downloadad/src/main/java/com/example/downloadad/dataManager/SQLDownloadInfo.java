package com.example.downloadad.dataManager;

/**
 * 类描述：为了保持本地持久化，将下载任务放到本地数据库中。
 * 这里头存放的是表中存放的每一串数据的信息，并且每当有信息来的时候还可以设置数据的信息。
 *
 * Created by wzxx on 16/9/13.
 * @author wzxx
 * @email wzxxkcer@foxmail.com
 * @version 1.0
 */
public class SQLDownloadInfo {

    private String taskID;
    private String url;
    private String filePath;
    private String fileName;
    private long fileSize;
    private long downloadSize;

    public String getTaskID() {
        return taskID;
    }
    public void setTaskID(String taskID) {
        this.taskID = taskID;
    }

    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }

    public String getFilePath() {
        return filePath;
    }
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public long getDownloadSize() {
        return downloadSize;
    }
    public void setDownloadSize(long downloadSize) {
        this.downloadSize = downloadSize;
    }


    @Override
    public String toString(){
        return "taskID="+taskID+";url="+url+ ";filePath="+filePath+";fileName="
                +fileName+";fileSize="+fileSize+";downloadSize="+downloadSize;
    }
}
