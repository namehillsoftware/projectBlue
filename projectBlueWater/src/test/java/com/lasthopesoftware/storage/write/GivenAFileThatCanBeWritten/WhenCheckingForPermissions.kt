package com.lasthopesoftware.storage.write.GivenAFileThatCanBeWritten

import com.lasthopesoftware.storage.write.permissions.FileWritePossibleArbitrator
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

class WhenCheckingForPermissions {
	private val fileWriteIsPossible by lazy {
		val fileWritePossibleArbitrator = FileWritePossibleArbitrator
		val file = mockk<File> {
			every { exists() } returns true
			every { canWrite() } returns true
		}
		fileWritePossibleArbitrator.isFileWritePossible(file)
	}

	@Test
	fun `then file write is possible`() {
		assertThat(fileWriteIsPossible).isTrue
	}
}
