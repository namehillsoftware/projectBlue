package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile;
import com.namehillsoftware.handoff.promises.Promise;

public class MaxFileVolumePreparer implements PlayableFilePreparationSource {

	private final PlayableFilePreparationSource playableFilePreparationSource;

	public MaxFileVolumePreparer(PlayableFilePreparationSource playableFilePreparationSource) {
		this.playableFilePreparationSource = playableFilePreparationSource;
	}

	@Override
	public Promise<PreparedPlayableFile> promisePreparedPlaybackFile(ServiceFile serviceFile, long preparedAt) {
		return playableFilePreparationSource
			.promisePreparedPlaybackFile(serviceFile, preparedAt)
			.then(ppf -> new PreparedPlayableFile(
				ppf.getPlaybackHandler(),
				new MaxFileVolumeManager(
					ppf.getPlayableFileVolumeManager(),
					1),
				ppf.getBufferingPlaybackFile()));
	}
}
