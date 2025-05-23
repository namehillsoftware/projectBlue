package com.lasthopesoftware.bluewater.client.browsing.remote.GivenAnItem.AndItHasChildItems

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.Playlist
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
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
		private val expectedMediaItems by lazy {
			listOf("605", "842", "264", "224", "33", "412", "488", "394").map { i ->
				val metadata = MediaMetadataCompat.Builder()
					.apply {
						putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "it:$i")
					}
					.build()

				MediaBrowserCompat.MediaItem(
					metadata.description,
					MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
				)
			} + listOf("7c5cd4cae5ea4253bd9eaec3ad148ea8", "k97auu6").map {
				val metadata = MediaMetadataCompat.Builder()
					.apply {
						putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "pl:$it")
					}
					.build()

				MediaBrowserCompat.MediaItem(
					metadata.description,
					MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
				)
			}
		}

		private val mediaItems by lazy {
			val selectedLibraryId = mockk<ProvideSelectedLibraryId>()
			every { selectedLibraryId.promiseSelectedLibraryId() } returns Promise(LibraryId(22))

			val itemsProvider = mockk<ProvideItems>()
			every { itemsProvider.promiseItems(LibraryId(22), ItemId("504")) } returns Promise(
				listOf("605", "842", "264", "224", "33", "412", "488", "394").map(::Item) + listOf("7c5cd4cae5ea4253bd9eaec3ad148ea8", "k97auu6").map(::Playlist)
			)

			val mediaItemsBrowser = MediaItemsBrowser(
                selectedLibraryId,
                itemsProvider,
                mockk(),
                mockk(),
			)
			mediaItemsBrowser
				.promiseItems(ItemId("504"))
				.toExpiringFuture()
				.get()
		}
	}

	@Test
	fun `then the media items have the correct ids`() {
		assertThat(mediaItems?.map { i -> i.mediaId }).isEqualTo(expectedMediaItems.map { i -> i.mediaId })
	}

	@Test
	fun `then the media items are browsable`() {
		assertThat(mediaItems!!).allMatch { i -> i.isBrowsable }
	}

	@Test
	fun `then the media items are not playable`() {
		assertThat(mediaItems!!).allMatch { i -> !i.isPlayable }
	}
}
