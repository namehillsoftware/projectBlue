package com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.exoplayer;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.lasthopesoftware.bluewater.client.playback.engine.IActivePlayer;
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.IStartPlayback;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.ActiveExoPlaylistPlayer;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue;

import java.io.Closeable;

public class ExoPlayerPlaybackBootstrapper implements IStartPlayback, Closeable {
	private final Renderer[] renderers;
	private final TrackSelector trackSelector;
	private final LoadControl loadControl;

	private ExoPlayer exoPlayer;

	public ExoPlayerPlaybackBootstrapper(Renderer[] renderers, TrackSelector trackSelector, LoadControl loadControl) {
		this.renderers = renderers;
		this.trackSelector = trackSelector;
		this.loadControl = loadControl;
	}

	@Override
	public synchronized IActivePlayer startPlayback(PreparedPlayableFileQueue preparedPlaybackQueue, long filePosition) {
		close();

		exoPlayer = ExoPlayerFactory.newInstance(
			renderers,
			trackSelector,
			loadControl);

		return new ActiveExoPlaylistPlayer(exoPlayer);
	}

	@Override
	public void close() {
		if (exoPlayer != null)
			exoPlayer.release();
	}
}
