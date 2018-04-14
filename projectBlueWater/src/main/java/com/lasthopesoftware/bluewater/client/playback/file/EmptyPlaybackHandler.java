package com.lasthopesoftware.bluewater.client.playback.file;

import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.progress.FileProgress;
import com.namehillsoftware.handoff.promises.Promise;

import org.joda.time.Duration;

import io.reactivex.Observable;

public class EmptyPlaybackHandler
implements IBufferingPlaybackFile, PlayableFile {

	private final int duration;

	public EmptyPlaybackHandler(int duration) {
		this.duration = duration;
	}

	@Override
	public boolean isPlaying() {
		return false;
	}

	@Override
	public void pause() {

	}

	@Override
	public Promise<PlayableFile> promisePlayback() {
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
	public Observable<FileProgress> observeProgress(Duration observationPeriod) {
		return Observable.just(new FileProgress(0, duration));
	}
}
