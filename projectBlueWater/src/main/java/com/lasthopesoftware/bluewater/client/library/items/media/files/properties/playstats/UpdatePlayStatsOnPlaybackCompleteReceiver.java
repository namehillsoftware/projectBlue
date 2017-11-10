package com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.factory.PlaystatsUpdateSelector;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.namehillsoftware.handoff.promises.response.ImmediateAction.perform;

public class UpdatePlayStatsOnPlaybackCompleteReceiver extends BroadcastReceiver {
	private static final Logger logger = LoggerFactory.getLogger(UpdatePlayStatsOnPlaybackCompleteReceiver.class);

	private final PlaystatsUpdateSelector playstatsUpdateSelector;

	public UpdatePlayStatsOnPlaybackCompleteReceiver(PlaystatsUpdateSelector playstatsUpdateSelector) {
		this.playstatsUpdateSelector = playstatsUpdateSelector;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		final int fileKey = intent.getIntExtra(PlaylistEvents.PlaybackFileParameters.fileKey, -1);
		if (fileKey < 0) return;

		playstatsUpdateSelector
			.promisePlaystatsUpdater()
			.eventually(updater -> updater.promisePlaystatsUpdate(new ServiceFile(fileKey)))
			.excuse(perform(e -> logger.error("There was an error updating the playstats for the file with key " + fileKey)));
	}
}
