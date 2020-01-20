package com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.access.parameters;

public class SearchFileParameterProvider {

	public static String[] getFileListParameters(String query) {
		return new String[] { "Files/Search", "Query=[Media Type]=[Audio] " + query };
	}
}
