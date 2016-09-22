package com.example.downloadad.dataManager;

import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 类功能描述：文件操作的辅助类。把下载文件存储到本地文件路径中。
 *
 * Created by wzxx on 16/9/13.
 *
 * @author wzxx
 * @email wzxxkcer@foxmail.com
 * @version 1.0
 */
public class FileHelper {

    private static String baseFilePath= Environment.getExternalStorageDirectory().toString()+"/downloadFiles";
    /**
     * 下载文件的路径
     */
    private static String downloadFilePath=baseFilePath+"/FILETEMP";
    /**
     * 下载文件的临时路径
     */
    private static String tempDirPath=baseFilePath+"/TEMPDir";

    private static String CustomFilePath;

    private static String[] wrongChars={
            "/","\\","*","?","<",">","\"","|"
    };

    /**
     * 创建文件（也不知道将来会不会用到，再说呗）
     *
     * @param file
     */
    public void newFile(File file){
        if (!file.exists()){
            try {
                file.createNewFile();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    /**
     * 创建目录（也不知道将来会不会用到，再说呗）
     *
     * @param file
     */
    public static void newDirfILE(File file){
        if (!file.exists()){
            file.mkdir();
        }
    }

    /**
     * 获取一个文件列表的总文件大小（也不知道将来会不会用到，再说呗）
     * @param willUpload
     * @return
     */
    public static double getSize(List<String> willUpload){
        return (double)getSizeUnitByte(willUpload)/(1024*1024);
    }

    /**
     * 计算所有文件总大小，单位是字节
     * @param willUpload 下载的文件的列表
     * @return
     */
    private static long getSizeUnitByte(List<String> willUpload){
        long allFileSize=0;//初始化所有文件大小为0
        for (int i=0;i<willUpload.size();i++){
            File newFile=new File(willUpload.get(i));
            if (newFile.exists()&&newFile.isFile()){//假如存在且它是个文件
                allFileSize=allFileSize+newFile.length();
            }
        }
        return allFileSize;
    }

    /**
     * 获取默认文件存放路径为baseFilePath+"/FILETEMP"；
     * @return
     */
    public static String getFileDefaultPath(){
        return downloadFilePath;
    }

    /**
     * 获取下载文件的临时路径
     */
    public static String getTempDirPath(){
        return tempDirPath;
    }

    public void setCustomFilePath(String filePath){
        CustomFilePath=filePath;
    }

    public String getCustomFilePath(){
        return CustomFilePath;
    }
    /**
     * 过滤附件id中某些不能存在在文件名中的字符。
     * 就是上面
     * @param attID
     * @return
     */
    public static String filterIDChars(String attID){
        if (attID!=null){
            for (int i=0;i<wrongChars.length;i++){
                String c=wrongChars[i];
                if (attID.contains(c)){
                    attID=attID.replaceAll(c,"");//清除
                }
            }
        }
        return attID;
    }
}
