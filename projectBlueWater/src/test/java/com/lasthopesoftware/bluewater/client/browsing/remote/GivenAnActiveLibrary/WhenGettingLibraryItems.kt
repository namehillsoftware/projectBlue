package com.lasthopesoftware.bluewater.client.browsing.remote.GivenAnActiveLibrary

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.remote.MediaItemsBrowser
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class `When Getting Library Items` {
	companion object {
		private val viewIds by lazy { listOf(911, 126, 231, 790, 574, 905, 471, 535) }

		private val expectedMediaItems by lazy {
			viewIds.map { i ->
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
			every { selectedLibraryId.promiseSelectedLibraryId() } returns Promise(LibraryId(489))

			val mediaItemsBrowser = MediaItemsBrowser(
                selectedLibraryId,
                mockk {
					every { promiseItems(LibraryId(489)) } returns Promise(viewIds.map(::Item))
				},
                mockk(),
				mockk(),
                mockk(),
			)
			mediaItemsBrowser
				.promiseLibraryItems()
				.toExpiringFuture()
				.get()
		}
	}

	@Test
	fun `then the media items are correct`() {
		assertThat(mediaItems?.map { i -> i.mediaId }).isEqualTo(expectedMediaItems.map { i -> i.mediaId })
	}
}
