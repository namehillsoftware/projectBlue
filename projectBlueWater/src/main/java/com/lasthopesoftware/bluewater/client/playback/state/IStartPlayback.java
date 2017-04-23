package com.lasthopesoftware.bluewater.client.playback.state;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.queues.PreparedPlaybackQueue;
import com.lasthopesoftware.bluewater.client.playback.state.volume.IVolumeManagement;

import java.io.IOException;

import io.reactivex.observables.ConnectableObservable;

/**
 * Created by david on 4/9/17.
 */

public interface IStartPlayback {
	ConnectableObservable<PositionedPlaybackFile> startPlayback(PreparedPlaybackQueue preparedPlaybackQueue, final int filePosition) throws IOException;
	IVolumeManagement manageVolume();
}
