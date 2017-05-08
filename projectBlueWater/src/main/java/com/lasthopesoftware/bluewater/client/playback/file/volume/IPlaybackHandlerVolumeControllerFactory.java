package com.lasthopesoftware.bluewater.client.playback.file.volume;


import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.state.volume.IVolumeManagement;
import com.lasthopesoftware.promises.Promise;

public interface IPlaybackHandlerVolumeControllerFactory {
	Promise<IVolumeManagement> manageVolume(PositionedPlaybackFile positionedPlaybackFile);
}
