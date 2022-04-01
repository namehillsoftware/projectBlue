package com.lasthopesoftware.bluewater.client.stored.sync.receivers.file

import android.content.Context
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ProvideLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.AccessStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileMessage
import com.lasthopesoftware.bluewater.client.stored.sync.notifications.PostSyncNotification
import com.lasthopesoftware.bluewater.shared.cls
import com.namehillsoftware.handoff.promises.Promise

class StoredFileDownloadingNotifier(
	private val storedFileAccess: AccessStoredFiles,
	private val fileProperties: ProvideLibraryFileProperties,
	private val syncNotification: PostSyncNotification,
	private val context: Context
) : ReceiveStoredFileEvent {

	private val downloadingStatusLabel by lazy { context.getString(R.string.downloading_status_label) }

	override fun receive(storedFileId: Int): Promise<Unit> {
		return storedFileAccess.getStoredFile(storedFileId)
			.eventually { storedFile -> storedFile?.run { notifyOfFileDownload(this) } }
	}

	override fun acceptedEvents(): Collection<Class<out StoredFileMessage>> =
		setOf<Class<out StoredFileMessage>>(cls<StoredFileMessage.FileDownloading>())

	private fun notifyOfFileDownload(storedFile: StoredFile): Promise<Unit> {
		return fileProperties.promiseFileProperties(LibraryId(storedFile.libraryId), ServiceFile(storedFile.serviceId))
			.then { fileProperties -> syncNotification.notify(String.format(downloadingStatusLabel, fileProperties[KnownFileProperties.NAME])) }
			.excuse { syncNotification.notify(String.format(downloadingStatusLabel, context.getString(R.string.unknown_file))) }
	}
}
