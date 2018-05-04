package com.lasthopesoftware.bluewater.client.playback.file;

import com.lasthopesoftware.bluewater.client.playback.file.progress.ReadFileDuration;
import com.lasthopesoftware.bluewater.client.playback.file.progress.ReadFileProgress;
import com.namehillsoftware.handoff.promises.Promise;

public interface PlayingFile extends ReadFileProgress, ReadFileDuration {
	Promise<PlayableFile> promisePause();
}
