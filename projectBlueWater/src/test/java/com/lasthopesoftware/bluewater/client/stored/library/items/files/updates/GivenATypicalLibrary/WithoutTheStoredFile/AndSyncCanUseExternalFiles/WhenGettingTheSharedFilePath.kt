package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.GivenATypicalLibrary.WithoutTheStoredFile.AndSyncCanUseExternalFiles

import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FakeFilesPropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.SharedStoredFilePathLookup
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WhenGettingTheSharedFilePath {
	companion object {
		private val filePath by lazy {
			val filePropertiesProvider = FakeFilesPropertiesProvider()
			filePropertiesProvider.addFilePropertiesToCache(
				ServiceFile(4),
				LibraryId(862),
				mapOf(
					Pair(KnownFileProperties.ARTIST, "artist"),
					Pair(KnownFileProperties.ALBUM, "album"),
					Pair(KnownFileProperties.FILENAME, "my-filename.mp3")
				)
			)

			val sharedStoredFilePathLookup = SharedStoredFilePathLookup(
				filePropertiesProvider,
				ApplicationProvider.getApplicationContext())

			sharedStoredFilePathLookup
				.promiseSharedStoredFilePath(LibraryId(862), ServiceFile(4))
				.toFuture()
				.get()
		}
	}

	@Test
	fun thenTheFileHasAPath() {
		assertThat(filePath).isNotNull
	}
}
