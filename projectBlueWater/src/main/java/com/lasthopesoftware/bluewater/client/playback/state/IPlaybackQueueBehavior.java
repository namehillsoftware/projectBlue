package com.lasthopesoftware.bluewater.client.playback.state;


import com.lasthopesoftware.promises.Promise;

public interface IPlaybackQueueBehavior {
	Promise<IStartedPlaylist> playRepeatedly();
	Promise<IStartedPlaylist> playToCompletion();
}
