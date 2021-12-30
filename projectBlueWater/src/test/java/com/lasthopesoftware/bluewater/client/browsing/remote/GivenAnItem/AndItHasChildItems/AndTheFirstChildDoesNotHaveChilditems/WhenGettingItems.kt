package com.lasthopesoftware.bluewater.client.browsing.remote.GivenAnItem.AndItHasChildItems.AndTheFirstChildDoesNotHaveChilditems

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
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
		private val expectedMediaItems by lazy {
			listOf(720, 322, 409, 890).map { i ->
				val metadata = MediaMetadataCompat.Builder()
					.apply {
						putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "it:$i")
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
			every { selectedLibraryId.selectedLibraryId } returns Promise(LibraryId(1))

			val itemsProvider = mockk<ProvideItems>()
			every { itemsProvider.promiseItems(LibraryId(1), 952) } returns Promise(
				listOf(720, 322, 409, 890).map(::Item)
			)

			every { itemsProvider.promiseItems(LibraryId(1), 720) } returns Promise(emptyList())

			val mediaItemsBrowser = MediaItemsBrowser(
				mockk(),
				selectedLibraryId,
				itemsProvider,
				mockk(),
				mockk(),
				mockk(),
			)
			mediaItemsBrowser
				.promiseItems(Item(952))
				.toFuture()
				.get()
		}
	}

	@Test
	fun `then the media items are correct`() {
		assertThat(mediaItems?.map { i -> i.mediaId }).isEqualTo(expectedMediaItems.map { i -> i.mediaId })
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
