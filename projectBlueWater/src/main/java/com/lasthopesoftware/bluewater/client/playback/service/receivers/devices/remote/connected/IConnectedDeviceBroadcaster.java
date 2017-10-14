package com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.connected;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;

public interface IConnectedDeviceBroadcaster {
	void setPlaying();
	void setPaused();
	void setStopped();
	void updateNowPlaying(ServiceFile serviceFile);
	void updateTrackPosition(long trackPosition);
}
