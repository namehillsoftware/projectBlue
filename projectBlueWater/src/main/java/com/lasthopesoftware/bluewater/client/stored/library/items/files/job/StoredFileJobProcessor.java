package com.lasthopesoftware.bluewater.client.stored.library.items.files.job;

import android.support.annotation.NonNull;
import com.lasthopesoftware.bluewater.client.library.items.media.files.io.IFileStreamWriter;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileSystemFileProducer;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.download.DownloadStoredFiles;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileJobException;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileWriteException;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.storage.read.permissions.IFileReadPossibleArbitrator;
import com.lasthopesoftware.storage.write.exceptions.StorageCreatePathException;
import com.lasthopesoftware.storage.write.permissions.IFileWritePossibleArbitrator;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy;
import com.namehillsoftware.handoff.promises.response.VoidResponse;
import io.reactivex.Emitter;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class StoredFileJobProcessor implements ProcessStoredFileJobs {

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
	public Observable<StoredFileJobStatus> observeStoredFileDownload(Iterable<StoredFileJob> jobs) {
		final CancellationProxy cancellationProxy = new CancellationProxy();
		final Observable<StoredFileJobStatus> streamedFileDownload = Observable.create(emitter -> {
			final LinkedList<StoredFileJob> jobsQueue = new LinkedList<>();
			final Set<StoredFileJob> queuedJobs = new HashSet<>();
			for (final StoredFileJob job : jobs) {
				if (!queuedJobs.add(job)) continue;

				final StoredFile storedFile = job.getStoredFile();
				final File file = storedFileFileProvider.getFile(storedFile);

				emitter.onNext(new StoredFileJobStatus(file, storedFile, StoredFileJobState.Queued));
				jobsQueue.offer(job);
			}

			final RecursiveQueueProcessor queueProcessor = new RecursiveQueueProcessor(
				jobsQueue,
				emitter,
				cancellationProxy);

			queueProcessor.processQueue().then(
				new VoidResponse<>(v -> emitter.onComplete()),
				new VoidResponse<>(emitter::onError));
		});

		return streamedFileDownload.doOnDispose(cancellationProxy::run);
	}

	private StoredFileJobStatus getCancelledStoredFileJobResult(File file, StoredFile storedFile) {
		return new StoredFileJobStatus(file, storedFile, StoredFileJobState.Cancelled);
	}

	private class RecursiveQueueProcessor {
		private final Queue<StoredFileJob> jobsQueue;
		private final Emitter<StoredFileJobStatus> emitter;
		private final CancellationProxy cancellationProxy;

		RecursiveQueueProcessor(Queue<StoredFileJob> jobsQueue, Emitter<StoredFileJobStatus> jobStatusEmitter, CancellationProxy cancellationProxy) {
			this.jobsQueue = jobsQueue;
			this.emitter = jobStatusEmitter;
			this.cancellationProxy = cancellationProxy;
		}

		Promise<Void> processQueue() {
			final StoredFileJob job = jobsQueue.poll();
			if (job == null) return Promise.empty();

			final StoredFile storedFile = job.getStoredFile();
			final File file = storedFileFileProvider.getFile(storedFile);

			if (file.exists()) {
				if (!fileReadPossibleArbitrator.isFileReadPossible(file)) {
					emitter.onNext(new StoredFileJobStatus(file, storedFile, StoredFileJobState.Unreadable));
					return processQueue();
				}

				if (storedFile.isDownloadComplete()) {
					emitter.onNext(new StoredFileJobStatus(file, storedFile, StoredFileJobState.Downloaded));
					return processQueue();
				}
			}

			if (!fileWritePossibleArbitrator.isFileWritePossible(file)) {
				emitter.onError(new StoredFileWriteException(file, storedFile));
				return processQueue();
			}

			final File parent = file.getParentFile();
			if (parent != null && !parent.exists() && !parent.mkdirs()) {
				emitter.onError(new StorageCreatePathException(parent));
				return processQueue();
			}

			if (cancellationProxy.isCancelled()) {
				emitter.onNext(getCancelledStoredFileJobResult(file, storedFile));
				return Promise.empty();
			}

			emitter.onNext(new StoredFileJobStatus(file, storedFile, StoredFileJobState.Downloading));

			final Promise<InputStream> promisedDownload = storedFiles.promiseDownload(storedFile);
			cancellationProxy.doCancel(promisedDownload);

			return promisedDownload.then(inputStream -> {
				try (final InputStream is = inputStream) {
					if (cancellationProxy.isCancelled()) return getCancelledStoredFileJobResult(file, storedFile);

					fileStreamWriter.writeStreamToFile(is, file);

					storedFileAccess.markStoredFileAsDownloaded(storedFile);

					return new StoredFileJobStatus(file, storedFile, StoredFileJobState.Downloaded);
				} catch (IOException ioe) {
					logger.error("Error writing file!", ioe);
					return new StoredFileJobStatus(file, storedFile, StoredFileJobState.Queued);
				} catch (Throwable t) {
					throw new StoredFileJobException(storedFile, t);
				}
			}, error -> {
				if (error instanceof IOException)
					return new StoredFileJobStatus(file, storedFile, StoredFileJobState.Queued);

				if (error instanceof StoredFileJobException)
					throw error;

				throw new StoredFileJobException(storedFile, error);
			})
			.then(new VoidResponse<>(emitter::onNext))
			.eventually(v -> processQueue());
		}
	}
}
