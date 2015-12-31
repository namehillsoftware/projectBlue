package com.lasthopesoftware.bluewater.servers.library.items.media.files.stored;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
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
	private final Context context;
	private final ConnectionProvider connectionProvider;
	private final Set<Integer> queuedFileKeys = new HashSet<>();
	private final Queue<QueuedFileHolder> queuedFiles = new LinkedList<>();

	private OneParameterRunnable<StoredFile> onFileDownloading;
	private OneParameterRunnable<StoredFile> onFileDownloaded;
	private OneParameterRunnable<StoredFile> onFileQueued;
	private Runnable onQueueProcessingCompleted;

	private volatile boolean isCancelled;

	public StoredFileDownloader(Context context, ConnectionProvider connectionProvider, Library library) {
		this.context = context;
		this.connectionProvider = connectionProvider;
		storedFileAccess = new StoredFileAccess(context, library);
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

		AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
			@Override
			public void run() {
				try {
					QueuedFileHolder queuedFileHolder;
					while ((queuedFileHolder = queuedFiles.poll()) != null) {
						if (isCancelled) return;

						final StoredFile storedFile = queuedFileHolder.storedFile;
						final IFile serviceFile = queuedFileHolder.file;

						final java.io.File file = new java.io.File(storedFile.getPath());
						if (storedFile.isDownloadComplete() && file.exists()) continue;

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

								context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));

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
}
