package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.GivenATypicalLibrary.WithTheStoredFile.AndItsInAnExternalLocation

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FakeFilesPropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.MediaItemCreator
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

private const val libraryId = 512
private const val serviceFileId = 338

@RunWith(AndroidJUnit4::class)
class WhenGettingTheMediaFileId {
	companion object {

		private val mediaId by lazy {
			val filePropertiesProvider = FakeFilesPropertiesProvider()
			filePropertiesProvider.addFilePropertiesToCache(
				ServiceFile(serviceFileId),
				LibraryId(libraryId),
				mapOf(
					Pair(KnownFileProperties.AlbumArtist, "sharp"),
					Pair(KnownFileProperties.Album, "low"),
					Pair(KnownFileProperties.Track, "72"),
					Pair(KnownFileProperties.Filename, """/mixed\path\separators/are\awesome/for_music.mp3""")
				)
			)

			val mediaItemCreator = MediaItemCreator(
				filePropertiesProvider,
				InstrumentationRegistry.getInstrumentation().targetContext.contentResolver,
			)

			mediaItemCreator
				.promiseCreatedItem(LibraryId(libraryId), ServiceFile(serviceFileId))
				.toExpiringFuture()
				.get()
		}
	}

//	private val rule by lazy { ProviderTestRule }

	@Test
	fun `then the media id is correct`() {
		assertThat(mediaId).isEqualTo(1)
	}
}
