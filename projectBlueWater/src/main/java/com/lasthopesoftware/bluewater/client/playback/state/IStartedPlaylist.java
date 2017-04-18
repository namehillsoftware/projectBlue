package com.lasthopesoftware.bluewater.client.playback.state;

import com.lasthopesoftware.promises.Promise;

/**
 * Created by david on 4/9/17.
 */

public interface IStartedPlaylist extends IPlaylistPosition {
	Promise<IPausedPlaylist> pause();
	void setVolume(float volume);
}
