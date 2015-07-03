package com.lasthopesoftware.bluewater.servers.library.items.media.files.storage.downloads.provider;

import android.app.DownloadManager;
import android.content.Context;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by david on 6/6/15.
 */
public class FileDownloadProvider {

    private static final Logger mLogger = LoggerFactory.getLogger(FileDownloadProvider.class);

    private final Context mContext;
    private final DownloadManager mDownloadManager;

    public FileDownloadProvider(Context context) {
        mContext = context;
        mDownloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
    }

    /**
     * Enqueues a file to be downloaded
     * @param file
     */
    public void downloadFile(IFile file) {
        try {
            mDownloadManager.enqueue(new DownloadManager.Request(file.getFileUri(mContext)));
        } catch (IOException e) {
            mLogger.error("Error getting URI for " + file.getValue(), e);
        }
    }
}
