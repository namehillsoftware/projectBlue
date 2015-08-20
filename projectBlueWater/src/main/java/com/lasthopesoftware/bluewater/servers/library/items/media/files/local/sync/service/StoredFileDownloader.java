package com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.service;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.BatteryManager;

import com.j256.ormlite.logger.Logger;
import com.j256.ormlite.logger.LoggerFactory;
import com.lasthopesoftware.bluewater.servers.connection.ConnectionInfo;
import com.lasthopesoftware.bluewater.servers.connection.SessionConnection;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.StoredFileAccess;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.store.StoredFile;
import com.lasthopesoftware.bluewater.servers.store.Library;
import com.lasthopesoftware.threading.IOneParameterAction;

import org.apache.commons.io.IOUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StoredFileDownloader {

	private static final ExecutorService mStoreFilesExecutor = Executors.newSingleThreadExecutor();
	private static final Logger mLogger = LoggerFactory.getLogger(StoredFileDownloader.class);

	private boolean mIsHalted = false;

	private final StoredFileAccess mStoredFileAccess;
	private final Context mContext;
	private final Set<Integer> mQueuedFileKeys = new HashSet<>();

	private IOneParameterAction<StoredFile> mOnFileDownloaded;
	private Runnable mOnFileQueueEmpty;

	public StoredFileDownloader(Context context, Library library) {
		mContext = context;
		mStoredFileAccess = new StoredFileAccess(context, library);
	}

	public void queueFileForDownload(final IFile serviceFile, final StoredFile storedFile) {
		final int fileKey = serviceFile.getKey();
		if (!mQueuedFileKeys.add(fileKey)) return;

		mStoreFilesExecutor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					final java.io.File file = new java.io.File(storedFile.getPath());
					if (mIsHalted || (storedFile.isDownloadComplete() && file.exists()))
						return;

					if (ConnectionInfo.getConnectionType(mContext) != ConnectivityManager.TYPE_WIFI) {
						halt();
						return;
					}

					final Intent batteryStatusReceiver = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
					if (batteryStatusReceiver == null) {
						halt();
						return;
					}

					final int batteryStatus = batteryStatusReceiver.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
					if (batteryStatus == 0) {
						halt();
						return;
					}

					HttpURLConnection connection;
					try {
						connection = SessionConnection.getSessionConnection(serviceFile.getPlaybackParams());
					} catch (IOException e) {
						mLogger.error("Error getting connection", e);
						return;
					}

					if (connection == null) return;

					try {
						InputStream is;
						try {
							is = connection.getInputStream();
						} catch (IOException ioe) {
							mLogger.error("Error opening data connection", ioe);
							return;
						}

						final java.io.File parent = file.getParentFile();
						if (!parent.exists() && !parent.mkdirs()) return;

						try {
							final FileOutputStream fos = new FileOutputStream(file);
							try {
								IOUtils.copy(is, fos);
								fos.flush();
							} finally {
								fos.close();
							}

							final int storedFileId = storedFile.getId();
							mStoredFileAccess.markStoredFileAsDownloaded(storedFileId);

							if (mOnFileDownloaded != null)
								mOnFileDownloaded.run(storedFile);

							mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
						} catch (IOException ioe) {
							mLogger.error("Error writing file!", ioe);
						} finally {
							if (is != null) {
								try {
									is.close();
								} catch (IOException e) {
									mLogger.error("Error closing input stream", e);
								}
							}
						}
					} finally {
						connection.disconnect();
					}
				} finally {
					// This needs to be tied to the executor runnable in order to maintain
					// a sync between the set and the executor queue
					mQueuedFileKeys.remove(fileKey);

					if (mQueuedFileKeys.size() == 0 && mOnFileQueueEmpty != null)
						mOnFileQueueEmpty.run();
				}
			}
		});
	}

	private void halt() {
		mIsHalted = true;
	}

	public void setOnFileDownloaded(IOneParameterAction<StoredFile> onFileDownloaded) {
		mOnFileDownloaded = onFileDownloaded;
	}

	public void setOnFileQueueEmpty(Runnable onFileQueueEmpty) {
		mOnFileQueueEmpty = onFileQueueEmpty;
	}
}
