package com.lasthopesoftware.bluewater.client.stored.library.items.files.download;

import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.namehillsoftware.handoff.promises.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public final class StoredFileDownloader implements DownloadStoredFiles {

	private static final Logger logger = LoggerFactory.getLogger(StoredFileDownloader.class);

	public StoredFileDownloader() {

	}

	@Override
	public Promise<InputStream> promiseDownload(StoredFile storedFile) {
		return new Promise<>(new ByteArrayInputStream(new byte[0]));
	}
}
