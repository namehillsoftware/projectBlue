package com.lasthopesoftware.bluewater.client.playback.state;


import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.state.volume.IVolumeManagement;

import io.reactivex.observables.ConnectableObservable;

public interface IActivePlayer {
	void pause();
	void resume();
	ConnectableObservable<PositionedPlaybackFile> observe();
	IVolumeManagement manageVolume();
}
