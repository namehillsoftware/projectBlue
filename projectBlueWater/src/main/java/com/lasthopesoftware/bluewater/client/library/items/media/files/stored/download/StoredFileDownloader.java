package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.exceptions.StoredFileJobException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.exceptions.StoredFileReadException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.exceptions.StoredFileWriteException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.job.ProcessStoredFileJobs;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.job.StoredFileJobState;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.job.StoredFileJobStatus;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.storage.write.exceptions.StorageCreatePathException;
import com.vedsoft.futures.runnables.OneParameterAction;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;

public final class StoredFileDownloader implements IStoredFileDownloader {

	private static final Logger logger = LoggerFactory.getLogger(StoredFileDownloader.class);
	@NonNull
	private final ProcessStoredFileJobs storedFileJobs;

	private OneParameterAction<StoredFile> onFileDownloading;
	private OneParameterAction<StoredFile> onFileReadError;
	private OneParameterAction<StoredFile> onFileWriteError;

	public StoredFileDownloader(@NonNull ProcessStoredFileJobs storedFileJobs) {
		this.storedFileJobs = storedFileJobs;
	}

	@Override
	public Observable<StoredFileJobStatus> process(Queue<StoredFileJob> jobsQueue) {
		return Observable.fromIterable(jobsQueue).flatMap(this::processStoredFileJob)
			.filter(storedFileJobStatus -> storedFileJobStatus.storedFileJobState != StoredFileJobState.None);
	}

	private Observable<StoredFileJobStatus> processStoredFileJob(StoredFileJob storedFileJob) {
		return storedFileJobs
			.observeStoredFileDownload(storedFileJob)
			.doOnNext(j -> {
				if (onFileDownloading != null && j.storedFileJobState == StoredFileJobState.Downloading)
					onFileDownloading.runWith(j.storedFile);
			})
			.filter(j -> j.storedFileJobState != StoredFileJobState.Downloading)
			.onErrorReturn(e -> {
				if (e instanceof StoredFileWriteException) {
					onFileWriteError.runWith(((StoredFileWriteException) e).getStoredFile());
					return StoredFileJobStatus.empty();
				}

				if (e instanceof StoredFileReadException) {
					onFileReadError.runWith(((StoredFileReadException) e).getStoredFile());
					return StoredFileJobStatus.empty();
				}

				if (e instanceof StoredFileJobException) {
					logger.error("There was an error downloading the stored file " + ((StoredFileJobException) e).getStoredFile(), e);
					return StoredFileJobStatus.empty();
				}

				if (e instanceof StorageCreatePathException) {
					logger.error("There was an error creating the path", e);
					return StoredFileJobStatus.empty();
				}

				if (e instanceof Exception) throw (Exception) e;

				throw new RuntimeException(e);
			});
	}

	@Override
	public void setOnFileDownloading(@Nullable OneParameterAction<StoredFile> onFileDownloading) {
		this.onFileDownloading = onFileDownloading;
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
