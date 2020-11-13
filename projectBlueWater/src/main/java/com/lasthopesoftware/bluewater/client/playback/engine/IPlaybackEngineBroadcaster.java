package com.lasthopesoftware.bluewater.client.playback.engine;

import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlaybackCompleted;
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlaybackStarted;
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlayingFileChanged;
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlaylistError;
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlaylistReset;

public interface IPlaybackEngineBroadcaster {
	IPlaybackEngineBroadcaster setOnPlayingFileChanged(OnPlayingFileChanged onPlayingFileChanged);

	IPlaybackEngineBroadcaster setOnPlaylistError(OnPlaylistError onPlaylistError);

	IPlaybackEngineBroadcaster setOnPlaybackStarted(OnPlaybackStarted onPlaybackStarted);

	IPlaybackEngineBroadcaster setOnPlaybackCompleted(OnPlaybackCompleted onPlaybackCompleted);

	IPlaybackEngineBroadcaster setOnPlaylistReset(OnPlaylistReset onPlaylistReset);
}
