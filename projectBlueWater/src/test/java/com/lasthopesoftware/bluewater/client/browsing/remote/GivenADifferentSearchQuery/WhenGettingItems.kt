package com.lasthopesoftware.bluewater.client.browsing.remote.GivenADifferentSearchQuery

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.ProvideFiles
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
		private val serviceFileIds by lazy { listOf(702, 586, 516, 78) }

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
			every { selectedLibraryId.selectedLibraryId } returns Promise(LibraryId(873))

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

			val provideFiles = mockk<ProvideFiles>()
			val parameters = SearchFileParameterProvider.getFileListParameters("king")
			every { provideFiles.promiseFiles(FileListParameters.Options.None, *parameters) } returns Promise(
				serviceFileIds.map(::ServiceFile)
			)

			val mediaItemsBrowser = MediaItemsBrowser(
				mockk(),
				selectedLibraryId,
				mockk(),
				provideFiles,
				mockk(),
				serviceFiles,
			)

			mediaItemsBrowser
				.promiseItems("king")
				.toFuture()
				.get()
		}
	}

	@Test
	fun `then the media items are correct`() {
		assertThat(mediaItems?.map { i -> i.mediaId }).isEqualTo(expectedMediaItems.map { i -> i.mediaId })
	}
}