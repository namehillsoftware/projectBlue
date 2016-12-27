package com.lasthopesoftware.bluewater.client.library.items.playlists.playback;

/**
 * Created by david on 12/17/16.
 */

public interface IPlaylistPlayerProducer {
	IPlaylistPlayer getCompletablePlaylistPlayer();

	IPlaylistPlayer getCyclicalPlaylistPlayer();
}
