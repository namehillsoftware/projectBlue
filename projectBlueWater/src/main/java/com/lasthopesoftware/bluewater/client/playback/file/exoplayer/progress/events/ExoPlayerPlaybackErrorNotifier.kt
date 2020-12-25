package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.progress.events;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.error.ExoPlayerException;
import com.lasthopesoftware.bluewater.client.playback.file.progress.NotifyFilePlaybackError;
import com.vedsoft.futures.runnables.OneParameterAction;

public class ExoPlayerPlaybackErrorNotifier
implements
	NotifyFilePlaybackError<ExoPlayerException>,
	Player.EventListener {

	private final ExoPlayerPlaybackHandler handler;
	private OneParameterAction<ExoPlayerException> errorAction;

	public ExoPlayerPlaybackErrorNotifier(ExoPlayerPlaybackHandler handler) {
		this.handler = handler;
	}

	@Override
	public void playbackError(OneParameterAction<ExoPlayerException> onError) {
		errorAction = onError;
	}

	@Override
	public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {
	}

	@Override
	public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

	}

	@Override
	public void onLoadingChanged(boolean isLoading) {

	}

	@Override
	public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

	}

	@Override
	public void onRepeatModeChanged(int repeatMode) {

	}

	@Override
	public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

	}

	@Override
	public void onPlayerError(ExoPlaybackException error) {
		if (errorAction != null)
			errorAction.runWith(new ExoPlayerException(handler, error));
	}

	@Override
	public void onPositionDiscontinuity(int reason) {

	}

	@Override
	public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

	}

	@Override
	public void onSeekProcessed() {

	}
}
