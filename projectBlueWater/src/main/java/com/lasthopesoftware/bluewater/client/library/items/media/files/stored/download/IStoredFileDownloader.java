package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.vedsoft.futures.runnables.OneParameterAction;

public interface IStoredFileDownloader {
	void queueFileForDownload(@NonNull ServiceFile serviceFile, @NonNull StoredFile storedFile);

	void cancel();

	void process();

	void setOnFileQueued(@Nullable OneParameterAction<StoredFile> onFileQueued);

	void setOnFileDownloading(@Nullable OneParameterAction<StoredFile> onFileDownloading);

	void setOnFileDownloaded(@Nullable OneParameterAction<StoredFileJobResult> onFileDownloaded);

	void setOnQueueProcessingCompleted(Runnable onQueueProcessingCompleted);

	void setOnFileReadError(@Nullable OneParameterAction<StoredFile> onFileReadError);

	void setOnFileWriteError(@Nullable OneParameterAction<StoredFile> onFileWriteError);
}
