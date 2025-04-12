package com.lasthopesoftware.bluewater.client.browsing.remote.GivenADifferentServiceFile

import android.support.v4.media.MediaBrowserCompat
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FakeFilesPropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.remote.MediaItemServiceFileLookup
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class `When Looking Up The Media Item` {
	companion object {
		private const val libraryId = 240

		private val mediaItem by lazy {
			val fileProperties = FakeFilesPropertiesProvider()
			fileProperties.addFilePropertiesToCache(
				ServiceFile("703"),
				LibraryId(libraryId),
				mapOf(
					Pair(KnownFileProperties.Key, "703"),
					Pair(KnownFileProperties.Artist, "division"),
					Pair(KnownFileProperties.Album, "slide"),
					Pair(KnownFileProperties.Name, "habit"),
					Pair(KnownFileProperties.Duration, "451")
				)
			)

			val mediaItemServiceFileLookup = MediaItemServiceFileLookup(
				fileProperties,
				mockk {
					every { promiseImageBytes(LibraryId(libraryId), ServiceFile("703")) } returns byteArrayOf(3, 4).toPromise()
				},
                com.lasthopesoftware.resources.bitmaps.ImmediateBitmapProducer,
			)
			mediaItemServiceFileLookup.promiseMediaItem(LibraryId(libraryId), ServiceFile("703"))
				.toExpiringFuture()
				.get()
		}
	}

	@Test
	fun `then the title is correct`() {
		assertThat(mediaItem?.description?.title)
			.isEqualTo("habit")
	}

	@Test
	fun `then the artist is correct`() {
		assertThat(mediaItem?.description?.subtitle)
			.isEqualTo("division")
	}

	@Test
	fun `then the media ID is correct`() {
		assertThat(mediaItem?.mediaId).isEqualTo("sf:703")
	}

	@Test
	fun `then the album is correct`() {
		assertThat(mediaItem?.description?.description)
			.isEqualTo("slide")
	}

	@Test
	fun `then the image is not returned`() {
		assertThat(mediaItem?.description?.iconBitmap).isNull()
	}

	@Test
	fun `then the item is playable`() {
		assertThat(mediaItem?.flags).isEqualTo(MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
	}
}
