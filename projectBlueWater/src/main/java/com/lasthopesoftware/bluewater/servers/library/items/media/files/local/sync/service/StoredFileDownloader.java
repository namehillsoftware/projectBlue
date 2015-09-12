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
import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.StoredFileAccess;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.repository.StoredFile;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
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

	private static final ExecutorService storeFilesExecutor = Executors.newSingleThreadExecutor();
	private static final Logger logger = LoggerFactory.getLogger(StoredFileDownloader.class);

	private boolean isHalted = false;

	private final StoredFileAccess storedFileAccess;
	private final Context context;
	private final ConnectionProvider connectionProvider;
	private final Set<Integer> queuedFileKeys = new HashSet<>();

	private IOneParameterAction<StoredFile> onFileDownloaded;
	private Runnable onFileQueueEmpty;

	public StoredFileDownloader(Context context, ConnectionProvider connectionProvider, Library library) {
		this.context = context;
		this.connectionProvider = connectionProvider;
		storedFileAccess = new StoredFileAccess(context, library);
	}

	public void queueFileForDownload(final IFile serviceFile, final StoredFile storedFile) {
		final int fileKey = serviceFile.getKey();
		if (!queuedFileKeys.add(fileKey)) return;

		storeFilesExecutor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					final java.io.File file = new java.io.File(storedFile.getPath());
					if (isHalted || (storedFile.isDownloadComplete() && file.exists()))
						return;
					
					if (ConnectionInfo.getConnectionType(context) != ConnectivityManager.TYPE_WIFI) {
						halt();
						return;
					}
					
					final Intent batteryStatusReceiver = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
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
						connection = connectionProvider.getConnection(serviceFile.getPlaybackParams());
					} catch (IOException e) {
						logger.error("Error getting connection", e);
						return;
					}
					
					if (connection == null) return;
					
					try {
						InputStream is;
						try {
							is = connection.getInputStream();
						} catch (IOException ioe) {
							logger.error("Error opening data connection", ioe);
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
							storedFileAccess.markStoredFileAsDownloaded(storedFileId);
							
							if (onFileDownloaded != null)
								onFileDownloaded.run(storedFile);
							
							context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
						} catch (IOException ioe) {
							logger.error("Error writing file!", ioe);
						} finally {
							if (is != null) {
								try {
									is.close();
								} catch (IOException e) {
									logger.error("Error closing input stream", e);
								}
							}
						}
					} finally {
						connection.disconnect();
					}
				} finally {
					// This needs to be tied to the executor runnable in order to maintain
					// a sync between the set and the executor queue
					queuedFileKeys.remove(fileKey);
					
					if (queuedFileKeys.size() == 0 && onFileQueueEmpty != null)
						onFileQueueEmpty.run();
				}
			}
		});
	}

	private void halt() {
		isHalted = true;
	}

	public void setOnFileDownloaded(IOneParameterAction<StoredFile> onFileDownloaded) {
		this.onFileDownloaded = onFileDownloaded;
	}

	public void setOnFileQueueEmpty(Runnable onFileQueueEmpty) {
		this.onFileQueueEmpty = onFileQueueEmpty;
	}
}
