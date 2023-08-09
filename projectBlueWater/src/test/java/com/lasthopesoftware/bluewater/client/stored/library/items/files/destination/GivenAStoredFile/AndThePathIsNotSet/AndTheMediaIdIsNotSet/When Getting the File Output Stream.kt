package com.lasthopesoftware.bluewater.client.stored.library.items.files.destination.GivenAStoredFile.AndThePathIsNotSet.AndTheMediaIdIsNotSet

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileUriDestinationBuilder
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.OutputStream

class `When Getting the File Output Stream` {

	private val mut by lazy {
		StoredFileUriDestinationBuilder(mockk())
	}

	private var outputStream: OutputStream? = null

	@BeforeAll
	fun act() {
		val storedFile = StoredFile(LibraryId(1), 0, ServiceFile(1), null, true)

		outputStream = mut.getOutputStream(storedFile)
	}

	@Test
	fun `then the output stream is correct`() {
		assertThat(outputStream).isNull()
	}
}
