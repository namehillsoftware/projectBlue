package com.lasthopesoftware.bluewater.client.browsing.files.details

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.image.ProvideImages
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideScopedFileProperties
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.images.ProvideDefaultImage
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


class FileDetailsViewModel(
	private val scopedFilePropertiesProvider: ProvideScopedFileProperties,
	defaultImageProvider: ProvideDefaultImage,
	private val imageProvider: ProvideImages,
	private val controlPlayback: ControlPlaybackService,
) : ViewModel() {

	companion object {
		private val propertiesToSkip = setOf(
			KnownFileProperties.AUDIO_ANALYSIS_INFO,
			KnownFileProperties.GET_COVER_ART_INFO,
			KnownFileProperties.IMAGE_FILE,
			KnownFileProperties.KEY,
			KnownFileProperties.STACK_FILES,
			KnownFileProperties.STACK_TOP,
			KnownFileProperties.STACK_VIEW,
			KnownFileProperties.WAVEFORM,
			KnownFileProperties.LengthInPcmBlocks
		)
	}

	private var associatedPlaylist = emptyList<ServiceFile>()
	private var activePositionedFile: PositionedFile? = null
	private val mutableFileName = MutableStateFlow("")

	private val mutableAlbum = MutableStateFlow("")
	private val mutableArtist = MutableStateFlow("")
	private val mutableFileProperties = MutableStateFlow(emptyList<Map.Entry<String, String>>())
	private val mutableIsLoading = MutableStateFlow(false)
	private val mutableCoverArt = MutableStateFlow<Bitmap?>(null)
	private val promisedSetDefaultCoverArt = defaultImageProvider.promiseFileBitmap()
		.then {
			mutableCoverArt.value = it
			it
		}
	private val mutableRating = MutableStateFlow(0)
	val fileName = mutableFileName.asStateFlow()

	val artist = mutableArtist.asStateFlow()
	val album = mutableAlbum.asStateFlow()
	val fileProperties = mutableFileProperties.asStateFlow()
	val isLoading = mutableIsLoading.asStateFlow()
	val coverArt = mutableCoverArt.asStateFlow()
	val rating = mutableRating.asStateFlow()

	fun loadFromList(playlist: List<ServiceFile>, position: Int): Promise<Unit> {
		val serviceFile = playlist[position]
		activePositionedFile = PositionedFile(position, serviceFile)
		associatedPlaylist = playlist

		mutableIsLoading.value = true
		val filePropertiesSetPromise = scopedFilePropertiesProvider
			.promiseFileProperties(serviceFile)
			.then { fileProperties ->
				fileProperties[KnownFileProperties.NAME]?.also { mutableFileName.value = it }
				fileProperties[KnownFileProperties.ARTIST]?.also { mutableArtist.value = it }
				fileProperties[KnownFileProperties.ALBUM]?.also { mutableAlbum.value = it }
				fileProperties[KnownFileProperties.RATING]?.toIntOrNull()?.also { mutableRating.value = it }

				mutableFileProperties.value = fileProperties.entries
					.filterNot { e -> propertiesToSkip.contains(e.key) }
					.sortedBy { e -> e.key }
			}
			.keepPromise()

		val bitmapSetPromise = promisedSetDefaultCoverArt // Ensure default cover art is first set before apply cover art from file properties
			.eventually { default ->
				imageProvider
					.promiseFileBitmap(serviceFile)
					.then { bitmap -> mutableCoverArt.value = bitmap ?: default }
			}

		return Promise
			.whenAll(filePropertiesSetPromise, bitmapSetPromise)
			.then { mutableIsLoading.value = false }
	}

	fun addToNowPlaying() {
		activePositionedFile?.serviceFile?.let(controlPlayback::addToPlaylist)
	}

	fun play() {
		val positionedFile = activePositionedFile ?: return
		controlPlayback.startPlaylist(associatedPlaylist, positionedFile.playlistPosition)
	}
}
