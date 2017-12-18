package com.lasthopesoftware.bluewater.client.playback.engine;

import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlaybackCompleted;
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlaybackStarted;
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlayingFileChanged;
import com.vedsoft.futures.runnables.OneParameterAction;

public interface IPlaylistStateBroadcaster {
	IPlaylistStateBroadcaster setOnPlayingFileChanged(OnPlayingFileChanged onPlayingFileChanged);

	IPlaylistStateBroadcaster setOnPlaylistError(OneParameterAction<Throwable> onPlaylistError);

	IPlaylistStateBroadcaster setOnPlaybackStarted(OnPlaybackStarted onPlaybackStarted);

	IPlaylistStateBroadcaster setOnPlaybackCompleted(OnPlaybackCompleted onPlaybackCompleted);
}
