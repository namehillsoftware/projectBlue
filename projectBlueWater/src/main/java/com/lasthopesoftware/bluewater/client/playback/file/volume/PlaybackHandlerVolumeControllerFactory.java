package com.lasthopesoftware.bluewater.client.playback.file.volume;


import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.state.volume.IVolumeManagement;
import com.lasthopesoftware.promises.Promise;

public class PlaybackHandlerVolumeControllerFactory implements IPlaybackHandlerVolumeControllerFactory {

	private final MaxFileVolumeProvider maxFileVolumeProvider;

	public PlaybackHandlerVolumeControllerFactory(MaxFileVolumeProvider maxFileVolumeProvider) {
		this.maxFileVolumeProvider = maxFileVolumeProvider;
	}

	@Override
	public Promise<IVolumeManagement> manageVolume(PositionedPlaybackFile positionedPlaybackFile) {
		return
			maxFileVolumeProvider
				.getMaxFileVolume(positionedPlaybackFile.getServiceFile())
				.then(maxFileVolume -> new PlaybackFileVolumeController(positionedPlaybackFile.getPlaybackHandler(), maxFileVolume));
	}
}
