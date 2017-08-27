package com.lasthopesoftware.bluewater.client.playback.file.volume;


import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.state.volume.IVolumeManagement;

import static com.lasthopesoftware.messenger.promises.response.ImmediateAction.perform;

public class PlaybackHandlerVolumeControllerFactory implements IPlaybackHandlerVolumeControllerFactory {

	private final MaxFileVolumeProvider maxFileVolumeProvider;

	public PlaybackHandlerVolumeControllerFactory(MaxFileVolumeProvider maxFileVolumeProvider) {
		this.maxFileVolumeProvider = maxFileVolumeProvider;
	}

	@Override
	public IVolumeManagement manageVolume(PositionedPlaybackFile positionedPlaybackFile, float initialHandlerVolume) {
		final PlaybackHandlerMaxVolumeModifier playbackHandlerMaxVolumeModifier =
			new PlaybackHandlerMaxVolumeModifier(positionedPlaybackFile.getPlaybackHandler(), initialHandlerVolume);

		maxFileVolumeProvider
			.getMaxFileVolume(positionedPlaybackFile.getServiceFile())
			.then(perform(playbackHandlerMaxVolumeModifier::setMaxFileVolume));

		return playbackHandlerMaxVolumeModifier;
	}
}
