package com.lasthopesoftware.bluewater.client.playback.file;

import org.joda.time.Duration;

import io.reactivex.Observable;

public interface PlayingFile {
	Observable<PlayingFileProgress> observeProgress(Duration observationPeriod);
}
