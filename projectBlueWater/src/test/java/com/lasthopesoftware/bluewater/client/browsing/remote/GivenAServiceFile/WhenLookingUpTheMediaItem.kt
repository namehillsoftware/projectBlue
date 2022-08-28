package com.lasthopesoftware.bluewater.client.browsing.remote.GivenAServiceFile

import android.graphics.BitmapFactory
import android.support.v4.media.MediaBrowserCompat
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FakeScopedCachedFilesPropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.image.ProvideImages
import com.lasthopesoftware.bluewater.client.browsing.remote.MediaItemServiceFileLookup
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class `When Looking Up The Media Item` {
	companion object {
		private val mediaItem by lazy {
			val fileProperties = FakeScopedCachedFilesPropertiesProvider()
			fileProperties.addFilePropertiesToCache(
				ServiceFile(14),
				mapOf(
					Pair(KnownFileProperties.KEY, "14"),
					Pair(KnownFileProperties.ARTIST, "Billy Bob"),
					Pair(KnownFileProperties.ALBUM, "Bob's BIIIG Adventure"),
					Pair(KnownFileProperties.NAME, "Billy Bob Jr. Jr."),
					Pair(KnownFileProperties.DURATION, "30")
				)
			)

			val imageProvider = mockk<ProvideImages>()
			every { imageProvider.promiseFileBitmap(ServiceFile(14)) } returns Promise(BitmapFactory.decodeByteArray(
				byteArrayOf(1, 2), 0, 2))

			val mediaItemServiceFileLookup = MediaItemServiceFileLookup(
				fileProperties,
				imageProvider
			)
			mediaItemServiceFileLookup.promiseMediaItem(ServiceFile(14))
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
	fun `then the image is not returned`() {
		assertThat(mediaItem?.description?.iconBitmap).isNull()
	}

	@Test
	fun `then the item is playable`() {
		assertThat(mediaItem?.flags).isEqualTo(MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
	}
}
