package com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.GivenATypicalPlaylist

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.Playlist
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WhenGettingFileListParameters {

	companion object {
		private val expectedFileListParameters = arrayOf(
			"Playlist/Files",
			"Playlist=57"
		)
		private val returnedFileListParameters by lazy { FileListParameters.getFileListParameters(Playlist(57)) }
	}

    @Test
    fun thenTheFileListParametersAreCorrect() {
        assertThat(returnedFileListParameters).containsOnly(*expectedFileListParameters)
    }
}
