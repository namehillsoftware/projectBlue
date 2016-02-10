package com.lasthopesoftware.bluewater.servers.library.items.media.files.access;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;

/**
 * Created by david on 11/26/15.
 */
public class SearchFileProvider extends FileProvider {
	private SearchFileProvider(ConnectionProvider connectionProvider, String query) {
		super(connectionProvider, FileListParameters.Options.None, "Files/Search", "Query=[Media Type]=[Audio] " + query);
	}

	public static SearchFileProvider get(ConnectionProvider connectionProvider, String query) {
		return new SearchFileProvider(connectionProvider, query);
	}
}
