package com.lasthopesoftware.bluewater.client.stored.service.receivers.file

import android.content.Context
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ProvideLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.service.notifications.PostSyncNotification
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ResponseAction
import com.namehillsoftware.handoff.promises.response.VoidResponse
import com.namehillsoftware.lazyj.AbstractSynchronousLazy
import com.namehillsoftware.lazyj.CreateAndHold

class StoredFileDownloadingNotifier(
	private val storedFileAccess: IStoredFileAccess,
	private val fileProperties: ProvideLibraryFileProperties,
	private val syncNotification: PostSyncNotification,
	private val context: Context) : ReceiveStoredFileEvent {

	private val downloadingStatusLabel: CreateAndHold<String> = object : AbstractSynchronousLazy<String>() {
		override fun create(): String {
			return context.getString(R.string.downloading_status_label)
		}
	}

	override fun receive(storedFileId: Int): Promise<Void> {
		return storedFileAccess.getStoredFile(storedFileId).eventually { storedFile -> notifyOfFileDownload(storedFile) }
	}

	override fun acceptedEvents(): Collection<String> {
		return setOf(StoredFileSynchronization.onFileDownloadingEvent)
	}

	private fun notifyOfFileDownload(storedFile: StoredFile): Promise<Void> {
		return fileProperties.promiseFileProperties(LibraryId(storedFile.libraryId), ServiceFile(storedFile.serviceId))
			.then(VoidResponse(ResponseAction { fileProperties -> syncNotification.notify(String.format(downloadingStatusLabel.getObject(), fileProperties[KnownFileProperties.NAME])) }))
			.excuse(VoidResponse(ResponseAction<Throwable> { syncNotification.notify(String.format(downloadingStatusLabel.getObject(), context.getString(R.string.unknown_file))) }))
	}
}
