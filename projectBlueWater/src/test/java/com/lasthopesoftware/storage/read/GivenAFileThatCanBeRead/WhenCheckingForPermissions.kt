package com.lasthopesoftware.storage.read.GivenAFileThatCanBeRead

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
			every { exists() } returns true
			every { canRead() } returns true
		}

		fileReadPossibleArbitrator.isFileReadPossible(file)
	}

	@Test
	fun `then file read is possible`() {
		assertThat(fileReadIsPossible).isTrue
	}
}
