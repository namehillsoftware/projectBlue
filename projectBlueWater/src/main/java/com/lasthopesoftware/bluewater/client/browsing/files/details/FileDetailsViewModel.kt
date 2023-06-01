package com.lasthopesoftware.bluewater.client.browsing.files.details

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.image.ProvideScopedImages
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FileProperty
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideEditableScopedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.getFormattedValue
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertiesUpdatedMessage
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.UpdateScopedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfScopedConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideScopedUrlKey
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.images.ProvideDefaultImage
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


class FileDetailsViewModel(
	private val connectionPermissions: CheckIfScopedConnectionIsReadOnly,
	private val scopedFilePropertiesProvider: ProvideEditableScopedFileProperties,
	private val updateFileProperties: UpdateScopedFileProperties,
	defaultImageProvider: ProvideDefaultImage,
	private val imageProvider: ProvideScopedImages,
	private val controlPlayback: ControlPlaybackService,
	registerForApplicationMessages: RegisterForApplicationMessages,
	private val scopedUrlKeyProvider: ProvideScopedUrlKey,
) : ViewModel() {

	companion object {
		private val propertiesToSkip = setOf(
			KnownFileProperties.AudioAnalysisInfo,
			KnownFileProperties.GetCoverArtInfo,
			KnownFileProperties.ImageFile,
			KnownFileProperties.Key,
			KnownFileProperties.StackFiles,
			KnownFileProperties.StackTop,
			KnownFileProperties.StackView,
			KnownFileProperties.Waveform,
			KnownFileProperties.LengthInPcmBlocks
		)
	}

	private var isConnectionReadOnly = false
	private var activeEditingFile: FilePropertyViewModel? = null
	private var associatedUrlKey: UrlKeyHolder<ServiceFile>? = null
	private var associatedPlaylist = emptyList<ServiceFile>()
	private var activePositionedFile: PositionedFile? = null
	private var activeLibraryId: LibraryId? = null
	private val propertyUpdateRegistrations = registerForApplicationMessages.registerReceiver { message: FilePropertiesUpdatedMessage ->
		if (message.urlServiceKey == associatedUrlKey)
			activePositionedFile?.serviceFile?.apply(::loadFileProperties)
	}

	private val mutableFileName = MutableStateFlow("")
	private val mutableAlbum = MutableStateFlow("")
	private val mutableArtist = MutableStateFlow("")
	private val mutableFileProperties = MutableStateFlow(emptyList<FilePropertyViewModel>())
	private val mutableIsLoading = MutableStateFlow(false)
	private val mutableCoverArt = MutableStateFlow<Bitmap?>(null)
	private val promisedSetDefaultCoverArt = defaultImageProvider.promiseFileBitmap()
		.then {
			mutableCoverArt.value = it
			it
		}
	private val mutableRating = MutableStateFlow(0)
	private val mutableHighlightedProperty = MutableStateFlow<FilePropertyViewModel?>(null)

	val fileName = mutableFileName.asStateFlow()
	val artist = mutableArtist.asStateFlow()
	val album = mutableAlbum.asStateFlow()
	val fileProperties = mutableFileProperties.asStateFlow()
	val isLoading = mutableIsLoading.asStateFlow()
	val coverArt = mutableCoverArt.asStateFlow()
	val rating = mutableRating.asStateFlow()
	val highlightedProperty = mutableHighlightedProperty.asStateFlow()

	override fun onCleared() {
		propertyUpdateRegistrations.close()
		super.onCleared()
	}

	fun loadFromList(libraryId: LibraryId, playlist: List<ServiceFile>, position: Int): Promise<Unit> {
		val serviceFile = playlist[position]
		activeLibraryId = libraryId
		activePositionedFile = PositionedFile(position, serviceFile)
		associatedPlaylist = playlist

		mutableIsLoading.value = true
		val isReadOnlyPromise = connectionPermissions.promiseIsReadOnly()
			.then { isConnectionReadOnly = it }

		val filePropertiesSetPromise = loadFileProperties(serviceFile)

		val urlKeyPromise = scopedUrlKeyProvider.promiseUrlKey(serviceFile)
			.then { associatedUrlKey = it }

		val bitmapSetPromise = promisedSetDefaultCoverArt // Ensure default cover art is first set before apply cover art from file properties
			.eventually { default ->
				imageProvider
					.promiseFileBitmap(serviceFile)
					.then { bitmap -> mutableCoverArt.value = bitmap ?: default }
			}

		return Promise
			.whenAll(filePropertiesSetPromise, bitmapSetPromise, urlKeyPromise, isReadOnlyPromise)
			.then { mutableIsLoading.value = false }
	}

	fun addToNowPlaying() {
		val serviceFile = activePositionedFile?.serviceFile ?: return
		val libraryId = activeLibraryId ?: return
		controlPlayback.addToPlaylist(libraryId, serviceFile)
	}

	fun play() {
		val positionedFile = activePositionedFile ?: return
		val libraryId = activeLibraryId ?: return
		controlPlayback.startPlaylist(libraryId, associatedPlaylist, positionedFile.playlistPosition)
	}

	private fun loadFileProperties(serviceFile: ServiceFile): Promise<Unit> =
		scopedFilePropertiesProvider
			.promiseFileProperties(serviceFile)
			.then { fileProperties ->
				val filePropertiesList = fileProperties.toList()
				val filePropertiesMap = filePropertiesList.associateBy { it.name }

				mutableFileName.value = filePropertiesMap[KnownFileProperties.Name]?.value ?: ""
				mutableArtist.value = filePropertiesMap[KnownFileProperties.Artist]?.value ?: ""
				mutableAlbum.value = filePropertiesMap[KnownFileProperties.Album]?.value ?: ""
				mutableRating.value = filePropertiesMap[KnownFileProperties.Rating]?.value?.toIntOrNull() ?: 0

				mutableFileProperties.value = filePropertiesList
					.filterNot { e -> propertiesToSkip.contains(e.name) }
					.sortedBy { it.name }
					.map(::FilePropertyViewModel)
			}
			.keepPromise(Unit)

	inner class FilePropertyViewModel(fileProperty: FileProperty) {

		private val formattedValue by lazy(LazyThreadSafetyMode.NONE) { fileProperty.getFormattedValue() }
		private val editableFilePropertyDefinition by lazy(LazyThreadSafetyMode.NONE) { fileProperty.editableFilePropertyDefinition }
		private val mutableCommittedValue by lazy { MutableStateFlow(formattedValue) }
		private val mutableUncommittedValue by lazy { MutableStateFlow(formattedValue) }
		private val mutableIsEditing = MutableStateFlow(false)

		val committedValue by lazy { mutableCommittedValue.asStateFlow() }
		val uncommittedValue by lazy { mutableUncommittedValue.asStateFlow() }
		val isEditing = mutableIsEditing.asStateFlow()
		val isEditable
			get() = !isConnectionReadOnly && editableFilePropertyDefinition != null
		val editableType by lazy(LazyThreadSafetyMode.NONE) { editableFilePropertyDefinition?.type }
		val property = fileProperty.name

		fun highlight() {
			mutableHighlightedProperty.value = this
		}

		fun edit() {
			activeEditingFile?.takeUnless { it == this }?.cancel()
			activeEditingFile = this
			mutableIsEditing.value = isEditable
		}

		fun updateValue(newValue: String) {
			mutableUncommittedValue.value = newValue
		}

		fun commitChanges(): Promise<Unit> {
			mutableIsEditing.value = false
			val newValue = uncommittedValue.value

			return activePositionedFile
				?.serviceFile
				?.let { serviceFile ->
					updateFileProperties
						.promiseFileUpdate(serviceFile, property, newValue, false)
						.then { mutableCommittedValue.value = newValue }
				}
				.keepPromise(Unit)
				.must(::cancel)
		}

		fun cancel() {
			mutableUncommittedValue.value = mutableCommittedValue.value
			mutableIsEditing.value = false
			mutableHighlightedProperty.compareAndSet(this, null)
		}
	}
}
