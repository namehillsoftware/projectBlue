package com.lasthopesoftware.bluewater.client.stored.library.items.files.job;

import androidx.annotation.NonNull;

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
import com.namehillsoftware.handoff.promises.response.PromisedResponse;
import com.namehillsoftware.handoff.promises.response.VoidResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

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
		return new RecursiveQueueProcessor(jobs);
	}

	private static StoredFileJobStatus getCancelledStoredFileJobResult(File file, StoredFile storedFile) {
		return new StoredFileJobStatus(file, storedFile, StoredFileJobState.Cancelled);
	}

	private class RecursiveQueueProcessor extends Observable<StoredFileJobStatus> implements PromisedResponse<StoredFileJobStatus, Void>, Disposable {
		private Iterable<StoredFileJob> jobs;
		private final CancellationProxy cancellationProxy = new CancellationProxy();
		private final LinkedList<StoredFileJob> jobsQueue = new LinkedList<>();
		private Observer<? super StoredFileJobStatus> observer;

		RecursiveQueueProcessor(Iterable<StoredFileJob> jobs) {
			this.jobs = jobs;
		}

		@Override
		protected synchronized void subscribeActual(Observer<? super StoredFileJobStatus> observer) {
			if (jobs == null) {
				observer.onComplete();
				return;
			}

			observer.onSubscribe(this);

			final Set<StoredFileJob> queuedJobs = new HashSet<>();
			for (final StoredFileJob job : jobs) {
				if (!queuedJobs.add(job)) continue;

				final StoredFile storedFile = job.getStoredFile();
				final File file = storedFileFileProvider.getFile(storedFile);

				observer.onNext(new StoredFileJobStatus(file, storedFile, StoredFileJobState.Queued));
				jobsQueue.offer(job);
			}

			jobs = null;

			this.observer = observer;
			processQueue().then(
				new VoidResponse<>(v -> observer.onComplete()),
				new VoidResponse<>(observer::onError));
		}

		private Promise<Void> processQueue() {
			if (cancellationProxy.isCancelled()) return Promise.empty();

			final StoredFileJob job = jobsQueue.poll();
			if (job == null) return Promise.empty();

			final StoredFile storedFile = job.getStoredFile();
			final File file = storedFileFileProvider.getFile(storedFile);

			if (file.exists()) {
				if (!fileReadPossibleArbitrator.isFileReadPossible(file)) {
					observer.onNext(new StoredFileJobStatus(file, storedFile, StoredFileJobState.Unreadable));
					return processQueue();
				}

				if (storedFile.isDownloadComplete()) {
					observer.onNext(new StoredFileJobStatus(file, storedFile, StoredFileJobState.Downloaded));
					return processQueue();
				}
			}

			if (!fileWritePossibleArbitrator.isFileWritePossible(file)) {
				observer.onError(new StoredFileWriteException(file, storedFile));
				return Promise.empty();
			}

			final File parent = file.getParentFile();
			if (parent != null && !parent.exists() && !parent.mkdirs()) {
				observer.onError(new StorageCreatePathException(parent));
				return Promise.empty();
			}

			if (cancellationProxy.isCancelled()) {
				observer.onNext(getCancelledStoredFileJobResult(file, storedFile));
				return Promise.empty();
			}

			observer.onNext(new StoredFileJobStatus(file, storedFile, StoredFileJobState.Downloading));

			final Promise<InputStream> promisedDownload = storedFiles.promiseDownload(job.getLibraryId(), storedFile);
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
			.eventually(this);
		}

		@Override
		public Promise<Void> promiseResponse(StoredFileJobStatus status) {
			observer.onNext(status);
			return processQueue();
		}

		@Override
		public void dispose() {
			cancellationProxy.run();
		}

		@Override
		public boolean isDisposed() {
			return cancellationProxy.isCancelled();
		}
	}
}
