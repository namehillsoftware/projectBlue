package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.job;

import android.support.annotation.NonNull;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IServiceFileUriQueryParamsProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.io.IFileStreamWriter;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.IStoredFileSystemFileProducer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.StoredFileJobResult;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.StoredFileJobResultOptions;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.exceptions.StoredFileJobException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.exceptions.StoredFileReadException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.exceptions.StoredFileWriteException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.storage.read.permissions.IFileReadPossibleArbitrator;
import com.lasthopesoftware.storage.write.exceptions.StorageCreatePathException;
import com.lasthopesoftware.storage.write.permissions.IFileWritePossibleArbitrator;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class StoredFileJobProcessor implements ProcessStoredFileJobs {
	private static final Executor storedFileExecutor = Executors.newSingleThreadExecutor();

	private static final Logger logger = LoggerFactory.getLogger(StoredFileJobProcessor.class);

	@NonNull private final IFileWritePossibleArbitrator fileWritePossibleArbitrator;
	@NonNull private final IServiceFileUriQueryParamsProvider serviceFileUriQueryParamsProvider;
	@NonNull private final IFileReadPossibleArbitrator fileReadPossibleArbitrator;
	@NonNull private final IStoredFileSystemFileProducer storedFileFileProvider;
	@NonNull private final IConnectionProvider connectionProvider;
	@NonNull private final IFileStreamWriter fileStreamWriter;
	@NonNull private final IStoredFileAccess storedFileAccess;

	public StoredFileJobProcessor(@NonNull IStoredFileSystemFileProducer storedFileFileProvider, @NonNull IConnectionProvider connectionProvider, @NonNull IStoredFileAccess storedFileAccess, @NonNull IServiceFileUriQueryParamsProvider serviceFileUriQueryParamsProvider, @NonNull IFileReadPossibleArbitrator fileReadPossibleArbitrator, @NonNull IFileWritePossibleArbitrator fileWritePossibleArbitrator, @NonNull IFileStreamWriter fileStreamWriter) {
		this.fileWritePossibleArbitrator = fileWritePossibleArbitrator;
		this.serviceFileUriQueryParamsProvider = serviceFileUriQueryParamsProvider;
		this.fileReadPossibleArbitrator = fileReadPossibleArbitrator;
		this.storedFileFileProvider = storedFileFileProvider;
		this.connectionProvider = connectionProvider;
		this.fileStreamWriter = fileStreamWriter;
		this.storedFileAccess = storedFileAccess;
	}

	@Override
	public Promise<StoredFileJobResult> promiseDownloadedStoredFile(StoredFileJob job) {
		final StoredFile storedFile = job.getStoredFile();
		final File file = storedFileFileProvider.getFile(storedFile);

		if (file.exists()) {
			if (!fileReadPossibleArbitrator.isFileReadPossible(file))
				return new Promise<>(new StoredFileReadException(file, storedFile));

			if (storedFile.isDownloadComplete())
				return new Promise<>(new StoredFileJobResult(file, storedFile, StoredFileJobResultOptions.AlreadyExists));
		}

		if (!fileWritePossibleArbitrator.isFileWritePossible(file))
			return new Promise<>(new StoredFileWriteException(file, storedFile));

		final File parent = file.getParentFile();
		if (parent != null && !parent.exists() && !parent.mkdirs())
			return new Promise<>(new StorageCreatePathException(parent));

		final ServiceFile serviceFile = job.getServiceFile();
		return connectionProvider.promiseResponse(serviceFileUriQueryParamsProvider.getServiceFileUriQueryParams(serviceFile))
			.eventually(response -> new QueuedPromise<>((cancellationToken) -> {
				final ResponseBody body = response.body();
				if (body == null) return null;

				if (cancellationToken.isCancelled()) return getCancelledStoredFileJobResult(file, storedFile);

				try (final InputStream is = body.byteStream()) {

					if (cancellationToken.isCancelled()) return getCancelledStoredFileJobResult(file, storedFile);

					try {
						this.fileStreamWriter.writeStreamToFile(is, file);

						storedFileAccess.markStoredFileAsDownloaded(storedFile);

						return new StoredFileJobResult(file, storedFile, StoredFileJobResultOptions.Downloaded);
					} catch (IOException ioe) {
						logger.error("Error writing file!", ioe);
						throw new StoredFileWriteException(file, storedFile, ioe);
					}
				} catch (Throwable t) {
					throw new StoredFileJobException(storedFile, t);
				} finally {
					body.close();
				}
			}, storedFileExecutor), error -> {
				logger.error("Error getting connection", error);
				return new Promise<>(new StoredFileJobException(storedFile, error));
			});
	}

	private StoredFileJobResult getCancelledStoredFileJobResult(File file, StoredFile storedFile) {
		return new StoredFileJobResult(file, storedFile, StoredFileJobResultOptions.Cancelled);
	}
}
