package com.lasthopesoftware.bluewater.client.browsing.files.properties.GivenALibraryId.AndNoStoredFileProperties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeFileConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeLibraryConnectionProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenGettingFileProperties {
	private val fileProperties by lazy {
        val fakeFileConnectionProvider = FakeFileConnectionProvider()
        fakeFileConnectionProvider.setupFile(
			ServiceFile(15),
			mapOf(Pair(KnownFileProperties.Key, "45")))
        val fakeLibraryConnectionProvider =
            FakeLibraryConnectionProvider(mapOf(Pair(LibraryId(14), fakeFileConnectionProvider)))
        val filePropertiesProvider = FilePropertiesProvider(
            fakeLibraryConnectionProvider,
            LibraryRevisionProvider(fakeLibraryConnectionProvider),
            mockk(relaxed = true)
        )
        filePropertiesProvider
			.promiseFileProperties(LibraryId(14), ServiceFile(15))
			.toExpiringFuture()
			.get()
    }

    @Test
    fun thenFilesAreRetrieved() {
        assertThat(fileProperties!![KnownFileProperties.Key]).isEqualTo("45")
    }
}
