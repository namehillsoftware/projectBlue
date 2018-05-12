package com.lasthopesoftware.bluewater.client.playback.file.progress;

import com.lasthopesoftware.bluewater.client.playback.file.PlayedFile;
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise;

import org.joda.time.Duration;

public class PromisedPlayedFile<Error extends Exception>
extends
	ProgressingPromise<Duration, PlayedFile>
implements
	PlayedFile {

	private final ReadFileProgress fileProgressReader;

	public PromisedPlayedFile(
		ReadFileProgress fileProgressReader,
		NotifyFilePlaybackComplete notifyFilePlaybackComplete,
		NotifyFilePlaybackError<Error> notifyPlaybackError) {

		this.fileProgressReader = fileProgressReader;
		notifyFilePlaybackComplete.playbackCompleted(this::whenPlaybackCompleted);
		notifyPlaybackError.playbackError(this::emitError);
	}

	private void emitError(Throwable error) {
		reject(error);
	}

	private void whenPlaybackCompleted() {
		resolve(this);
	}

	@Override
	public Duration getProgress() {
		return fileProgressReader.getProgress();
	}
}
