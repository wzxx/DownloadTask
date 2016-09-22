package com.example.downloadad.dataManager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

/**
 * 类功能描述：信息存储类，主要在任务下载各个环节执行数据的存储(也可以删除下载任务)
 * p.s.每一个dataKeeper对象，代表一个下载任务
 *
 * Created by wzxx on 16/9/13.
 *
 * @author wzxx
 * @wzxxkcer@foxmail.com
 * @version 1.0
 */
public class DataKeeper {
    private SQLiteHelper dbHelper;
    private SQLiteDatabase db;
    private int saveTimes=0;

    /**
     * 构造器
     * @param context
     */
    public DataKeeper(Context context){
        this.dbHelper=new SQLiteHelper(context);
    }

    /**
     * 保存一个任务的下载信息到数据库中
     *
     * @param downloadInfo
     */
    public void saveDownloadInfo(SQLDownloadInfo downloadInfo){
        ContentValues values=new ContentValues();
        values.put("taskID",downloadInfo.getTaskID());
        values.put("url",downloadInfo.getUrl());
        values.put("filePath",downloadInfo.getFilePath());
        values.put("fileName",downloadInfo.getFileName());
        values.put("fileSize",downloadInfo.getFileSize());
        values.put("downLoadSize",downloadInfo.getDownloadSize());
        //ok，已经设置好了

        //要么添加数据要么更新数据，根据情况
        Cursor cursor=null;
        try {
            db=dbHelper.getWritableDatabase();
            cursor=db.rawQuery("select * from"+SQLiteHelper.TABLE_NAME
            +"where taskID=?",new String[]{downloadInfo.getTaskID()});//获得索引

            if (cursor.moveToNext()){//存在就更新数据
                db.update(SQLiteHelper.TABLE_NAME,values,"taskID=?",new String[]{downloadInfo.getTaskID()});
            }else {//不存在就插入数据
                db.insert(SQLiteHelper.TABLE_NAME,null,values);
            }
            cursor.close();
            db.close();
        }catch (Exception e){//更新或插入失败了
            saveTimes++;
            if (saveTimes<5){//最多只做5次数据保存，降低数据保存失败率
                saveDownloadInfo(downloadInfo);
            }else {
                saveTimes=0;
            }

            if (cursor!=null){
                cursor.close();
            }

            if (db!=null){
                db.close();
            }
        }
        saveTimes=0;
    }

    /**
     * 从数据库中获取某个下载任务
     *
     * @param taskID
     * @return
     */
    public SQLDownloadInfo getDownloadInfo(String taskID){
        SQLDownloadInfo downloadInfo=null;
        db=dbHelper.getWritableDatabase();
        Cursor cursor=db.rawQuery("select * from"+SQLiteHelper.TABLE_NAME
        +"where taskID=?",new String[]{taskID});//找到哪个任务ID的索引呐

        if (cursor.moveToFirst()){//数据库获取的数据保存到SQLDownloadInfo中
            downloadInfo=new SQLDownloadInfo();
            downloadInfo.setTaskID(cursor.getString(cursor.getColumnIndex("taskID")));
            downloadInfo.setUrl(cursor.getString(cursor.getColumnIndex("url")));
            downloadInfo.setFilePath(cursor.getString(cursor.getColumnIndex("filePath")));
            downloadInfo.setFileName(cursor.getString(cursor.getColumnIndex("fileName")));
            downloadInfo.setFileSize(cursor.getLong(cursor.getColumnIndex("fileSize")));
            downloadInfo.setDownloadSize(cursor.getLong(cursor.getColumnIndex("downLoadSize")));
        }
        cursor.close();
        db.close();
        return downloadInfo;
    }

    /**
     * 从数据库中获取所有的下载任务并赋值。
     *
     * @return
     */
    public ArrayList<SQLDownloadInfo> getAllDownloadInfo(){
        ArrayList<SQLDownloadInfo> downloadInfoArrayList=new ArrayList<SQLDownloadInfo>();
        db=dbHelper.getWritableDatabase();

        Cursor cursor = db.rawQuery("SELECT * from " + SQLiteHelper.TABLE_NAME, null);

           while (cursor.moveToNext()) {
               SQLDownloadInfo downloadInfo = new SQLDownloadInfo();
               downloadInfo.setTaskID(cursor.getString(cursor.getColumnIndex("taskID")));
               downloadInfo.setUrl(cursor.getString(cursor.getColumnIndex("url")));
               downloadInfo.setFilePath(cursor.getString(cursor.getColumnIndex("filePath")));
               downloadInfo.setFileName(cursor.getString(cursor.getColumnIndex("fileName")));
               downloadInfo.setFileSize(cursor.getLong(cursor.getColumnIndex("fileSize")));
               downloadInfo.setDownloadSize(cursor.getLong(cursor.getColumnIndex("downLoadSize")));
               downloadInfoArrayList.add(downloadInfo);
           }
           cursor.close();
        db.close();
        return downloadInfoArrayList;
    }

    /**
     * 删除某个下载任务
     *
     * @param taskID
     */
    public void deleteDownloadInfo(String taskID){
        db.delete(SQLiteHelper.TABLE_NAME,"taskID=?",new String[]{taskID});
        db.close();
    }

    /**
     * 删除所有下载任务
     */
    public void deleteAllDownloadInfo(){
        db=dbHelper.getWritableDatabase();
        db.delete(SQLiteHelper.TABLE_NAME,null,null);
        db.close();
    }
}
