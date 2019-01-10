package com.lasthopesoftware.bluewater.client.stored.library.items.files.download;

import android.support.annotation.Nullable;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.vedsoft.futures.runnables.OneParameterAction;
import io.reactivex.Observable;

import java.util.Queue;

public interface IStoredFileDownloader {
	Observable<StoredFileJobStatus> process(Queue<StoredFileJob> jobsQueue);

	void setOnFileDownloading(@Nullable OneParameterAction<StoredFile> onFileDownloading);

	void setOnFileReadError(@Nullable OneParameterAction<StoredFile> onFileReadError);

	void setOnFileWriteError(@Nullable OneParameterAction<StoredFile> onFileWriteError);
}
