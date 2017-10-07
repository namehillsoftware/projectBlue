package com.lasthopesoftware.bluewater.client.playback.file.initialization;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;

import java.io.IOException;

public class ExoPlayerInitialization implements IPlaybackInitialization<ExoPlayer> {
	private final Context context;

	public ExoPlayerInitialization(Context context) {
		this.context = context;
	}

	@Override
	public ExoPlayer initializeMediaPlayer(Uri fileUri) throws IOException {
		DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();

		TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
		DefaultTrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

		return ExoPlayerFactory.newSimpleInstance(context, trackSelector);
	}
}
