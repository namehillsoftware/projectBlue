package com.lasthopesoftware.bluewater.client.browsing.files.details

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.EditableFileProperty
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FileProperty
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideEditableLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.editableFilePropertyDefinition
import com.lasthopesoftware.bluewater.client.browsing.files.properties.getFormattedValue
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertiesUpdatedMessage
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.UpdateFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideUrlKey
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.images.ProvideDefaultImage
import com.lasthopesoftware.bluewater.shared.images.bytes.GetImageBytes
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.observables.LiftedInteractionState
import com.lasthopesoftware.bluewater.shared.observables.MutableInteractionState
import com.lasthopesoftware.bluewater.shared.observables.mapNotNull
import com.lasthopesoftware.bluewater.shared.observables.toMaybeObservable
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.unitResponse
import com.lasthopesoftware.resources.emptyByteArray
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateAction


class FileDetailsViewModel(
	private val connectionPermissions: CheckIfConnectionIsReadOnly,
	private val filePropertiesProvider: ProvideEditableLibraryFileProperties,
	private val updateFileProperties: UpdateFileProperties,
	defaultImageProvider: ProvideDefaultImage,
	private val imageProvider: GetImageBytes,
	private val controlPlayback: ControlPlaybackService,
	registerForApplicationMessages: RegisterForApplicationMessages,
	private val urlKeyProvider: ProvideUrlKey,
) : ViewModel(), ImmediateAction {

	companion object {
		private val propertiesToSkip = setOf(
			NormalizedFileProperties.AudioAnalysisInfo,
			NormalizedFileProperties.GetCoverArtInfo,
			NormalizedFileProperties.ImageFile,
			NormalizedFileProperties.Key,
			NormalizedFileProperties.StackFiles,
			NormalizedFileProperties.StackTop,
			NormalizedFileProperties.StackView,
			NormalizedFileProperties.Waveform,
			NormalizedFileProperties.LengthInPcmBlocks
		)
	}

	private var isConnectionReadOnly = false
	private var activeEditingFile: FilePropertyViewModel? = null
	private var associatedUrlKey: UrlKeyHolder<ServiceFile>? = null
	private var associatedPlaylist = emptyList<ServiceFile>()
	private var activePositionedFile: PositionedFile? = null
	private val propertyUpdateRegistrations = registerForApplicationMessages.registerReceiver { message: FilePropertiesUpdatedMessage ->
		if (message.urlServiceKey == associatedUrlKey) reloadFileProperties()
	}

	private val mutableFileName = MutableInteractionState("")
	private val mutableAlbum = MutableInteractionState("")
	private val mutableArtist = MutableInteractionState("")
	private val mutableFileProperties = MutableInteractionState(emptyList<FilePropertyViewModel>())
	private val mutableIsLoading = MutableInteractionState(false)
	private val mutableCoverArt = MutableInteractionState(emptyByteArray)
	private val promisedDefaultCoverArt by lazy { defaultImageProvider.promiseImageBytes() }
	private val mutableRating = MutableInteractionState(0)
	private val mutableHighlightedProperty = MutableInteractionState<FilePropertyViewModel?>(null)

	val fileName = mutableFileName.asInteractionState()
	val artist = mutableArtist.asInteractionState()
	val album = mutableAlbum.asInteractionState()
	val fileProperties = mutableFileProperties.asInteractionState()
	val isLoading = mutableIsLoading.asInteractionState()
	val coverArt = LiftedInteractionState(
		promisedDefaultCoverArt
			.toMaybeObservable()
			.toObservable()
			.concatWith(mutableCoverArt.mapNotNull()),
		emptyByteArray
	)
	val rating = mutableRating.asInteractionState()
	val highlightedProperty = mutableHighlightedProperty.asInteractionState()
	var activeLibraryId: LibraryId? = null
		private set

	override fun onCleared() {
		propertyUpdateRegistrations.close()
		coverArt.close()
		super.onCleared()
	}

	fun loadFromList(libraryId: LibraryId, playlist: List<ServiceFile>, position: Int): Promise<Unit> {
		val serviceFile = playlist[position]
		activeLibraryId = libraryId
		activePositionedFile = PositionedFile(position, serviceFile)
		associatedPlaylist = playlist

		return promiseLoadedActiveFile()
	}

	fun promiseLoadedActiveFile(): Promise<Unit> = activeLibraryId
		?.let { libraryId ->
			activePositionedFile?.serviceFile?.let {  serviceFile ->
				mutableIsLoading.value = true
				val isReadOnlyPromise = connectionPermissions
					.promiseIsReadOnly(libraryId)
					.then { r -> isConnectionReadOnly = r }

				val filePropertiesSetPromise = loadFileProperties(libraryId, serviceFile)

				val urlKeyPromise = urlKeyProvider
					.promiseUrlKey(libraryId, serviceFile)
					.then { u -> associatedUrlKey = u }

				val bitmapSetPromise = promisedDefaultCoverArt
					.eventually { default ->
						mutableCoverArt.value = default
						imageProvider
							.promiseImageBytes(libraryId, serviceFile)
							.then { bytes -> mutableCoverArt.value = bytes.takeIf { it.isNotEmpty() } ?: default }
					}

				Promise
					.whenAll(filePropertiesSetPromise, bitmapSetPromise, urlKeyPromise, isReadOnlyPromise)
					.must(this)
					.unitResponse()
			}
		}
		.keepPromise(Unit)

	private fun reloadFileProperties(): Promise<Unit> =
		activeLibraryId
			?.let { l ->
				activePositionedFile?.serviceFile?.let {  sf ->
					loadFileProperties(l, sf)
				}
			}
			.keepPromise(Unit)

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

	override fun act() {
		mutableIsLoading.value = false
	}

	private fun loadFileProperties(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Unit> =
		filePropertiesProvider
			.promiseFileProperties(libraryId, serviceFile)
			.then { fileProperties ->
				val filePropertiesList = fileProperties.toList()
				val filePropertiesMap = filePropertiesList.associateBy { it.name }

				mutableFileName.value = filePropertiesMap[NormalizedFileProperties.Name]?.value ?: ""
				mutableArtist.value = filePropertiesMap[NormalizedFileProperties.Artist]?.value ?: ""
				mutableAlbum.value = filePropertiesMap[NormalizedFileProperties.Album]?.value ?: ""
				mutableRating.value = filePropertiesMap[NormalizedFileProperties.Rating]?.value?.toIntOrNull() ?: 0

				mutableFileProperties.value = filePropertiesList
					.filterNot { e -> propertiesToSkip.contains(e.name) }
					.sortedBy { it.name }
					.map(::FilePropertyViewModel)
			}
			.keepPromise(Unit)

	inner class FilePropertyViewModel(fileProperty: FileProperty) {

		private val formattedValue by lazy(LazyThreadSafetyMode.PUBLICATION) { fileProperty.getFormattedValue() }
		private val editableFilePropertyDefinition by lazy(LazyThreadSafetyMode.PUBLICATION) {
			fileProperty.editableFilePropertyDefinition
		}
		private val mutableCommittedValue by lazy { MutableInteractionState(formattedValue) }
		private val mutableUncommittedValue by lazy { MutableInteractionState(formattedValue) }
		private val mutableIsEditing = MutableInteractionState(false)

		val committedValue
			get() = mutableCommittedValue.asInteractionState()
		val uncommittedValue
			get() = mutableUncommittedValue.asInteractionState()

		val isEditing = mutableIsEditing.asInteractionState()
		val isEditable by lazy(LazyThreadSafetyMode.PUBLICATION) {
			!isConnectionReadOnly && fileProperty is EditableFileProperty
		}
		val editableType by lazy(LazyThreadSafetyMode.PUBLICATION) { editableFilePropertyDefinition?.type }
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

			return activeLibraryId?.let { l ->
					activePositionedFile
						?.serviceFile
						?.let { serviceFile ->
							updateFileProperties
								.promiseFileUpdate(l, serviceFile, property, newValue, true)
								.then { _ -> mutableCommittedValue.value = newValue }
						}
				}
				.keepPromise(Unit)
				.must(::cancel)
		}

		fun cancel() {
			mutableUncommittedValue.value = mutableCommittedValue.value
			mutableIsEditing.value = false

			while (mutableHighlightedProperty.value == this) {
				mutableHighlightedProperty.value = null
			}
		}
	}
}
