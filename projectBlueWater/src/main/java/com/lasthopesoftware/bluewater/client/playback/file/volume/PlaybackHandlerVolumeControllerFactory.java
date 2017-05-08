package com.lasthopesoftware.bluewater.client.playback.file.volume;


import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.state.volume.IVolumeManagement;
import com.lasthopesoftware.promises.Promise;

import static com.vedsoft.futures.callables.VoidFunc.runCarelessly;

public class PlaybackHandlerVolumeControllerFactory implements IPlaybackHandlerVolumeControllerFactory {

	private final MaxFileVolumeProvider maxFileVolumeProvider;

	public PlaybackHandlerVolumeControllerFactory(MaxFileVolumeProvider maxFileVolumeProvider) {
		this.maxFileVolumeProvider = maxFileVolumeProvider;
	}

	@Override
	public IVolumeManagement manageVolume(PositionedPlaybackFile positionedPlaybackFile) {
		final PlaybackHandlerMaxVolumeModifier playbackHandlerMaxVolumeModifier =
			new PlaybackHandlerMaxVolumeModifier(positionedPlaybackFile.getPlaybackHandler());

		maxFileVolumeProvider
			.getMaxFileVolume(positionedPlaybackFile.getServiceFile())
			.then(runCarelessly(playbackHandlerMaxVolumeModifier::setMaxFileVolume));

		return playbackHandlerMaxVolumeModifier;
	}
}
