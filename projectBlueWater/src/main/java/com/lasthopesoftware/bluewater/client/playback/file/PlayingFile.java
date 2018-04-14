package com.lasthopesoftware.bluewater.client.playback.file;

import com.lasthopesoftware.bluewater.client.playback.file.progress.FileProgress;

import org.joda.time.Duration;

import io.reactivex.Observable;

public interface PlayingFile {
	Observable<FileProgress> observeProgress(Duration observationPeriod);
}
