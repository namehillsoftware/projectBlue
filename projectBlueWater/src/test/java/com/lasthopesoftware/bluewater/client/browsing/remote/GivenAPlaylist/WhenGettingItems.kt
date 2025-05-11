package com.lasthopesoftware.bluewater.client.browsing.remote.GivenAPlaylist

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.remote.GetMediaItemsFromServiceFiles
import com.lasthopesoftware.bluewater.client.browsing.remote.MediaItemsBrowser
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
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
		private const val libraryId = 720
		private const val playlistId = "IDBgoCx"

		private val serviceFileIds by lazy { listOf("135", "399", "304", "731") }

		private val expectedMediaItems by lazy {
			serviceFileIds.indices.map { i ->
				MediaMetadataCompat.Builder()
					.apply {
						putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "pl:$playlistId:$i")
						putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Ultricesmaximus")
						putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "Pellentesquedictum")
						putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Nunceu")
						putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 709)
						putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, 908)
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
			every { selectedLibraryId.promiseSelectedLibraryId() } returns Promise(LibraryId(libraryId))

			val itemsProvider = mockk<ProvideItems>()
			every { itemsProvider.promiseItems(LibraryId(libraryId), ItemId(playlistId)) } returns Promise(emptyList())

			val serviceFiles = mockk<GetMediaItemsFromServiceFiles>()
			for (id in serviceFileIds) {
				every { serviceFiles.promiseMediaItem(LibraryId(libraryId), ServiceFile(id)) } returns Promise(
					MediaMetadataCompat.Builder()
						.apply {
							putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "sf:$id")
							putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Ultricesmaximus")
							putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "Pellentesquedictum")
							putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Nunceu")
							putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 709)
							putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, 908)
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

			val mediaItemsBrowser = MediaItemsBrowser(
                selectedLibraryId,
                itemsProvider,
                mockk {
					every { promiseFiles(LibraryId(libraryId), PlaylistId(playlistId)) } returns Promise(
						serviceFileIds.map(::ServiceFile)
					)
				},
                serviceFiles,
			)

			mediaItemsBrowser
				.promiseItems(PlaylistId(playlistId))
				.toExpiringFuture()
				.get()
		}
	}

	@Test
	fun `then the media item ids are correct`() {
		assertThat(mediaItems?.map { i -> i.mediaId }).isEqualTo(expectedMediaItems.map { i -> i.mediaId })
	}

	@Test
	fun `then the media item titles are correct`() {
		assertThat(mediaItems?.map { i -> i.description.title }).isEqualTo(expectedMediaItems.map { i -> i.description.title })
	}

	@Test
	fun `then the media item subtitles are correct`() {
		assertThat(mediaItems?.map { i -> i.description.subtitle }).isEqualTo(expectedMediaItems.map { i -> i.description.subtitle })
	}


	@Test
	fun `then the media item extras are correct`() {
		assertThat(mediaItems?.map { i -> i.description.extras }).isEqualTo(expectedMediaItems.map { i -> i.description.extras })
	}

	@Test
	fun `then the media item descriptions are correct`() {
		assertThat(mediaItems?.map { i -> i.description.description }).isEqualTo(expectedMediaItems.map { i -> i.description.description })
	}

	@Test
	fun `then the media items are not browsable`() {
		assertThat(mediaItems!!).allMatch { i -> !i.isBrowsable }
	}

	@Test
	fun `then the media items are playable`() {
		assertThat(mediaItems!!).allMatch { i -> i.isPlayable }
	}
}
