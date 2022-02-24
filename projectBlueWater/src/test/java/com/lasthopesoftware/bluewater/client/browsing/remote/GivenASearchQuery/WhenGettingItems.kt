package com.lasthopesoftware.bluewater.client.browsing.remote.GivenASearchQuery

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.ProvideLibraryFiles
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.SearchFileParameterProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.remote.GetMediaItemsFromServiceFiles
import com.lasthopesoftware.bluewater.client.browsing.remote.MediaItemsBrowser
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class `When Getting Items` {
	companion object {
		private val serviceFileIds by lazy { listOf(482, 763, 291, 386, 993, 541, 482, 351, 390) }

		private val expectedMediaItems by lazy {
			serviceFileIds.map { i ->
				MediaMetadataCompat.Builder()
					.apply {
						putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "sf:$i")
					}
					.build()
					.let { metadata ->
						MediaBrowserCompat.MediaItem(
							metadata.description,
							MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
						)
					}
			}
		}

		private val mediaItems by lazy {
			val selectedLibraryId = mockk<ProvideSelectedLibraryId>()
			every { selectedLibraryId.selectedLibraryId } returns Promise(LibraryId(22))

			val itemsProvider = mockk<ProvideItems>()
			every { itemsProvider.promiseItems(LibraryId(22), ItemId(743)) } returns Promise(emptyList())

			val serviceFiles = mockk<GetMediaItemsFromServiceFiles>()
			for (id in serviceFileIds) {
				every { serviceFiles.promiseMediaItem(ServiceFile(id)) } returns Promise(
					MediaMetadataCompat.Builder()
						.apply {
							putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "sf:$id")
						}
						.build()
						.let { metadata ->
							MediaBrowserCompat.MediaItem(
								metadata.description,
								MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
							)
						}
				)
			}

			val provideFiles = mockk<ProvideLibraryFiles>()
			val parameters = SearchFileParameterProvider.getFileListParameters("water")
			every { provideFiles.promiseFiles(LibraryId(22), FileListParameters.Options.None, *parameters) } returns Promise(
				serviceFileIds.map(::ServiceFile)
			)

			val mediaItemsBrowser = MediaItemsBrowser(
                selectedLibraryId,
                itemsProvider,
                provideFiles,
				mockk(),
                mockk(),
                serviceFiles,
			)

			mediaItemsBrowser
				.promiseItems("water")
				.toFuture()
				.get()
		}
	}

	@Test
	fun `then the media items are correct`() {
		assertThat(mediaItems?.map { i -> i.mediaId }).isEqualTo(expectedMediaItems.map { i -> i.mediaId })
	}
}
