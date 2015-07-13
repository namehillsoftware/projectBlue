package com.lasthopesoftware.bluewater.servers.library.items.media.files.storage.downloads.provider;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    public long downloadFile(IFile file, String path) {
        try {
	        final File systemFile = new File(path);
	        final File parentFile = systemFile.getParentFile();
	        if (!parentFile.exists())
		        parentFile.mkdirs();

            final DownloadManager.Request request = new DownloadManager.Request(file.getRemoteFileUri(mContext))
		            .setDestinationUri(Uri.fromFile(systemFile))
		            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
		            .setVisibleInDownloadsUi(true);
            return mDownloadManager.enqueue(request);
        } catch (IOException e) {
            mLogger.error("Error getting URI for " + file.getValue(), e);
            return -1;
        }
    }

	public List<FileDownload> getAllActiveDownloads() {
		final Cursor downloadCursor = mDownloadManager.query(new DownloadManager.Query());
		ArrayList<FileDownload> fileDownloads = new ArrayList<>(downloadCursor.getCount());
		while (downloadCursor.moveToNext()) {
			final int columnIdIndex = downloadCursor.getColumnIndex(DownloadManager.COLUMN_ID);
			final int statusIndex = downloadCursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
			final int errorReasonIndex = downloadCursor.getColumnIndex(DownloadManager.COLUMN_REASON);
			fileDownloads.add(new FileDownload(downloadCursor.getLong(columnIdIndex), downloadCursor.getInt(statusIndex), downloadCursor.getInt(errorReasonIndex)));
		}

		return fileDownloads;
	}

	public class FileDownload {
		public final long id;
		public final String status;
		public final String reason;

		public FileDownload(long id, int status, int reason) {
			this.id = id;

			switch (status) {
				case DownloadManager.STATUS_FAILED:
					this.status = "FAILED";
					break;
				case DownloadManager.STATUS_PAUSED:
					this.status = "PAUSED";
					break;
				case DownloadManager.STATUS_PENDING:
					this.status = "PENDING";
					break;
				case DownloadManager.STATUS_RUNNING:
					this.status = "RUNNING";
					break;
				case DownloadManager.STATUS_SUCCESSFUL:
					this.status = "SUCCESSFUL";
					break;
				default:
					this.status = "Unknown";
			}

			switch (reason) {
				case DownloadManager.ERROR_CANNOT_RESUME:
					this.reason = "CANNOT RESUME";
					break;
				case DownloadManager.ERROR_DEVICE_NOT_FOUND:
					this.reason = "DEVICE NOT FOUND";
					break;
				case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
					this.reason = "FILE ALREADY EXISTS";
					break;
				case DownloadManager.ERROR_FILE_ERROR:
					this.reason = "FILE ERROR";
					break;
				case DownloadManager.ERROR_HTTP_DATA_ERROR:
					this.reason = "HTTP DATA ERROR";
					break;
				case DownloadManager.ERROR_INSUFFICIENT_SPACE:
					this.reason = "INSUFFICIENT SPACE";
					break;
				case DownloadManager.ERROR_TOO_MANY_REDIRECTS:
					this.reason = "TOO MANY REDIRECTS";
					break;
				case DownloadManager.ERROR_UNHANDLED_HTTP_CODE:
					this.reason = "UNHANDLED HTTP CODE";
					break;
				case DownloadManager.ERROR_UNKNOWN:
				default:
					this.reason = "UNKNOWN";
					break;
			}
		}
	}
}
