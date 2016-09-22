package com.example.downloadad.dataManager;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

/**
 * 类功能描述：继承自SQLiteOpenHelper帮助类，借助此类可对数据库进行创建、uodate
 *
 * Created by wzxx 16/9/13.
 *
 * @author wzxx
 * @wzxxkcer@foxmail.com
 * @version 1.0
 */
public class SQLiteHelper extends SQLiteOpenHelper{

    private final static String databaseName="fileDownloader";//数据库的名称
    private static SQLiteDatabase.CursorFactory mFactory=null;
    private final static int mVersion=1;
    public final static String TABLE_NAME="downloadInfo";//数据表的名称

    private String downloadSQL="CREATE TABLE IF NOT EXISTS "+ TABLE_NAME +" ("
            + "id INTEGER PRIMARY KEY  AUTOINCREMENT  NOT NULL , "
            + "taskID VARCHAR, "
            + "url VARCHAR, "
            + "filePath VARCHAR, "
            + "fileName VARCHAR, "
            + "fileSize VARCHAR, "
            + "downLoadSize VARCHAR "
            + ")";

    private Context mContext;

    public SQLiteHelper(Context context){
        super(context,databaseName,mFactory,mVersion);
    }

    public SQLiteHelper(Context context, String name, SQLiteDatabase.CursorFactory factory,int version){
        super(context,name,factory,version);
        mContext=context;
    }

    @Override
    public void onCreate(SQLiteDatabase db){
        db.execSQL(downloadSQL);
//        Toast.makeText(mContext,"调试（可删）：保存到数据库中",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){
        //暂时无更新
    }

    @Override
    public void onOpen(SQLiteDatabase db){
        super.onOpen(db);//每次打开数据库的时候会调用
    }
}
