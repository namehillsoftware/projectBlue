package com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.GivenATypicalItem

import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WhenGettingFileListParameters {
    companion object {
        private val expectedFileListParameters = arrayOf(
            "Browse/Files",
            "ID=48",
            "Version=2"
        )
        private val returnedFileListParameters by lazy { FileListParameters.getFileListParameters(ItemId(48)) }
    }

	@Test
	fun thenTheFileListParametersAreCorrect() {
		assertThat(returnedFileListParameters).containsOnly(*expectedFileListParameters)
	}
}
