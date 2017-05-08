package com.lasthopesoftware.bluewater.client.playback.file.volume.specs.fakes;

import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.volume.IPlaybackHandlerVolumeControllerFactory;
import com.lasthopesoftware.bluewater.client.playback.state.volume.IVolumeManagement;
import com.lasthopesoftware.promises.Promise;


public class FakeVolumeControllerFactory implements IPlaybackHandlerVolumeControllerFactory {
	@Override
	public IVolumeManagement manageVolume(PositionedPlaybackFile positionedPlaybackFile) {
		return new FakeVolumeManager(positionedPlaybackFile);
	}

	private static class FakeVolumeManager implements IVolumeManagement {

		private final PositionedPlaybackFile positionedPlaybackFile;

		FakeVolumeManager(PositionedPlaybackFile positionedPlaybackFile) {
			this.positionedPlaybackFile = positionedPlaybackFile;
		}

		@Override
		public float setVolume(float volume) {
			positionedPlaybackFile.getPlaybackHandler().setVolume(volume);
			return positionedPlaybackFile.getPlaybackHandler().getVolume();
		}
	}
}
