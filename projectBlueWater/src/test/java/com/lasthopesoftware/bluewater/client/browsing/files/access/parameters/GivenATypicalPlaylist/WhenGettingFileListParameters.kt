package com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.GivenATypicalPlaylist

import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenGettingFileListParameters {

	private val expectedFileListParameters = arrayOf(
		"Playlist/Files",
		"Playlist=57"
	)
	private val returnedFileListParameters by lazy { FileListParameters.getFileListParameters(PlaylistId(57)) }

    @Test
    fun thenTheFileListParametersAreCorrect() {
        assertThat(returnedFileListParameters).containsOnly(*expectedFileListParameters)
    }
}
