package com.lasthopesoftware.bluewater.client.playback.file;

import com.lasthopesoftware.bluewater.client.playback.file.progress.ReadFileDuration;
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.joda.time.Duration;

public interface PlayingFile extends ReadFileDuration {
	Promise<PlayableFile> promisePause();

	ProgressingPromise<Duration, PlayedFile> promisePlayedFile();
}
