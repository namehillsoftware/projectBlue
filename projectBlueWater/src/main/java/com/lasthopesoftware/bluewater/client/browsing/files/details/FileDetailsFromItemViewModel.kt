package com.lasthopesoftware.bluewater.client.browsing.files.details

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.ProvideLibraryFiles
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideEditableLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.UpdateFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.KeyedIdentifier
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideUrlKey
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.images.ProvideDefaultImage
import com.lasthopesoftware.bluewater.shared.images.bytes.GetImageBytes
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.namehillsoftware.handoff.promises.Promise

class FileDetailsFromItemViewModel(
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
	var activeKeyedId: KeyedIdentifier? = null
		private set

	fun load(libraryId: LibraryId, itemId: KeyedIdentifier, positionedFile: PositionedFile): Promise<Unit> {
		activeKeyedId = itemId
		return load(libraryId, positionedFile)
	}

	override fun promiseFiles(libraryId: LibraryId): Promise<List<ServiceFile>> = when (val id = activeKeyedId) {
		is ItemId -> libraryFileProvider.promiseFiles(libraryId, id)
		is PlaylistId -> libraryFileProvider.promiseFiles(libraryId, id)
		else -> super.promiseFiles(libraryId)
	}
}
