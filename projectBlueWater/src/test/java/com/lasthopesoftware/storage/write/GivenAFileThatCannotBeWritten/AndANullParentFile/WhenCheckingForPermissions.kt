package com.lasthopesoftware.storage.write.GivenAFileThatCannotBeWritten.AndANullParentFile

import com.lasthopesoftware.storage.write.permissions.FileWritePossibleTester
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

class WhenCheckingForPermissions {
	private val fileWriteIsPossible by lazy {
		val fileWritePossibleArbitrator = FileWritePossibleTester
		val file = mockk<File> {
			every { exists() } returns false
			every { canWrite() } returns false
			every { parentFile } returns null
		}
		fileWritePossibleArbitrator.isFileWritePossible(file)
	}

	@Test
	fun `then file write is not possible`() {
		assertThat(fileWriteIsPossible).isFalse
	}
}
