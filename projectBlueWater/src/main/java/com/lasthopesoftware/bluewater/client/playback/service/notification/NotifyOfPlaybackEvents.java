package com.lasthopesoftware.bluewater.client.playback.service.notification;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;

public interface NotifyOfPlaybackEvents {
	void notifyPlaying();
	void notifyPaused();
	void notifyStopped();
	void notifyPlayingFileChanged(ServiceFile serviceFile);
}
