package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download;

import android.support.annotation.Nullable;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.namehillsoftware.handoff.promises.Promise;
import com.vedsoft.futures.runnables.OneParameterAction;

import java.util.Collection;
import java.util.Queue;

public interface IStoredFileDownloader {
	Promise<Collection<StoredFileJobResult>> process(Queue<StoredFileJob> jobsQueue);

	void setOnFileQueued(@Nullable OneParameterAction<StoredFile> onFileQueued);

	void setOnFileDownloading(@Nullable OneParameterAction<StoredFile> onFileDownloading);

	void setOnFileDownloaded(@Nullable OneParameterAction<StoredFileJobResult> onFileDownloaded);

	void setOnQueueProcessingCompleted(Runnable onQueueProcessingCompleted);

	void setOnFileReadError(@Nullable OneParameterAction<StoredFile> onFileReadError);

	void setOnFileWriteError(@Nullable OneParameterAction<StoredFile> onFileWriteError);
}
