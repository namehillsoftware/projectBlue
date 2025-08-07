package com.lasthopesoftware.bluewater.client.browsing.files.details

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.ProvideLibraryFiles
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideEditableLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.UpdateFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideUrlKey
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.images.ProvideDefaultImage
import com.lasthopesoftware.bluewater.shared.images.bytes.GetImageBytes
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.namehillsoftware.handoff.promises.Promise

class FileDetailsFromSearchViewModel(
	connectionPermissions: CheckIfConnectionIsReadOnly,
	filePropertiesProvider: ProvideEditableLibraryFileProperties,
	updateFileProperties: UpdateFileProperties,
	defaultImageProvider: ProvideDefaultImage,
	imageProvider: GetImageBytes,
	controlPlayback: ControlPlaybackService,
	registerForApplicationMessages: RegisterForApplicationMessages,
	urlKeyProvider: ProvideUrlKey,
	private val libraryFileProvider: ProvideLibraryFiles,
) : AbstractFileDetailsViewModel(
	connectionPermissions,
	filePropertiesProvider,
	updateFileProperties,
	defaultImageProvider,
	imageProvider,
	controlPlayback,
	registerForApplicationMessages,
	urlKeyProvider,
) {
	var activeQuery: String? = null
		private set

	fun load(libraryId: LibraryId, searchQuery: String, positionedFile: PositionedFile): Promise<Unit> {
		activeQuery = searchQuery
		return load(libraryId, positionedFile)
	}

	override fun promiseFiles(libraryId: LibraryId): Promise<List<ServiceFile>> =
		activeQuery
			?.let { libraryFileProvider.promiseAudioFiles(libraryId, it) }
			?: super.promiseFiles(libraryId)
}
