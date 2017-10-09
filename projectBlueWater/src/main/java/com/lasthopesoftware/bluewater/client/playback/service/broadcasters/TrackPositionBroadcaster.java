package com.lasthopesoftware.bluewater.client.playback.service.broadcasters;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.client.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.namehillsoftware.lazyj.ILazy;
import com.namehillsoftware.lazyj.Lazy;

import io.reactivex.functions.Consumer;

public class TrackPositionBroadcaster implements Consumer<Long> {
	private ILazy<LocalBroadcastManager> lazyLocalBroadcastManager;
	private final IPlaybackHandler playbackHandler;

	public TrackPositionBroadcaster(Context context, IPlaybackHandler playbackHandler) {
		lazyLocalBroadcastManager = new Lazy<>(() -> LocalBroadcastManager.getInstance(context));
		this.playbackHandler = playbackHandler;
	}

	@Override
	public void accept(Long newPosition) throws Exception {
		final Intent trackPositionChangedIntent = new Intent(trackPositionUpdate);
		trackPositionChangedIntent.putExtra(TrackPositionChangedParameters.filePosition, newPosition);
		trackPositionChangedIntent.putExtra(TrackPositionChangedParameters.fileDuration, playbackHandler.getDuration());

		lazyLocalBroadcastManager.getObject().sendBroadcast(trackPositionChangedIntent);
	}

	private static final MagicPropertyBuilder magicPropertyBuilder = new MagicPropertyBuilder(TrackPositionBroadcaster.class);

	public static final String trackPositionUpdate = magicPropertyBuilder.buildProperty("trackPositionUpdate");

	public static class TrackPositionChangedParameters {
		private static final MagicPropertyBuilder magicPropertyBuilder = new MagicPropertyBuilder(TrackPositionChangedParameters.class);

		public static final String filePosition = magicPropertyBuilder.buildProperty("filePosition");
		public static final String fileDuration = magicPropertyBuilder.buildProperty("fileDuration");
	}
}
