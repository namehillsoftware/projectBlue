package com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote;

import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.ServiceFile;

public interface IRemoteBroadcaster {
	void setPlaying();
	void setPaused();
	void setStopped();
	void updateNowPlaying(ServiceFile serviceFile);
	void updateTrackPosition(long trackPosition);
}
