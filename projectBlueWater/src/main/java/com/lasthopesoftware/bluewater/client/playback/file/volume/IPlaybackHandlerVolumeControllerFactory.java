package com.lasthopesoftware.bluewater.client.playback.file.volume;


import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.volume.IVolumeManagement;

public interface IPlaybackHandlerVolumeControllerFactory {
	IVolumeManagement manageVolume(PositionedPlayableFile positionedPlayableFile, float initialHandlerVolume);
}
