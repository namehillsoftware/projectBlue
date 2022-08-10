package com.lasthopesoftware.bluewater.client.browsing.files.details

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.image.ProvideImages
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FormattedScopedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ScopedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.ScopedRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.selected.ProvideSelectedConnection
import com.lasthopesoftware.bluewater.shared.images.ProvideDefaultImage
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FileDetailsViewModel
(
	private val selectedConnectionProvider: ProvideSelectedConnection,
	defaultImageProvider: ProvideDefaultImage,
	private val imageProvider: ProvideImages,
)
	: ViewModel() {

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

	private val mutableFileName = MutableStateFlow("")
	private val mutableArtist = MutableStateFlow("")
	private val mutableFileProperties = MutableStateFlow(emptyList<Map.Entry<String, String>>())
	private val mutableIsLoading = MutableStateFlow(false)
	private val mutableCoverArt = MutableStateFlow<Bitmap?>(null)
	private val promisedSetDefaultCoverArt = defaultImageProvider.promiseFileBitmap()
		.then {
			mutableCoverArt.value = it
			it
		}
	private val mutableRating = MutableStateFlow(0f)

	val fileName = mutableFileName.asStateFlow()
	val artist = mutableArtist.asStateFlow()
	val fileProperties = mutableFileProperties.asStateFlow()
	val isLoading = mutableIsLoading.asStateFlow()
	val coverArt = mutableCoverArt.asStateFlow()
	val rating = mutableRating.asStateFlow()

	fun loadFile(serviceFile: ServiceFile): Promise<FileDetailsViewModel> {
		mutableIsLoading.value = true
		val filePropertiesSetPromise = selectedConnectionProvider
			.promiseSessionConnection()
			.eventually { connectionProvider ->
				connectionProvider
					?.let { c -> ScopedFilePropertiesProvider(c,  ScopedRevisionProvider(c), FilePropertyCache.getInstance()) }
					?.let(::FormattedScopedFilePropertiesProvider)
					?.promiseFileProperties(serviceFile)
					?.then { fileProperties ->
						fileProperties[KnownFileProperties.NAME]?.also { mutableFileName.value = it }
						fileProperties[KnownFileProperties.ARTIST]?.also { mutableArtist.value = it }
						fileProperties[KnownFileProperties.RATING]?.toFloatOrNull()?.also { mutableRating.value = it }

						mutableFileProperties.value = fileProperties.entries
							.filterNot { e -> propertiesToSkip.contains(e.key) }
							.sortedBy { e -> e.key }
					}
					.keepPromise()
			}

		val bitmapSetPromise = promisedSetDefaultCoverArt // Ensure default cover art is first set before apply cover art from file properties
			.eventually { default ->
				imageProvider
					.promiseFileBitmap(serviceFile)
					.then { bitmap -> mutableCoverArt.value = bitmap ?: default }
			}

		return Promise.whenAll(filePropertiesSetPromise, bitmapSetPromise)
			.then {
				mutableIsLoading.value = false
				this
			}
	}

	fun addToNowPlaying() {
		TODO("Not yet implemented")
	}

	fun viewFileDetails() {
		TODO("Not yet implemented")
	}

	fun play() {
		TODO("Not yet implemented")
	}
}
