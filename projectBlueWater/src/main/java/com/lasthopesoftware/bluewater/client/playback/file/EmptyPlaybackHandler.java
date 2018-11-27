package com.lasthopesoftware.bluewater.client.playback.file;

import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackFile;
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise;
import com.namehillsoftware.handoff.promises.Promise;
import org.joda.time.Duration;

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
	public Duration getProgress() {
		return Duration.ZERO;
	}

	@Override
	public Promise<PlayableFile> promisePause() {
		return null;
	}

	@Override
	public ProgressingPromise<Duration, PlayedFile> promisePlayedFile() {
		return new ProgressingPromise<Duration, PlayedFile>() {
			{
				resolve(null);
			}

			@Override
			public Duration getProgress() {
				return Duration.millis(duration);
			}
		};
	}

	@Override
	public Duration getDuration() {
		return Duration.millis(duration);
	}
}
