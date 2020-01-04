package com.lasthopesoftware.bluewater.client.stored.service.receivers.file;

import android.content.Context;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.KnownFileProperties;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.ProvideLibraryFileProperties;
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.stored.service.notifications.PostSyncNotification;
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.VoidResponse;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;

import java.util.Collection;
import java.util.Collections;

public class StoredFileDownloadingNotifier implements ReceiveStoredFileEvent {

	private final CreateAndHold<String> downloadingStatusLabel = new AbstractSynchronousLazy<String>() {
		@Override
		protected String create() {
			return context.getString(R.string.downloading_status_label);
		}
	};

	private final IStoredFileAccess storedFileAccess;
	private final ProvideLibraryFileProperties fileProperties;
	private final PostSyncNotification syncNotification;
	private final Context context;

	public StoredFileDownloadingNotifier(
		IStoredFileAccess storedFileAccess,
		ProvideLibraryFileProperties fileProperties,
		PostSyncNotification syncNotification,
		Context context) {
		this.storedFileAccess = storedFileAccess;
		this.fileProperties = fileProperties;
		this.syncNotification = syncNotification;
		this.context = context;
	}

	@Override
	public Promise<Void> receive(int storedFileId) {
		return storedFileAccess.getStoredFile(storedFileId).eventually(this::notifyOfFileDownload);
	}

	@Override
	public Collection<String> acceptedEvents() {
		return Collections.singleton(StoredFileSynchronization.onFileDownloadingEvent);
	}

	private Promise<Void> notifyOfFileDownload(StoredFile storedFile) {
		return fileProperties.promiseFileProperties(new LibraryId(storedFile.getLibraryId()), new ServiceFile(storedFile.getServiceId()))
			.then(new VoidResponse<>(fileProperties -> syncNotification.notify(String.format(downloadingStatusLabel.getObject(), fileProperties.get(KnownFileProperties.NAME)))))
			.excuse(new VoidResponse<>(exception -> syncNotification.notify(String.format(downloadingStatusLabel.getObject(), context.getString(R.string.unknown_file)))));
	}
}
