package com.lasthopesoftware.bluewater.client.browsing.remote.GivenAServiceFile

import android.support.v4.media.MediaBrowserCompat
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FakeFilesPropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.remote.MediaItemServiceFileLookup
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.bitmaps.ImmediateBitmapProducer
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class `When Looking Up The Media Item` : AndroidContext() {

	private val libraryId = LibraryId(743)

	private val mediaItemServiceFileLookup by lazy {
		val fileProperties = FakeFilesPropertiesProvider()
		fileProperties.addFilePropertiesToCache(
			ServiceFile(14),
			libraryId,
			mapOf(
				Pair(KnownFileProperties.Key, "14"),
				Pair(KnownFileProperties.Artist, "Billy Bob"),
				Pair(KnownFileProperties.Album, "Bob's BIIIG Adventure"),
				Pair(KnownFileProperties.Name, "Billy Bob Jr. Jr."),
				Pair(KnownFileProperties.Duration, "30")
			)
		)

		MediaItemServiceFileLookup(
			fileProperties,
			mockk {
				every { promiseImageBytes(libraryId, ServiceFile(14)) } returns byteArrayOf(1, 2).toPromise()
			},
			ImmediateBitmapProducer,
		)
	}

	companion object {
		private var mediaItem: MediaBrowserCompat.MediaItem? = null
	}

	override fun before() {
		mediaItem = mediaItemServiceFileLookup.promiseMediaItem(libraryId, ServiceFile(14))
			.toExpiringFuture()
			.get()
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

//	@Test
//	fun `then the image is not returned`() {
//		assertThat(mediaItem?.description?.iconBitmap).isNull()
//	}

	@Test
	fun `then the item is playable`() {
		assertThat(mediaItem?.flags).isEqualTo(MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
	}
}
