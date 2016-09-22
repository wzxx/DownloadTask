package com.example.downloadad;

import com.example.downloadad.dataManager.SQLDownloadInfo;

/**
 * 类功能描述：监听器。
 *
 * Created by wzxx on 16/9/13.
 */
public interface DownloadListener {

    /**
     * 开始下载文件
     * @param sqlDownloadInfo 下载任务对象
     */
    void onStart(SQLDownloadInfo sqlDownloadInfo);

    /**
     * 文件下载进度情况
     * @param sqlDownloadInfo 下载任务对象
     * @param isSupportBreakpoint 服务器是否支持断点续传
     */
    void onProgress(SQLDownloadInfo sqlDownloadInfo,boolean isSupportBreakpoint);

    /**
     * 停止下载完毕
     * @param sqlDownloadInfo 下载任务对象
     * @param isSupportBreakpoint 服务器是否支持断点续传
     */
    void onStop(SQLDownloadInfo sqlDownloadInfo,boolean isSupportBreakpoint);

    /**
     * 文件下载失败
     * @param sqlDownloadInfo 下载任务对象
     */
    void onError(SQLDownloadInfo sqlDownloadInfo);

    /**
     * 文件下载成功
     * @param sqlDownloadInfo 下载任务对象
     */
    void onSuccess(SQLDownloadInfo sqlDownloadInfo);
}
