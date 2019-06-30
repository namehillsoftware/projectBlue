package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.volume.ProvideMaxFileVolume;
import com.namehillsoftware.handoff.promises.Promise;

public class MaxFileVolumePreparer implements PlayableFilePreparationSource {

	private final PlayableFilePreparationSource playableFilePreparationSource;
	private final ProvideMaxFileVolume provideMaxFileVolume;

	public MaxFileVolumePreparer(PlayableFilePreparationSource playableFilePreparationSource, ProvideMaxFileVolume provideMaxFileVolume) {
		this.playableFilePreparationSource = playableFilePreparationSource;
		this.provideMaxFileVolume = provideMaxFileVolume;
	}

	@Override
	public Promise<PreparedPlayableFile> promisePreparedPlaybackFile(ServiceFile serviceFile, long preparedAt) {
		return playableFilePreparationSource
			.promisePreparedPlaybackFile(serviceFile, preparedAt)
			.then(ppf -> new PreparedPlayableFile(
				ppf.getPlaybackHandler(),
				new MaxFileVolumeManager(
					ppf.getPlayableFileVolumeManager()),
				ppf.getBufferingPlaybackFile()));
	}
}
