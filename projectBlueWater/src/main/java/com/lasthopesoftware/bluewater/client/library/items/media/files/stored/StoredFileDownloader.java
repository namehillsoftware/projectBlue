package com.lasthopesoftware.bluewater.client.library.items.media.files.stored;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.broadcasts.IScanMediaFileBroadcaster;
import com.lasthopesoftware.bluewater.client.library.items.media.files.broadcasts.ScanMediaFileBroadcaster;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.library.permissions.storage.read.request.IStorageReadPermissionsRequestedBroadcast;
import com.lasthopesoftware.bluewater.client.library.permissions.storage.read.request.StorageReadPermissionsRequestedBroadcaster;
import com.lasthopesoftware.bluewater.client.library.permissions.storage.write.request.IStorageWritePermissionsRequestedBroadcaster;
import com.lasthopesoftware.bluewater.client.library.permissions.storage.write.request.StorageWritePermissionsRequestedBroadcaster;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.permissions.read.ApplicationReadPermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.permissions.read.IApplicationReadPermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.permissions.write.ApplicationWritePermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.permissions.write.IApplicationWritePermissionsRequirementsProvider;
import com.vedsoft.futures.runnables.OneParameterRunnable;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class StoredFileDownloader {

	private static final Logger logger = LoggerFactory.getLogger(StoredFileDownloader.class);

	private static class QueuedFileHolder {
		public final IFile file;
		public final StoredFile storedFile;

		private QueuedFileHolder(IFile file, StoredFile storedFile) {
			this.file = file;
			this.storedFile = storedFile;
		}
	}

	private boolean isProcessing;

	private final StoredFileAccess storedFileAccess;
	private final IStorageWritePermissionsRequestedBroadcaster storageWritePermissionsNeededBroadcast;
	private final ConnectionProvider connectionProvider;
	private final IScanMediaFileBroadcaster scanMediaFileBroadcaster;
	private final IApplicationReadPermissionsRequirementsProvider applicationReadPermissionsRequirementsProvider;
	private final IStorageReadPermissionsRequestedBroadcast storageReadPermissionsRequestedBroadcast;
	private final IApplicationWritePermissionsRequirementsProvider applicationWritePermissionsRequirementsProvider;
	private final Set<Integer> queuedFileKeys = new HashSet<>();
	private final Queue<QueuedFileHolder> queuedFiles = new LinkedList<>();

	private OneParameterRunnable<StoredFile> onFileDownloading;
	private OneParameterRunnable<StoredFile> onFileDownloaded;
	private OneParameterRunnable<StoredFile> onFileQueued;
	private OneParameterRunnable<StoredFile> onFileReadError;
	private OneParameterRunnable<StoredFile> onFileWriteError;
	private Runnable onQueueProcessingCompleted;

	private volatile boolean isCancelled;

	public StoredFileDownloader(Context context, ConnectionProvider connectionProvider, Library library) {
		this(
				connectionProvider,
				new StoredFileAccess(context, library),
				new ScanMediaFileBroadcaster(context),
				new ApplicationReadPermissionsRequirementsProvider(context, library),
				new StorageReadPermissionsRequestedBroadcaster(LocalBroadcastManager.getInstance(context)),
				new ApplicationWritePermissionsRequirementsProvider(context, library),
				new StorageWritePermissionsRequestedBroadcaster(LocalBroadcastManager.getInstance(context)));
	}

	public StoredFileDownloader(ConnectionProvider connectionProvider, StoredFileAccess storedFileAccess, IScanMediaFileBroadcaster scanMediaFileBroadcaster, IApplicationReadPermissionsRequirementsProvider applicationReadPermissionsRequirementsProvider, IStorageReadPermissionsRequestedBroadcast storageReadPermissionsRequestedBroadcast, IApplicationWritePermissionsRequirementsProvider applicationWritePermissionsRequirementsProvider, IStorageWritePermissionsRequestedBroadcaster storageWritePermissionsNeededBroadcast) {
		this.connectionProvider = connectionProvider;
		this.scanMediaFileBroadcaster = scanMediaFileBroadcaster;
		this.applicationReadPermissionsRequirementsProvider = applicationReadPermissionsRequirementsProvider;
		this.storageReadPermissionsRequestedBroadcast = storageReadPermissionsRequestedBroadcast;
		this.applicationWritePermissionsRequirementsProvider = applicationWritePermissionsRequirementsProvider;
		this.storedFileAccess = storedFileAccess;
		this.storageWritePermissionsNeededBroadcast = storageWritePermissionsNeededBroadcast;
	}

	public void queueFileForDownload(final IFile serviceFile, final StoredFile storedFile) {
		if (isProcessing || isCancelled)
			throw new IllegalStateException("New files cannot be added to the queue after processing has began.");

		final int fileKey = serviceFile.getKey();
		if (!queuedFileKeys.add(fileKey)) return;

		queuedFiles.add(new QueuedFileHolder(serviceFile, storedFile));
		if (onFileQueued != null)
			onFileQueued.run(storedFile);
	}

	public void cancel() {
		isCancelled = true;

		if (isProcessing || onQueueProcessingCompleted == null) return;

		onQueueProcessingCompleted.run();
	}

	public void process() {
		if (isCancelled)
			throw new IllegalStateException("Processing cannot be started once the stored file downloader has been cancelled.");

		if (isProcessing)
			throw new IllegalStateException("Processing has already begun");

		isProcessing = true;

		AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
			try {
				QueuedFileHolder queuedFileHolder;
				while ((queuedFileHolder = queuedFiles.poll()) != null) {
					if (isCancelled) return;

					final StoredFile storedFile = queuedFileHolder.storedFile;
					final IFile serviceFile = queuedFileHolder.file;

					final java.io.File file = new java.io.File(storedFile.getPath());
					if (!file.canRead() && applicationReadPermissionsRequirementsProvider.isReadPermissionsRequired()) {
						if (onFileReadError != null)
							onFileReadError.run(storedFile);

						continue;
					}

					if (storedFile.isDownloadComplete() && file.exists()) continue;

					if (!file.canWrite() && applicationWritePermissionsRequirementsProvider.isWritePermissionsRequired()) {
						if (onFileWriteError != null)
							onFileWriteError.run(storedFile);

						continue;
					}

					if (onFileDownloading != null)
						onFileDownloading.run(storedFile);

					HttpURLConnection connection;
					try {
						connection = connectionProvider.getConnection(serviceFile.getPlaybackParams());
					} catch (IOException e) {
						logger.error("Error getting connection", e);
						return;
					}

					if (isCancelled || connection == null) return;

					try {
						InputStream is;
						try {
							is = connection.getInputStream();
						} catch (IOException ioe) {
							logger.error("Error opening data connection", ioe);
							return;
						}

						if (isCancelled) return;

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

							storedFileAccess.markStoredFileAsDownloaded(storedFile);

							scanMediaFileBroadcaster.sendScanMediaFileBroadcastForFile(file);

							if (onFileDownloaded != null)
								onFileDownloaded.run(storedFile);
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
				}
			} finally {
				if (onQueueProcessingCompleted != null) onQueueProcessingCompleted.run();
			}
		});
	}

	public void setOnFileQueued(OneParameterRunnable<StoredFile> onFileQueued) {
		this.onFileQueued = onFileQueued;
	}

	public void setOnFileDownloading(OneParameterRunnable<StoredFile> onFileDownloading) {
		this.onFileDownloading = onFileDownloading;
	}

	public void setOnFileDownloaded(OneParameterRunnable<StoredFile> onFileDownloaded) {
		this.onFileDownloaded = onFileDownloaded;
	}

	public void setOnQueueProcessingCompleted(Runnable onQueueProcessingCompleted) {
		this.onQueueProcessingCompleted = onQueueProcessingCompleted;
	}

	public void setOnFileReadError(OneParameterRunnable<StoredFile> onFileReadError) {
		this.onFileReadError = onFileReadError;
	}

	public void setOnFileWriteError(OneParameterRunnable<StoredFile> onFileWriteError) {
		this.onFileWriteError = onFileWriteError;
	}
}
