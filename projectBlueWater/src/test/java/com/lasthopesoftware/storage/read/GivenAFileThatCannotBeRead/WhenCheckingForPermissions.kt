package com.lasthopesoftware.storage.read.GivenAFileThatCannotBeRead

import com.lasthopesoftware.storage.read.permissions.FileReadPossibleArbitrator
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

class WhenCheckingForPermissions {
	private val fileReadIsPossible by lazy {
		val fileReadPossibleArbitrator = FileReadPossibleArbitrator()
		val file = mockk<File> {
			every { exists() } returns false
			every { canRead() } returns false
			every { parentFile } returns null
		}
		fileReadPossibleArbitrator.isFileReadPossible(file)
	}

	@Test
	fun `then file read is not possible`() {
		assertThat(fileReadIsPossible).isFalse
	}
}
