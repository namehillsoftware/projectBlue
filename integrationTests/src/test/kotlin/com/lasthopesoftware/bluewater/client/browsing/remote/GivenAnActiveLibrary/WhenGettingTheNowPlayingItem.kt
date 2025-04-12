package com.lasthopesoftware.bluewater.client.browsing.remote.GivenAnActiveLibrary

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.remote.GetMediaItemsFromServiceFiles
import com.lasthopesoftware.bluewater.client.browsing.remote.NowPlayingMediaItemLookup
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.ManageNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val libraryId = 861

@RunWith(RobolectricTestRunner::class)
class `When Getting The Now Playing Item` {
	companion object {
		private val expectedMediaItem by lazy {
			val metadata = MediaMetadataCompat.Builder()
				.apply {
					putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "sf:393")
				}
				.build()

			MediaBrowserCompat.MediaItem(
				metadata.description,
				MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
			)
		}

		private val mediaItem by lazy {
			val nowPlaying = mockk<ManageNowPlayingState>()
			every { nowPlaying.promiseNowPlaying(LibraryId(libraryId)) } returns Promise(
				NowPlaying(
					LibraryId(libraryId),
					listOf(ServiceFile("89"), ServiceFile("393"), ServiceFile("714"), ServiceFile("760")),
					1,
					561L,
					true
				)
			)

			val serviceFiles = mockk<GetMediaItemsFromServiceFiles>()
			every { serviceFiles.promiseMediaItemWithImage(LibraryId(libraryId), ServiceFile("393")) } returns expectedMediaItem.toPromise()

			val mediaItemsBrowser = NowPlayingMediaItemLookup(
				mockk {
					every { promiseSelectedLibraryId() } returns LibraryId(libraryId).toPromise()
				},
				nowPlaying,
				serviceFiles,
			)
			mediaItemsBrowser
				.promiseNowPlayingItem()
				.toExpiringFuture()
				.get()
		}
	}

	@Test
	fun `then the media item is correct`() {
		assertThat(mediaItem).isEqualTo(expectedMediaItem)
	}

	@Test
	fun `then the media ID is correct`() {
		assertThat(mediaItem?.mediaId).isEqualTo("sf:393")
	}
}
