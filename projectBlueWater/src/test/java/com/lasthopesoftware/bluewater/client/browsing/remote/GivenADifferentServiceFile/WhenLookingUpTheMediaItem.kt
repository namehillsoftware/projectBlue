package com.lasthopesoftware.bluewater.client.browsing.remote.GivenADifferentServiceFile

import android.graphics.BitmapFactory
import android.support.v4.media.MediaBrowserCompat
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.image.ProvideScopedImages
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FakeScopedCachedFilesPropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
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
				ServiceFile(703),
				mapOf(
					Pair(KnownFileProperties.Key, "703"),
					Pair(KnownFileProperties.Artist, "division"),
					Pair(KnownFileProperties.Album, "slide"),
					Pair(KnownFileProperties.Name, "habit"),
					Pair(KnownFileProperties.Duration, "451")
				)
			)

			val imageProvider = mockk<ProvideScopedImages>()
			every { imageProvider.promiseFileBitmap(ServiceFile(703)) } returns Promise(
				BitmapFactory.decodeByteArray(
				byteArrayOf(3, 4), 0, 2))

			val mediaItemServiceFileLookup = MediaItemServiceFileLookup(
				fileProperties,
				imageProvider
			)
			mediaItemServiceFileLookup.promiseMediaItem(ServiceFile(703))
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
