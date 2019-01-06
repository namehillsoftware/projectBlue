package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download;

import android.support.annotation.Nullable;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.job.StoredFileJobStatus;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.vedsoft.futures.runnables.OneParameterAction;
import io.reactivex.Observable;

import java.util.Queue;

public interface IStoredFileDownloader {
	Observable<StoredFileJobStatus> process(Queue<StoredFileJob> jobsQueue);

	void setOnFileQueued(@Nullable OneParameterAction<StoredFile> onFileQueued);

	void setOnFileDownloading(@Nullable OneParameterAction<StoredFile> onFileDownloading);

	void setOnFileReadError(@Nullable OneParameterAction<StoredFile> onFileReadError);

	void setOnFileWriteError(@Nullable OneParameterAction<StoredFile> onFileWriteError);
}
