package com.lasthopesoftware.bluewater.client.playback.file;

import org.joda.time.Period;

import io.reactivex.Observable;

public interface PlayingFile {
	Observable<PlayingFileProgress> observeProgress(Period observationPeriod);
}
