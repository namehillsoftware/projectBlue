package com.lasthopesoftware.bluewater.client.browsing.files.properties.GivenALibraryId.AndFileProperties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.CaseInsensitiveFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When getting case insensitive file properties` {

	companion object {
		private const val libraryId = 702
		private const val serviceFile = "hO0nMeva"
	}

	private val mut by lazy {
        CaseInsensitiveFilePropertiesProvider(
            mockk {
                every {
                    promiseFileProperties(
                        LibraryId(libraryId),
                        ServiceFile(serviceFile)
                    )
                } returns mapOf(
                    Pair("Duistristique", "eh7PefSv"),
                    Pair("Sempersuspendisse", "DZvmWnKI"),
                    Pair("Aaliquet", "DswIgCM"),
                    Pair("Consecteturnisi", "1KSH9v13"),
                    Pair("suscipitligula", "1AzksByxk"),
                ).toPromise()
            }
        )
	}

	private var fileProperties: Map<String, String>? = null

	@BeforeAll
	fun act() {
		fileProperties = mut.promiseFileProperties(
            LibraryId(libraryId),
            ServiceFile(serviceFile)
        ).toExpiringFuture().get()
	}

	@Test
	fun `then the file properties can be retrieved`() {
		val testMap = mapOf(
			Pair("AALIQUET", "DswIgCM"),
			Pair("consecteturnisi", "1KSH9v13"),
			Pair("DUISTRISTIQUE", "eh7PefSv"),
			Pair("semperSUSPENDISSE", "DZvmWnKI"),
			Pair("SUSCIPITLIGULA", "1AzksByxk"),
		)
		assertThat(fileProperties).containsAllEntriesOf(testMap).hasSameSizeAs(testMap)
	}
}
