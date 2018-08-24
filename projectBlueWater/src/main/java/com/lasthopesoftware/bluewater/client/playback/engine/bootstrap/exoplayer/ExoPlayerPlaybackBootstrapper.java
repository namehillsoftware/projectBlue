package com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.exoplayer;

import com.lasthopesoftware.bluewater.client.playback.engine.IActivePlayer;
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.IStartPlayback;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.ActiveExoPlaylistPlayer;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue;

public class ExoPlayerPlaybackBootstrapper implements IStartPlayback {
	@Override
	public IActivePlayer startPlayback(PreparedPlayableFileQueue preparedPlaybackQueue, long filePosition) {
		return new ActiveExoPlaylistPlayer();
	}
}
