package com.lasthopesoftware.bluewater.client.playback.service.receivers;

import android.content.Context;
import android.support.v4.media.session.MediaSessionCompat;

import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;

public class MediaSessionCallbackReceiver extends MediaSessionCompat.Callback {
	private final Context context;

	public MediaSessionCallbackReceiver(Context context) {
		this.context = context;
	}

	@Override
	public void onPlay() {
		PlaybackService.play(context);
	}

	@Override
	public void onStop() {
		PlaybackService.pause(context);
	}

	@Override
	public void onPause() {
		PlaybackService.pause(context);
	}

	@Override
	public void onSkipToNext() {
		PlaybackService.next(context);
	}

	@Override
	public void onSkipToPrevious() {
		PlaybackService.previous(context);
	}
}
