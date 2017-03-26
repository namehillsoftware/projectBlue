package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download;

import android.support.annotation.NonNull;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.StoredFileAccess;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.exceptions.StoredFileJobException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.exceptions.StoredFileReadException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.exceptions.StoredFileWriteException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.RemoteFileUriProvider;
import com.lasthopesoftware.bluewater.shared.promises.extensions.QueuedPromise;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.Promise;
import com.lasthopesoftware.storage.read.permissions.IFileReadPossibleArbitrator;
import com.lasthopesoftware.storage.write.exceptions.StorageCreatePathException;
import com.lasthopesoftware.storage.write.permissions.IFileWritePossibleArbitrator;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by david on 7/17/16.
 */
class StoredFileJob {

	private static final Logger logger = LoggerFactory.getLogger(StoredFileJob.class);

	private static final Executor downloadExecutor = Executors.newSingleThreadExecutor();

	private final IFileWritePossibleArbitrator fileWritePossibleArbitrator;
	private final IFileReadPossibleArbitrator fileReadPossibleArbitrator;
	private final IFile serviceFile;
	private final StoredFile storedFile;
	private boolean isCancelled;
	private IConnectionProvider connectionProvider;
	@NonNull
	private final RemoteFileUriProvider remoteFileUriProvider;
	private StoredFileAccess storedFileAccess;

	StoredFileJob(@NonNull IConnectionProvider connectionProvider, @NonNull StoredFileAccess storedFileAccess, @NonNull IFileReadPossibleArbitrator fileReadPossibleArbitrator, @NonNull IFileWritePossibleArbitrator fileWritePossibleArbitrator, @NonNull IFile serviceFile, @NonNull StoredFile storedFile) {
		this.connectionProvider = connectionProvider;
		this.remoteFileUriProvider = new RemoteFileUriProvider(connectionProvider);
		this.storedFileAccess = storedFileAccess;
		this.fileReadPossibleArbitrator = fileReadPossibleArbitrator;
		this.fileWritePossibleArbitrator = fileWritePossibleArbitrator;
		this.serviceFile = serviceFile;
		this.storedFile = storedFile;
	}

	public void cancel() {
		isCancelled = true;
	}

	IPromise<StoredFileJobResult> processJob() throws StoredFileJobException, StoredFileReadException, StoredFileWriteException, StorageCreatePathException {
		final java.io.File file = new java.io.File(storedFile.getPath());
		if (isCancelled) return new Promise<>(getCancelledStoredFileJobResult(file));

		if (file.exists()) {
			if (!fileReadPossibleArbitrator.isFileReadPossible(file))
				throw new StoredFileReadException(file, storedFile);

			if (storedFile.isDownloadComplete())
				return new Promise<>(new StoredFileJobResult(file, storedFile, StoredFileJobResultOptions.AlreadyExists));
		}

		if (!fileWritePossibleArbitrator.isFileWritePossible(file))
			throw new StoredFileWriteException(file, storedFile);

		return
			remoteFileUriProvider
				.getFileUri(serviceFile)
				.thenPromise(uri -> {
					final HttpURLConnection connection;
					try {
						connection = connectionProvider.getConnection(uri.toString().replace(connectionProvider.getUrlProvider().getBaseUrl(), ""));
					} catch (IOException e) {
						logger.error("Error getting connection", e);
						throw new StoredFileJobException(storedFile, e);
					}

					if (isCancelled) return new Promise<>(getCancelledStoredFileJobResult(file));

					return new QueuedPromise<>(() -> {
						try {
							InputStream is;
							try {
								is = connection.getInputStream();
							} catch (IOException ioe) {
								logger.error("Error opening data connection", ioe);
								throw new StoredFileJobException(storedFile, ioe);
							}

							if (isCancelled) return getCancelledStoredFileJobResult(file);

							final java.io.File parent = file.getParentFile();
							if (!parent.exists() && !parent.mkdirs()) throw new StorageCreatePathException(parent);

							try {
								try (FileOutputStream fos = new FileOutputStream(file)) {
									IOUtils.copy(is, fos);
									fos.flush();
								}

								storedFileAccess.markStoredFileAsDownloaded(storedFile);

								return new StoredFileJobResult(file, storedFile, StoredFileJobResultOptions.Downloaded);
							} catch (IOException ioe) {
								logger.error("Error writing file!", ioe);
								throw new StoredFileWriteException(file, storedFile, ioe);
							} finally {
								if (is != null) {
									try {
										is.close();
									} catch (IOException e) {
										logger.error("Error closing input stream", e);
									}
								}
							}
						} finally {
							connection.disconnect();
						}
					}, downloadExecutor);
				});
	}

	public StoredFile getStoredFile() {
		return storedFile;
	}

	private StoredFileJobResult getCancelledStoredFileJobResult(File file) {
		return new StoredFileJobResult(file, storedFile, StoredFileJobResultOptions.Cancelled);
	}
}
