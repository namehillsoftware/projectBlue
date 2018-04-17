package com.lasthopesoftware.bluewater.client.playback.service.broadcasters;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import org.joda.time.Duration;

import io.reactivex.functions.Consumer;

public class TrackPositionBroadcaster implements Consumer<Duration> {
	private CreateAndHold<LocalBroadcastManager> lazyLocalBroadcastManager;
	private final PlayingFile playingFile;

	public TrackPositionBroadcaster(Context context, PlayingFile playingFile) {
		lazyLocalBroadcastManager = new Lazy<>(() -> LocalBroadcastManager.getInstance(context));
		this.playingFile = playingFile;
	}

	@Override
	public void accept(Duration fileProgress) {
		final Intent trackPositionChangedIntent = new Intent(trackPositionUpdate);
		trackPositionChangedIntent.putExtra(TrackPositionChangedParameters.filePosition, fileProgress.getMillis());
		trackPositionChangedIntent.putExtra(TrackPositionChangedParameters.fileDuration, playingFile.getDuration());

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
