package com.lasthopesoftware.bluewater.client.browsing.remote.GivenAServiceFile

import android.support.v4.media.MediaBrowserCompat
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FakeFilesPropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.remote.MediaItemServiceFileLookup
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.bitmaps.ImmediateBitmapProducer
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class `When Looking Up The Media Item With The Image` {
	companion object {
		private const val libraryId = 794

		private val mediaItem by lazy {
			val fileProperties = FakeFilesPropertiesProvider()
			fileProperties.addFilePropertiesToCache(
				ServiceFile("14"),
				LibraryId(libraryId),
				mapOf(
					Pair(NormalizedFileProperties.Key, "14"),
					Pair(NormalizedFileProperties.Artist, "Billy Bob"),
					Pair(NormalizedFileProperties.Album, "Bob's BIIIG Adventure"),
					Pair(NormalizedFileProperties.Name, "Billy Bob Jr. Jr."),
					Pair(NormalizedFileProperties.Duration, "30")
				)
			)

			val mediaItemServiceFileLookup = MediaItemServiceFileLookup(
				fileProperties,
				mockk {
					every { promiseImageBytes(LibraryId(libraryId), ServiceFile("14")) } returns byteArrayOf(1, 2).toPromise()
				},
				ImmediateBitmapProducer,
			)
			mediaItemServiceFileLookup.promiseMediaItemWithImage(LibraryId(libraryId), ServiceFile("14"))
				.toExpiringFuture()
				.get()
		}
	}

	@Test
	fun `then the title is correct`() {
		assertThat(mediaItem?.description?.title)
			.isEqualTo("Billy Bob Jr. Jr.")
	}

	@Test
	fun `then the artist is correct`() {
		assertThat(mediaItem?.description?.subtitle)
			.isEqualTo("Billy Bob")
	}

	@Test
	fun `then the media ID is correct`() {
		assertThat(mediaItem?.mediaId).isEqualTo("sf:14")
	}

	@Test
	fun `then the album is correct`() {
		assertThat(mediaItem?.description?.description)
			.isEqualTo("Bob's BIIIG Adventure")
	}

	@Test
	fun `then the image is returned`() {
		assertThat(mediaItem?.description?.iconBitmap).isNotNull
	}

	@Test
	fun `then the item is playable`() {
		assertThat(mediaItem?.flags).isEqualTo(MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
	}
}
