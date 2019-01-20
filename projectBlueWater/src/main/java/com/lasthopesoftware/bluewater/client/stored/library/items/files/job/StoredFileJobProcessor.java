package com.lasthopesoftware.bluewater.client.stored.library.items.files.job;

import android.support.annotation.NonNull;
import com.lasthopesoftware.bluewater.client.library.items.media.files.io.IFileStreamWriter;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileSystemFileProducer;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.download.DownloadStoredFiles;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileJobException;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileReadException;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileWriteException;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.storage.read.permissions.IFileReadPossibleArbitrator;
import com.lasthopesoftware.storage.write.exceptions.StorageCreatePathException;
import com.lasthopesoftware.storage.write.permissions.IFileWritePossibleArbitrator;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;
import com.namehillsoftware.handoff.promises.response.VoidResponse;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class StoredFileJobProcessor implements ProcessStoredFileJobs {
	private static final Executor downloadExecutor = Executors.newSingleThreadExecutor();

	private static final Logger logger = LoggerFactory.getLogger(StoredFileJobProcessor.class);

	@NonNull
	private final DownloadStoredFiles storedFiles;
	@NonNull private final IFileWritePossibleArbitrator fileWritePossibleArbitrator;
	@NonNull private final IFileReadPossibleArbitrator fileReadPossibleArbitrator;
	@NonNull private final IStoredFileSystemFileProducer storedFileFileProvider;
	@NonNull private final IFileStreamWriter fileStreamWriter;
	@NonNull private final IStoredFileAccess storedFileAccess;

	public StoredFileJobProcessor(@NonNull IStoredFileSystemFileProducer storedFileFileProvider, @NonNull IStoredFileAccess storedFileAccess, @NonNull DownloadStoredFiles storedFiles, @NonNull IFileReadPossibleArbitrator fileReadPossibleArbitrator, @NonNull IFileWritePossibleArbitrator fileWritePossibleArbitrator, @NonNull IFileStreamWriter fileStreamWriter) {
		this.storedFiles = storedFiles;
		this.fileWritePossibleArbitrator = fileWritePossibleArbitrator;
		this.fileReadPossibleArbitrator = fileReadPossibleArbitrator;
		this.storedFileFileProvider = storedFileFileProvider;
		this.fileStreamWriter = fileStreamWriter;
		this.storedFileAccess = storedFileAccess;
	}

	@Override
	public Observable<StoredFileJobStatus> observeStoredFileDownload(Set<StoredFileJob> jobs) {
		final CancellationProxy cancellationProxy = new CancellationProxy();
		final StoredFileJob job = jobs.iterator().next();
		final Observable<StoredFileJobStatus> streamedFileDownload = Observable.create(emitter -> {
			final StoredFile storedFile = job.getStoredFile();
			final File file = storedFileFileProvider.getFile(storedFile);

			emitter.onNext(new StoredFileJobStatus(file, storedFile, StoredFileJobState.Queued));

			if (file.exists()) {
				if (!fileReadPossibleArbitrator.isFileReadPossible(file)) {
					emitter.onError(new StoredFileReadException(file, storedFile));
					return;
				}

				if (storedFile.isDownloadComplete()) {
					emitter.onNext(new StoredFileJobStatus(file, storedFile, StoredFileJobState.AlreadyExists));
					emitter.onComplete();
					return;
				}
			}

			if (!fileWritePossibleArbitrator.isFileWritePossible(file)) {
				emitter.onError(new StoredFileWriteException(file, storedFile));
				return;
			}

			final File parent = file.getParentFile();
			if (parent != null && !parent.exists() && !parent.mkdirs()) {
				emitter.onError(new StorageCreatePathException(parent));
				return;
			}

			if (cancellationProxy.isCancelled()) {
				emitter.onNext(getCancelledStoredFileJobResult(file, storedFile));
				return;
			}

			emitter.onNext(new StoredFileJobStatus(file, storedFile, StoredFileJobState.Downloading));

			final Promise<InputStream> promisedResponse = storedFiles.promiseDownload(storedFile);
			cancellationProxy.doCancel(promisedResponse);
			promisedResponse
				.eventually(inputStream -> new QueuedPromise<>(() -> {
					if (cancellationProxy.isCancelled()) return getCancelledStoredFileJobResult(file, storedFile);

					try (final InputStream is = inputStream) {

						this.fileStreamWriter.writeStreamToFile(is, file);

						storedFileAccess.markStoredFileAsDownloaded(storedFile);

						return new StoredFileJobStatus(file, storedFile, StoredFileJobState.Downloaded);
					} catch (IOException ioe) {
						logger.error("Error writing file!", ioe);
						throw new StoredFileWriteException(file, storedFile, ioe);
					} catch (Throwable t) {
						throw new StoredFileJobException(storedFile, t);
					}
				}, downloadExecutor), error -> {
					logger.error("Error getting connection", error);
					return new Promise<>(new StoredFileJobException(storedFile, error));
				})
				.then(
					new VoidResponse<>(emitter::onNext),
					new VoidResponse<>(emitter::onError))
				.then(new VoidResponse<>(v -> emitter.onComplete()));
		});

		return streamedFileDownload.doOnDispose(cancellationProxy::run);
	}

	private StoredFileJobStatus getCancelledStoredFileJobResult(File file, StoredFile storedFile) {
		return new StoredFileJobStatus(file, storedFile, StoredFileJobState.Cancelled);
	}
}
