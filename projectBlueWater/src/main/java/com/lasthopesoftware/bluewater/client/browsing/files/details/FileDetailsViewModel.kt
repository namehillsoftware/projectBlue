package com.lasthopesoftware.bluewater.client.browsing.files.details

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.image.ProvideImages
import com.lasthopesoftware.bluewater.client.browsing.files.properties.EditableFilePropertyDefinition
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideScopedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertiesUpdatedMessage
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.UpdateScopedFileProperties
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideScopedUrlKeyProvider
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
	private val scopedFilePropertiesProvider: ProvideScopedFileProperties,
	private val updateFileProperties: UpdateScopedFileProperties,
	defaultImageProvider: ProvideDefaultImage,
	private val imageProvider: ProvideImages,
	private val controlPlayback: ControlPlaybackService,
	registerForApplicationMessages: RegisterForApplicationMessages,
	private val scopedUrlKeyProvider: ProvideScopedUrlKeyProvider,
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

	private var activeEditingFile: FilePropertyViewModel? = null
	private var associatedUrlKey: UrlKeyHolder<ServiceFile>? = null
	private var associatedPlaylist = emptyList<ServiceFile>()
	private var activePositionedFile: PositionedFile? = null
	private val propertyUpdateRegistrations = registerForApplicationMessages.registerReceiver { message: FilePropertiesUpdatedMessage ->
		if (message.urlServiceKey == associatedUrlKey)
			activePositionedFile?.serviceFile?.apply(::loadFileProperties)
	}

	private val mutableFileName = MutableStateFlow("")
	private val mutableAlbum = MutableStateFlow("")
	private val mutableArtist = MutableStateFlow("")
	private val mutableFileProperties = MutableStateFlow(emptyMap<String, FilePropertyViewModel>())
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

	fun loadFromList(playlist: List<ServiceFile>, position: Int): Promise<Unit> {
		val serviceFile = playlist[position]
		activePositionedFile = PositionedFile(position, serviceFile)
		associatedPlaylist = playlist

		mutableIsLoading.value = true
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
			.whenAll(filePropertiesSetPromise, bitmapSetPromise, urlKeyPromise)
			.then { mutableIsLoading.value = false }
	}

	fun addToNowPlaying() {
		activePositionedFile?.serviceFile?.let(controlPlayback::addToPlaylist)
	}

	fun play() {
		val positionedFile = activePositionedFile ?: return
		controlPlayback.startPlaylist(associatedPlaylist, positionedFile.playlistPosition)
	}

	private fun loadFileProperties(serviceFile: ServiceFile): Promise<Unit> =
		scopedFilePropertiesProvider
			.promiseFileProperties(serviceFile)
			.then { fileProperties ->
				fileProperties[KnownFileProperties.Name]?.also { mutableFileName.value = it }
				fileProperties[KnownFileProperties.Artist]?.also { mutableArtist.value = it }
				fileProperties[KnownFileProperties.Album]?.also { mutableAlbum.value = it }
				fileProperties[KnownFileProperties.Rating]?.toIntOrNull()?.also { mutableRating.value = it }

				mutableFileProperties.value = fileProperties.entries
					.filterNot { e -> propertiesToSkip.contains(e.key) }
					.associate { e -> Pair(e.key, FilePropertyViewModel(e.key, e.value)) }
					.toSortedMap()
			}
			.keepPromise(Unit)

	inner class FilePropertyViewModel(
		val property: String,
		private val originalValue: String
	) {
		private val editableFilePropertyDefinition by lazy { EditableFilePropertyDefinition.fromDescriptor(property) }

		private val mutableValue = MutableStateFlow(originalValue)
		private val mutableIsEditing = MutableStateFlow(false)

		val value = mutableValue.asStateFlow()
		val isEditing = mutableIsEditing.asStateFlow()
		val isEditable by lazy { editableFilePropertyDefinition != null }

		fun highlight() {
			mutableHighlightedProperty.value = this
		}

		fun edit() {
			activeEditingFile?.cancel()
			activeEditingFile = this
			mutableIsEditing.value = isEditable
		}

		fun updateValue(newValue: String) {
			mutableValue.value = newValue
		}

		fun commitChanges(): Promise<Unit> {
			val newValue = value.value

			return activePositionedFile
				?.serviceFile
				?.let { serviceFile ->
					updateFileProperties.promiseFileUpdate(serviceFile, property, newValue, false)
				}
				.keepPromise(Unit)
				.must(::cancel)
		}

		fun cancel() {
			mutableValue.value = originalValue
			mutableIsEditing.value = false
			mutableHighlightedProperty.compareAndSet(this, null)
		}
	}
}
