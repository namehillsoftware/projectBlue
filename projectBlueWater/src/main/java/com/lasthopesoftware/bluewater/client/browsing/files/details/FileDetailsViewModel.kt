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
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
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
	private val mutableFileProperties = MutableStateFlow(emptyMap<String, String>())
	private val mutableIsLoading = MutableStateFlow(false)
	private val mutableCoverArt = MutableStateFlow<Bitmap?>(null)
	private val promisedSetDefaultCoverArt = defaultImageProvider.promiseFileBitmap()
		.then {
			mutableCoverArt.value = it
			it
		}
	private val mutableRating = MutableStateFlow(0)
	private val mutableIsEditing = MutableStateFlow(false)
	private val emptyEditableFileProperties = lazy { emptyMap<EditableFilePropertyDefinition, EditableFileProperty>() }
	private var editableFileProperties = emptyEditableFileProperties
	private val mutableEditableFileProperty = MutableStateFlow<EditableFileProperty?>(null)
	private val mutableHighlightedProperty = MutableStateFlow<Pair<String, String>?>(null)

	val fileName = mutableFileName.asStateFlow()
	val artist = mutableArtist.asStateFlow()
	val album = mutableAlbum.asStateFlow()
	val fileProperties = mutableFileProperties.asStateFlow()
	val isLoading = mutableIsLoading.asStateFlow()
	val coverArt = mutableCoverArt.asStateFlow()
	val rating = mutableRating.asStateFlow()
	val isEditing = mutableIsEditing.asStateFlow()
	val editableFileProperty = mutableEditableFileProperty.asStateFlow()
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

	fun highlightProperty(property: String) {
		val propertyValue = fileProperties.value[property] ?: ""
		mutableHighlightedProperty.value = Pair(property, propertyValue)
	}

	fun editFileProperties() {
		mutableIsEditing.value = true
		editableFileProperties = lazy {
			fileProperties.value
				.mapNotNull {
					EditableFilePropertyDefinition
						.fromDescriptor(it.key)
						?.let { k -> Pair(k, EditableFileProperty(k, it.value)) }
				}
				.toMap()
		}
	}

	fun editFileProperty(property: EditableFilePropertyDefinition): Promise<Unit> {
		return mutableEditableFileProperty.value
			?.commitChanges()
			.keepPromise(Unit)
			.must { mutableEditableFileProperty.value = editableFileProperties.value[property] }
	}

	fun saveAndStopEditing(): Promise<Unit> =
		mutableEditableFileProperty.value
			?.commitChanges()
			?.must(::resetBoard)
			?: resetBoard().toPromise()

	fun stopEditing() {
		mutableEditableFileProperty.value?.cancel()
		resetBoard()
	}

	private fun resetBoard() {
		editableFileProperties = emptyEditableFileProperties
		mutableIsEditing.value = false
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
					.associate { e -> Pair(e.key, e.value) }
					.toSortedMap()
			}
			.keepPromise(Unit)

	fun clearHighlights() {
		mutableHighlightedProperty.value = null
	}

	inner class EditableFileProperty(val property: EditableFilePropertyDefinition, currentValue: String) {
		private val mutablePropertyValue = MutableStateFlow(currentValue)

		val propertyValue = mutablePropertyValue.asStateFlow()

		fun updateValue(newValue: String) {
			mutablePropertyValue.value = newValue
		}

		fun commitChanges(): Promise<Unit> {
			val newValue = propertyValue.value

			return activePositionedFile
				?.serviceFile
				?.let { serviceFile ->
					updateFileProperties.promiseFileUpdate(serviceFile, property.descriptor, newValue, false)
				}
				.keepPromise(Unit)
				.must(::cancel)
		}

		fun cancel() {
			mutableEditableFileProperty.value = null
		}
	}
}
