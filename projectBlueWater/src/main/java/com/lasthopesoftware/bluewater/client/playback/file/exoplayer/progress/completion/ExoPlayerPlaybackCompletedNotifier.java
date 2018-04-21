package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.progress.completion;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.lasthopesoftware.bluewater.client.playback.file.progress.NotifyFilePlaybackComplete;

public class ExoPlayerPlaybackCompletedNotifier
implements
	NotifyFilePlaybackComplete,
	Player.EventListener {

	private Runnable playbackCompletedAction;

	@Override
	public void playbackCompleted(Runnable runnable) {
		playbackCompletedAction = runnable;
	}

	@Override
	public void onTimelineChanged(Timeline timeline, Object manifest) {

	}

	@Override
	public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

	}

	@Override
	public void onLoadingChanged(boolean isLoading) {

	}

	@Override
	public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
		if (playbackState == Player.STATE_ENDED && playbackCompletedAction != null)
			playbackCompletedAction.run();
	}

	@Override
	public void onRepeatModeChanged(int repeatMode) {

	}

	@Override
	public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

	}

	@Override
	public void onPlayerError(ExoPlaybackException error) {

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
