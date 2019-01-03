package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IServiceFileUriQueryParamsProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.io.IFileStreamWriter;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.IStoredFileSystemFileProducer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.storage.read.permissions.IFileReadPossibleArbitrator;
import com.lasthopesoftware.storage.write.permissions.IFileWritePossibleArbitrator;
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellationToken;
import com.vedsoft.futures.runnables.OneParameterAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public final class StoredFileDownloader implements IStoredFileDownloader {

	private static final Logger logger = LoggerFactory.getLogger(StoredFileDownloader.class);

	private boolean isProcessing;

	@NonNull private final IStoredFileAccess storedFileAccess;
	@NonNull private final IFileReadPossibleArbitrator fileReadPossibleArbitrator;
	@NonNull private final IFileWritePossibleArbitrator fileWritePossibleArbitrator;
	@NonNull private final IFileStreamWriter fileStreamWriter;
	@NonNull private final IConnectionProvider connectionProvider;
	@NonNull private final Set<Integer> queuedFileKeys = new HashSet<>();
	@NonNull private final Queue<StoredFileJob> storedFileJobQueue = new LinkedList<>();
	@NonNull private final IServiceFileUriQueryParamsProvider serviceFileQueryUriParamsProvider;
	@NonNull private final IStoredFileSystemFileProducer storedFileSystemFileProducer;

	private OneParameterAction<StoredFile> onFileDownloading;
	private OneParameterAction<StoredFileJobResult> onFileDownloaded;
	private OneParameterAction<StoredFile> onFileQueued;
	private OneParameterAction<StoredFile> onFileReadError;
	private OneParameterAction<StoredFile> onFileWriteError;
	private Runnable onQueueProcessingCompleted;

	private final CancellationToken cancellationToken = new CancellationToken();

	public StoredFileDownloader(@NonNull IStoredFileSystemFileProducer storedFileSystemFileProducer, @NonNull IConnectionProvider connectionProvider, @NonNull IStoredFileAccess storedFileAccess, @NonNull IServiceFileUriQueryParamsProvider serviceFileQueryUriParamsProvider, @NonNull IFileReadPossibleArbitrator fileReadPossibleArbitrator, @NonNull IFileWritePossibleArbitrator fileWritePossibleArbitrator, @NonNull IFileStreamWriter fileStreamWriter) {
		this.storedFileSystemFileProducer = storedFileSystemFileProducer;
		this.connectionProvider = connectionProvider;
		this.storedFileAccess = storedFileAccess;
		this.serviceFileQueryUriParamsProvider = serviceFileQueryUriParamsProvider;
		this.fileReadPossibleArbitrator = fileReadPossibleArbitrator;
		this.fileWritePossibleArbitrator = fileWritePossibleArbitrator;
		this.fileStreamWriter = fileStreamWriter;
	}

	@Override
	public void queueFileForDownload(@NonNull final ServiceFile serviceFile, @NonNull final StoredFile storedFile) {
		if (isProcessing || cancellationToken.isCancelled())
			throw new IllegalStateException("New files cannot be added to the queue after processing has began.");

		final int fileKey = serviceFile.getKey();
		if (!queuedFileKeys.add(fileKey)) return;

		storedFileJobQueue.add(
			new StoredFileJob(
				storedFileSystemFileProducer,
				connectionProvider,
				storedFileAccess,
				serviceFileQueryUriParamsProvider,
				fileReadPossibleArbitrator,
				fileWritePossibleArbitrator,
				fileStreamWriter,
				serviceFile,
				storedFile));

		if (onFileQueued != null)
			onFileQueued.runWith(storedFile);
	}

	@Override
	public void cancel() {
		cancellationToken.run();

		Stream.of(storedFileJobQueue).forEach(StoredFileJob::cancel);

		if (isProcessing || onQueueProcessingCompleted == null) return;

		onQueueProcessingCompleted.run();
	}

	@Override
	public void process() {
		if (cancellationToken.isCancelled())
			throw new IllegalStateException("Processing cannot be started once the stored serviceFile downloader has been cancelled.");

		if (isProcessing)
			throw new IllegalStateException("Processing has already begun");

		isProcessing = true;

		new Thread(() -> {
			try {
				StoredFileJob storedFileJob;
				while ((storedFileJob = storedFileJobQueue.poll()) != null) {
					if (cancellationToken.isCancelled()) return;

					final StoredFile storedFile = storedFileJob.getStoredFile();

					if (onFileDownloading != null)
						onFileDownloading.runWith(storedFile);

//					try {
//						final StoredFileJobResult storedFileJobResult = storedFileJob.processJob();
//
//						if (onFileDownloaded != null)
//							onFileDownloaded.runWith(storedFileJobResult);
//					} catch (StoredFileWriteException se) {
//						if (onFileWriteError != null)
//							onFileWriteError.runWith(se.getStoredFile());
//					} catch (StoredFileReadException se) {
//						if (onFileReadError != null)
//							onFileReadError.runWith(se.getStoredFile());
//					} catch (StoredFileJobException e) {
//						logger.error("There was an error downloading the stored file " + e.getStoredFile(), e);
//					} catch (StorageCreatePathException e) {
//						logger.error("There was an error creating the path for a file", e);
//					}
				}
			} finally {
				if (onQueueProcessingCompleted != null) onQueueProcessingCompleted.run();
			}
		}).start();
	}

	@Override
	public void setOnFileQueued(@Nullable OneParameterAction<StoredFile> onFileQueued) {
		this.onFileQueued = onFileQueued;
	}

	@Override
	public void setOnFileDownloading(@Nullable OneParameterAction<StoredFile> onFileDownloading) {
		this.onFileDownloading = onFileDownloading;
	}

	@Override
	public void setOnFileDownloaded(@Nullable OneParameterAction<StoredFileJobResult> onFileDownloaded) {
		this.onFileDownloaded = onFileDownloaded;
	}

	@Override
	public void setOnQueueProcessingCompleted(Runnable onQueueProcessingCompleted) {
		this.onQueueProcessingCompleted = onQueueProcessingCompleted;
	}

	@Override
	public void setOnFileReadError(@Nullable OneParameterAction<StoredFile> onFileReadError) {
		this.onFileReadError = onFileReadError;
	}

	@Override
	public void setOnFileWriteError(@Nullable OneParameterAction<StoredFile> onFileWriteError) {
		this.onFileWriteError = onFileWriteError;
	}
}
