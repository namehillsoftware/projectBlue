package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.specs;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.volume.ManagePlayableFileVolume;
import com.namehillsoftware.handoff.promises.Promise;

import static org.mockito.Mockito.mock;

public class FakeFilePreparer implements PlayableFilePreparationSource {

	private final PlayableFile playableFile;
	private final IBufferingPlaybackFile bufferingPlaybackFile;

	public FakeFilePreparer(PlayableFile playableFile, IBufferingPlaybackFile bufferingPlaybackFile) {
		this.playableFile = playableFile;
		this.bufferingPlaybackFile = bufferingPlaybackFile;
	}

	@Override
	public Promise<PreparedPlayableFile> promisePreparedPlaybackFile(ServiceFile serviceFile, long preparedAt) {
		return new Promise<>(
			new PreparedPlayableFile(
				playableFile,
				mock(ManagePlayableFileVolume.class),
				bufferingPlaybackFile));
	}
}
