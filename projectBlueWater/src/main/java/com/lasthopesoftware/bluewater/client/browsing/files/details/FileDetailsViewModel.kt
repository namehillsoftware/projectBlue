package com.lasthopesoftware.bluewater.client.browsing.files.details

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.EditableFileProperty
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FileProperty
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyHelpers.editableFilePropertyDefinition
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideFreshLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.getFormattedValue
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertiesUpdatedMessage
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.UpdateFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideUrlKey
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.images.ProvideDefaultImage
import com.lasthopesoftware.bluewater.shared.images.bytes.GetImageBytes
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.observables.LiftedInteractionState
import com.lasthopesoftware.observables.MutableInteractionState
import com.lasthopesoftware.observables.mapNotNull
import com.lasthopesoftware.observables.toSingleObservable
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.promises.extensions.unitResponse
import com.lasthopesoftware.resources.emptyByteArray
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateAction


class FileDetailsViewModel(
	private val connectionPermissions: CheckIfConnectionIsReadOnly,
	private val filePropertiesProvider: ProvideFreshLibraryFileProperties,
	private val updateFileProperties: UpdateFileProperties,
	defaultImageProvider: ProvideDefaultImage,
	private val imageProvider: GetImageBytes,
	private val controlPlayback: ControlPlaybackService,
	registerForApplicationMessages: RegisterForApplicationMessages,
	private val urlKeyProvider: ProvideUrlKey,
) : ViewModel(), ImmediateAction, FileDetailsState, LoadFileDetailsState {

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

	override val fileName = mutableFileName.asInteractionState()
	override val artist = mutableArtist.asInteractionState()
	override val album = mutableAlbum.asInteractionState()
	override val fileProperties = mutableFileProperties.asInteractionState()
	override val isLoading = mutableIsLoading.asInteractionState()
	override val coverArt = LiftedInteractionState(
		promisedDefaultCoverArt
			.toSingleObservable()
			.toObservable()
			.concatWith(mutableCoverArt.mapNotNull()),
		emptyByteArray
	)
	override val rating = mutableRating.asInteractionState()
	override val highlightedProperty = mutableHighlightedProperty.asInteractionState()

	override var activeLibraryId: LibraryId? = null
		private set
	override var activeServiceFile: ServiceFile? = null
		private set

	override fun onCleared() {
		propertyUpdateRegistrations.close()
		coverArt.close()
		super.onCleared()
	}

	override fun load(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Unit> {
		activeLibraryId = libraryId
		activeServiceFile = serviceFile

		return promiseLoadedActiveFile()
	}

	override fun promiseLoadedActiveFile(): Promise<Unit> = activeLibraryId
		?.let { libraryId ->
			activeServiceFile?.let {  serviceFile ->
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

	override fun addToNowPlaying() {
		val serviceFile = activeServiceFile ?: return
		val libraryId = activeLibraryId ?: return
		controlPlayback.addToPlaylist(libraryId, serviceFile)
	}

	override fun playNext() {
		val serviceFile = activeServiceFile ?: return
		val libraryId = activeLibraryId ?: return
		controlPlayback.addAfterNowPlayingFile(libraryId, serviceFile)
	}

	private fun reloadFileProperties(): Promise<Unit> =
		activeLibraryId
			?.let { l ->
				activeServiceFile?.let {  sf ->
					loadFileProperties(l, sf)
				}
			}
			.keepPromise(Unit)

	override fun act() {
		mutableIsLoading.value = false
	}

	private fun loadFileProperties(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Unit> =
		filePropertiesProvider
			.promiseFileProperties(libraryId, serviceFile)
			.eventually { fileProperties ->
				ThreadPools.compute.preparePromise { cs ->
					if (cs.isCancelled) return@preparePromise

					mutableFileName.value = fileProperties.name?.value ?: ""
					mutableArtist.value = fileProperties.artist?.value ?: ""
					mutableAlbum.value = fileProperties.album?.value ?: ""
					mutableRating.value = fileProperties.rating?.value?.toIntOrNull() ?: 0

					if (cs.isCancelled) return@preparePromise

					mutableFileProperties.value = fileProperties.allProperties
						.filterNot { e -> propertiesToSkip.contains(e.name) }
						.sortedBy { it.name }
						.map(::FilePropertyViewModel)
						.toList()
				}
			}
			.keepPromise(Unit)

	inner class FilePropertyViewModel(val fileProperty: FileProperty) {

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
		val propertyName = fileProperty.name

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
					activeServiceFile
						?.let { serviceFile ->
							updateFileProperties
								.promiseFileUpdate(l, serviceFile, propertyName, newValue, true)
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
