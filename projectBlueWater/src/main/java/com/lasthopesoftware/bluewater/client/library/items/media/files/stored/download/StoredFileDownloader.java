package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFileUriQueryParamsProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.StoredFileAccess;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.exceptions.StoredFileJobException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.exceptions.StoredFileReadException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.exceptions.StoredFileWriteException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.storage.read.permissions.FileReadPossibleArbitrator;
import com.lasthopesoftware.storage.read.permissions.IFileReadPossibleArbitrator;
import com.lasthopesoftware.storage.write.exceptions.StorageCreatePathException;
import com.lasthopesoftware.storage.write.permissions.FileWritePossibleArbitrator;
import com.lasthopesoftware.storage.write.permissions.IFileWritePossibleArbitrator;
import com.vedsoft.futures.runnables.OneParameterAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class StoredFileDownloader {

	private static final Logger logger = LoggerFactory.getLogger(StoredFileDownloader.class);

	private boolean isProcessing;

	private final StoredFileAccess storedFileAccess;
	private final IFileReadPossibleArbitrator fileReadPossibleArbitrator;
	private final IFileWritePossibleArbitrator fileWritePossibleArbitrator;
	private final IConnectionProvider connectionProvider;
	private final Set<Integer> queuedFileKeys = new HashSet<>();
	private final Queue<StoredFileJob> storedFileJobQueue = new LinkedList<>();

	private OneParameterAction<StoredFile> onFileDownloading;
	private OneParameterAction<StoredFileJobResult> onFileDownloaded;
	private OneParameterAction<StoredFile> onFileQueued;
	private OneParameterAction<StoredFile> onFileReadError;
	private OneParameterAction<StoredFile> onFileWriteError;
	private Runnable onQueueProcessingCompleted;

	private volatile boolean isCancelled;

	public StoredFileDownloader(@NonNull Context context, @NonNull IConnectionProvider connectionProvider, @NonNull Library library) {
		this(
				connectionProvider,
				new StoredFileAccess(context, library),
				new FileReadPossibleArbitrator(),
				new FileWritePossibleArbitrator());
	}

	public StoredFileDownloader(@NonNull IConnectionProvider connectionProvider, @NonNull StoredFileAccess storedFileAccess, @NonNull IFileReadPossibleArbitrator fileReadPossibleArbitrator, @NonNull IFileWritePossibleArbitrator fileWritePossibleArbitrator) {
		this.connectionProvider = connectionProvider;
		this.storedFileAccess = storedFileAccess;
		this.fileReadPossibleArbitrator = fileReadPossibleArbitrator;
		this.fileWritePossibleArbitrator = fileWritePossibleArbitrator;
	}

	public void queueFileForDownload(@NonNull final ServiceFile serviceFile, @NonNull final StoredFile storedFile) {
		if (isProcessing || isCancelled)
			throw new IllegalStateException("New files cannot be added to the queue after processing has began.");

		final int fileKey = serviceFile.getKey();
		if (!queuedFileKeys.add(fileKey)) return;

		storedFileJobQueue.add(new StoredFileJob(connectionProvider, storedFileAccess, ServiceFileUriQueryParamsProvider.getInstance(), fileReadPossibleArbitrator, fileWritePossibleArbitrator, serviceFile, storedFile));
		if (onFileQueued != null)
			onFileQueued.runWith(storedFile);
	}

	public void cancel() {
		isCancelled = true;

		Stream.of(storedFileJobQueue).forEach(StoredFileJob::cancel);

		if (isProcessing || onQueueProcessingCompleted == null) return;

		onQueueProcessingCompleted.run();
	}

	public void process() {
		if (isCancelled)
			throw new IllegalStateException("Processing cannot be started once the stored serviceFile downloader has been cancelled.");

		if (isProcessing)
			throw new IllegalStateException("Processing has already begun");

		isProcessing = true;

		AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
			try {
				StoredFileJob storedFileJob;
				while ((storedFileJob = storedFileJobQueue.poll()) != null) {
					if (isCancelled) return;

					final StoredFile storedFile = storedFileJob.getStoredFile();

					if (onFileDownloading != null)
						onFileDownloading.runWith(storedFile);

					try {
						final StoredFileJobResult storedFileJobResult = storedFileJob.processJob();

						if (onFileDownloaded != null)
							onFileDownloaded.runWith(storedFileJobResult);
					} catch (StoredFileWriteException se) {
						if (onFileWriteError != null)
							onFileWriteError.runWith(se.getStoredFile());
					} catch (StoredFileReadException se) {
						if (onFileReadError != null)
							onFileReadError.runWith(se.getStoredFile());
					} catch (StoredFileJobException e) {
						logger.error("There was an error downloading the stored file " + e.getStoredFile(), e);
					} catch (StorageCreatePathException e) {
						logger.error("There was an error creating the path for a file", e);
					}
				}
			} finally {
				if (onQueueProcessingCompleted != null) onQueueProcessingCompleted.run();
			}
		});
	}

	public void setOnFileQueued(@Nullable OneParameterAction<StoredFile> onFileQueued) {
		this.onFileQueued = onFileQueued;
	}

	public void setOnFileDownloading(@Nullable OneParameterAction<StoredFile> onFileDownloading) {
		this.onFileDownloading = onFileDownloading;
	}

	public void setOnFileDownloaded(@Nullable OneParameterAction<StoredFileJobResult> onFileDownloaded) {
		this.onFileDownloaded = onFileDownloaded;
	}

	public void setOnQueueProcessingCompleted(Runnable onQueueProcessingCompleted) {
		this.onQueueProcessingCompleted = onQueueProcessingCompleted;
	}

	public void setOnFileReadError(@Nullable OneParameterAction<StoredFile> onFileReadError) {
		this.onFileReadError = onFileReadError;
	}

	public void setOnFileWriteError(@Nullable OneParameterAction<StoredFile> onFileWriteError) {
		this.onFileWriteError = onFileWriteError;
	}
}
