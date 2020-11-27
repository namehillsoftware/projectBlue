package com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.GivenATypicalPlaylist;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.Playlist;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenGettingFileListParameters {

	private static final String[] expectedFileListParameters = {
		"Playlist/Files",
		"Playlist=57",
	};

	private static String[] returnedFileListParameters;

	@BeforeClass
	public static void before() {
		final FileListParameters fileListParameters = FileListParameters.getInstance();
		returnedFileListParameters = fileListParameters.getFileListParameters(new Playlist(57));
	}

	@Test
	public void thenTheFileListParametersAreCorrect() {
		assertThat(returnedFileListParameters).containsOnly(expectedFileListParameters);
	}
}
