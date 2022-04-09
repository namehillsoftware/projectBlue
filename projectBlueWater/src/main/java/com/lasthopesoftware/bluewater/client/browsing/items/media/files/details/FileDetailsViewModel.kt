package com.lasthopesoftware.bluewater.client.browsing.items.media.files.details

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FormattedScopedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.ProvideImages
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.ScopedRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.selected.ProvideSelectedConnection
import com.lasthopesoftware.bluewater.shared.images.ProvideDefaultImage
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FileDetailsViewModel
(
	private val selectedConnectionProvider: ProvideSelectedConnection,
	private val defaultImageProvider: ProvideDefaultImage,
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
			KnownFileProperties.LengthInPcmBlocks)
	}

	private val mutableFileName = MutableStateFlow("")
	private val mutableArtist = MutableStateFlow("")
	private val mutableFileProperties = MutableStateFlow(emptyList<Map.Entry<String, String>>())
	private val mutableIsLoadingFileDetails = MutableStateFlow(false)
	private val mutableCoverArt = MutableStateFlow<Bitmap?>(null)

	val fileName = mutableFileName.asStateFlow()
	val artist = mutableArtist.asStateFlow()
	val fileProperties = mutableFileProperties.asStateFlow()
	val isLoadingFileDetails = mutableIsLoadingFileDetails.asStateFlow()
	val coverArt = mutableCoverArt.asStateFlow()

	fun loadFile(serviceFile: ServiceFile): Promise<FileDetailsViewModel> {
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

						mutableFileProperties.value = fileProperties.entries
							.filterNot { e -> propertiesToSkip.contains(e.key) }
							.sortedBy { e -> e.key }

						mutableIsLoadingFileDetails.value = false
					}
					.keepPromise()
			}

		val bitmapSetPromise = imageProvider
			.promiseFileBitmap(serviceFile)
			.eventually { bitmap ->
				bitmap?.toPromise() ?: defaultImageProvider.promiseFileBitmap()
			}
			.then { mutableCoverArt.value = it }

		return Promise.whenAll(filePropertiesSetPromise, bitmapSetPromise).then { this }
	}
}
