package com.lasthopesoftware.bluewater.client.browsing.files.properties.GivenNoStoredFileProperties

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
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenGettingFileProperties {
	private var fileProperties: Map<String, String>? = null

    @BeforeAll
    fun before() {
        val fakeFileConnectionProvider = FakeFileConnectionProvider()
        fakeFileConnectionProvider.setupFile(
			ServiceFile(15),
			mapOf(Pair(KnownFileProperties.KEY, "45")))
        val fakeLibraryConnectionProvider =
            FakeLibraryConnectionProvider(mapOf(Pair(LibraryId(14), fakeFileConnectionProvider)))
        val filePropertiesProvider = FilePropertiesProvider(
            fakeLibraryConnectionProvider,
            LibraryRevisionProvider(fakeLibraryConnectionProvider),
            mockk(relaxed = true)
        )
        fileProperties = filePropertiesProvider
				.promiseFileProperties(LibraryId(14), ServiceFile(15))
				.toExpiringFuture().get()
    }

    @Test
    fun thenFilesAreRetrieved() {
        assertThat(fileProperties!![KnownFileProperties.KEY]).isEqualTo("45")
    }
}