package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IServiceFileUriQueryParamsProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.io.IFileStreamWriter;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.IStoredFileSystemFileProducer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.exceptions.StoredFileJobException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.exceptions.StoredFileReadException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.exceptions.StoredFileWriteException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.job.ProcessStoredFileJobs;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.storage.read.permissions.IFileReadPossibleArbitrator;
import com.lasthopesoftware.storage.write.permissions.IFileWritePossibleArbitrator;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellationToken;
import com.namehillsoftware.handoff.promises.response.VoidResponse;
import com.vedsoft.futures.runnables.OneParameterAction;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;

public final class StoredFileDownloader implements IStoredFileDownloader {

	private static final Logger logger = LoggerFactory.getLogger(StoredFileDownloader.class);
	@NonNull
	private final ProcessStoredFileJobs storedFileJobs;

	private boolean isProcessing;

	private OneParameterAction<StoredFile> onFileDownloading;
	private OneParameterAction<StoredFileJobResult> onFileDownloaded;
	private OneParameterAction<StoredFile> onFileQueued;
	private OneParameterAction<StoredFile> onFileReadError;
	private OneParameterAction<StoredFile> onFileWriteError;
	private Runnable onQueueProcessingCompleted;

	private final CancellationToken cancellationToken = new CancellationToken();

	public StoredFileDownloader(@NonNull ProcessStoredFileJobs storedFileJobs) {
		this.storedFileJobs = storedFileJobs;
	}

	public StoredFileDownloader(@NonNull IStoredFileSystemFileProducer storedFileSystemFileProducer, @NonNull IConnectionProvider connectionProvider, @NonNull IStoredFileAccess storedFileAccess, @NonNull IServiceFileUriQueryParamsProvider serviceFileQueryUriParamsProvider, @NonNull IFileReadPossibleArbitrator fileReadPossibleArbitrator, @NonNull IFileWritePossibleArbitrator fileWritePossibleArbitrator, @NonNull IFileStreamWriter fileStreamWriter) {
		storedFileJobs = null;
	}

	@Override
	public Observable<StoredFileJobResult> process(Queue<StoredFileJob> jobsQueue) {
		if (cancellationToken.isCancelled())
			throw new IllegalStateException("Processing cannot be started once the stored serviceFile downloader has been cancelled.");

		if (isProcessing)
			throw new IllegalStateException("Processing has already begun");

		isProcessing = true;

		return Observable.create(emitter -> {
			Promise.whenAll(Stream.of(jobsQueue).map(storedFileJob -> processStoredFileJob(storedFileJob, emitter)).toList())
				.then(
					new VoidResponse<>(v -> emitter.onComplete()),
					new VoidResponse<>(emitter::onError));
		});

//		new Thread(() -> {
//			try {
//				StoredFileJob storedFileJob;
//				while ((storedFileJob = storedFileJobQueue.poll()) != null) {
//					if (cancellationToken.isCancelled()) return;
//
//					final StoredFile storedFile = storedFileJob.getStoredFile();
//
//					if (onFileDownloading != null)
//						onFileDownloading.runWith(storedFile);
//
//					try {
//						final StoredFileJobResult storedFileJobResult = storedFileJob.processJob();
//
//						if (onFileDownloaded != null)
//							onFileDownloaded.runWith(storedFileJobResult);
//					} catch (StoredFileJobException e) {
//						logger.error("There was an error downloading the stored file " + e.getStoredFile(), e);
//					} catch (StorageCreatePathException e) {
//						logger.error("There was an error creating the path for a file", e);
//					}
//				}
//			} finally {
//				if (onQueueProcessingCompleted != null) onQueueProcessingCompleted.run();
//			}
//		}).start();
	}

	private Promise<Void> processStoredFileJob(StoredFileJob storedFileJob, ObservableEmitter<StoredFileJobResult> emitter) {
		if (onFileDownloading != null)
			onFileDownloading.runWith(storedFileJob.getStoredFile());

		return storedFileJobs
			.promiseDownloadedStoredFile(storedFileJob)
			.then(
				new VoidResponse<>(emitter::onNext),
				new VoidResponse<>(e -> {
					if (e instanceof StoredFileWriteException) {
						onFileWriteError.runWith(((StoredFileWriteException)e).getStoredFile());
						return;
					}

					if (e instanceof StoredFileReadException) {
						onFileReadError.runWith(((StoredFileReadException)e).getStoredFile());
						return;
					}

					if (e instanceof StoredFileJobException) {
						logger.error("There was an error downloading the stored file " + ((StoredFileJobException)e).getStoredFile(), e);
						return;
					}

					emitter.onError(e);
				}));
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
