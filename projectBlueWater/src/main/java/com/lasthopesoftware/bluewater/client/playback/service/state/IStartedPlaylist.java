package com.lasthopesoftware.bluewater.client.playback.service.state;

import com.lasthopesoftware.promises.Promise;

/**
 * Created by david on 4/9/17.
 */

public interface IStartedPlaylist extends IPlaylistPosition {
	Promise<IPausedPlaylist> pause();
	Promise<IStartedPlaylist> playRepeatedly();
	Promise<IStartedPlaylist> playToCompletion();
	void setVolume(float volume);
}
