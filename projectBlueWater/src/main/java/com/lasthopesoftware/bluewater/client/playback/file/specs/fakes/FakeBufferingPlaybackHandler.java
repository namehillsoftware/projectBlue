package com.lasthopesoftware.bluewater.client.playback.file.specs.fakes;

import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackFile;
import com.namehillsoftware.handoff.promises.Promise;

import org.joda.time.Duration;

import io.reactivex.Observable;

public class FakeBufferingPlaybackHandler
implements
	IBufferingPlaybackFile,
	PlayableFile,
	PlayingFile
{
	private boolean isPlaying;
	private int currentPosition;

	public boolean isPlaying() {
		return isPlaying;
	}

	public void setCurrentPosition(int position) {
		this.currentPosition = position;
	}

	@Override
	public Promise<PlayingFile> promisePlayback() {
		isPlaying = true;
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
		return Observable.just(Duration.millis(currentPosition));
	}

	@Override
	public Promise<PlayableFile> promisePause() {
		isPlaying = false;
		return new Promise<>(this);
	}

	@Override
	public Duration getDuration() {
		return Duration.ZERO;
	}
}
