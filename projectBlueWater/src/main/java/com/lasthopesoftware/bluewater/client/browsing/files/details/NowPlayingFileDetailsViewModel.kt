package com.lasthopesoftware.bluewater.client.browsing.files.details

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideEditableLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.UpdateFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideUrlKey
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.GetNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.images.ProvideDefaultImage
import com.lasthopesoftware.bluewater.shared.images.bytes.GetImageBytes
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.namehillsoftware.handoff.promises.Promise

class NowPlayingFileDetailsViewModel(
	connectionPermissions: CheckIfConnectionIsReadOnly,
	filePropertiesProvider: ProvideEditableLibraryFileProperties,
	updateFileProperties: UpdateFileProperties,
	defaultImageProvider: ProvideDefaultImage,
	imageProvider: GetImageBytes,
	controlPlayback: ControlPlaybackService,
	registerForApplicationMessages: RegisterForApplicationMessages,
	urlKeyProvider: ProvideUrlKey,
	private val nowPlayingRepository: GetNowPlayingState,
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
	override fun promiseFiles(libraryId: LibraryId): Promise<List<ServiceFile>> =
		nowPlayingRepository
			.promiseNowPlaying(libraryId)
			.then { np -> np?.playlist ?: emptyList() }
}
