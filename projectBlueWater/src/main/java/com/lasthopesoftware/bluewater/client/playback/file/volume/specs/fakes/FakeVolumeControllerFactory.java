package com.lasthopesoftware.bluewater.client.playback.file.volume.specs.fakes;

import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.volume.IPlaybackHandlerVolumeControllerFactory;
import com.lasthopesoftware.bluewater.client.playback.state.volume.IVolumeManagement;


public class FakeVolumeControllerFactory implements IPlaybackHandlerVolumeControllerFactory {
	@Override
	public IVolumeManagement manageVolume(PositionedPlaybackFile positionedPlaybackFile, float initialVolume) {
		positionedPlaybackFile.getPlayableFile().setVolume(initialVolume);
		return new FakeVolumeManager(positionedPlaybackFile);
	}

	private static class FakeVolumeManager implements IVolumeManagement {

		private final PositionedPlaybackFile positionedPlaybackFile;

		FakeVolumeManager(PositionedPlaybackFile positionedPlaybackFile) {
			this.positionedPlaybackFile = positionedPlaybackFile;
		}

		@Override
		public float setVolume(float volume) {
			positionedPlaybackFile.getPlayableFile().setVolume(volume);
			return positionedPlaybackFile.getPlayableFile().getVolume();
		}
	}
}
