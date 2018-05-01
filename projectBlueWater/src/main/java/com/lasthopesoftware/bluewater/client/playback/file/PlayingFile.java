package com.lasthopesoftware.bluewater.client.playback.file;

import com.namehillsoftware.handoff.promises.Promise;

import org.joda.time.Duration;

import io.reactivex.Observable;

public interface PlayingFile {
	Observable<Duration> observeProgress(Duration observationPeriod);

	Promise<PlayableFile> promisePause();

	Duration getDuration();
}
