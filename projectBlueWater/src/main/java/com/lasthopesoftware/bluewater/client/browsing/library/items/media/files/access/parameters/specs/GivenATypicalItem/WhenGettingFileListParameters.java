package com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.access.parameters.specs.GivenATypicalItem;

import com.lasthopesoftware.bluewater.client.browsing.library.items.Item;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.access.parameters.FileListParameters;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenGettingFileListParameters {

	private static final String[] expectedFileListParameters = {
		"Browse/Files",
		"ID=48",
		"Version=2"
	};

	private static String[] returnedFileListParameters;

	@BeforeClass
	public static void before() {
		final FileListParameters fileListParameters = FileListParameters.getInstance();
		returnedFileListParameters = fileListParameters.getFileListParameters(new Item(48));
	}

	@Test
	public void thenTheFileListParametersAreCorrect() {
		assertThat(returnedFileListParameters).containsOnly(expectedFileListParameters);
	}
}
