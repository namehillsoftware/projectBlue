package com.lasthopesoftware.bluewater.client.playback.file.volume;


import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.state.volume.IVolumeManagement;

public interface IPlaybackHandlerVolumeControllerFactory {
	IVolumeManagement manageVolume(PositionedPlaybackFile positionedPlaybackFile, float initialHandlerVolume);
}
