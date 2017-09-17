package com.lasthopesoftware.bluewater.client.playback.state;

import com.lasthopesoftware.bluewater.client.playback.state.events.OnPlayingFileChanged;
import com.lasthopesoftware.bluewater.client.playback.state.events.OnPlaylistStarted;
import com.vedsoft.futures.runnables.OneParameterAction;

public interface IPlaylistStateBroadcaster {
	IPlaylistStateBroadcaster setOnPlayingFileChanged(OnPlayingFileChanged onPlayingFileChanged);

	IPlaylistStateBroadcaster setOnPlaylistError(OneParameterAction<Throwable> onPlaylistError);

	IPlaylistStateBroadcaster setOnPlaylistStarted(OnPlaylistStarted onPlaylistStarted);
}
