package com.lasthopesoftware.bluewater.client.playback.engine;


import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile;
import com.namehillsoftware.handoff.promises.Promise;

import io.reactivex.observables.ConnectableObservable;

public interface IActivePlayer {
	Promise<Void> pause();
	Promise<Void> resume();
	ConnectableObservable<PositionedPlayingFile> observe();
}
