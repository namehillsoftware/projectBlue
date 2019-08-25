package com.lasthopesoftware.bluewater.client.playback.service.broadcasters;

import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import io.reactivex.functions.Consumer;
import org.joda.time.Duration;

public class TrackPositionBroadcaster implements Consumer<Duration> {
	private final LocalBroadcastManager localBroadcastManager;
	private final PlayingFile playingFile;

	public TrackPositionBroadcaster(LocalBroadcastManager localBroadcastManager, PlayingFile playingFile) {
		this.localBroadcastManager = localBroadcastManager;
		this.playingFile = playingFile;
	}

	@Override
	public void accept(Duration fileProgress) {
		final Intent trackPositionChangedIntent = new Intent(trackPositionUpdate);
		trackPositionChangedIntent.putExtra(TrackPositionChangedParameters.filePosition, fileProgress.getMillis());
		trackPositionChangedIntent.putExtra(TrackPositionChangedParameters.fileDuration, playingFile.getDuration().getMillis());

		localBroadcastManager.sendBroadcast(trackPositionChangedIntent);
	}

	private static final MagicPropertyBuilder magicPropertyBuilder = new MagicPropertyBuilder(TrackPositionBroadcaster.class);

	public static final String trackPositionUpdate = magicPropertyBuilder.buildProperty("trackPositionUpdate");

	public static class TrackPositionChangedParameters {
		private static final MagicPropertyBuilder magicPropertyBuilder = new MagicPropertyBuilder(TrackPositionChangedParameters.class);

		public static final String filePosition = magicPropertyBuilder.buildProperty("filePosition");
		public static final String fileDuration = magicPropertyBuilder.buildProperty("fileDuration");
	}
}
