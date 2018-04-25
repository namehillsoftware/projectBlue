package com.lasthopesoftware.bluewater.client.playback.file;

import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackFile;
import com.namehillsoftware.handoff.promises.Promise;

import org.joda.time.Duration;

import io.reactivex.Observable;

public class EmptyPlaybackHandler
implements
	IBufferingPlaybackFile,
	PlayableFile,
	PlayingFile {

	private final int duration;

	public EmptyPlaybackHandler(int duration) {
		this.duration = duration;
	}

	@Override
	public Promise<PlayingFile> promisePlayback() {
		return new Promise<>(this);
	}

	@Override
	public void close() {

	}

	@Override
	public Promise<IBufferingPlaybackFile> promiseBufferedPlaybackFile() {
		return new Promise<>(this);
	}

	@Override
	public Observable<Duration> observeProgress(Duration observationPeriod) {
		return Observable.just(Duration.ZERO);
	}

	@Override
	public Promise<PlayableFile> promisePause() {
		return null;
	}

	@Override
	public Duration getDuration() {
		return Duration.millis(duration);
	}
}
