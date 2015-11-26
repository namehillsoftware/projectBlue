package com.lasthopesoftware.bluewater.servers.library.items.media.files.access;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.Playlist;

/**
 * Created by david on 11/25/15.
 */
public class PlaylistFileProvider extends AbstractFileProvider {
	public PlaylistFileProvider(ConnectionProvider connectionProvider, Playlist playlist) {
		this(connectionProvider, playlist, -1);
	}

	public PlaylistFileProvider(ConnectionProvider connectionProvider, Playlist playlist, int option) {
		super(connectionProvider, option, "Playlist/Files", "Playlist=" + String.valueOf(playlist.getKey()));
	}
}
